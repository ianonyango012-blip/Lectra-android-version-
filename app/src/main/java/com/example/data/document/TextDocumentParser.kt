package com.example.data.document

import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class TextDocumentModel(
    val title: String,
    val lines: List<String>,
    val fullText: String,
    val lineCount: Int,
    val wordCount: Int,
    val charCount: Int
)

object TextDocumentParser {

    fun parse(file: File, fallbackTitle: String): TextDocumentModel {
        val cleanTitle = fallbackTitle.removeSuffix(".txt").removeSuffix(".TXT")
        val bytes = file.readBytes()

        // Detect charset or default to UTF-8
        val charset = detectCharset(bytes)
        val fullText = String(bytes, charset)
        val lines = fullText.lines()

        val words = fullText.split("\\s+".toRegex()).count { it.isNotBlank() }

        return TextDocumentModel(
            title = cleanTitle,
            lines = lines,
            fullText = fullText,
            lineCount = lines.size,
            wordCount = words,
            charCount = fullText.length
        )
    }

    private fun detectCharset(bytes: ByteArray): Charset {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return StandardCharsets.UTF_8
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return StandardCharsets.UTF_16BE
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return StandardCharsets.UTF_16LE
        }
        return StandardCharsets.UTF_8
    }
}
