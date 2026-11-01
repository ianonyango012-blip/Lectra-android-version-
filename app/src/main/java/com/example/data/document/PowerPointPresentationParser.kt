package com.example.data.document

import android.content.Context
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

data class SlideModel(
    val slideNumber: Int,
    val title: String,
    val bullets: List<String>,
    val notes: String? = null,
    val images: List<String> = emptyList() // List of ZIP entry paths for images
)

data class PresentationModel(
    val title: String,
    val slides: List<SlideModel>,
    val totalSlides: Int,
    val mediaFiles: Map<String, String> = emptyMap() // Map rId to ZIP entry path
)

object PowerPointPresentationParser {

    private const val TAG = "PowerPointParser"

    fun parse(context: Context, file: File, fallbackTitle: String): PresentationModel {
        return if (file.name.endsWith(".pptx", ignoreCase = true)) {
            parsePptx(context, file, fallbackTitle)
        } else {
            parsePptBinary(file, fallbackTitle)
        }
    }

    private fun parsePptx(context: Context, file: File, fallbackTitle: String): PresentationModel {
        val slides = mutableListOf<SlideModel>()
        val mediaMap = MediaExtractor.extractMediaToCache(context, file, "ppt/media/", "pptx_${file.name.hashCode()}")
        val cleanTitle = fallbackTitle.removeSuffix(".pptx").removeSuffix(".PPTX")

        try {
            ZipFile(file).use { zip ->
                val slideEntries = zip.entries().asSequence()
                    .filter { it.name.startsWith("ppt/slides/slide") && it.name.endsWith(".xml") }
                    .sortedBy { entry ->
                        val numStr = entry.name.substringAfter("slide").substringBefore(".xml")
                        numStr.toIntOrNull() ?: 0
                    }
                    .toList()

                for ((index, entry) in slideEntries.withIndex()) {
                    val slideNum = index + 1
                    val slideImages = mutableListOf<String>()
                    val relsEntry = zip.getEntry("ppt/slides/_rels/slide$slideNum.xml.rels")
                    
                    relsEntry?.let { relEntry ->
                        zip.getInputStream(relEntry).use { relsStream ->
                            val factory = XmlPullParserFactory.newInstance()
                            val parser = factory.newPullParser()
                            parser.setInput(relsStream, "UTF-8")
                            var event = parser.eventType
                            while (event != XmlPullParser.END_DOCUMENT) {
                                if (event == XmlPullParser.START_TAG && parser.name == "Relationship") {
                                    val target = parser.getAttributeValue(null, "Target") ?: ""
                                    if (target.contains("media/")) {
                                        val mediaEntryPath = "ppt/${target.substringAfter("../")}"
                                        mediaMap[mediaEntryPath]?.let { localPath ->
                                            slideImages.add(localPath)
                                        }
                                    }
                                }
                                event = parser.next()
                            }
                        }
                    }

                    zip.getInputStream(entry).use { stream ->
                        val (slideTitle, bullets) = parseSlideXml(stream, slideNum)
                        slides.add(
                            SlideModel(
                                slideNumber = slideNum,
                                title = slideTitle,
                                bullets = bullets,
                                images = slideImages
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing pptx: ${e.message}", e)
        }

        if (slides.isEmpty()) {
            slides.add(
                SlideModel(
                    slideNumber = 1,
                    title = cleanTitle,
                    bullets = listOf("Presentation loaded with 1 slide.")
                )
            )
        }

        val presTitle = if (slides.first().title.isNotBlank() && slides.first().title != "Slide 1") {
            slides.first().title
        } else {
            cleanTitle
        }

        return PresentationModel(
            title = presTitle,
            slides = slides,
            totalSlides = slides.size,
            mediaFiles = mediaMap
        )
    }

    private fun parseSlideXml(stream: InputStream, slideIndex: Int): Pair<String, List<String>> {
        var slideTitle = "Slide $slideIndex"
        val bullets = mutableListOf<String>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(stream, "UTF-8")

            var eventType = parser.eventType
            var inParagraph = false
            var currentParaText = StringBuilder()
            var isFirstPara = true

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name ?: ""

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tag) {
                            "p" -> {
                                inParagraph = true
                                currentParaText = StringBuilder()
                            }
                            "t" -> {
                                val text = parser.nextText()
                                if (inParagraph) {
                                    currentParaText.append(text)
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (tag) {
                            "p" -> {
                                inParagraph = false
                                val line = currentParaText.toString().trim()
                                if (line.isNotBlank()) {
                                    if (isFirstPara) {
                                        slideTitle = line
                                        isFirstPara = false
                                    } else {
                                        bullets.add(line)
                                    }
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Slide XML parse error: ${e.message}")
        }

        return Pair(slideTitle, bullets)
    }

    private fun parsePptBinary(file: File, fallbackTitle: String): PresentationModel {
        val cleanTitle = fallbackTitle.removeSuffix(".ppt").removeSuffix(".PPT")
        val slides = mutableListOf<SlideModel>()

        try {
            val bytes = file.readBytes()
            val textBuilder = StringBuilder()
            var i = 0
            while (i < bytes.size) {
                val b = bytes[i].toInt() and 0xFF
                if (b in 32..126 || b == 10 || b == 13) {
                    textBuilder.append(b.toChar())
                } else {
                    if (textBuilder.isNotEmpty() && textBuilder.last() != '\n') {
                        textBuilder.append('\n')
                    }
                }
                i++
            }

            val lines = textBuilder.toString().split('\n')
                .map { it.trim() }
                .filter { it.length > 3 && !it.all { ch -> ch.isDigit() || !ch.isLetterOrDigit() } }

            // Group into chunks of 4-6 lines per slide
            val chunkSize = 5
            val chunks = lines.chunked(chunkSize)
            for ((index, chunk) in chunks.withIndex()) {
                val slideTitle = chunk.firstOrNull() ?: "Slide ${index + 1}"
                val bullets = if (chunk.size > 1) chunk.subList(1, chunk.size) else emptyList()
                slides.add(
                    SlideModel(
                        slideNumber = index + 1,
                        title = slideTitle,
                        bullets = bullets
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing legacy .ppt binary: ${e.message}")
        }

        if (slides.isEmpty()) {
            slides.add(
                SlideModel(
                    slideNumber = 1,
                    title = cleanTitle,
                    bullets = listOf("Legacy PowerPoint slide content.")
                )
            )
        }

        return PresentationModel(
            title = cleanTitle,
            slides = slides,
            totalSlides = slides.size
        )
    }
}
