package com.familymoney.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin Groq Cloud client (OpenAI-compatible chat completions endpoint).
 *
 * Several keys are supplied so a rate-limited or revoked key rotates to the next
 * one instead of breaking the assistant.
 */
class GroqClient(
    private val apiKeys: List<String>,
    private val model: String = DEFAULT_MODEL,
    private val fallbackModel: String = FALLBACK_MODEL
) {

    companion object {
        const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
        const val DEFAULT_MODEL = "openai/gpt-oss-120b"
        const val FALLBACK_MODEL = "qwen/qwen3.8-27b"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    @Serializable
    data class Message(val role: String, val content: String)

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double = 0.3,
        @SerialName("max_tokens") val maxTokens: Int = 1400,
        val stream: Boolean = false
    )

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList()) {
        @Serializable data class Choice(val message: Message? = null, @SerialName("finish_reason") val finishReason: String? = null)
    }

    sealed interface Result {
        data class Ok(val text: String) : Result
        data class Error(val message: String, val recoverable: Boolean) : Result
    }

    suspend fun chat(messages: List<Message>, temperature: Double = 0.3): Result =
        withContext(Dispatchers.IO) {
            if (apiKeys.isEmpty()) {
                return@withContext Result.Error("לא הוגדר מפתח AI בהגדרות.", recoverable = false)
            }

            var lastError = "שגיאה לא ידועה"
            for (useFallback in listOf(false, true)) {
                val chosenModel = if (useFallback) fallbackModel else model
                for (key in apiKeys) {
                    when (val r = call(key, chosenModel, messages, temperature)) {
                        is Result.Ok -> return@withContext r
                        is Result.Error -> {
                            lastError = r.message
                            if (!r.recoverable) return@withContext r
                        }
                    }
                }
            }
            Result.Error(lastError, recoverable = true)
        }

    private fun call(
        apiKey: String,
        modelId: String,
        messages: List<Message>,
        temperature: Double
    ): Result {
        val payload = json.encodeToString(
            ChatRequest.serializer(),
            ChatRequest(model = modelId, messages = messages, temperature = temperature)
        )

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody(mediaType))
            .build()

        return try {
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> {
                        val parsed = json.decodeFromString(ChatResponse.serializer(), body)
                        val text = parsed.choices.firstOrNull()?.message?.content?.trim()
                        if (text.isNullOrBlank()) {
                            Result.Error("התקבלה תשובה ריקה מהשרת.", recoverable = true)
                        } else {
                            Result.Ok(stripReasoning(text))
                        }
                    }
                    // Bad key / quota: try the next key.
                    response.code == 401 || response.code == 403 || response.code == 429 ->
                        Result.Error("מפתח ה־AI נדחה או חרג ממכסה (${response.code}).", recoverable = true)
                    // Unknown model: let the caller fall back to the other model.
                    response.code == 404 || response.code == 400 ->
                        Result.Error("הבקשה נדחתה (${response.code}).", recoverable = true)
                    else ->
                        Result.Error("שגיאת שרת ${response.code}.", recoverable = true)
                }
            }
        } catch (e: IOException) {
            Result.Error("אין חיבור לאינטרנט. נסה שוב.", recoverable = true)
        } catch (e: Exception) {
            Result.Error("שגיאה: ${e.message ?: "לא ידועה"}", recoverable = true)
        }
    }

    /**
     * Open-weight models emit a <think> block and Markdown emphasis. Compose
     * renders text literally, so both are stripped before display — otherwise
     * the user sees raw `**asterisks**` in the answer.
     */
    private fun stripReasoning(text: String): String =
        text.replace(Regex("(?s)<think>.*?</think>"), "")
            .replace(Regex("(?s)^<\\|channel\\|>.*?<\\|message\\|>"), "")
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
            .replace(Regex("__(.+?)__"), "$1")
            .replace(Regex("(?m)^#{1,6}\\s+"), "")
            .replace(Regex("(?m)^\\s*[-*]\\s+"), "• ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
}
