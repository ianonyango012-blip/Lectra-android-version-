package com.example.data.document

import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

data class ExcelSheetModel(
    val name: String,
    val rowCount: Int,
    val columnCount: Int,
    val rows: List<List<String>>
)

data class ExcelWorkbookModel(
    val title: String,
    val sheets: List<ExcelSheetModel>,
    val totalCells: Int
)

object ExcelSpreadsheetParser {

    private const val TAG = "ExcelSpreadsheetParser"

    fun parse(file: File, fallbackTitle: String): ExcelWorkbookModel {
        return if (file.name.endsWith(".xlsx", ignoreCase = true)) {
            parseXlsx(file, fallbackTitle)
        } else {
            parseXlsBinary(file, fallbackTitle)
        }
    }

    private fun parseXlsx(file: File, fallbackTitle: String): ExcelWorkbookModel {
        val sheets = mutableListOf<ExcelSheetModel>()
        val cleanTitle = fallbackTitle.removeSuffix(".xlsx").removeSuffix(".XLSX")

        try {
            ZipFile(file).use { zip ->
                // 1. Shared Strings Table
                val sharedStrings = mutableListOf<String>()
                val sstEntry = zip.getEntry("xl/sharedStrings.xml")
                if (sstEntry != null) {
                    zip.getInputStream(sstEntry).use { stream ->
                        sharedStrings.addAll(parseSharedStrings(stream))
                    }
                }

                // 2. Workbook sheet names and IDs
                val sheetMetaList = mutableListOf<Pair<String, String>>() // (name, sheetId or filename)
                val wbEntry = zip.getEntry("xl/workbook.xml")
                if (wbEntry != null) {
                    zip.getInputStream(wbEntry).use { stream ->
                        sheetMetaList.addAll(parseWorkbookSheets(stream))
                    }
                }

                // 3. Parse individual worksheets
                if (sheetMetaList.isEmpty()) {
                    // Fallback: look for any sheet*.xml
                    var idx = 1
                    while (true) {
                        val sheetEntry = zip.getEntry("xl/worksheets/sheet$idx.xml") ?: break
                        zip.getInputStream(sheetEntry).use { stream ->
                            val sheetRows = parseWorksheetXml(stream, sharedStrings)
                            val maxCols = sheetRows.maxOfOrNull { it.size } ?: 0
                            sheets.add(
                                ExcelSheetModel(
                                    name = "Sheet $idx",
                                    rowCount = sheetRows.size,
                                    columnCount = maxCols,
                                    rows = sheetRows
                                )
                            )
                        }
                        idx++
                    }
                } else {
                    for ((index, pair) in sheetMetaList.withIndex()) {
                        val sheetName = pair.first
                        val targetName = "xl/worksheets/sheet${index + 1}.xml"
                        val entry = zip.getEntry(targetName) ?: zip.entries().asSequence().find {
                            it.name.startsWith("xl/worksheets/sheet") && it.name.endsWith(".xml")
                        }
                        if (entry != null) {
                            zip.getInputStream(entry).use { stream ->
                                val sheetRows = parseWorksheetXml(stream, sharedStrings)
                                val maxCols = sheetRows.maxOfOrNull { it.size } ?: 0
                                sheets.add(
                                    ExcelSheetModel(
                                        name = sheetName,
                                        rowCount = sheetRows.size,
                                        columnCount = maxCols,
                                        rows = sheetRows
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing xlsx: ${e.message}", e)
        }

        if (sheets.isEmpty()) {
            sheets.add(
                ExcelSheetModel(
                    name = "Sheet 1",
                    rowCount = 1,
                    columnCount = 1,
                    rows = listOf(listOf("Empty or unreadable spreadsheet"))
                )
            )
        }

        val cellCount = sheets.sumOf { s -> s.rows.sumOf { r -> r.count { cell -> cell.isNotBlank() } } }

        return ExcelWorkbookModel(
            title = cleanTitle,
            sheets = sheets,
            totalCells = cellCount
        )
    }

    private fun parseSharedStrings(stream: InputStream): List<String> {
        val strings = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(stream, "UTF-8")

            var eventType = parser.eventType
            var inText = false
            var currentText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tag == "t") {
                            inText = true
                            currentText = StringBuilder()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inText) {
                            currentText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tag == "t") {
                            inText = false
                            strings.add(currentText.toString())
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Shared strings error: ${e.message}")
        }
        return strings
    }

    private fun parseWorkbookSheets(stream: InputStream): List<Pair<String, String>> {
        val sheets = mutableListOf<Pair<String, String>>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(stream, "UTF-8")

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "sheet") {
                    val name = parser.getAttributeValue(null, "name") ?: "Sheet"
                    val id = parser.getAttributeValue(null, "sheetId") ?: "1"
                    sheets.add(Pair(name, id))
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Workbook sheets error: ${e.message}")
        }
        return sheets
    }

    private fun parseWorksheetXml(stream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(stream, "UTF-8")

            var eventType = parser.eventType
            var currentRow = mutableListOf<String>()
            var cellType = ""
            var inValue = false
            var currentVal = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tag) {
                            "row" -> {
                                currentRow = mutableListOf()
                            }
                            "c" -> {
                                cellType = parser.getAttributeValue(null, "t") ?: ""
                                currentVal = StringBuilder()
                            }
                            "v" -> {
                                inValue = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inValue) {
                            currentVal.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (tag) {
                            "v" -> {
                                inValue = false
                            }
                            "c" -> {
                                val rawVal = currentVal.toString().trim()
                                val displayVal = if (cellType == "s") {
                                    val idx = rawVal.toIntOrNull()
                                    if (idx != null && idx >= 0 && idx < sharedStrings.size) {
                                        sharedStrings[idx]
                                    } else rawVal
                                } else {
                                    rawVal
                                }
                                currentRow.add(displayVal)
                            }
                            "row" -> {
                                if (currentRow.isNotEmpty()) {
                                    rows.add(currentRow)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Worksheet parse error: ${e.message}")
        }
        return rows
    }

    private fun parseXlsBinary(file: File, fallbackTitle: String): ExcelWorkbookModel {
        val cleanTitle = fallbackTitle.removeSuffix(".xls").removeSuffix(".XLS")
        val rows = mutableListOf<List<String>>()

        try {
            val bytes = file.readBytes()
            val textBuilder = StringBuilder()
            var i = 0
            while (i < bytes.size) {
                val b = bytes[i].toInt() and 0xFF
                if (b in 32..126 || b == 9) {
                    textBuilder.append(b.toChar())
                } else if (b == 10 || b == 13) {
                    if (textBuilder.isNotEmpty() && textBuilder.last() != '\n') {
                        textBuilder.append('\n')
                    }
                }
                i++
            }

            val lines = textBuilder.toString().split('\n')
            for (line in lines) {
                val clean = line.trim()
                if (clean.length > 2) {
                    val cells = clean.split('\t').map { it.trim() }.filter { it.isNotBlank() }
                    if (cells.isNotEmpty()) {
                        rows.add(cells)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading .xls binary: ${e.message}")
        }

        if (rows.isEmpty()) {
            rows.add(listOf("Sheet 1", "No tabular records found in legacy .xls"))
        }

        val maxCols = rows.maxOfOrNull { it.size } ?: 1
        return ExcelWorkbookModel(
            title = cleanTitle,
            sheets = listOf(
                ExcelSheetModel(
                    name = "Sheet 1",
                    rowCount = rows.size,
                    columnCount = maxCols,
                    rows = rows
                )
            ),
            totalCells = rows.sumOf { it.size }
        )
    }
}
