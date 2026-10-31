package com.example.data.document

import androidx.compose.ui.graphics.Color
import java.util.Locale

/**
 * Enumeration of all supported academic document formats in LECTRA.
 */
enum class DocumentType(
    val extension: String,
    val filterCategory: String, // "PDF", "DOC", "XLS", "PPT", "TXT", "EPUB"
    val displayName: String,
    val primaryMimeType: String,
    val badgeColorHex: String,
    val badgeColor: Color
) {
    PDF(
        extension = "pdf",
        filterCategory = "PDF",
        displayName = "PDF Document",
        primaryMimeType = "application/pdf",
        badgeColorHex = "#EF4444",
        badgeColor = Color(0xFFEF4444)
    ),
    DOCX(
        extension = "docx",
        filterCategory = "DOC",
        displayName = "Word Document",
        primaryMimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        badgeColorHex = "#2563EB",
        badgeColor = Color(0xFF2563EB)
    ),
    DOC(
        extension = "doc",
        filterCategory = "DOC",
        displayName = "Word 97-2003",
        primaryMimeType = "application/msword",
        badgeColorHex = "#3B82F6",
        badgeColor = Color(0xFF3B82F6)
    ),
    XLSX(
        extension = "xlsx",
        filterCategory = "XLS",
        displayName = "Excel Workbook",
        primaryMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        badgeColorHex = "#10B981",
        badgeColor = Color(0xFF10B981)
    ),
    XLS(
        extension = "xls",
        filterCategory = "XLS",
        displayName = "Excel 97-2003",
        primaryMimeType = "application/vnd.ms-excel",
        badgeColorHex = "#059669",
        badgeColor = Color(0xFF059669)
    ),
    PPTX(
        extension = "pptx",
        filterCategory = "PPT",
        displayName = "PowerPoint Presentation",
        primaryMimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        badgeColorHex = "#F97316",
        badgeColor = Color(0xFFF97316)
    ),
    PPT(
        extension = "ppt",
        filterCategory = "PPT",
        displayName = "PowerPoint 97-2003",
        primaryMimeType = "application/vnd.ms-powerpoint",
        badgeColorHex = "#EA580C",
        badgeColor = Color(0xFFEA580C)
    ),
    TXT(
        extension = "txt",
        filterCategory = "TXT",
        displayName = "Plain Text",
        primaryMimeType = "text/plain",
        badgeColorHex = "#64748B",
        badgeColor = Color(0xFF64748B)
    ),
    EPUB(
        extension = "epub",
        filterCategory = "EPUB",
        displayName = "EPUB eBook",
        primaryMimeType = "application/epub+zip",
        badgeColorHex = "#8B5CF6",
        badgeColor = Color(0xFF8B5CF6)
    );

    companion object {
        val ALL_FILTER_CATEGORIES = listOf("All", "PDF", "DOC", "XLS", "PPT", "TXT", "EPUB")

        val SUPPORTED_MIME_TYPES = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "application/epub+zip",
            "application/octet-stream",
            "*/*"
        )

        /**
         * Resolves the DocumentType from MIME type, filename, or file extension.
         */
        fun fromMimeOrFilename(mime: String?, fileName: String): DocumentType? {
            val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT).trim()

            // 1. Try resolving by explicit extension first
            when (ext) {
                "pdf" -> return PDF
                "docx" -> return DOCX
                "doc" -> return DOC
                "xlsx" -> return XLSX
                "xls" -> return XLS
                "pptx" -> return PPTX
                "ppt" -> return PPT
                "txt", "text", "log", "md" -> return TXT
                "epub" -> return EPUB
            }

            // 2. Try resolving by MIME type
            if (!mime.isNullOrBlank()) {
                val cleanMime = mime.lowercase(Locale.ROOT).trim()
                for (type in entries) {
                    if (cleanMime == type.primaryMimeType) {
                        return type
                    }
                }
                when {
                    cleanMime.contains("pdf") -> return PDF
                    cleanMime.contains("wordprocessingml") || cleanMime.contains("docx") -> return DOCX
                    cleanMime.contains("msword") || cleanMime.contains("doc") -> return DOC
                    cleanMime.contains("spreadsheetml") || cleanMime.contains("xlsx") -> return XLSX
                    cleanMime.contains("ms-excel") || cleanMime.contains("xls") -> return XLS
                    cleanMime.contains("presentationml") || cleanMime.contains("pptx") -> return PPTX
                    cleanMime.contains("ms-powerpoint") || cleanMime.contains("ppt") -> return PPT
                    cleanMime.contains("text/plain") -> return TXT
                    cleanMime.contains("epub") -> return EPUB
                }
            }

            return null
        }

        /**
         * Returns whether a material matches the specified filter category.
         */
        fun matchesCategory(category: String, materialMime: String?, materialTitle: String): Boolean {
            if (category.equals("All", ignoreCase = true)) return true
            val type = fromMimeOrFilename(materialMime, materialTitle) ?: return false
            return type.filterCategory.equals(category, ignoreCase = true)
        }
    }
}
