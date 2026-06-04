package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- Data Classes ---

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

// --- API Service ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE })
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

// --- Helper Functions ---

fun Bitmap.toBase64(): String {
    val outputStream = ByteArrayOutputStream()
    // compress to reduce size to avoid token overflow
    compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

suspend fun analyzeImageWithGemini(context: Context, bitmap: Bitmap, prompt: String): String = withContext(Dispatchers.IO) {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var apiKey = prefs.getString("gemini_api_key", "")?.trim() ?: ""
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
        apiKey = BuildConfig.GEMINI_API_KEY.trim()
    }
    
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
        return@withContext "Error: Gemini API Key not configured. Please set it in Settings."
    }
    
    val fullPrompt = "You are a helpful inventory assistant. Describe the objects in the image accurately so the user can catalog them. Keep descriptions concise. \n\n" + prompt
    
    val requestBody = GenerateContentRequest(
        contents = listOf(Content(
            parts = listOf(
                Part(text = fullPrompt),
                Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64()))
            )
        ))
    )
    
    try {
        val response = GeminiClient.service.generateContent(apiKey, requestBody)
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No objects detected."
    } catch (e: retrofit2.HttpException) {
        val errorBody = e.response()?.errorBody()?.string() ?: ""
        if (e.code() == 400 || e.code() == 403 || e.code() == 404) {
            "Error: API Key is likely invalid or missing. Please configure a valid Gemini API Key in Settings. (${e.code()})"
        } else {
            "Error parsing image: HTTP ${e.code()} - $errorBody"
        }
    } catch (e: Exception) {
        "Error parsing image: ${e.message}"
    }
}
