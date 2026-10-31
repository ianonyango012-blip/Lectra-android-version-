package com.example.ui.screens.document

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.document.DocumentStorageManager
import com.example.data.document.DocumentType
import com.example.data.model.LectureMaterialEntity
import com.example.data.model.PdfAnnotationEntity
import com.example.ui.screens.PdfViewerScreen

@Composable
fun DocumentViewerDispatcher(
    material: LectureMaterialEntity,
    annotations: List<PdfAnnotationEntity>,
    isDarkTheme: Boolean,
    onBackClick: () -> Unit,
    onPageChanged: (pageNumber: Int, totalPages: Int) -> Unit,
    onAddAnnotation: (pageIndex: Int, type: String, colorHex: String, content: String, dataJson: String, relX: Float, relY: Float) -> Unit,
    onDeleteAnnotation: (annotationId: Long) -> Unit,
    onToggleBookmark: (materialId: Long, pageIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val docType = remember(material.mimeType, material.fileUriString, material.title) {
        DocumentType.fromMimeOrFilename(material.mimeType, material.title) ?: DocumentType.PDF
    }

    val file = remember(material.fileUriString, material.title) {
        DocumentStorageManager.resolveFile(context, material.fileUriString, material.title)
    }

    if (file == null || (!file.exists() && docType != DocumentType.PDF)) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "File Not Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The document \"${material.title}\" could not be opened from storage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Go Back")
                    }
                }
            }
        }
        return
    }

    when (docType) {
        DocumentType.PDF -> {
            PdfViewerScreen(
                material = material,
                annotations = annotations,
                isDarkTheme = isDarkTheme,
                onBackClick = onBackClick,
                onPageChanged = onPageChanged,
                onAddAnnotation = onAddAnnotation,
                onDeleteAnnotation = onDeleteAnnotation,
                onToggleBookmark = onToggleBookmark
            )
        }

        DocumentType.DOCX, DocumentType.DOC -> {
            WordReaderScreen(
                material = material,
                isDarkTheme = isDarkTheme,
                onBackClick = onBackClick
            )
        }

        DocumentType.XLSX, DocumentType.XLS -> {
            ExcelReaderScreen(
                material = material,
                isDarkTheme = isDarkTheme,
                onBackClick = onBackClick
            )
        }

        DocumentType.PPTX, DocumentType.PPT -> {
            PowerPointReaderScreen(
                material = material,
                isDarkTheme = isDarkTheme,
                onBackClick = onBackClick
            )
        }

        DocumentType.TXT -> {
            TextReaderScreen(
                material = material,
                isDarkTheme = isDarkTheme,
                onBackClick = onBackClick
            )
        }

        DocumentType.EPUB -> {
            EpubReaderScreen(
                material = material,
                isDarkTheme = isDarkTheme,
                onBackClick = onBackClick
            )
        }
    }
}
