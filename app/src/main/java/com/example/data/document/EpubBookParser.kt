package com.example.data.document

import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

data class EpubChapterModel(
    val index: Int,
    val title: String,
    val content: String,
    val wordCount: Int
)

data class EpubBookModel(
    val title: String,
    val author: String,
    val chapters: List<EpubChapterModel>,
    val totalChapters: Int
)

object EpubBookParser {

    private const val TAG = "EpubBookParser"

    fun parse(file: File, fallbackTitle: String): EpubBookModel {
        var bookTitle = fallbackTitle.removeSuffix(".epub").removeSuffix(".EPUB")
        var bookAuthor = "Academic Author"
        val chapters = mutableListOf<EpubChapterModel>()

        try {
            ZipFile(file).use { zip ->
                // 1. Locate rootfile from META-INF/container.xml
                val containerEntry = zip.getEntry("META-INF/container.xml")
                var opfPath = "content.opf"
                if (containerEntry != null) {
                    zip.getInputStream(containerEntry).use { stream ->
                        val parsedOpf = parseContainerForOpfPath(stream)
                        if (parsedOpf.isNotBlank()) {
                            opfPath = parsedOpf
                        }
                    }
                }

                val opfEntry = zip.getEntry(opfPath) ?: zip.entries().asSequence().find { it.name.endsWith(".opf") }
                val opfDir = if (opfEntry != null && opfEntry.name.contains("/")) {
                    opfEntry.name.substringBeforeLast("/") + "/"
                } else ""

                val manifestMap = mutableMapOf<String, String>() // id -> href
                val spineOrder = mutableListOf<String>() // idref list

                if (opfEntry != null) {
                    zip.getInputStream(opfEntry).use { stream ->
                        val (metaTitle, metaAuthor, manifest, spine) = parseOpf(stream)
                        if (metaTitle.isNotBlank()) bookTitle = metaTitle
                        if (metaAuthor.isNotBlank()) bookAuthor = metaAuthor
                        manifestMap.putAll(manifest)
                        spineOrder.addAll(spine)
                    }
                }

                // 2. Read chapters in spine order
                val chapterHrefs = if (spineOrder.isNotEmpty()) {
                    spineOrder.mapNotNull { id -> manifestMap[id] }
                } else {
                    // Fallback to all xhtml / html entries
                    zip.entries().asSequence()
                        .filter { it.name.endsWith(".xhtml") || it.name.endsWith(".html") }
                        .map { it.name }
                        .toList()
                }

                for ((index, href) in chapterHrefs.withIndex()) {
                    val fullEntryPath = if (href.startsWith("/")) href.substring(1) else (opfDir + href)
                    val entry = zip.getEntry(fullEntryPath) ?: zip.getEntry(href)
                        ?: zip.entries().asSequence().find { it.name.endsWith(href.substringAfterLast("/")) }
                    if (entry != null) {
                        zip.getInputStream(entry).use { stream ->
                            val (chTitle, chContent) = parseHtmlChapter(stream, index + 1)
                            if (chContent.isNotBlank()) {
                                val words = chContent.split("\\s+".toRegex()).count { it.isNotBlank() }
                                chapters.add(
                                    EpubChapterModel(
                                        index = index + 1,
                                        title = chTitle,
                                        content = chContent,
                                        wordCount = words
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing EPUB: ${e.message}", e)
        }

        if (chapters.isEmpty()) {
            chapters.add(
                EpubChapterModel(
                    index = 1,
                    title = "Chapter 1",
                    content = "Unable to extract chapters or this EPUB contains no standard HTML documents.",
                    wordCount = 13
                )
            )
        }

        return EpubBookModel(
            title = bookTitle,
            author = bookAuthor,
            chapters = chapters,
            totalChapters = chapters.size
        )
    }

    private fun parseContainerForOpfPath(stream: InputStream): String {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(stream, "UTF-8")
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name.equals("rootfile", ignoreCase = true)) {
                    val fullPath = parser.getAttributeValue(null, "full-path")
                    if (!fullPath.isNullOrBlank()) {
                        return fullPath
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "container.xml parse error: ${e.message}")
        }
        return ""
    }

    private data class OpfResult(
        val title: String,
        val author: String,
        val manifest: Map<String, String>,
        val spine: List<String>
    )

    private fun parseOpf(stream: InputStream): OpfResult {
        var title = ""
        var author = ""
        val manifest = mutableMapOf<String, String>()
        val spine = mutableListOf<String>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(stream, "UTF-8")
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when {
                            tag.equals("title", ignoreCase = true) || tag.equals("dc:title", ignoreCase = true) -> {
                                title = parser.nextText()
                            }
                            tag.equals("creator", ignoreCase = true) || tag.equals("dc:creator", ignoreCase = true) -> {
                                author = parser.nextText()
                            }
                            tag.equals("item", ignoreCase = true) -> {
                                val id = parser.getAttributeValue(null, "id")
                                val href = parser.getAttributeValue(null, "href")
                                if (id != null && href != null) {
                                    manifest[id] = href
                                }
                            }
                            tag.equals("itemref", ignoreCase = true) -> {
                                val idref = parser.getAttributeValue(null, "idref")
                                if (idref != null) {
                                    spine.add(idref)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "OPF parse error: ${e.message}")
        }

        return OpfResult(title, author, manifest, spine)
    }

    private fun parseHtmlChapter(stream: InputStream, chapterIndex: Int): Pair<String, String> {
        val rawHtml = stream.bufferedReader().use { it.readText() }
        var chapterTitle = "Chapter $chapterIndex"

        // Extract <title> or <h1> if available
        val titleRegex = "<title>(.*?)</title>".toRegex(RegexOption.IGNORE_CASE)
        val h1Regex = "<h1>(.*?)</h1>".toRegex(RegexOption.IGNORE_CASE)
        val h2Regex = "<h2>(.*?)</h2>".toRegex(RegexOption.IGNORE_CASE)

        val matchH1 = h1Regex.find(rawHtml)?.groupValues?.get(1)
        val matchTitle = titleRegex.find(rawHtml)?.groupValues?.get(1)
        val matchH2 = h2Regex.find(rawHtml)?.groupValues?.get(1)

        val foundTitle = cleanHtml(matchH1 ?: matchH2 ?: matchTitle ?: "")
        if (foundTitle.isNotBlank() && foundTitle.length < 80) {
            chapterTitle = foundTitle
        }

        // Clean and format text content
        val bodyContent = if (rawHtml.contains("<body", ignoreCase = true)) {
            rawHtml.substringAfter("<body").substringBefore("</body>")
        } else {
            rawHtml
        }

        val formattedText = cleanHtmlToReadableText(bodyContent)
        return Pair(chapterTitle, formattedText)
    }

    private fun cleanHtml(html: String): String {
        return html.replace("<[^>]*>".toRegex(), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .trim()
    }

    private fun cleanHtmlToReadableText(html: String): String {
        return html
            .replace("(?i)<br\\s*/?>".toRegex(), "\n")
            .replace("(?i)</p>".toRegex(), "\n\n")
            .replace("(?i)</h1>".toRegex(), "\n\n")
            .replace("(?i)</h2>".toRegex(), "\n\n")
            .replace("(?i)</h3>".toRegex(), "\n\n")
            .replace("(?i)</li>".toRegex(), "\n")
            .replace("(?i)<li>".toRegex(), "• ")
            .replace("(?i)</blockquote>".toRegex(), "\n\n")
            .replace("<[^>]*>".toRegex(), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("\n{3,}".toRegex(), "\n\n")
            .trim()
    }
}
