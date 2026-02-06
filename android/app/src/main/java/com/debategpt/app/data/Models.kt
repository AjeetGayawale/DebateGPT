package com.debategpt.app.data

import com.google.gson.annotations.SerializedName

data class TranscribeResponse(
    val status: String,
    val message: String,
    val transcript: String?,
    val transcript_file: String?
)

data class AnalyzeResponse(
    val mode: String,
    val message: String,
    val output_file: String?,
    val sentences_analyzed: Int?
)

data class WinnerResponse(
    val status: String,
    val data: WinnerData?
)

data class WinnerData(
    val mode: String,
    val winner: String,
    val scores: Map<String, Double>,
    val output_file: String?
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
