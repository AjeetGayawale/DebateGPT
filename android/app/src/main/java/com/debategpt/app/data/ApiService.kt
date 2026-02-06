package com.debategpt.app.data

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("/")
    suspend fun ping(): retrofit2.Response<Map<String, String>>

    @Multipart
    @POST("stt/transcribe")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part,
        @Query("user") user: Int? = null,
        @Query("reset") reset: Boolean = false,
        @Query("topic") topic: String? = null
    ): Response<TranscribeResponse>

    @POST("analyze/stt")
    suspend fun analyzeStt(): Response<AnalyzeResponse>

    @POST("analyze/chatbot")
    suspend fun analyzeChatbot(): Response<AnalyzeResponse>

    @POST("winner/stt")
    suspend fun winnerStt(): Response<WinnerResponse>

    @POST("winner/chatbot")
    suspend fun winnerChatbot(): Response<WinnerResponse>

    @POST("chatbot/respond")
    suspend fun chatbotRespond(
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
