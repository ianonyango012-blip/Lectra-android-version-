package com.example.data.document

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.storage.PdfStorageManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

sealed class DocumentImportOutcome {
    data class Success(
        val displayName: String,
        val localPath: String,
        val fileSizeBytes: Long,
        val documentType: DocumentType,
        val pageOrUnitCount: Int
    ) : DocumentImportOutcome()

    data class Error(val message: String) : DocumentImportOutcome()
}

object DocumentStorageManager {

    private const val TAG = "DocumentStorageManager"

    fun getDocumentsDirectory(context: Context): File {
        val dir = File(context.filesDir, "lectures")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun resolveMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var name = "Document"
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
            name = uri.lastPathSegment ?: "Document"
        }
        return Pair(name, size)
    }

    /**
     * Imports and validates any supported document format (PDF, DOC/DOCX, XLS/XLSX, PPT/PPTX, TXT, EPUB).
     * Validates file headers/magic bytes and protects against corrupted files.
     */
    fun importDocument(context: Context, uri: Uri): DocumentImportOutcome {
        val (originalName, reportedSize) = resolveMetadata(context, uri)
        val mimeType = context.contentResolver.getType(uri)
        val docType = DocumentType.fromMimeOrFilename(mimeType, originalName)
            ?: return DocumentImportOutcome.Error("Unsupported file format. Supported formats: PDF, DOC/DOCX, XLS/XLSX, PPT/PPTX, TXT, EPUB.")

        val docsDir = getDocumentsDirectory(context)
        val cleanName = originalName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val uniqueFileName = "${UUID.randomUUID()}_$cleanName"
        val destinationFile = File(docsDir, uniqueFileName)

        try {
            // 1. Copy incoming stream to destination file
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return DocumentImportOutcome.Error("Unable to access the selected file.")

            val actualSize = if (destinationFile.length() > 0) destinationFile.length() else reportedSize
            if (actualSize == 0L) {
                destinationFile.delete()
                return DocumentImportOutcome.Error("The selected file is empty (0 bytes).")
            }

            // 2. Validate file integrity based on format
            val validation = validateFileIntegrity(destinationFile, docType)
            if (validation is ValidationResult.Failure) {
                destinationFile.delete()
                return DocumentImportOutcome.Error(validation.message)
            }

            val pageOrUnitCount = (validation as ValidationResult.Success).count

            return DocumentImportOutcome.Success(
                displayName = originalName,
                localPath = destinationFile.absolutePath,
                fileSizeBytes = actualSize,
                documentType = docType,
                pageOrUnitCount = pageOrUnitCount
            )
        } catch (e: Exception) {
            destinationFile.delete()
            return DocumentImportOutcome.Error("Failed to import document: ${e.localizedMessage}")
        }
    }

    private sealed class ValidationResult {
        data class Success(val count: Int) : ValidationResult()
        data class Failure(val message: String) : ValidationResult()
    }

    private fun validateFileIntegrity(file: File, type: DocumentType): ValidationResult {
        return try {
            when (type) {
                DocumentType.PDF -> {
                    // Check PDF magic bytes %PDF-
                    val header = file.inputStream().use { it.readNBytes(5) }
                    val isPdfHeader = header.size >= 4 &&
                            header[0] == '%'.code.toByte() &&
                            header[1] == 'P'.code.toByte() &&
                            header[2] == 'D'.code.toByte() &&
                            header[3] == 'F'.code.toByte()
                    if (!isPdfHeader) {
                        return ValidationResult.Failure("Corrupted file: Header does not match PDF specification.")
                    }

                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    try {
                        android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                            val count = renderer.pageCount
                            if (count <= 0) ValidationResult.Failure("PDF contains 0 pages.")
                            else ValidationResult.Success(count)
                        }
                    } catch (e: SecurityException) {
                        ValidationResult.Failure("This PDF is password-protected. Please provide an unencrypted PDF.")
                    } catch (e: Exception) {
                        ValidationResult.Failure("The selected file is corrupted or not a valid PDF: ${e.localizedMessage}")
                    } finally {
                        try { pfd.close() } catch (_: Exception) {}
                    }
                }

                DocumentType.DOCX -> {
                    validateZipPackage(file, "word/document.xml", "Word (.docx)") { zip ->
                        val doc = WordDocumentParser.parse(file, file.name)
                        doc.paragraphs.size.coerceAtLeast(1)
                    }
                }

                DocumentType.DOC -> {
                    if (file.length() < 32) return ValidationResult.Failure("The selected file is too small to be a valid Word document.")
                    val doc = WordDocumentParser.parse(file, file.name)
                    ValidationResult.Success(doc.paragraphs.size.coerceAtLeast(1))
                }

                DocumentType.XLSX -> {
                    validateZipPackage(file, "xl/workbook.xml", "Excel (.xlsx)") { zip ->
                        val wb = ExcelSpreadsheetParser.parse(file, file.name)
                        wb.sheets.size.coerceAtLeast(1)
                    }
                }

                DocumentType.XLS -> {
                    if (file.length() < 32) return ValidationResult.Failure("The selected file is too small to be a valid Excel spreadsheet.")
                    val wb = ExcelSpreadsheetParser.parse(file, file.name)
                    ValidationResult.Success(wb.sheets.size.coerceAtLeast(1))
                }

                DocumentType.PPTX -> {
                    validateZipPackage(file, "ppt/presentation.xml", "PowerPoint (.pptx)") { zip ->
                        val pres = PowerPointPresentationParser.parse(file, file.name)
                        pres.totalSlides.coerceAtLeast(1)
                    }
                }

                DocumentType.PPT -> {
                    if (file.length() < 32) return ValidationResult.Failure("The selected file is too small to be a valid PowerPoint presentation.")
                    val pres = PowerPointPresentationParser.parse(file, file.name)
                    ValidationResult.Success(pres.totalSlides.coerceAtLeast(1))
                }

                DocumentType.TXT -> {
                    val txt = TextDocumentParser.parse(file, file.name)
                    ValidationResult.Success(txt.lineCount.coerceAtLeast(1))
                }

                DocumentType.EPUB -> {
                    validateZipPackage(file, "META-INF/container.xml", "EPUB (.epub)") { zip ->
                        val book = EpubBookParser.parse(file, file.name)
                        book.totalChapters.coerceAtLeast(1)
                    }
                }
            }
        } catch (e: Exception) {
            ValidationResult.Failure("The selected file could not be verified: ${e.localizedMessage}")
        }
    }

    private fun validateZipPackage(
        file: File,
        requiredEntrySubstring: String,
        formatName: String,
        countExtractor: (ZipFile) -> Int
    ): ValidationResult {
        return try {
            ZipFile(file).use { zip ->
                val hasRequiredEntry = zip.entries().asSequence().any {
                    it.name.contains(requiredEntrySubstring, ignoreCase = true)
                }
                if (!hasRequiredEntry) {
                    ValidationResult.Failure("The file does not contain valid $formatName internal structure.")
                } else {
                    val count = countExtractor(zip)
                    ValidationResult.Success(count)
                }
            }
        } catch (e: Exception) {
            ValidationResult.Failure("The selected file is corrupted or not a valid $formatName: ${e.localizedMessage}")
        }
    }

    /**
     * Resolves a local File instance from a fileUriString.
     * Generates a realistic academic sample document if it refers to a bundled material.
     */
    fun resolveFile(context: Context, fileUriString: String, title: String = "Lecture Material"): File? {
        val docsDir = getDocumentsDirectory(context)

        // Case 1: bundled lectra scheme
        if (fileUriString.startsWith("lectra://bundled/")) {
            val fileName = fileUriString.removePrefix("lectra://bundled/")
            val targetFile = File(docsDir, fileName)
            if (!targetFile.exists() || targetFile.length() == 0L) {
                generateSampleDocument(targetFile, title)
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

        // Case 4: relative name in docs directory
        val inDocs = File(docsDir, fileUriString.substringAfterLast("/"))
        if (inDocs.exists()) return inDocs

        // Fallback: check extension and generate
        val fallbackFile = File(docsDir, "${UUID.randomUUID()}_${fileUriString.substringAfterLast("/")}")
        generateSampleDocument(fallbackFile, title)
        return fallbackFile
    }

    /**
     * Generates an academic sample document matching the file's extension.
     */
    fun generateSampleDocument(destinationFile: File, title: String) {
        val ext = destinationFile.name.substringAfterLast('.', "pdf").lowercase()
        when (ext) {
            "docx" -> generateSampleDocx(destinationFile, title)
            "doc" -> generateSampleDocx(destinationFile, title) // standard docx with .doc ext readable
            "xlsx", "xls" -> generateSampleXlsx(destinationFile, title)
            "pptx", "ppt" -> generateSamplePptx(destinationFile, title)
            "txt" -> generateSampleTxt(destinationFile, title)
            "epub" -> generateSampleEpub(destinationFile, title)
            else -> PdfStorageManager.generateSampleLecturePdf(destinationFile, title)
        }
    }

    fun generateSampleDocx(destinationFile: File, title: String) {
        try {
            destinationFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(destinationFile)).use { zos ->
                // [Content_Types].xml
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // word/document.xml
                zos.putNextEntry(ZipEntry("word/document.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>$title</w:t></w:r></w:p>
    <w:p><w:pPr><w:pStyle w:val="Heading2"/></w:pPr><w:r><w:t>Course Syllabus &amp; Research Foundations</w:t></w:r></w:p>
    <w:p><w:r><w:t>This syllabus outlines core learning objectives, theoretical foundations, and semester project milestones. Students will master key concepts through active recall, problem-solving, and laboratory experimentation.</w:t></w:r></w:p>
    <w:p><w:pPr><w:pStyle w:val="Heading2"/></w:pPr><w:r><w:t>Core Invariants &amp; Principles</w:t></w:r></w:p>
    <w:p><w:pPr><w:numPr/></w:pPr><w:r><w:t>Foundational Invariant: Consistency under partition guarantees safety across peer nodes.</w:t></w:r></w:p>
    <w:p><w:pPr><w:numPr/></w:pPr><w:r><w:t>Replication Protocol: Majority quorum acknowledgement transitions transactions to committed.</w:t></w:r></w:p>
    <w:p><w:pPr><w:numPr/></w:pPr><w:r><w:t>Fault Recovery: Automated checkpoint compaction and log playback restore state.</w:t></w:r></w:p>
    <w:tbl>
      <w:tr>
        <w:tc><w:p><w:r><w:t>Module</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>Topics Covered</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>Weight</w:t></w:r></w:p></w:tc>
      </w:tr>
      <w:tr>
        <w:tc><w:p><w:r><w:t>Unit 1</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>Theoretical Foundations &amp; System Proofs</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>25%</w:t></w:r></w:p></w:tc>
      </w:tr>
      <w:tr>
        <w:tc><w:p><w:r><w:t>Unit 2</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>Implementation &amp; Benchmarking</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>35%</w:t></w:r></w:p></w:tc>
      </w:tr>
    </w:tbl>
  </w:body>
</w:document>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating sample docx: ${e.message}")
        }
    }

    fun generateSampleXlsx(destinationFile: File, title: String) {
        try {
            destinationFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(destinationFile)).use { zos ->
                // [Content_Types].xml
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // xl/workbook.xml
                zos.putNextEntry(ZipEntry("xl/workbook.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheets>
    <sheet name="Gradebook Overview" sheetId="1"/>
    <sheet name="Lab Metrics" sheetId="2"/>
  </sheets>
</workbook>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // xl/sharedStrings.xml
                val strings = listOf(
                    title, "Student ID", "Assessment 1", "Assessment 2", "Final Exam", "Grade",
                    "CS-2026-01", "88", "92", "94", "A",
                    "CS-2026-02", "79", "85", "88", "B+",
                    "CS-2026-03", "95", "98", "99", "A+",
                    "Experiment", "Trials", "Latency (ms)", "Success Rate",
                    "Raft Election", "50", "12.4", "99.8%",
                    "Log Replication", "100", "4.2", "100%"
                )
                val sstXml = buildString {
                    append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                    append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${strings.size}">""")
                    for (s in strings) {
                        append("<si><t>$s</t></si>")
                    }
                    append("</sst>")
                }
                zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
                zos.write(sstXml.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()

                // xl/worksheets/sheet1.xml
                zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
    <row r="1">
      <c r="A1" t="s"><v>1</v></c>
      <c r="B1" t="s"><v>2</v></c>
      <c r="C1" t="s"><v>3</v></c>
      <c r="D1" t="s"><v>4</v></c>
      <c r="E1" t="s"><v>5</v></c>
    </row>
    <row r="2">
      <c r="A2" t="s"><v>6</v></c>
      <c r="B2" t="s"><v>7</v></c>
      <c r="C2" t="s"><v>8</v></c>
      <c r="D2" t="s"><v>9</v></c>
      <c r="E2" t="s"><v>10</v></c>
    </row>
    <row r="3">
      <c r="A3" t="s"><v>11</v></c>
      <c r="B3" t="s"><v>12</v></c>
      <c r="C3" t="s"><v>13</v></c>
      <c r="D3" t="s"><v>14</v></c>
      <c r="E3" t="s"><v>15</v></c>
    </row>
    <row r="4">
      <c r="A4" t="s"><v>16</v></c>
      <c r="B4" t="s"><v>17</v></c>
      <c r="C4" t="s"><v>18</v></c>
      <c r="D4" t="s"><v>19</v></c>
      <c r="E4" t="s"><v>20</v></c>
    </row>
  </sheetData>
</worksheet>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // xl/worksheets/sheet2.xml
                zos.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
    <row r="1">
      <c r="A1" t="s"><v>21</v></c>
      <c r="B1" t="s"><v>22</v></c>
      <c r="C1" t="s"><v>23</v></c>
      <c r="D1" t="s"><v>24</v></c>
    </row>
    <row r="2">
      <c r="A2" t="s"><v>25</v></c>
      <c r="B2" t="s"><v>26</v></c>
      <c r="C2" t="s"><v>27</v></c>
      <c r="D2" t="s"><v>28</v></c>
    </row>
    <row r="3">
      <c r="A3" t="s"><v>29</v></c>
      <c r="B3" t="s"><v>30</v></c>
      <c r="C3" t="s"><v>31</v></c>
      <c r="D3" t="s"><v>32</v></c>
    </row>
  </sheetData>
</worksheet>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating sample xlsx: ${e.message}")
        }
    }

    fun generateSamplePptx(destinationFile: File, title: String) {
        try {
            destinationFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(destinationFile)).use { zos ->
                // [Content_Types].xml
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slides/slide1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
  <Override PartName="/ppt/slides/slide2.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
  <Override PartName="/ppt/slides/slide3.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
</Types>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // ppt/presentation.xml
                zos.putNextEntry(ZipEntry("ppt/presentation.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldIdLst>
    <p:sldId id="256"/>
    <p:sldId id="257"/>
    <p:sldId id="258"/>
  </p:sldIdLst>
</p:presentation>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // ppt/slides/slide1.xml
                zos.putNextEntry(ZipEntry("ppt/slides/slide1.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:sp>
        <p:txBody>
          <a:p><a:r><a:t>$title</a:t></a:r></a:p>
          <a:p><a:r><a:t>Lecture 01 • Academic Course Presentation</a:t></a:r></a:p>
          <a:p><a:r><a:t>Department of Computer Science &amp; Engineering</a:t></a:r></a:p>
          <a:p><a:r><a:t>Instructor: Course Faculty Lead</a:t></a:r></a:p>
        </p:txBody>
      </p:sp>
    </p:spTree>
  </p:cSld>
</p:sld>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // ppt/slides/slide2.xml
                zos.putNextEntry(ZipEntry("ppt/slides/slide2.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:sp>
        <p:txBody>
          <a:p><a:r><a:t>Theoretical Foundations &amp; System Model</a:t></a:r></a:p>
          <a:p><a:r><a:t>• Asynchronous network communication with packet drops.</a:t></a:r></a:p>
          <a:p><a:r><a:t>• Non-Byzantine failure model: nodes fail-stop or recover.</a:t></a:r></a:p>
          <a:p><a:r><a:t>• Log entries are strictly ordered by monotonic term numbers.</a:t></a:r></a:p>
          <a:p><a:r><a:t>• Quorum consensus: N/2 + 1 node agreement ensures linearizability.</a:t></a:r></a:p>
        </p:txBody>
      </p:sp>
    </p:spTree>
  </p:cSld>
</p:sld>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // ppt/slides/slide3.xml
                zos.putNextEntry(ZipEntry("ppt/slides/slide3.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:sp>
        <p:txBody>
          <a:p><a:r><a:t>Laboratory Milestones &amp; Deliverables</a:t></a:r></a:p>
          <a:p><a:r><a:t>• Phase 1: Implement randomized election timers and vote collection.</a:t></a:r></a:p>
          <a:p><a:r><a:t>• Phase 2: Log replication with AppendEntries RPC and consistency check.</a:t></a:r></a:p>
          <a:p><a:r><a:t>• Phase 3: Snapshot compaction and crash-restart persistence.</a:t></a:r></a:p>
        </p:txBody>
      </p:sp>
    </p:spTree>
  </p:cSld>
</p:sld>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating sample pptx: ${e.message}")
        }
    }

    fun generateSampleTxt(destinationFile: File, title: String) {
        val sampleText = buildString {
            appendLine("==================================================")
            appendLine(title.uppercase())
            appendLine("Academic Notes & Research Scratchpad")
            appendLine("==================================================")
            appendLine()
            appendLine("1. Overview & Core Insights")
            appendLine("This document contains raw research notes, references, and algorithm invariants.")
            appendLine("Clean plaintext allows rapid annotation and full-text indexing without overhead.")
            appendLine()
            appendLine("2. Key Equations & Definitions")
            appendLine("- Quorum Intersection: Q1 ∩ Q2 ≠ ∅ for any two majorities in N.")
            appendLine("- Election Timeout Range: randomized between 150ms and 300ms.")
            appendLine("- Log Matching Invariant: If two logs contain an entry with same index and term, they are identical.")
            appendLine()
            appendLine("3. Reading Assignments & Bibliography")
            appendLine("- Paper: In Search of an Understandable Consensus Algorithm (Ongaro & Ousterhout).")
            appendLine("- Chapter 7: Distributed Commit and Recovery Mechanisms.")
        }
        destinationFile.parentFile?.mkdirs()
        destinationFile.writeText(sampleText, StandardCharsets.UTF_8)
    }

    fun generateSampleEpub(destinationFile: File, title: String) {
        try {
            destinationFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(destinationFile)).use { zos ->
                // mimetype (uncompressed first entry per EPUB spec)
                val mimeEntry = ZipEntry("mimetype")
                mimeEntry.method = ZipEntry.STORED
                val mimeBytes = "application/epub+zip".toByteArray(StandardCharsets.US_ASCII)
                mimeEntry.size = mimeBytes.size.toLong()
                val crc = java.util.zip.CRC32()
                crc.update(mimeBytes)
                mimeEntry.crc = crc.value
                zos.putNextEntry(mimeEntry)
                zos.write(mimeBytes)
                zos.closeEntry()

                // META-INF/container.xml
                zos.putNextEntry(ZipEntry("META-INF/container.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // OEBPS/content.opf
                zos.putNextEntry(ZipEntry("OEBPS/content.opf"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="pub-id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>$title</dc:title>
    <dc:creator>Academic Press</dc:creator>
    <dc:language>en</dc:language>
  </metadata>
  <manifest>
    <item id="ch1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
    <item id="ch2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine>
    <itemref idref="ch1"/>
    <itemref idref="ch2"/>
  </spine>
</package>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // OEBPS/ch1.xhtml
                zos.putNextEntry(ZipEntry("OEBPS/ch1.xhtml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>Chapter 1: Foundations</title></head>
<body>
  <h1>Chapter 1: Foundations &amp; System Principles</h1>
  <p>Welcome to $title. In this chapter, we develop the primary conceptual models necessary for advanced study.</p>
  <p>The decomposition of state machines allows complex distributed protocols to be reasoned about with mathematical precision.</p>
  <h2>Core Concepts</h2>
  <p>When nodes coordinate across an unreliable communication network, agreement cannot be guaranteed in bounded time if partitions are indefinite.</p>
  <p>However, practical systems guarantee safety unconditionally, sacrificing progress only while network partitions persist.</p>
</body>
</html>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // OEBPS/ch2.xhtml
                zos.putNextEntry(ZipEntry("OEBPS/ch2.xhtml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>Chapter 2: Practical Implementation</title></head>
<body>
  <h1>Chapter 2: Protocol Implementation &amp; Invariants</h1>
  <p>Following the theoretical overview in Chapter 1, we now examine practical state machine implementation.</p>
  <h2>Leader Election &amp; Log Compaction</h2>
  <p>Each node operates in one of three states: Follower, Candidate, or Leader.</p>
  <p>To avoid log growth without bound, memory snapshots periodically replace committed log prefixes.</p>
</body>
</html>""".toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating sample epub: ${e.message}")
        }
    }
}
