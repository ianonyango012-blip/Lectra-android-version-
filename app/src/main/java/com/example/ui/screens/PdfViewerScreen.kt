package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.data.model.LectureMaterialEntity
import com.example.data.model.PdfAnnotationEntity
import com.example.data.storage.PdfStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

enum class ReaderNightMode(val title: String) {
    NORMAL("Normal"),
    NIGHT("Night (Inverted)"),
    SEPIA("Warm Sepia")
}

enum class AnnotationTool {
    PAN,
    HIGHLIGHT,
    UNDERLINE,
    PEN,
    STICKY_NOTE,
    ERASER
}

private val AnnotationColors = listOf(
    "#FFEB3B", // Classic Yellow
    "#7565F6", // Lectra Violet
    "#4ADE80", // Soft Mint Green
    "#F472B6", // Rose Pink
    "#38BDF8"  // Sky Cyan
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    material: LectureMaterialEntity,
    annotations: List<PdfAnnotationEntity>,
    isDarkTheme: Boolean,
    onBackClick: () -> Unit,
    onPageChanged: (pageNumber: Int, totalPages: Int) -> Unit,
    onAddAnnotation: (pageIndex: Int, type: String, colorHex: String, content: String, dataJson: String, relX: Float, relY: Float) -> Unit,
    onDeleteAnnotation: (annotationId: Long) -> Unit,
    onToggleBookmark: (materialId: Long, pageIndex: Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Resolve or generate local PDF File
    val pdfFile = remember(material.fileUriString) {
        PdfStorageManager.resolveFile(context, material.fileUriString, material.title)
    }

    // PDF Renderer Manager
    val rendererState = remember(pdfFile) {
        PdfRendererState(pdfFile)
    }

    DisposableEffect(rendererState) {
        rendererState.initialize()
        onDispose {
            rendererState.close()
        }
    }

    // Reading & View States
    var isFullscreen by remember { mutableStateOf(false) }
    var nightMode by remember { mutableStateOf(ReaderNightMode.NORMAL) }
    var isFitWidth by remember { mutableStateOf(true) }
    var activeTool by remember { mutableStateOf(AnnotationTool.PAN) }
    var selectedColorHex by remember { mutableStateOf(AnnotationColors[0]) }
    var penStrokeWidth by remember { mutableFloatStateOf(4f) }

    // Dialog & Sheet States
    var showJumpDialog by remember { mutableStateOf(false) }
    var showDocInfoDialog by remember { mutableStateOf(false) }
    var showAnnotationsSheet by remember { mutableStateOf(false) }
    var noteDialogPosition by remember { mutableStateOf<Pair<Int, Offset>?>(null) } // pageIndex, relativeOffset
    var editingAnnotation by remember { mutableStateOf<PdfAnnotationEntity?>(null) }

    // List & Page Scroll State
    val initialPage = (material.lastReadPage - 1).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)

    val currentPageIndex by remember {
        derivedStateOf {
            if (rendererState.pageCount > 0) {
                listState.firstVisibleItemIndex.coerceIn(0, rendererState.pageCount - 1)
            } else 0
        }
    }

    // Persist reading progress
    LaunchedEffect(currentPageIndex, rendererState.pageCount) {
        if (rendererState.pageCount > 0) {
            onPageChanged(currentPageIndex + 1, rendererState.pageCount)
        }
    }

    // Active bookmarks on current document
    val bookmarkedPages by remember(annotations) {
        derivedStateOf {
            annotations.filter { it.type == "BOOKMARK" }.map { it.pageIndex }.toSet()
        }
    }
    val isCurrentPageBookmarked = currentPageIndex in bookmarkedPages

    // Theme color palette
    val bgPrimary = if (isDarkTheme) Color(0xFF0B1020) else Color(0xFFFFFFFF)
    val bgCard = if (isDarkTheme) Color(0xFF13192F) else Color(0xFFF8FAFC)
    val borderCol = if (isDarkTheme) Color(0xFF263353) else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0B1020)
    val textSecondary = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accentViolet = Color(0xFF7565F6)

    BackHandler {
        onBackClick()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (nightMode == ReaderNightMode.NIGHT) Color(0xFF050811) else bgPrimary)
            .testTag("pdf_viewer_screen")
    ) {
        if (rendererState.errorMessage != null) {
            // Error Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Could Not Open PDF",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = rendererState.errorMessage ?: "Unknown error",
                    fontSize = 14.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBackClick) {
                        Text("Go Back")
                    }
                    Button(
                        onClick = {
                            openPdfExternally(context, pdfFile)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentViolet)
                    ) {
                        Text("Open Externally")
                    }
                }
            }
        } else if (!rendererState.isReady) {
            // Loading Screen
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = accentViolet)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Preparing Lecture Document...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
            }
        } else {
            // Main Document Reading View
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = if (isFullscreen) 0.dp else 64.dp,
                        bottom = if (isFullscreen) 0.dp else 72.dp
                    )
            ) {
                val availableWidth = constraints.maxWidth
                val availableHeight = constraints.maxHeight

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(
                        count = rendererState.pageCount,
                        key = { "pdf_page_$it" }
                    ) { pageIdx ->
                        val pageAnnotations = remember(annotations, pageIdx) {
                            annotations.filter { it.pageIndex == pageIdx }
                        }

                        PdfPageItem(
                            pageIndex = pageIdx,
                            totalPages = rendererState.pageCount,
                            availableWidthPx = availableWidth,
                            availableHeightPx = availableHeight,
                            isFitWidth = isFitWidth,
                            nightMode = nightMode,
                            activeTool = activeTool,
                            currentColorHex = selectedColorHex,
                            penStrokeWidth = penStrokeWidth,
                            annotations = pageAnnotations,
                            rendererState = rendererState,
                            onToggleFullscreen = { isFullscreen = !isFullscreen },
                            onDropStickyNote = { relOffset ->
                                noteDialogPosition = Pair(pageIdx, relOffset)
                            },
                            onSaveStroke = { type, colorHex, strokeJson ->
                                onAddAnnotation(pageIdx, type, colorHex, "", strokeJson, 0f, 0f)
                            },
                            onTapAnnotation = { annotation ->
                                if (activeTool == AnnotationTool.ERASER) {
                                    onDeleteAnnotation(annotation.id)
                                } else if (annotation.type == "NOTE") {
                                    editingAnnotation = annotation
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }

            // Top Control Bar
            AnimatedVisibility(
                visible = !isFullscreen,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopReaderBar(
                    title = material.title,
                    currentPage = currentPageIndex + 1,
                    totalPages = rendererState.pageCount,
                    isBookmarked = isCurrentPageBookmarked,
                    nightMode = nightMode,
                    isFitWidth = isFitWidth,
                    onBackClick = onBackClick,
                    onJumpClick = { showJumpDialog = true },
                    onBookmarkClick = {
                        onToggleBookmark(material.id, currentPageIndex)
                    },
                    onToggleNightMode = {
                        nightMode = when (nightMode) {
                            ReaderNightMode.NORMAL -> ReaderNightMode.NIGHT
                            ReaderNightMode.NIGHT -> ReaderNightMode.SEPIA
                            ReaderNightMode.SEPIA -> ReaderNightMode.NORMAL
                        }
                    },
                    onToggleFit = { isFitWidth = !isFitWidth },
                    onShareClick = { sharePdfFile(context, pdfFile, material.title) },
                    onOpenExternalClick = { openPdfExternally(context, pdfFile) },
                    onInfoClick = { showDocInfoDialog = true },
                    onToggleFullscreen = { isFullscreen = true }
                )
            }

            // Bottom Annotation Toolbar
            AnimatedVisibility(
                visible = !isFullscreen,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BottomAnnotationBar(
                    activeTool = activeTool,
                    selectedColorHex = selectedColorHex,
                    penStrokeWidth = penStrokeWidth,
                    annotationCount = annotations.size,
                    onSelectTool = { tool ->
                        activeTool = if (activeTool == tool) AnnotationTool.PAN else tool
                    },
                    onSelectColor = { color ->
                        selectedColorHex = color
                    },
                    onChangeStrokeWidth = { width ->
                        penStrokeWidth = width
                    },
                    onOpenAnnotationsSheet = { showAnnotationsSheet = true }
                )
            }

            // Floating Fullscreen Exit Button
            if (isFullscreen) {
                FloatingExitFullscreenButton(
                    currentPage = currentPageIndex + 1,
                    totalPages = rendererState.pageCount,
                    onClick = { isFullscreen = false },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }
        }
    }

    // Direct Jump-to-Page Dialog
    if (showJumpDialog && rendererState.pageCount > 0) {
        JumpToPageDialog(
            currentPage = currentPageIndex + 1,
            totalPages = rendererState.pageCount,
            onDismiss = { showJumpDialog = false },
            onJump = { targetPage ->
                scope.launch {
                    listState.scrollToItem((targetPage - 1).coerceIn(0, rendererState.pageCount - 1))
                }
                showJumpDialog = false
            }
        )
    }

    // Document Details Dialog
    if (showDocInfoDialog) {
        DocumentInfoDialog(
            material = material,
            totalPages = rendererState.pageCount,
            pdfFile = pdfFile,
            annotationsCount = annotations.size,
            bookmarksCount = bookmarkedPages.size,
            onDismiss = { showDocInfoDialog = false }
        )
    }

    // Sticky Note Creation Dialog
    noteDialogPosition?.let { (pageIdx, relOffset) ->
        StickyNoteDialog(
            initialText = "",
            initialColor = selectedColorHex,
            pageNumber = pageIdx + 1,
            isNew = true,
            onDismiss = { noteDialogPosition = null },
            onSave = { text, colorHex ->
                onAddAnnotation(
                    pageIdx,
                    "NOTE",
                    colorHex,
                    text,
                    "",
                    relOffset.x,
                    relOffset.y
                )
                noteDialogPosition = null
            },
            onDelete = null
        )
    }

    // Sticky Note Edit/View Dialog
    editingAnnotation?.let { note ->
        StickyNoteDialog(
            initialText = note.content,
            initialColor = note.colorHex,
            pageNumber = note.pageIndex + 1,
            isNew = false,
            onDismiss = { editingAnnotation = null },
            onSave = { text, colorHex ->
                onAddAnnotation(
                    note.pageIndex,
                    "NOTE",
                    colorHex,
                    text,
                    note.dataJson,
                    note.relativeX,
                    note.relativeY
                )
                onDeleteAnnotation(note.id)
                editingAnnotation = null
            },
            onDelete = {
                onDeleteAnnotation(note.id)
                editingAnnotation = null
            }
        )
    }

    // Annotations & Bookmarks Overview Bottom Sheet
    if (showAnnotationsSheet) {
        AnnotationsOverviewSheet(
            annotations = annotations,
            totalPages = rendererState.pageCount,
            onDismiss = { showAnnotationsSheet = false },
            onJumpToPage = { targetPageIndex ->
                scope.launch {
                    listState.scrollToItem(targetPageIndex.coerceIn(0, rendererState.pageCount - 1))
                }
                showAnnotationsSheet = false
            },
            onDeleteAnnotation = { annotationId ->
                onDeleteAnnotation(annotationId)
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Page Rendering Item with Multi-layer Annotations Canvas
// ---------------------------------------------------------------------------
@Composable
private fun PdfPageItem(
    pageIndex: Int,
    totalPages: Int,
    availableWidthPx: Int,
    availableHeightPx: Int,
    isFitWidth: Boolean,
    nightMode: ReaderNightMode,
    activeTool: AnnotationTool,
    currentColorHex: String,
    penStrokeWidth: Float,
    annotations: List<PdfAnnotationEntity>,
    rendererState: PdfRendererState,
    onToggleFullscreen: () -> Unit,
    onDropStickyNote: (Offset) -> Unit,
    onSaveStroke: (type: String, colorHex: String, strokeJson: String) -> Unit,
    onTapAnnotation: (PdfAnnotationEntity) -> Unit
) {
    // Determine target width
    val renderWidth = if (isFitWidth) {
        availableWidthPx.coerceAtLeast(300)
    } else {
        (availableWidthPx * 0.85f).toInt().coerceAtLeast(300)
    }

    // Load bitmap asynchronously
    val pageBitmap by produceState<Bitmap?>(initialValue = null, pageIndex, renderWidth) {
        value = rendererState.renderPage(pageIndex, renderWidth)
    }

    // Active live stroke during freehand pen or highlight drag
    val activeStrokePoints = remember { mutableStateListOf<Offset>() }

    // Color filter for night/sepia modes
    val colorFilter = remember(nightMode) {
        when (nightMode) {
            ReaderNightMode.NORMAL -> null
            ReaderNightMode.NIGHT -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            ReaderNightMode.SEPIA -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isFitWidth) 0.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Page Header Tag
        Row(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Page ${pageIndex + 1} of $totalPages",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (nightMode == ReaderNightMode.NIGHT) Color(0xFF64748B) else Color(0xFF94A3B8)
            )

            // Bookmarked indicator icon
            if (annotations.any { it.type == "BOOKMARK" }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Bookmarked",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Bookmarked",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }
        }

        // Page Content Box
        Box(
            modifier = Modifier
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(if (nightMode == ReaderNightMode.NIGHT) Color(0xFF0F172A) else Color.White)
                .border(
                    width = 0.5.dp,
                    color = if (nightMode == ReaderNightMode.NIGHT) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            if (pageBitmap != null) {
                val bmp = pageBitmap!!
                val aspectRatio = bmp.width.toFloat() / bmp.height.toFloat()

                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (isFitWidth) 1f else 0.94f)
                        .aspectRatio(aspectRatio)
                ) {
                    // 1. Rendered PDF Page Bitmap
                    androidx.compose.foundation.Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PDF Page ${pageIndex + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillWidth,
                        colorFilter = colorFilter
                    )

                    // 2. Saved Vector Annotations (Drawings, Highlights, Underlines)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasW = size.width
                        val canvasH = size.height

                        annotations.forEach { ann ->
                            when (ann.type) {
                                "DRAWING" -> {
                                    drawSerializedStroke(ann.dataJson, parseColor(ann.colorHex), 3.5f, canvasW, canvasH)
                                }
                                "HIGHLIGHT" -> {
                                    val highlightColor = parseColor(ann.colorHex).copy(alpha = 0.38f)
                                    drawSerializedStroke(ann.dataJson, highlightColor, 18f, canvasW, canvasH)
                                }
                                "UNDERLINE" -> {
                                    val underlineColor = parseColor(ann.colorHex).copy(alpha = 0.85f)
                                    drawSerializedStroke(ann.dataJson, underlineColor, 4f, canvasW, canvasH)
                                }
                            }
                        }

                        // Live active stroke in progress
                        if (activeStrokePoints.isNotEmpty()) {
                            val activeColor = when (activeTool) {
                                AnnotationTool.HIGHLIGHT -> parseColor(currentColorHex).copy(alpha = 0.38f)
                                AnnotationTool.UNDERLINE -> parseColor(currentColorHex).copy(alpha = 0.85f)
                                AnnotationTool.PEN -> parseColor(currentColorHex)
                                else -> Color.Transparent
                            }
                            val width = when (activeTool) {
                                AnnotationTool.HIGHLIGHT -> 18f
                                AnnotationTool.UNDERLINE -> 4f
                                AnnotationTool.PEN -> penStrokeWidth * 1.5f
                                else -> 3f
                            }
                            drawStrokeFromPoints(activeStrokePoints, activeColor, width)
                        }
                    }

                    // 3. Interactive Sticky Note Pins
                    annotations.filter { it.type == "NOTE" }.forEach { note ->
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (note.relativeX * bmp.width).roundToInt().coerceIn(0, (bmp.width - 40)),
                                        y = (note.relativeY * bmp.height).roundToInt().coerceIn(0, (bmp.height - 40))
                                    )
                                }
                                .clip(RoundedCornerShape(8.dp))
                                .background(parseColor(note.colorHex))
                                .shadow(2.dp, RoundedCornerShape(8.dp))
                                .clickable { onTapAnnotation(note) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StickyNote2,
                                    contentDescription = "Note",
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (note.content.length > 8) note.content.take(8) + "…" else note.content.ifEmpty { "Note" },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    // 4. Gesture Capture Layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(activeTool, currentColorHex) {
                                when (activeTool) {
                                    AnnotationTool.PAN -> {
                                        detectTapGestures(
                                            onDoubleTap = { onToggleFullscreen() }
                                        )
                                    }
                                    AnnotationTool.STICKY_NOTE -> {
                                        detectTapGestures { offset ->
                                            val relX = (offset.x / size.width).coerceIn(0f, 1f)
                                            val relY = (offset.y / size.height).coerceIn(0f, 1f)
                                            onDropStickyNote(Offset(relX, relY))
                                        }
                                    }
                                    AnnotationTool.PEN, AnnotationTool.HIGHLIGHT, AnnotationTool.UNDERLINE -> {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                activeStrokePoints.clear()
                                                activeStrokePoints.add(offset)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                activeStrokePoints.add(change.position)
                                            },
                                            onDragEnd = {
                                                if (activeStrokePoints.size >= 2) {
                                                    val serialized = serializeStroke(activeStrokePoints, size.width.toFloat(), size.height.toFloat())
                                                    val toolType = when (activeTool) {
                                                        AnnotationTool.HIGHLIGHT -> "HIGHLIGHT"
                                                        AnnotationTool.UNDERLINE -> "UNDERLINE"
                                                        else -> "DRAWING"
                                                    }
                                                    onSaveStroke(toolType, currentColorHex, serialized)
                                                }
                                                activeStrokePoints.clear()
                                            },
                                            onDragCancel = {
                                                activeStrokePoints.clear()
                                            }
                                        )
                                    }
                                    AnnotationTool.ERASER -> {
                                        detectTapGestures {
                                            // Handled on individual annotation pins or clear
                                        }
                                    }
                                }
                            }
                    )
                }
            } else {
                // Page loading placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (isFitWidth) 1f else 0.94f)
                        .height(420.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF7565F6)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Top Navigation & Reader Controls Bar
// ---------------------------------------------------------------------------
@Composable
private fun TopReaderBar(
    title: String,
    currentPage: Int,
    totalPages: Int,
    isBookmarked: Boolean,
    nightMode: ReaderNightMode,
    isFitWidth: Boolean,
    onBackClick: () -> Unit,
    onJumpClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onToggleNightMode: () -> Unit,
    onToggleFit: () -> Unit,
    onShareClick: () -> Unit,
    onOpenExternalClick: () -> Unit,
    onInfoClick: () -> Unit,
    onToggleFullscreen: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back Button & Document Title
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("pdf_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.padding(start = 2.dp)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Page indicator button
                    Text(
                        text = "Page $currentPage of $totalPages • Tap to Jump",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF7565F6),
                        modifier = Modifier
                            .clickable { onJumpClick() }
                            .testTag("pdf_jump_button")
                    )
                }
            }

            // Quick Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Bookmark Toggle
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.testTag("pdf_bookmark_toggle")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove Bookmark" else "Bookmark Page",
                        tint = if (isBookmarked) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Night Mode Toggle
                IconButton(onClick = onToggleNightMode) {
                    Icon(
                        imageVector = when (nightMode) {
                            ReaderNightMode.NORMAL -> Icons.Default.Nightlight
                            ReaderNightMode.NIGHT -> Icons.Default.WbSunny
                            ReaderNightMode.SEPIA -> Icons.Default.ColorLens
                        },
                        contentDescription = "Reading Mode: ${nightMode.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Fit Width Toggle
                IconButton(onClick = onToggleFit) {
                    Icon(
                        imageVector = Icons.Default.FitScreen,
                        contentDescription = if (isFitWidth) "Fit Margin" else "Fit Width",
                        tint = if (isFitWidth) Color(0xFF7565F6) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Share / External Viewer Menu Trigger
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share PDF",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onOpenExternalClick) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open in External Viewer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onInfoClick) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Document Info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Fullscreen
                IconButton(onClick = onToggleFullscreen) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen Mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bottom Annotation & Revision Bar
// ---------------------------------------------------------------------------
@Composable
private fun BottomAnnotationBar(
    activeTool: AnnotationTool,
    selectedColorHex: String,
    penStrokeWidth: Float,
    annotationCount: Int,
    onSelectTool: (AnnotationTool) -> Unit,
    onSelectColor: (String) -> Unit,
    onChangeStrokeWidth: (Float) -> Unit,
    onOpenAnnotationsSheet: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Tool sub-settings (Palette when Pen/Highlight/Underline is selected)
            if (activeTool == AnnotationTool.PEN || activeTool == AnnotationTool.HIGHLIGHT || activeTool == AnnotationTool.UNDERLINE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Color Palette
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnnotationColors.forEach { hex ->
                            val color = parseColor(hex)
                            val isSelected = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF7565F6) else Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectColor(hex) }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (hex == "#FFEB3B") Color.Black else Color.White,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }

                    // Stroke width indicator
                    Text(
                        text = if (activeTool == AnnotationTool.PEN) "Stroke: ${penStrokeWidth.toInt()}pt" else activeTool.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Primary Tools Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ToolButton(
                    icon = Icons.Default.PanTool,
                    label = "Pan",
                    isActive = activeTool == AnnotationTool.PAN,
                    onClick = { onSelectTool(AnnotationTool.PAN) }
                )

                ToolButton(
                    icon = Icons.Default.Highlight,
                    label = "Highlight",
                    isActive = activeTool == AnnotationTool.HIGHLIGHT,
                    onClick = { onSelectTool(AnnotationTool.HIGHLIGHT) }
                )

                ToolButton(
                    icon = Icons.Default.FormatUnderlined,
                    label = "Underline",
                    isActive = activeTool == AnnotationTool.UNDERLINE,
                    onClick = { onSelectTool(AnnotationTool.UNDERLINE) }
                )

                ToolButton(
                    icon = Icons.Default.Draw,
                    label = "Pen",
                    isActive = activeTool == AnnotationTool.PEN,
                    onClick = { onSelectTool(AnnotationTool.PEN) }
                )

                ToolButton(
                    icon = Icons.Default.StickyNote2,
                    label = "Note",
                    isActive = activeTool == AnnotationTool.STICKY_NOTE,
                    onClick = { onSelectTool(AnnotationTool.STICKY_NOTE) }
                )

                ToolButton(
                    icon = Icons.Default.Clear,
                    label = "Eraser",
                    isActive = activeTool == AnnotationTool.ERASER,
                    onClick = { onSelectTool(AnnotationTool.ERASER) }
                )

                // Overview of Bookmarks & Notes
                IconButton(
                    onClick = onOpenAnnotationsSheet,
                    modifier = Modifier.testTag("pdf_annotations_sheet_button")
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Bookmarks,
                            contentDescription = "Annotations & Bookmarks",
                            tint = Color(0xFF7565F6)
                        )
                        if (annotationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (annotationCount > 9) "9+" else annotationCount.toString(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) Color(0xFF7565F6) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Color(0xFF7565F6) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FloatingExitFullscreenButton(
    currentPage: Int,
    totalPages: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        color = Color(0xFF7565F6),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FullscreenExit,
                contentDescription = "Exit Fullscreen",
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "$currentPage / $totalPages",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Dialogs: Jump to Page, Sticky Note, Document Info, Annotations Sheet
// ---------------------------------------------------------------------------
@Composable
private fun JumpToPageDialog(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit
) {
    var selectedPage by remember { mutableFloatStateOf(currentPage.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jump to Page", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Page ${selectedPage.toInt()} of $totalPages",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7565F6)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = selectedPage,
                    onValueChange = { selectedPage = it },
                    valueRange = 1f..totalPages.toFloat(),
                    steps = if (totalPages > 2) totalPages - 2 else 0,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF7565F6),
                        activeTrackColor = Color(0xFF7565F6)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1", fontSize = 12.sp, color = Color.Gray)
                    Text("$totalPages", fontSize = 12.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onJump(selectedPage.toInt()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7565F6))
            ) {
                Text("Go to Page")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun StickyNoteDialog(
    initialText: String,
    initialColor: String,
    pageNumber: Int,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (text: String, colorHex: String) -> Unit,
    onDelete: (() -> Unit)?
) {
    var noteText by remember { mutableStateOf(initialText) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNew) "Add Sticky Note" else "Edit Note",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Page $pageNumber",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF7565F6)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Color Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Color:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnnotationColors.forEach { hex ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(parseColor(hex))
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) Color(0xFF7565F6) else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Write personal study notes, reminders, or questions...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Note",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (noteText.isNotBlank()) {
                                    onSave(noteText.trim(), selectedColor)
                                }
                            },
                            enabled = noteText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7565F6))
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentInfoDialog(
    material: LectureMaterialEntity,
    totalPages: Int,
    pdfFile: File?,
    annotationsCount: Int,
    bookmarksCount: Int,
    onDismiss: () -> Unit
) {
    val sizeMb = if (material.fileSizeBytes > 0) {
        String.format("%.2f MB", material.fileSizeBytes / (1024f * 1024f))
    } else {
        "Local Document"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Document Details",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoRow("Title", material.title)
                InfoRow("Pages", "$totalPages pages")
                InfoRow("File Size", sizeMb)
                InfoRow("Storage", "Lectra Private Storage (Offline)")
                InfoRow("Annotations", "$annotationsCount items")
                InfoRow("Bookmarks", "$bookmarksCount pages")
                if (pdfFile != null) {
                    InfoRow("Local File", pdfFile.name)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7565F6))
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.65f),
            textAlign = TextAlign.End
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnotationsOverviewSheet(
    annotations: List<PdfAnnotationEntity>,
    totalPages: Int,
    onDismiss: () -> Unit,
    onJumpToPage: (pageIndex: Int) -> Unit,
    onDeleteAnnotation: (annotationId: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val bookmarks = remember(annotations) { annotations.filter { it.type == "BOOKMARK" } }
    val notes = remember(annotations) { annotations.filter { it.type == "NOTE" } }
    val highlights = remember(annotations) { annotations.filter { it.type == "HIGHLIGHT" || it.type == "UNDERLINE" || it.type == "DRAWING" } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Revision & Annotations",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Bookmarks (${bookmarks.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Notes (${notes.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Marks (${highlights.size})") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // Bookmarks List
                    if (bookmarks.isEmpty()) {
                        EmptyAnnotationState("No pages bookmarked yet. Tap the bookmark icon in the top bar to save key slides.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(bookmarks.size) { i ->
                                val bm = bookmarks[i]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onJumpToPage(bm.pageIndex) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = null,
                                                tint = Color(0xFFF59E0B)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Page ${bm.pageIndex + 1} of $totalPages",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        IconButton(onClick = { onDeleteAnnotation(bm.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove Bookmark",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Notes List
                    if (notes.isEmpty()) {
                        EmptyAnnotationState("No sticky notes created. Select the Note tool and tap anywhere on a page to attach a study reminder.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(notes.size) { i ->
                                val note = notes[i]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onJumpToPage(note.pageIndex) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(parseColor(note.colorHex))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Page ${note.pageIndex + 1}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF7565F6)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = note.content,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(onClick = { onDeleteAnnotation(note.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Note",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Highlights & Marks List
                    if (highlights.isEmpty()) {
                        EmptyAnnotationState("No highlights or drawings yet. Use the Highlight, Underline, or Pen tools to annotate your slides.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(highlights.size) { i ->
                                val mark = highlights[i]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onJumpToPage(mark.pageIndex) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(parseColor(mark.colorHex))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "${mark.type.lowercase().replaceFirstChar { it.uppercase() }} on Page ${mark.pageIndex + 1}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        IconButton(onClick = { onDeleteAnnotation(mark.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Mark",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAnnotationState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// Thread-Safe PdfRenderer Manager with LRU Cache
// ---------------------------------------------------------------------------
class PdfRendererState(private val file: File?) {
    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private val mutex = Mutex()
    private val memoryCache = LruCache<Int, Bitmap>(16)

    var pageCount by mutableIntStateOf(0)
    var isReady by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun initialize() {
        if (file == null || !file.exists()) {
            errorMessage = "Document file could not be located on storage."
            return
        }

        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd!!)
            pageCount = renderer!!.pageCount
            isReady = true
        } catch (e: SecurityException) {
            errorMessage = "This PDF document is password-protected."
        } catch (e: Exception) {
            errorMessage = "Unable to read PDF file: ${e.localizedMessage}"
        }
    }

    suspend fun renderPage(pageIndex: Int, targetWidth: Int): Bitmap? {
        if (!isReady || renderer == null) return null
        memoryCache.get(pageIndex)?.let { return it }

        return withContext(Dispatchers.IO) {
            mutex.withLock {
                memoryCache.get(pageIndex)?.let { return@withLock it }
                try {
                    val page = renderer!!.openPage(pageIndex)
                    val width = targetWidth.coerceAtLeast(300)
                    val height = (width.toFloat() * page.height / page.width).toInt().coerceAtLeast(400)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    memoryCache.put(pageIndex, bitmap)
                    bitmap
                } catch (e: Exception) {
                    Log.e("PdfRendererState", "Failed to render page $pageIndex: ${e.message}")
                    null
                }
            }
        }
    }

    fun close() {
        try {
            renderer?.close()
            pfd?.close()
            memoryCache.evictAll()
        } catch (_: Exception) {}
    }
}

// ---------------------------------------------------------------------------
// Canvas Stroke Utilities & Math
// ---------------------------------------------------------------------------
private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF7565F6)
    }
}

private fun serializeStroke(points: List<Offset>, canvasW: Float, canvasH: Float): String {
    return points.joinToString(";") { pt ->
        val rx = String.format("%.4f", (pt.x / canvasW).coerceIn(0f, 1f))
        val ry = String.format("%.4f", (pt.y / canvasH).coerceIn(0f, 1f))
        "$rx,$ry"
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStrokeFromPoints(
    points: List<Offset>,
    color: Color,
    strokeWidth: Float
) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSerializedStroke(
    serialized: String,
    color: Color,
    strokeWidth: Float,
    canvasW: Float,
    canvasH: Float
) {
    if (serialized.isBlank()) return
    val pairs = serialized.split(";")
    if (pairs.size < 2) return

    val path = Path()
    pairs.forEachIndexed { i, pair ->
        val parts = pair.split(",")
        if (parts.size == 2) {
            val rx = parts[0].toFloatOrNull() ?: 0f
            val ry = parts[1].toFloatOrNull() ?: 0f
            val px = rx * canvasW
            val py = ry * canvasH
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

// ---------------------------------------------------------------------------
// Share and Open External Intent Helpers
// ---------------------------------------------------------------------------
private fun sharePdfFile(context: Context, file: File?, title: String) {
    if (file == null || !file.exists()) return
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Lecture PDF"))
    } catch (e: Exception) {
        Log.e("PdfViewerScreen", "Failed to share PDF: ${e.message}")
    }
}

private fun openPdfExternally(context: Context, file: File?) {
    if (file == null || !file.exists()) return
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(viewIntent)
    } catch (e: Exception) {
        Log.e("PdfViewerScreen", "Failed to open PDF externally: ${e.message}")
    }
}
