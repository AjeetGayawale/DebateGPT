package com.debategpt.app.data

import com.google.gson.annotations.SerializedName

data class TranscribeResponse(
    val status: String,
    val message: String,
    val transcript: String?,
    val transcript_file: String?
)

data class AnalyzeResponse(
    val mode: String? = null,
    val message: String? = null,
    val output_file: String? = null,
    val sentences_analyzed: Int? = null,
    @SerializedName("analysis_text") val analysisText: String? = null,
    val stats: Map<String, Map<String, Int>>? = null,
    val marking: Map<String, MarkingPoints>? = null
)

data class MarkingPoints(
    val total: Double,
    val sentiment_points: Double,
    val argument_points: Double
)

data class WinnerResponse(
    val status: String? = null,
    val data: WinnerData? = null
)

data class WinnerData(
    val mode: String? = null,
    val winner: String? = null,
    val scores: Map<String, Double>? = null,
    val output_file: String? = null
)

data class ChatRequest(
    val topic: String,
    val stance: String,
    val message: String
)

data class ChatResponse(
    val status: String,
    val reply: String
)
