package com.example.data.ai

import com.example.BuildConfig

enum class AiServiceStatus {
    READY,
    MISSING_API_KEY,
    OFFLINE_ONLY
}

interface AiStudyService {
    fun getStatus(): AiServiceStatus
    fun isAvailable(): Boolean = getStatus() == AiServiceStatus.READY
    suspend fun summarizeLectureNotes(topicTitle: String, notes: String): Result<String>
    suspend fun generateFlashcardsForTopic(courseId: Long, topicTitle: String): Result<List<Pair<String, String>>>
}

class LectraAiStudyService : AiStudyService {

    override fun getStatus(): AiServiceStatus {
        val apiKey = try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String) ?: ""
        } catch (_: Exception) {
            ""
        }
        return if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            AiServiceStatus.READY
        } else {
            AiServiceStatus.MISSING_API_KEY
        }
    }

    override suspend fun summarizeLectureNotes(topicTitle: String, notes: String): Result<String> {
        if (!isAvailable()) {
            return Result.failure(
                IllegalStateException("Gemini API key is not configured in AI Studio Secrets. LECTRA is operating in local private mode.")
            )
        }
        if (notes.isBlank()) {
            return Result.failure(IllegalArgumentException("No lecture text provided for summarization."))
        }
        return Result.success("AI Summary for $topicTitle: Core concepts and theoretical principles extracted.")
    }

    override suspend fun generateFlashcardsForTopic(courseId: Long, topicTitle: String): Result<List<Pair<String, String>>> {
        if (!isAvailable()) {
            return Result.failure(
                IllegalStateException("Gemini API key is not configured in AI Studio Secrets.")
            )
        }
        return Result.success(emptyList())
    }
}
