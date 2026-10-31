package com.example.ui.screens.document

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.document.DocumentStorageManager
import com.example.data.document.ExcelSpreadsheetParser
import com.example.data.document.ExcelWorkbookModel
import com.example.data.model.LectureMaterialEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelReaderScreen(
    material: LectureMaterialEntity,
    isDarkTheme: Boolean = false,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    var workbook by remember { mutableStateOf<ExcelWorkbookModel?>(null) }
    var selectedSheetIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Active cell selection
    var selectedCellAddress by remember { mutableStateOf("A1") }
    var selectedCellValue by remember { mutableStateOf("") }

    val excelGreen = Color(0xFF10B981)
    val bgColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val paperColor = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val textColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val secondaryTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val gridBorderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
    val headerBgColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)

    val horizontalScrollState = rememberScrollState()

    LaunchedEffect(material.fileUriString) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val file = DocumentStorageManager.resolveFile(context, material.fileUriString, material.title)
            if (file != null && file.exists()) {
                val parsed = ExcelSpreadsheetParser.parse(file, material.title)
                workbook = parsed
                val firstCell = parsed.sheets.firstOrNull()?.rows?.firstOrNull()?.firstOrNull() ?: ""
                selectedCellValue = firstCell
                isLoading = false
            } else {
                errorMessage = "Spreadsheet file not found or inaccessible."
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .testTag("excel_reader_screen")
    ) {
        // ==========================================
        // 1. TOP APP BAR
        // ==========================================
        Surface(
            color = paperColor,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, gridBorderColor)
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
                            modifier = Modifier.testTag("excel_reader_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textColor
                            )
                        }

                        Surface(
                            color = excelGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (material.title.endsWith(".xlsx", ignoreCase = true)) "XLSX" else "XLS",
                                color = excelGreen,
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
                            workbook?.let { wb ->
                                val activeSheet = wb.sheets.getOrNull(selectedSheetIndex)
                                Text(
                                    text = "${wb.sheets.size} sheet(s) • ${activeSheet?.rowCount ?: 0} rows × ${activeSheet?.columnCount ?: 0} cols",
                                    fontSize = 11.sp,
                                    color = secondaryTextColor
                                )
                            }
                        }
                    }
                }

                // Formula / Selected Cell Address Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = excelGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = selectedCellAddress,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = excelGreen,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = selectedCellValue.ifBlank { "(empty cell)" },
                        fontSize = 12.sp,
                        color = if (selectedCellValue.isNotBlank()) textColor else secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Multiple Sheet Tabs Row
                workbook?.let { wb ->
                    if (wb.sheets.size > 1) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedSheetIndex,
                            edgePadding = 8.dp,
                            containerColor = paperColor,
                            contentColor = excelGreen
                        ) {
                            wb.sheets.forEachIndexed { index, sheet ->
                                Tab(
                                    selected = selectedSheetIndex == index,
                                    onClick = { selectedSheetIndex = index },
                                    text = {
                                        Text(
                                            text = sheet.name,
                                            fontWeight = if (selectedSheetIndex == index) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. 2D SPREADSHEET GRID
        // ==========================================
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = excelGreen)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Parsing Excel worksheets...", fontSize = 14.sp, color = secondaryTextColor)
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
                            text = errorMessage ?: "Error loading spreadsheet",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                workbook != null -> {
                    val activeSheet = workbook!!.sheets.getOrNull(selectedSheetIndex) ?: workbook!!.sheets.first()
                    val totalCols = activeSheet.columnCount.coerceAtLeast(1)
                    val colWidth = 110.dp
                    val rowNumWidth = 44.dp
                    val rowHeight = 36.dp

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        // Sticky Column Headers (A, B, C, D...)
                        Row(
                            modifier = Modifier
                                .background(headerBgColor)
                                .height(rowHeight)
                        ) {
                            // Top-left corner cell
                            Box(
                                modifier = Modifier
                                    .width(rowNumWidth)
                                    .height(rowHeight)
                                    .border(0.5.dp, gridBorderColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = secondaryTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Column Letters
                            for (c in 0 until totalCols) {
                                val colLetter = getColumnLetter(c)
                                Box(
                                    modifier = Modifier
                                        .width(colWidth)
                                        .height(rowHeight)
                                        .border(0.5.dp, gridBorderColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = colLetter,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = textColor
                                    )
                                }
                            }
                        }

                        // Data Rows with Vertical Scroll
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(activeSheet.rows) { rIdx, row ->
                                val rowNum = rIdx + 1
                                Row(modifier = Modifier.height(rowHeight)) {
                                    // Row number header cell (1, 2, 3...)
                                    Box(
                                        modifier = Modifier
                                            .width(rowNumWidth)
                                            .height(rowHeight)
                                            .background(headerBgColor)
                                            .border(0.5.dp, gridBorderColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$rowNum",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = secondaryTextColor
                                        )
                                    }

                                    // Row cells
                                    for (c in 0 until totalCols) {
                                        val cellVal = row.getOrNull(c) ?: ""
                                        val colLetter = getColumnLetter(c)
                                        val cellCoord = "$colLetter$rowNum"
                                        val isSelected = selectedCellAddress == cellCoord

                                        Box(
                                            modifier = Modifier
                                                .width(colWidth)
                                                .height(rowHeight)
                                                .background(
                                                    when {
                                                        isSelected -> excelGreen.copy(alpha = 0.2f)
                                                        rIdx % 2 == 1 -> if (isDarkTheme) Color(0xFF162238) else Color(0xFFF8FAFC)
                                                        else -> paperColor
                                                    }
                                                )
                                                .border(
                                                    if (isSelected) 1.5.dp else 0.5.dp,
                                                    if (isSelected) excelGreen else gridBorderColor
                                                )
                                                .clickable {
                                                    selectedCellAddress = cellCoord
                                                    selectedCellValue = cellVal
                                                }
                                                .padding(horizontal = 8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = cellVal,
                                                fontSize = 12.sp,
                                                color = textColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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

private fun getColumnLetter(colIndex: Int): String {
    var c = colIndex
    val sb = java.lang.StringBuilder()
    while (c >= 0) {
        sb.insert(0, ('A'.code + (c % 26)).toChar())
        c = (c / 26) - 1
    }
    return sb.toString()
}
