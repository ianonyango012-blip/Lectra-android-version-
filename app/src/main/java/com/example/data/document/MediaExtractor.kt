package com.example.data.document

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object MediaExtractor {
    private const val TAG = "MediaExtractor"

    fun extractMediaToCache(context: Context, file: File, mediaPathPrefix: String, cacheSubDir: String): Map<String, String> {
        val mediaMap = mutableMapOf<String, String>() // Path in ZIP to local path
        val cacheDir = File(context.cacheDir, cacheSubDir)
        if (!cacheDir.exists()) cacheDir.mkdirs()

        try {
            ZipFile(file).use { zip ->
                zip.entries().asSequence()
                    .filter { it.name.startsWith(mediaPathPrefix) }
                    .forEach { entry ->
                        val outputFile = File(cacheDir, entry.name.substringAfter(mediaPathPrefix))
                        if (!outputFile.exists()) {
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(outputFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        mediaMap[entry.name] = outputFile.absolutePath
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting media: ${e.message}", e)
        }
        return mediaMap
    }
}
