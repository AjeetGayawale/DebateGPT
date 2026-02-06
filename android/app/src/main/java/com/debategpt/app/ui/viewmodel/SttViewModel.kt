package com.debategpt.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.debategpt.app.data.ApiClient
import com.debategpt.app.util.AudioRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

data class TranscriptTurn(
    val user: Int,
    val text: String
)

data class SttUiState(
    val topic: String = "",
    val transcriptTurns: List<TranscriptTurn> = emptyList(),
    val fullTranscriptText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRecording: Boolean = false,
    val transcribeSuccess: Boolean = false,
    val currentUser: Int = 1,
    val turnCount: Int = 0,
    val isDebateEnded: Boolean = false
)

class SttViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SttUiState())
    val uiState: StateFlow<SttUiState> = _uiState.asStateFlow()

    private val audioRecorder = AudioRecorder()

    fun setTopic(topic: String) {
        _uiState.value = _uiState.value.copy(topic = topic)
    }

    fun startNewDebate() {
        _uiState.value = SttUiState(topic = _uiState.value.topic)
    }

    fun stopWholeDebate() {
        _uiState.value = _uiState.value.copy(isDebateEnded = true)
    }

    private fun formatTranscriptAsText(turns: List<TranscriptTurn>): String {
        return turns.joinToString("\n\n") { "User ${it.user}:\n${it.text}" }
    }

    /** Extract text for the most recent "User X:" segment (for the turn we just got back). */
    private fun extractLatestUserText(raw: String, user: Int): String {
        val marker = "User $user:"
        val idx = raw.lastIndexOf(marker)
        if (idx < 0) return raw.trim().takeIf { it.isNotBlank() } ?: "(transcribed)"
        var start = idx + marker.length
        while (start < raw.length && (raw[start] == ' ' || raw[start] == '\n')) start++
        var end = start
        while (end < raw.length) {
            if (raw.substring(end).startsWith("\n\nUser 1:") || raw.substring(end).startsWith("\n\nUser 2:") ||
                raw.substring(end).startsWith("\n\n=====") || raw.substring(end).startsWith("================================="))
                break
            end++
        }
        return raw.substring(start, end).trim().ifBlank { "(transcribed)" }
    }

    private fun parseTranscriptToTurns(raw: String): List<TranscriptTurn> {
        val turns = mutableListOf<TranscriptTurn>()
        val blocks = raw.split(Regex("(?=User [12]:)"))
        for (block in blocks) {
            val trimmed = block.trim()
            when {
                trimmed.startsWith("User 1:") -> {
                    val text = trimmed.removePrefix("User 1:").trim().lines()
                        .filter { it.isNotBlank() && !it.startsWith("=") && !it.startsWith("Topic") }
                        .joinToString(" ").trim()
                    if (text.isNotBlank()) turns.add(TranscriptTurn(1, text))
                }
                trimmed.startsWith("User 2:") -> {
                    val text = trimmed.removePrefix("User 2:").trim().lines()
                        .filter { it.isNotBlank() && !it.startsWith("=") && !it.startsWith("Topic") }
                        .joinToString(" ").trim()
                    if (text.isNotBlank()) turns.add(TranscriptTurn(2, text))
                }
            }
        }
        return turns
    }

    fun transcribeAudio(file: File, user: Int, isFirstTurn: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                // reset=true ONLY for first turn (User 1, empty transcript) - never reset for User 2
                val shouldReset = isFirstTurn && user == 1
                val response = ApiClient.api.transcribeAudio(
                    body,
                    user = user,
                    reset = shouldReset,
                    topic = _uiState.value.topic.takeIf { shouldReset }?.takeIf { it.isNotBlank() }
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val transcript = body?.transcript ?: ""
                    val existingTurns = _uiState.value.transcriptTurns
                    var parsedTurns = parseTranscriptToTurns(transcript)
                    if (parsedTurns.isEmpty() && transcript.isNotBlank()) {
                        val latestText = extractLatestUserText(transcript, user)
                        parsedTurns = existingTurns + TranscriptTurn(user, latestText)
                    } else if (parsedTurns.isEmpty()) {
                        parsedTurns = existingTurns + TranscriptTurn(user, "(no speech detected)")
                    }
                    val updatedTurns = parsedTurns
                    val fullText = formatTranscriptAsText(updatedTurns)
                    val nextUser = if (user == 1) 2 else 1
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        transcribeSuccess = true,
                        transcriptTurns = updatedTurns,
                        fullTranscriptText = fullText,
                        currentUser = nextUser,
                        turnCount = _uiState.value.turnCount + 1
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.message() ?: "Transcription failed"
                    )
                }
                file.delete()
            } catch (e: Exception) {
                val msg = e.message ?: "Network error"
                val friendlyMsg = if (msg.contains("ETIMEDOUT") || msg.contains("timed out") || msg.contains("failed to connect")) {
                    "Cannot reach backend. Ensure: 1) Backend runs with --host 0.0.0.0  2) Use correct URL (Test on Home first)  3) Same Wi‑Fi for device"
                } else msg
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = friendlyMsg
                )
            }
        }
    }

    fun startRecording(context: Context? = null) {
        _uiState.value = _uiState.value.copy(error = null)
        if (audioRecorder.startRecording(context)) {
            _uiState.value = _uiState.value.copy(isRecording = true)
        } else {
            _uiState.value = _uiState.value.copy(error = "Could not start recording. Check mic permission.")
        }
    }

    fun stopAndTranscribe() {
        viewModelScope.launch {
            audioRecorder.stopRecording()
                .onSuccess { file ->
                    _uiState.value = _uiState.value.copy(isRecording = false)
                    val currentUser = _uiState.value.currentUser
                    val isFirstTurn = _uiState.value.transcriptTurns.isEmpty()
                    transcribeAudio(file, currentUser, isFirstTurn)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isRecording = false,
                        error = it.message ?: "Recording failed. Speak longer before stopping."
                    )
                }
        }
    }

    fun resetState() {
        _uiState.value = SttUiState(topic = _uiState.value.topic)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
