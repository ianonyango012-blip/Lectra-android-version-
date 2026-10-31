package com.example.ui.screens.document

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Toc
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.document.DocumentStorageManager
import com.example.data.document.WordDocumentModel
import com.example.data.document.WordDocumentParser
import com.example.data.document.WordParagraph
import com.example.data.document.WordTable
import com.example.data.model.LectureMaterialEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordReaderScreen(
    material: LectureMaterialEntity,
    isDarkTheme: Boolean = false,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var docModel by remember { mutableStateOf<WordDocumentModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Search state
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var matchingIndices by remember { mutableStateOf(listOf<Int>()) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    // Table of contents / Outline sheet
    var showOutlineSheet by remember { mutableStateOf(false) }
    val outlineSheetState = rememberModalBottomSheetState()

    val primaryViolet = if (isDarkTheme) Color(0xFF806BFF) else Color(0xFF7565F6)
    val docBlue = Color(0xFF2563EB)
    val bgColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val paperColor = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val textColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val secondaryTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)

    // Load document on IO thread
    LaunchedEffect(material.fileUriString) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val file = DocumentStorageManager.resolveFile(context, material.fileUriString, material.title)
            if (file != null && file.exists()) {
                val parsed = WordDocumentParser.parse(file, material.title)
                docModel = parsed
                isLoading = false
            } else {
                errorMessage = "Document file not found or inaccessible."
                isLoading = false
            }
        }
    }

    // Filter search matches
    LaunchedEffect(searchQuery, docModel) {
        if (searchQuery.isNotBlank() && docModel != null) {
            val matches = mutableListOf<Int>()
            docModel!!.paragraphs.forEachIndexed { index, p ->
                if (p.text.contains(searchQuery, ignoreCase = true)) {
                    matches.add(index)
                }
            }
            matchingIndices = matches
            currentMatchIndex = 0
            if (matches.isNotEmpty()) {
                listState.animateScrollToItem(matches.first())
            }
        } else {
            matchingIndices = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .testTag("word_reader_screen")
    ) {
        // ==========================================
        // 1. TOP APP BAR
        // ==========================================
        Surface(
            color = paperColor,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("word_reader_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textColor
                            )
                        }

                        Surface(
                            color = docBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (material.title.endsWith(".docx", ignoreCase = true)) "DOCX" else "DOC",
                                color = docBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = material.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textColor,
                                maxLines = 1
                            )
                            docModel?.let {
                                Text(
                                    text = "${it.wordCount} words • ~${it.readingTimeMinutes} min read",
                                    fontSize = 11.sp,
                                    color = secondaryTextColor
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showSearchBar = !showSearchBar },
                            modifier = Modifier.testTag("word_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search in document",
                                tint = if (showSearchBar) docBlue else textColor
                            )
                        }

                        IconButton(
                            onClick = { showOutlineSheet = true },
                            modifier = Modifier.testTag("word_outline_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Toc,
                                contentDescription = "Table of contents",
                                tint = textColor
                            )
                        }
                    }
                }

                // Search Bar Bar
                AnimatedVisibility(visible = showSearchBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Find in document...", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("word_search_input"),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = docBlue,
                                unfocusedBorderColor = borderColor
                            )
                        )

                        if (matchingIndices.isNotEmpty()) {
                            Text(
                                text = "${currentMatchIndex + 1}/${matchingIndices.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = docBlue
                            )

                            IconButton(
                                onClick = {
                                    if (currentMatchIndex > 0) {
                                        currentMatchIndex--
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(matchingIndices[currentMatchIndex])
                                        }
                                    }
                                },
                                enabled = currentMatchIndex > 0
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match")
                            }

                            IconButton(
                                onClick = {
                                    if (currentMatchIndex < matchingIndices.size - 1) {
                                        currentMatchIndex++
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(matchingIndices[currentMatchIndex])
                                        }
                                    }
                                },
                                enabled = currentMatchIndex < matchingIndices.size - 1
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. DOCUMENT CONTENT
        // ==========================================
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = docBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Reading Word document...", fontSize = 14.sp, color = secondaryTextColor)
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Error loading document",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                docModel != null -> {
                    val model = docModel!!
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title Header Card
                        item(key = "word_doc_header") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = paperColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = model.title,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = docBlue,
                                        lineHeight = 28.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Microsoft Word Document • ${model.paragraphs.size} paragraphs",
                                        fontSize = 12.sp,
                                        color = secondaryTextColor
                                    )
                                }
                            }
                        }

                        // Paragraphs rendering
                        itemsIndexed(model.paragraphs, key = { index, _ -> "para_$index" }) { index, para ->
                            val isMatch = matchingIndices.getOrNull(currentMatchIndex) == index

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isMatch) Color(0xFFFEF08A).copy(alpha = 0.5f)
                                        else Color.Transparent
                                    )
                                    .padding(vertical = 2.dp)
                            ) {
                                when {
                                    para.isHeading -> {
                                        val headingSize = when (para.headingLevel) {
                                            1 -> 20.sp
                                            2 -> 17.sp
                                            else -> 15.sp
                                        }
                                        Text(
                                            text = highlightSearch(para.text, searchQuery),
                                            fontSize = headingSize,
                                            fontWeight = FontWeight.Bold,
                                            color = if (para.headingLevel == 1) docBlue else textColor,
                                            lineHeight = 24.sp,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }

                                    para.isBullet -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "•",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = docBlue
                                            )
                                            Text(
                                                text = highlightSearch(para.text, searchQuery),
                                                fontSize = 14.sp,
                                                color = textColor,
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }

                                    else -> {
                                        Text(
                                            text = highlightSearch(para.text, searchQuery),
                                            fontSize = 14.sp,
                                            color = textColor,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Tables rendering
                        if (model.tables.isNotEmpty()) {
                            item(key = "tables_divider") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tables (${model.tables.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = textColor
                                )
                            }

                            itemsIndexed(model.tables, key = { tIdx, _ -> "table_$tIdx" }) { _, table ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = paperColor),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(12.dp)
                                    ) {
                                        // Header row
                                        Row(
                                            modifier = Modifier
                                                .background(docBlue.copy(alpha = 0.1f))
                                                .padding(vertical = 8.dp, horizontal = 6.dp)
                                        ) {
                                            table.headers.forEach { h ->
                                                Text(
                                                    text = h,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = docBlue,
                                                    modifier = Modifier
                                                        .width(140.dp)
                                                        .padding(horizontal = 6.dp)
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = borderColor)

                                        // Data rows
                                        table.rows.forEachIndexed { rIdx, row ->
                                            Row(
                                                modifier = Modifier
                                                    .background(
                                                        if (rIdx % 2 == 1) docBlue.copy(alpha = 0.03f)
                                                        else Color.Transparent
                                                    )
                                                    .padding(vertical = 8.dp, horizontal = 6.dp)
                                            ) {
                                                row.forEach { cell ->
                                                    Text(
                                                        text = cell,
                                                        fontSize = 12.sp,
                                                        color = textColor,
                                                        modifier = Modifier
                                                            .width(140.dp)
                                                            .padding(horizontal = 6.dp)
                                                    )
                                                }
                                            }
                                            if (rIdx < table.rows.size - 1) {
                                                HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
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

    // ==========================================
    // 3. TABLE OF CONTENTS / OUTLINE BOTTOM SHEET
    // ==========================================
    if (showOutlineSheet && docModel != null) {
        val headings = docModel!!.paragraphs.withIndex().filter { it.value.isHeading }

        ModalBottomSheet(
            onDismissRequest = { showOutlineSheet = false },
            sheetState = outlineSheetState,
            containerColor = paperColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Document Outline",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (headings.isEmpty()) {
                    Text(
                        text = "No explicit headings detected in this document.",
                        fontSize = 13.sp,
                        color = secondaryTextColor,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(headings) { _, (idx, heading) ->
                            val indent = ((heading.headingLevel - 1) * 16).dp.coerceAtLeast(0.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            outlineSheetState.hide()
                                            showOutlineSheet = false
                                            listState.animateScrollToItem(idx)
                                        }
                                    }
                                    .padding(start = indent, top = 8.dp, bottom = 8.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(docBlue)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = heading.text,
                                    fontSize = 13.sp,
                                    fontWeight = if (heading.headingLevel <= 1) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun highlightSearch(text: String, query: String) = buildAnnotatedString {
    if (query.isBlank()) {
        append(text)
        return@buildAnnotatedString
    }
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var startIndex = 0

    while (startIndex < text.length) {
        val foundIndex = lowerText.indexOf(lowerQuery, startIndex)
        if (foundIndex == -1) {
            append(text.substring(startIndex))
            break
        }
        if (foundIndex > startIndex) {
            append(text.substring(startIndex, foundIndex))
        }
        withStyle(SpanStyle(background = Color(0xFFFEF08A), color = Color(0xFF854D0E), fontWeight = FontWeight.Bold)) {
            append(text.substring(foundIndex, foundIndex + query.length))
        }
        startIndex = foundIndex + query.length
    }
}
