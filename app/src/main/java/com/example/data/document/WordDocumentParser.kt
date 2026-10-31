package com.example.data.document

import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

data class WordParagraph(
    val text: String,
    val isHeading: Boolean = false,
    val headingLevel: Int = 0,
    val isBullet: Boolean = false
)

data class WordTable(
    val headers: List<String>,
    val rows: List<List<String>>
)

data class WordDocumentModel(
    val title: String,
    val paragraphs: List<WordParagraph>,
    val tables: List<WordTable>,
    val wordCount: Int,
    val readingTimeMinutes: Int
)

object WordDocumentParser {

    private const val TAG = "WordDocumentParser"

    fun parse(file: File, fallbackTitle: String): WordDocumentModel {
        return if (file.name.endsWith(".docx", ignoreCase = true)) {
            parseDocx(file, fallbackTitle)
        } else {
            parseDocBinary(file, fallbackTitle)
        }
    }

    private fun parseDocx(file: File, fallbackTitle: String): WordDocumentModel {
        val paragraphs = mutableListOf<WordParagraph>()
        val tables = mutableListOf<WordTable>()
        var docTitle = fallbackTitle.removeSuffix(".docx").removeSuffix(".DOCX")

        try {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("word/document.xml")
                    ?: zip.entries().asSequence().find { it.name.endsWith("document.xml") }
                if (entry != null) {
                    zip.getInputStream(entry).use { stream ->
                        val (parsedParas, parsedTables) = parseDocumentXml(stream)
                        paragraphs.addAll(parsedParas)
                        tables.addAll(parsedTables)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unzipping docx: ${e.message}", e)
            paragraphs.add(
                WordParagraph(
                    text = "Unable to fully parse this Word document structure: ${e.localizedMessage}",
                    isHeading = false
                )
            )
        }

        if (paragraphs.isNotEmpty()) {
            val firstHeading = paragraphs.firstOrNull { it.isHeading }
            if (firstHeading != null && firstHeading.text.isNotBlank()) {
                docTitle = firstHeading.text
            }
        }

        val totalWords = paragraphs.sumOf { p ->
            p.text.split("\\s+".toRegex()).count { it.isNotBlank() }
        } + tables.sumOf { t ->
            t.headers.sumOf { it.split("\\s+".toRegex()).count { w -> w.isNotBlank() } } +
                    t.rows.sumOf { row -> row.sumOf { c -> c.split("\\s+".toRegex()).count { w -> w.isNotBlank() } } }
        }

        return WordDocumentModel(
            title = docTitle,
            paragraphs = paragraphs,
            tables = tables,
            wordCount = totalWords,
            readingTimeMinutes = (totalWords / 200).coerceAtLeast(1)
        )
    }

    private fun parseDocumentXml(inputStream: InputStream): Pair<List<WordParagraph>, List<WordTable>> {
        val paragraphs = mutableListOf<WordParagraph>()
        val tables = mutableListOf<WordTable>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var inParagraph = false
            var inTable = false
            var inTableRow = false
            var inTableCell = false

            var currentParagraphText = StringBuilder()
            var currentHeadingLevel = 0
            var isCurrentBullet = false

            var currentTableRows = mutableListOf<MutableList<String>>()
            var currentRowCells = mutableListOf<String>()
            var currentCellText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name ?: ""

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tag) {
                            "tbl" -> {
                                inTable = true
                                currentTableRows = mutableListOf()
                            }
                            "tr" -> {
                                if (inTable) {
                                    inTableRow = true
                                    currentRowCells = mutableListOf()
                                }
                            }
                            "tc" -> {
                                if (inTableRow) {
                                    inTableCell = true
                                    currentCellText = StringBuilder()
                                }
                            }
                            "p" -> {
                                inParagraph = true
                                currentParagraphText = StringBuilder()
                                currentHeadingLevel = 0
                                isCurrentBullet = false
                            }
                            "pStyle" -> {
                                val styleVal = parser.getAttributeValue(null, "val") ?: ""
                                if (styleVal.contains("Heading1", ignoreCase = true) || styleVal.contains("Title", ignoreCase = true)) {
                                    currentHeadingLevel = 1
                                } else if (styleVal.contains("Heading2", ignoreCase = true)) {
                                    currentHeadingLevel = 2
                                } else if (styleVal.contains("Heading3", ignoreCase = true)) {
                                    currentHeadingLevel = 3
                                } else if (styleVal.contains("Heading", ignoreCase = true)) {
                                    currentHeadingLevel = 2
                                }
                            }
                            "numPr" -> {
                                isCurrentBullet = true
                            }
                            "t" -> {
                                val text = parser.nextText()
                                if (inTableCell) {
                                    currentCellText.append(text)
                                } else if (inParagraph) {
                                    currentParagraphText.append(text)
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (tag) {
                            "tc" -> {
                                if (inTableRow) {
                                    currentRowCells.add(currentCellText.toString().trim())
                                    inTableCell = false
                                }
                            }
                            "tr" -> {
                                if (inTable) {
                                    if (currentRowCells.isNotEmpty()) {
                                        currentTableRows.add(currentRowCells)
                                    }
                                    inTableRow = false
                                }
                            }
                            "tbl" -> {
                                inTable = false
                                if (currentTableRows.isNotEmpty()) {
                                    val headers = currentTableRows.first()
                                    val rows = if (currentTableRows.size > 1) currentTableRows.subList(1, currentTableRows.size) else emptyList()
                                    tables.add(WordTable(headers, rows))
                                }
                            }
                            "p" -> {
                                inParagraph = false
                                val pText = currentParagraphText.toString().trim()
                                if (pText.isNotBlank() && !inTableCell) {
                                    paragraphs.add(
                                        WordParagraph(
                                            text = pText,
                                            isHeading = currentHeadingLevel > 0,
                                            headingLevel = currentHeadingLevel,
                                            isBullet = isCurrentBullet
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "XML parse issue in docx: ${e.message}")
        }

        return Pair(paragraphs, tables)
    }

    private fun parseDocBinary(file: File, fallbackTitle: String): WordDocumentModel {
        val paragraphs = mutableListOf<WordParagraph>()
        val docTitle = fallbackTitle.removeSuffix(".doc").removeSuffix(".DOC")

        try {
            val bytes = file.readBytes()
            // Extract printable text chunks from binary .doc stream
            val textBuilder = StringBuilder()
            var i = 0
            while (i < bytes.size) {
                val b = bytes[i].toInt() and 0xFF
                // ASCII printable or standard Unicode text piece
                if (b in 32..126 || b == 10 || b == 13 || b == 9) {
                    textBuilder.append(b.toChar())
                } else if (b in 160..255) {
                    textBuilder.append(b.toChar())
                } else {
                    if (textBuilder.isNotEmpty() && textBuilder.last() != '\n') {
                        textBuilder.append('\n')
                    }
                }
                i++
            }

            val rawLines = textBuilder.toString().split('\n')
            for (line in rawLines) {
                val clean = line.trim()
                if (clean.length > 3 && !clean.all { it.isDigit() || !it.isLetterOrDigit() }) {
                    val isHeading = clean.length < 60 && (clean.endsWith(":") || clean.all { it.isUpperCase() || it.isWhitespace() })
                    paragraphs.add(
                        WordParagraph(
                            text = clean,
                            isHeading = isHeading,
                            headingLevel = if (isHeading) 2 else 0
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading .doc binary: ${e.message}")
            paragraphs.add(WordParagraph(text = "Error reading legacy Word .doc: ${e.localizedMessage}"))
        }

        val totalWords = paragraphs.sumOf { it.text.split("\\s+".toRegex()).count { w -> w.isNotBlank() } }

        return WordDocumentModel(
            title = docTitle,
            paragraphs = paragraphs,
            tables = emptyList(),
            wordCount = totalWords,
            readingTimeMinutes = (totalWords / 200).coerceAtLeast(1)
        )
    }
}
