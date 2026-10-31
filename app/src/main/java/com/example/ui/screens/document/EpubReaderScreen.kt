package com.example.ui.screens.document

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.document.DocumentStorageManager
import com.example.data.document.EpubBookModel
import com.example.data.document.EpubBookParser
import com.example.data.model.LectureMaterialEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class EpubReadingTheme(
    val displayName: String,
    val backgroundColor: Color,
    val textColor: Color,
    val cardColor: Color
) {
    LIGHT("Light", Color(0xFFFAFAFA), Color(0xFF1C1B1F), Color(0xFFFFFFFF)),
    SEPIA("Sepia", Color(0xFFFBF0D9), Color(0xFF5F4B32), Color(0xFFF4E6C8)),
    DARK("Dark", Color(0xFF1E1E24), Color(0xFFE3E2E6), Color(0xFF2B2B33))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubReaderScreen(
    material: LectureMaterialEntity,
    isDarkTheme: Boolean = false,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val file = remember(material.fileUriString, material.title) {
        DocumentStorageManager.resolveFile(context, material.fileUriString, material.title)
    }

    var book by remember { mutableStateOf<EpubBookModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentChapterIndex by remember { mutableIntStateOf(0) }
    var showTocSheet by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    // Reading preferences
    var readingTheme by remember { mutableStateOf(if (isDarkTheme) EpubReadingTheme.DARK else EpubReadingTheme.SEPIA) }
    var fontSizeSp by remember { mutableFloatStateOf(17f) }
    var useSerifFont by remember { mutableStateOf(true) }

    LaunchedEffect(file) {
        isLoading = true
        book = withContext(Dispatchers.IO) {
            EpubBookParser.parse(file ?: java.io.File(""), material.title)
        }
        isLoading = false
    }

    val currentBook = book
    val currentChapter = currentBook?.chapters?.getOrNull(currentChapterIndex)
    val totalChapters = currentBook?.totalChapters ?: 0
    val scrollState = rememberScrollState()

    // Reset scroll when changing chapter
    LaunchedEffect(currentChapterIndex) {
        scrollState.scrollTo(0)
    }

    Scaffold(
        modifier = Modifier.testTag("epub_reader_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentChapter?.title ?: currentBook?.title ?: material.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (totalChapters > 0) "Chapter ${currentChapterIndex + 1} of $totalChapters • ${currentBook?.author ?: "EPUB eBook"}" else "EPUB Reader",
                            style = MaterialTheme.typography.labelSmall,
                            color = readingTheme.textColor.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("epub_reader_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = readingTheme.textColor
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showTocSheet = true },
                        modifier = Modifier.testTag("epub_toc_btn")
                    ) {
                        Icon(imageVector = Icons.Default.MenuBook, contentDescription = "Table of Contents", tint = readingTheme.textColor)
                    }
                    Box {
                        IconButton(
                            onClick = { showThemeMenu = true },
                            modifier = Modifier.testTag("epub_theme_btn")
                        ) {
                            Icon(imageVector = Icons.Default.FormatSize, contentDescription = "Reading Settings", tint = readingTheme.textColor)
                        }

                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            Text(
                                text = "Reading Theme",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            EpubReadingTheme.entries.forEach { theme ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = theme.backgroundColor,
                                                border = BorderStroke(1.dp, Color.Gray),
                                                modifier = Modifier.size(16.dp)
                                            ) {}
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = theme.displayName,
                                                fontWeight = if (readingTheme == theme) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    },
                                    onClick = {
                                        readingTheme = theme
                                        showThemeMenu = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(if (useSerifFont) "Use Sans-Serif font" else "Use Book Serif font")
                                },
                                onClick = {
                                    useSerifFont = !useSerifFont
                                    showThemeMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = readingTheme.backgroundColor
                )
            )
        },
        bottomBar = {
            if (totalChapters > 1) {
                Surface(
                    color = readingTheme.cardColor,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        LinearProgressIndicator(
                            progress = { if (totalChapters > 0) (currentChapterIndex + 1).toFloat() / totalChapters else 0f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = readingTheme.backgroundColor
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    if (currentChapterIndex > 0) currentChapterIndex--
                                },
                                enabled = currentChapterIndex > 0,
                                modifier = Modifier.testTag("epub_prev_chapter")
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Prev")
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (fontSizeSp > 12f) fontSizeSp -= 2f },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("A-", fontWeight = FontWeight.Bold, color = readingTheme.textColor)
                                }
                                Text(
                                    text = "${currentChapterIndex + 1} / $totalChapters",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = readingTheme.textColor
                                )
                                IconButton(
                                    onClick = { if (fontSizeSp < 32f) fontSizeSp += 2f },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("A+", fontWeight = FontWeight.Bold, color = readingTheme.textColor)
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    if (currentChapterIndex < totalChapters - 1) currentChapterIndex++
                                },
                                enabled = currentChapterIndex < totalChapters - 1,
                                modifier = Modifier.testTag("epub_next_chapter")
                            ) {
                                Text("Next")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(readingTheme.backgroundColor)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading EPUB eBook...",
                            color = readingTheme.textColor
                        )
                    }
                }
            } else if (currentChapter != null) {
                val fontFamily = if (useSerifFont) FontFamily.Serif else FontFamily.Default

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = currentChapter.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        color = readingTheme.textColor
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${currentChapter.wordCount} words",
                        style = MaterialTheme.typography.labelSmall,
                        color = readingTheme.textColor.copy(alpha = 0.6f)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = readingTheme.textColor.copy(alpha = 0.2f)
                    )

                    Text(
                        text = currentChapter.content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = fontFamily,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.6f).sp,
                            color = readingTheme.textColor
                        )
                    )

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    if (showTocSheet && currentBook != null) {
        ModalBottomSheet(
            onDismissRequest = { showTocSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Table of Contents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentBook.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(currentBook.chapters) { ch ->
                        val isSelected = ch.index - 1 == currentChapterIndex
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = ch.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supportingContent = {
                                Text("${ch.wordCount} words")
                            },
                            leadingContent = {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${ch.index}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentChapterIndex = ch.index - 1
                                    showTocSheet = false
                                }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
