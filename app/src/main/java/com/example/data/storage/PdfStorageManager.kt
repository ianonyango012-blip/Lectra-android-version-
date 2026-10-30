package com.example.data.storage

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed class PdfImportOutcome {
    data class Success(
        val displayName: String,
        val localPath: String,
        val fileSizeBytes: Long,
        val pageCount: Int
    ) : PdfImportOutcome()

    data class Error(val message: String) : PdfImportOutcome()
}

object PdfStorageManager {

    private const val TAG = "PdfStorageManager"

    fun getLecturesDirectory(context: Context): File {
        val dir = File(context.filesDir, "lectures")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Resolves the original display name and byte size of a document from a Uri.
     */
    fun resolveMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var name = "Document.pdf"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve metadata from URI: ${e.message}")
            name = uri.lastPathSegment ?: "Document.pdf"
        }
        return Pair(name, size)
    }

    /**
     * Validates and securely copies an imported PDF into app-internal storage.
     * Checks for password encryption and document corruption.
     */
    fun importPdf(context: Context, uri: Uri): PdfImportOutcome {
        val (originalName, reportedSize) = resolveMetadata(context, uri)
        val lecturesDir = getLecturesDirectory(context)
        val cleanName = originalName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val uniqueFileName = "${UUID.randomUUID()}_$cleanName"
        val destinationFile = File(lecturesDir, uniqueFileName)

        try {
            // 1. Copy incoming stream to destination file
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return PdfImportOutcome.Error("Unable to access the selected file.")

            val actualSize = if (destinationFile.length() > 0) destinationFile.length() else reportedSize

            // 2. Validate PDF format and extract page count using PdfRenderer
            val pfd = ParcelFileDescriptor.open(destinationFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pageCount = try {
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount
                    if (count <= 0) {
                        throw IllegalStateException("PDF contains zero pages")
                    }
                    count
                }
            } catch (e: SecurityException) {
                destinationFile.delete()
                return PdfImportOutcome.Error("This PDF is password-protected. Please provide an unencrypted PDF.")
            } catch (e: Exception) {
                destinationFile.delete()
                return PdfImportOutcome.Error("The selected file is not a valid or readable PDF: ${e.localizedMessage}")
            } finally {
                try {
                    pfd.close()
                } catch (_: Exception) {}
            }

            return PdfImportOutcome.Success(
                displayName = originalName,
                localPath = destinationFile.absolutePath,
                fileSizeBytes = actualSize,
                pageCount = pageCount
            )
        } catch (e: Exception) {
            destinationFile.delete()
            return PdfImportOutcome.Error("Failed to import PDF: ${e.localizedMessage}")
        }
    }

    /**
     * Resolves a local File instance from a fileUriString.
     * Generates a realistic academic sample PDF if it refers to a bundled material.
     */
    fun resolveFile(context: Context, fileUriString: String, title: String = "Lecture Slides"): File? {
        val lecturesDir = getLecturesDirectory(context)

        // Case 1: bundled lectra scheme
        if (fileUriString.startsWith("lectra://bundled/")) {
            val fileName = fileUriString.removePrefix("lectra://bundled/")
            val targetFile = File(lecturesDir, fileName)
            if (!targetFile.exists() || targetFile.length() == 0L) {
                generateSampleLecturePdf(targetFile, title)
            }
            return targetFile
        }

        // Case 2: standard absolute file path
        if (fileUriString.startsWith("/")) {
            val file = File(fileUriString)
            if (file.exists()) return file
        }

        // Case 3: file URI
        if (fileUriString.startsWith("file://")) {
            val file = File(Uri.parse(fileUriString).path ?: "")
            if (file.exists()) return file
        }

        // Case 4: relative name in lectures directory
        val inLectures = File(lecturesDir, fileUriString.substringAfterLast("/"))
        if (inLectures.exists()) return inLectures

        // If file doesn't exist yet but has a title, generate a placeholder academic PDF
        val fallbackFile = File(lecturesDir, "${UUID.randomUUID()}_lecture.pdf")
        generateSampleLecturePdf(fallbackFile, title)
        return fallbackFile
    }

    /**
     * Generates a multi-page, formatted academic lecture PDF using Android's native PdfDocument.
     */
    fun generateSampleLecturePdf(destinationFile: File, title: String) {
        try {
            val document = PdfDocument()
            val pageWidth = 595 // standard A4 @ 72 DPI
            val pageHeight = 842

            val slides = listOf(
                SlideContent(
                    title = title,
                    subtitle = "University Lecture Series • Academic Notes",
                    bullets = listOf(
                        "Instructor: Academic Faculty Department",
                        "Topic Overview: Core principles, architectures & applications",
                        "Primary Goal: Master foundational invariants and system proofs",
                        "Recommended Study Plan: Review with Active Recall & Spaced Repetition"
                    ),
                    slideNumber = 1
                ),
                SlideContent(
                    title = "Key Concepts & Theoretical Foundations",
                    subtitle = "Module Framework & Invariants",
                    bullets = listOf(
                        "1. Decomposition of State: Splitting complex consensus into discrete phases.",
                        "2. Safety Invariants: State machine safety guaranteed under all network topologies.",
                        "3. Liveness Guarantee: Quorums and randomized election timeouts prevent split votes.",
                        "4. Storage Durability: Sequential append log with in-memory MemTable compaction."
                    ),
                    slideNumber = 2
                ),
                SlideContent(
                    title = "System Architecture & Execution Flow",
                    subtitle = "Detailed Protocol Walkthrough",
                    bullets = listOf(
                        "• Step A: Client dispatches mutation request to designated coordinator.",
                        "• Step B: Coordinator replicates entry across peer quorums (N/2 + 1).",
                        "• Step C: Once majority ACK is received, entry transitions to COMMITTED.",
                        "• Step D: State machine executes transaction and yields response to caller."
                    ),
                    slideNumber = 3
                ),
                SlideContent(
                    title = "Analysis & Performance Characteristics",
                    subtitle = "Latency, Throughput, and Compaction",
                    bullets = listOf(
                        "• Write Latency: Bound by disk fsync and RTT of majority quorum.",
                        "• Read Throughput: Accelerated via in-memory Bloom Filters and cache layers.",
                        "• Fault Tolerance: Continues uninterrupted provided minority f nodes fail.",
                        "• Memory Overhead: Bounded via periodic background log snapshotting."
                    ),
                    slideNumber = 4
                ),
                SlideContent(
                    title = "Summary & Exam Review Questions",
                    subtitle = "Active Recall Questions",
                    bullets = listOf(
                        "Q1: Under what conditions can a network partition cause a temporary split?",
                        "Q2: How does randomized back-off resolve leader election collisions?",
                        "Q3: Why are append-only sequential writes strictly faster than random I/O?",
                        "Action: Mark highlights and add sticky notes on key exam takeaways above!"
                    ),
                    slideNumber = 5
                )
            )

            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 22f
                isFakeBoldText = true
                color = Color.rgb(24, 32, 56)
            }

            val subtitlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 13f
                color = Color.rgb(117, 101, 246) // Lectra Violet
            }

            val bodyPaint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
                color = Color.rgb(55, 65, 81)
            }

            val cardPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(245, 243, 255) // Pale Lavender
            }

            val borderPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(226, 232, 240)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }

            val footerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.rgb(156, 163, 175)
            }

            slides.forEachIndexed { index, slide ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Background
                canvas.drawColor(Color.WHITE)

                // Top decorative banner
                val bannerRect = RectF(40f, 36f, (pageWidth - 40).toFloat(), 110f)
                canvas.drawRoundRect(bannerRect, 12f, 12f, cardPaint)
                canvas.drawRoundRect(bannerRect, 12f, 12f, borderPaint)

                canvas.drawText("LECTRA ACADEMIC STUDY", 56f, 62f, subtitlePaint)
                val displayTitle = if (slide.title.length > 40) slide.title.take(38) + "..." else slide.title
                canvas.drawText(displayTitle, 56f, 92f, titlePaint)

                // Subtitle
                canvas.drawText(slide.subtitle, 40f, 142f, subtitlePaint)

                // Content card
                val contentRect = RectF(40f, 160f, (pageWidth - 40).toFloat(), 720f)
                val whiteCardPaint = Paint().apply { color = Color.rgb(250, 250, 252) }
                canvas.drawRoundRect(contentRect, 14f, 14f, whiteCardPaint)
                canvas.drawRoundRect(contentRect, 14f, 14f, borderPaint)

                var yPos = 200f
                slide.bullets.forEach { bullet ->
                    // Wrap long text
                    if (bullet.length > 60) {
                        val part1 = bullet.take(55)
                        val part2 = "    " + bullet.drop(55)
                        canvas.drawText(part1, 64f, yPos, bodyPaint)
                        yPos += 22f
                        canvas.drawText(part2, 64f, yPos, bodyPaint)
                    } else {
                        canvas.drawText(bullet, 64f, yPos, bodyPaint)
                    }
                    yPos += 36f
                }

                // Decorative lecture stamp
                val stampPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.rgb(117, 101, 246)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRoundRect(RectF(64f, 630f, 260f, 680f), 8f, 8f, stampPaint)
                canvas.drawText("Verified Course Material", 78f, 660f, subtitlePaint)

                // Footer
                canvas.drawLine(40f, 780f, (pageWidth - 40).toFloat(), 780f, borderPaint)
                canvas.drawText("Lectra University Study Companion", 40f, 804f, footerPaint)
                canvas.drawText("Page ${slide.slideNumber} of ${slides.size}", (pageWidth - 110).toFloat(), 804f, footerPaint)

                document.finishPage(page)
            }

            FileOutputStream(destinationFile).use { out ->
                document.writeTo(out)
            }
            document.close()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to generate sample lecture PDF: ${e.message}", e)
            try {
                FileOutputStream(destinationFile).use { out ->
                    out.write("%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 595 842]/Parent 2 0 R/Resources<<>>>>endobj\nxref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000052 00000 n \n0000000101 00000 n \ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF\n".toByteArray())
                }
            } catch (_: Throwable) {}
        }
    }

    private data class SlideContent(
        val title: String,
        val subtitle: String,
        val bullets: List<String>,
        val slideNumber: Int
    )
}
