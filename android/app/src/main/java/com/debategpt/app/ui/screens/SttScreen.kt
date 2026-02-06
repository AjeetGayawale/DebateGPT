package com.debategpt.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.debategpt.app.ui.viewmodel.AnalysisViewModel
import com.debategpt.app.ui.viewmodel.SttViewModel
import com.debategpt.app.ui.viewmodel.TranscriptTurn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SttScreen(
    navController: NavController,
    sttViewModel: SttViewModel = viewModel(),
    analysisViewModel: AnalysisViewModel = viewModel()
) {
    val sttState by sttViewModel.uiState.collectAsState()
    val analysisState by analysisViewModel.analysisState.collectAsState()
    val winnerState by analysisViewModel.winnerState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(sttState.transcriptTurns.size) {
        if (sttState.transcriptTurns.isNotEmpty()) {
            listState.animateScrollToItem(sttState.transcriptTurns.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        analysisViewModel.resetAnalysisState()
        analysisViewModel.resetWinnerState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("STT Debate") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = sttState.topic,
                onValueChange = { sttViewModel.setTopic(it) },
                label = { Text("Debate Topic") },
                placeholder = { Text("e.g. Should AI replace teachers?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                enabled = sttState.transcriptTurns.isEmpty()
            )

            sttState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.weight(1f).padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = { sttViewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            if (sttState.transcriptTurns.isNotEmpty()) {
                Text(
                    text = "Transcript",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        count = sttState.transcriptTurns.size,
                        key = { index -> "turn_$index" }
                    ) { index ->
                        TranscriptBubble(turn = sttState.transcriptTurns[index])
                    }
                    if (sttState.fullTranscriptText.isNotBlank()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Full transcript (both users)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = sttState.fullTranscriptText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Start recording to build the debate transcript.\nUser 1 speaks first, then User 2.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (!sttState.isDebateEnded) {
                    Text(
                        text = "Now speaking: User ${sttState.currentUser}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (!sttState.isRecording) {
                        Button(
                            onClick = { sttViewModel.startRecording(context.applicationContext) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !sttState.isLoading
                        ) {
                            Text("Start Recording (User ${sttState.currentUser})")
                        }
                        Text(
                            "Speak clearly for at least 2 seconds, then tap Stop.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    } else {
                        Button(
                            onClick = { sttViewModel.stopAndTranscribe() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Stop & Transcribe")
                        }
                        Text(
                            "Recording… speak now, then tap Stop (min 2 sec).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (sttState.transcriptTurns.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { sttViewModel.stopWholeDebate() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Stop whole debate")
                        }
                    }
                }

                if (sttState.isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transcribing...", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (sttState.transcriptTurns.size >= 2 || sttState.isDebateEnded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Analyzer & Winner",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { sttViewModel.startNewDebate() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("New Debate")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    analysisState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = { analysisViewModel.analyzeStt() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !analysisState.isLoading
                    ) {
                        if (analysisState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (analysisState.isLoading) "Analyzing..." else "Run Analysis")
                    }

                    if (analysisState.analysisSuccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "✓ ${analysisState.sentencesAnalyzed ?: 0} sentences analyzed",
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        winnerState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Button(
                            onClick = { analysisViewModel.getWinnerStt() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !winnerState.isLoading
                        ) {
                            if (winnerState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (winnerState.isLoading) "Computing..." else "Get Winner")
                        }

                        winnerState.winner?.let { winner ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text("🏆 Winner: $winner", style = MaterialTheme.typography.headlineSmall)
                                    winnerState.scores?.forEach { (user, score) ->
                                        Text("$user: $score")
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

@Composable
private fun TranscriptBubble(turn: TranscriptTurn) {
    val isUser1 = turn.user == 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser1) Arrangement.Start else Arrangement.End
    ) {
        Card(
            modifier = Modifier.widthIn(min = 48.dp, max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser1) 4.dp else 16.dp,
                bottomEnd = if (isUser1) 16.dp else 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "User ${turn.user}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUser1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = turn.text.ifBlank { "(no speech detected)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
