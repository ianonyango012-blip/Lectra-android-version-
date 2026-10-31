package com.example.ui.screens.document

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.document.DocumentStorageManager
import com.example.data.document.TextDocumentModel
import com.example.data.document.TextDocumentParser
import com.example.data.model.LectureMaterialEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextReaderScreen(
    material: LectureMaterialEntity,
    isDarkTheme: Boolean = false,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val file = remember(material.fileUriString, material.title) {
        DocumentStorageManager.resolveFile(context, material.fileUriString, material.title)
    }

    var document by remember { mutableStateOf<TextDocumentModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Display options
    var fontSizeSp by remember { mutableFloatStateOf(15f) }
    var isMonospace by remember { mutableStateOf(false) }
    var wrapText by remember { mutableStateOf(true) }
    var showLineNumbers by remember { mutableStateOf(true) }

    // Search
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(file) {
        isLoading = true
        document = withContext(Dispatchers.IO) {
            TextDocumentParser.parse(file ?: java.io.File(""), material.title)
        }
        isLoading = false
    }

    Scaffold(
        modifier = Modifier.testTag("text_reader_screen"),
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Find in text...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("text_search_field")
                        )
                    } else {
                        Column {
                            Text(
                                text = document?.title ?: material.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val doc = document
                            Text(
                                text = if (doc != null) "${doc.lineCount} lines • ${doc.wordCount} words • Plain Text" else "Plain Text",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSearchActive) {
                                isSearchActive = false
                                searchQuery = ""
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier.testTag("text_reader_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isSearchActive) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.testTag("text_search_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search text")
                        }
                        IconButton(
                            onClick = { isMonospace = !isMonospace },
                            modifier = Modifier.testTag("text_font_toggle")
                        ) {
                            Icon(
                                imageVector = if (isMonospace) Icons.Default.TextFields else Icons.Default.Code,
                                contentDescription = if (isMonospace) "Proportional font" else "Monospace font"
                            )
                        }
                        IconButton(
                            onClick = { wrapText = !wrapText },
                            modifier = Modifier.testTag("text_wrap_toggle")
                        ) {
                            Icon(
                                imageVector = if (wrapText) Icons.Default.WrapText else Icons.Default.FormatAlignLeft,
                                contentDescription = "Toggle line wrapping"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Size:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { if (fontSizeSp > 10f) fontSizeSp -= 2f },
                            modifier = Modifier.size(36.dp).testTag("text_size_decrease")
                        ) {
                            Text("A-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = "${fontSizeSp.toInt()}sp",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = { if (fontSizeSp < 28f) fontSizeSp += 2f },
                            modifier = Modifier.size(36.dp).testTag("text_size_increase")
                        ) {
                            Text("A+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    FilterChip(
                        selected = showLineNumbers,
                        onClick = { showLineNumbers = !showLineNumbers },
                        label = { Text("Line #") },
                        modifier = Modifier.testTag("toggle_line_numbers")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val doc = document
                if (doc == null || doc.lines.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Text document is empty.")
                    }
                } else {
                    val fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
                    val horizScrollState = rememberScrollState()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (!wrapText) Modifier.horizontalScroll(horizScrollState) else Modifier)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        itemsIndexed(doc.lines) { index, line ->
                            val lineNum = index + 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                if (showLineNumbers) {
                                    Text(
                                        text = "$lineNum",
                                        modifier = Modifier
                                            .width(44.dp)
                                            .padding(end = 8.dp),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = (fontSizeSp - 3).sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    )
                                }

                                val annotatedText = buildAnnotatedString {
                                    if (searchQuery.isNotBlank() && line.contains(searchQuery, ignoreCase = true)) {
                                        var startIdx = 0
                                        val lowerLine = line.lowercase()
                                        val lowerQuery = searchQuery.lowercase()
                                        while (startIdx < line.length) {
                                            val found = lowerLine.indexOf(lowerQuery, startIdx)
                                            if (found >= 0) {
                                                append(line.substring(startIdx, found))
                                                withStyle(
                                                    SpanStyle(
                                                        background = Color(0xFFFFEB3B),
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                ) {
                                                    append(line.substring(found, found + searchQuery.length))
                                                }
                                                startIdx = found + searchQuery.length
                                            } else {
                                                append(line.substring(startIdx))
                                                break
                                            }
                                        }
                                    } else {
                                        append(line)
                                    }
                                }

                                Text(
                                    text = annotatedText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = fontFamily,
                                        fontSize = fontSizeSp.sp,
                                        lineHeight = (fontSizeSp * 1.4f).sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    ),
                                    softWrap = wrapText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
