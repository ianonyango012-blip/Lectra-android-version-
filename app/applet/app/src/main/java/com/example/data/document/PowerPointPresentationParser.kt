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
    val images: List<String> = emptyList(), // List of ZIP entry paths or local paths for images
    val elements: List<SlideElement> = emptyList() // Mixed text & images in exact chronological order
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
                val slideEntries = getSlideEntriesInOrder(zip)
                for ((index, entryName) in slideEntries.withIndex()) {
                    val slideNum = index + 1
                    val slideModel = parseSlideContent(zip, entryName, slideNum, mediaMap)
                    slides.add(slideModel)
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
                    bullets = listOf("Presentation loaded with 1 slide."),
                    elements = listOf(SlideElement.Text(cleanTitle, true))
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

    private fun getSlideEntriesInOrder(zip: ZipFile): List<String> {
        val slidePathOrder = mutableListOf<String>()
        try {
            val presRelsMap = mutableMapOf<String, String>()
            val relsEntry = zip.getEntry("ppt/_rels/presentation.xml.rels")
            relsEntry?.let { entry ->
                zip.getInputStream(entry).use { stream ->
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(stream, "UTF-8")
                    var event = parser.eventType
                    while (event != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG && parser.name == "Relationship") {
                            val id = parser.getAttributeValue(null, "Id") ?: ""
                            val target = parser.getAttributeValue(null, "Target") ?: ""
                            if (id.isNotBlank() && target.isNotBlank()) {
                                val cleanTarget = target.substringAfter("../")
                                presRelsMap[id] = if (cleanTarget.startsWith("ppt/")) cleanTarget else "ppt/$cleanTarget"
                            }
                        }
                        event = parser.next()
                    }
                }
            }

            val presEntry = zip.getEntry("ppt/presentation.xml")
            presEntry?.let { entry ->
                zip.getInputStream(entry).use { stream ->
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(stream, "UTF-8")
                    var event = parser.eventType
                    while (event != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG && (parser.name == "sldId" || parser.name?.endsWith(":sldId") == true)) {
                            for (i in 0 until parser.attributeCount) {
                                val attrName = parser.getAttributeName(i)
                                val attrVal = parser.getAttributeValue(i)
                                if (attrName.contains("id") || attrName == "r:id" || attrName.endsWith("id")) {
                                    presRelsMap[attrVal]?.let { path ->
                                        if (!slidePathOrder.contains(path)) {
                                            slidePathOrder.add(path)
                                        }
                                    }
                                }
                            }
                        }
                        event = parser.next()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading presentation order: ${e.message}")
        }

        if (slidePathOrder.isEmpty()) {
            slidePathOrder.addAll(
                zip.entries().asSequence()
                    .filter { it.name.startsWith("ppt/slides/slide") && it.name.endsWith(".xml") && !it.name.contains("_rels") }
                    .sortedBy { entry ->
                        val numStr = entry.name.substringAfter("slide").substringBefore(".xml")
                        numStr.toIntOrNull() ?: 0
                    }
                    .map { it.name }
                    .toList()
            )
        }
        return slidePathOrder
    }

    private fun parseSlideContent(
        zip: ZipFile,
        slideEntryName: String,
        slideIndex: Int,
        mediaMap: Map<String, String>
    ): SlideModel {
        val slideNum = slideIndex
        val fileName = slideEntryName.substringAfterLast("/")
        val relsEntryPath = "ppt/slides/_rels/$fileName.rels"

        val relsMap = mutableMapOf<String, String>()
        try {
            zip.getEntry(relsEntryPath)?.let { relsEntry ->
                zip.getInputStream(relsEntry).use { stream ->
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(stream, "UTF-8")
                    var event = parser.eventType
                    while (event != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG && parser.name == "Relationship") {
                            val id = parser.getAttributeValue(null, "Id") ?: ""
                            val target = parser.getAttributeValue(null, "Target") ?: ""
                            if (id.isNotBlank() && target.isNotBlank()) {
                                relsMap[id] = target
                            }
                        }
                        event = parser.next()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing rels for $slideEntryName: ${e.message}")
        }

        val elements = mutableListOf<SlideElement>()
        var slideTitle = "Slide $slideNum"
        var hasTitle = false
        val bullets = mutableListOf<String>()
        val images = mutableListOf<String>()

        try {
            zip.getEntry(slideEntryName)?.let { slideEntry ->
                zip.getInputStream(slideEntry).use { stream ->
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    val parser = factory.newPullParser()
                    parser.setInput(stream, "UTF-8")
                    var eventType = parser.eventType
                    var inParagraph = false
                    var currentParaText = StringBuilder()

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        val tag = parser.name ?: ""
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                when {
                                    tag == "p" || tag.endsWith(":p") -> {
                                        inParagraph = true
                                        currentParaText = StringBuilder()
                                    }
                                    tag == "t" || tag.endsWith(":t") -> {
                                        if (inParagraph) {
                                            val text = parser.nextText()
                                            currentParaText.append(text)
                                        }
                                    }
                                    tag == "pic" || tag.endsWith(":pic") || tag == "blip" || tag.endsWith(":blip") -> {
                                        var embedId: String? = null
                                        for (i in 0 until parser.attributeCount) {
                                            val attrName = parser.getAttributeName(i)
                                            val attrVal = parser.getAttributeValue(i)
                                            if (attrName == "embed" || attrName == "r:embed" || attrName.endsWith("embed")) {
                                                embedId = attrVal
                                                break
                                            }
                                        }
                                        if (embedId != null) {
                                            resolveAndAddImage(embedId, relsMap, mediaMap, elements, images)
                                        }
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                when {
                                    tag == "p" || tag.endsWith(":p") -> {
                                        inParagraph = false
                                        val line = currentParaText.toString().trim()
                                        if (line.isNotBlank()) {
                                            val isTitle = !hasTitle
                                            elements.add(SlideElement.Text(line, isTitle))
                                            if (!hasTitle) {
                                                slideTitle = line
                                                hasTitle = true
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
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing slide XML $slideEntryName: ${e.message}")
        }

        if (elements.isEmpty()) {
            elements.add(SlideElement.Text(slideTitle, true))
        }

        return SlideModel(
            slideNumber = slideNum,
            title = slideTitle,
            bullets = bullets,
            images = images,
            elements = elements
        )
    }

    private fun resolveAndAddImage(
        embedId: String,
        relsMap: Map<String, String>,
        mediaMap: Map<String, String>,
        elements: MutableList<SlideElement>,
        images: MutableList<String>
    ) {
        val target = relsMap[embedId] ?: return
        val mediaEntryPath = if (target.contains("media/")) {
            val cleanTarget = target.substringAfter("../")
            if (cleanTarget.startsWith("ppt/")) cleanTarget else "ppt/$cleanTarget"
        } else {
            "ppt/media/${target.substringAfterLast("/")}"
        }
        mediaMap[mediaEntryPath]?.let { localPath ->
            elements.add(SlideElement.Image(localPath))
            if (!images.contains(localPath)) {
                images.add(localPath)
            }
        }
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

            val chunkSize = 5
            val chunks = lines.chunked(chunkSize)
            for ((index, chunk) in chunks.withIndex()) {
                val slideTitle = chunk.firstOrNull() ?: "Slide ${index + 1}"
                val bullets = if (chunk.size > 1) chunk.subList(1, chunk.size) else emptyList()
                val elements = mutableListOf<SlideElement>()
                elements.add(SlideElement.Text(slideTitle, true))
                bullets.forEach { b -> elements.add(SlideElement.Text(b, false)) }
                slides.add(
                    SlideModel(
                        slideNumber = index + 1,
                        title = slideTitle,
                        bullets = bullets,
                        elements = elements
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
                    bullets = listOf("Legacy PowerPoint slide content."),
                    elements = listOf(SlideElement.Text(cleanTitle, true))
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
