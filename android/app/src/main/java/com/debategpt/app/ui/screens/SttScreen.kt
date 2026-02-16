package com.debategpt.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    var selectedTab by remember { mutableStateOf(0) } // 0 = Debate, 1 = Analysis

    LaunchedEffect(sttState.transcriptTurns.size) {
        if (sttState.transcriptTurns.isNotEmpty()) {
            listState.animateScrollToItem(sttState.transcriptTurns.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        analysisViewModel.resetAnalysisState()
        analysisViewModel.resetWinnerState()
    }

    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("STT Debate", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Record & transcribe",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    titleContentColor = colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                OutlinedTextField(
                    value = sttState.topic,
                    onValueChange = { sttViewModel.setTopic(it) },
                    label = { Text("Debate Topic") },
                    placeholder = { Text("e.g. Should AI replace teachers?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = sttState.transcriptTurns.isEmpty(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pill(
                            text = if (sttState.isDebateEnded) "Debate ended" else "Turn: User ${sttState.currentUser}",
                            container = if (sttState.isDebateEnded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                            content = if (sttState.isDebateEnded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (sttState.isRecording) {
                            Pill(
                                text = "Recording",
                                container = MaterialTheme.colorScheme.errorContainer,
                                content = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else if (sttState.isLoading) {
                            Pill(
                                text = "Transcribing",
                                container = MaterialTheme.colorScheme.secondaryContainer,
                                content = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Text(
                        text = "${sttState.transcriptTurns.size} turns",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = { sttViewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = colorScheme.primary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Debate") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Analysis") })
            }

            // Main content
            when (selectedTab) {
                0 -> DebateTabContent(
                    transcriptTurns = sttState.transcriptTurns,
                    fullTranscriptText = sttState.fullTranscriptText,
                    listState = listState,
                    modifier = Modifier.weight(1f)
                )
                else -> AnalysisTabContent(
                    canAnalyze = sttState.transcriptTurns.size >= 2 || sttState.isDebateEnded,
                    analysisState = analysisState,
                    winnerState = winnerState,
                    onRunAnalysis = { analysisViewModel.analyzeStt() },
                    onGetWinner = { analysisViewModel.getWinnerStt() },
                    modifier = Modifier.weight(1f)
                )
            }

            // Bottom controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Controls",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.primary
                    )

                    if (!sttState.isDebateEnded) {
                        if (!sttState.isRecording) {
                            Button(
                                onClick = { sttViewModel.startRecording(context.applicationContext) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !sttState.isLoading,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Start Recording (User ${sttState.currentUser})")
                            }
                            Text(
                                "Speak clearly for at least 2 seconds, then tap Stop.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        } else {
                            Button(
                                onClick = { sttViewModel.stopAndTranscribe() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Stop & Transcribe")
                            }
                            Text(
                                "Recording… speak now, then tap Stop (min 2 sec).",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.primary
                            )
                        }

                        if (sttState.transcriptTurns.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { sttViewModel.stopWholeDebate() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Stop whole debate")
                            }
                        }
                    }

                    if (sttState.isLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Transcribing...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    OutlinedButton(
                        onClick = { sttViewModel.startNewDebate() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("New Debate")
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
            .fillMaxWidth(),
        horizontalArrangement = if (isUser1) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 48.dp, max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser1) 4.dp else 16.dp,
                bottomEnd = if (isUser1) 16.dp else 4.dp
            ),
            color = if (isUser1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Pill(
                    text = "User ${turn.user}",
                    container = MaterialTheme.colorScheme.surface,
                    content = MaterialTheme.colorScheme.onSurface
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

@Composable
private fun DebateTabContent(
    transcriptTurns: List<TranscriptTurn>,
    fullTranscriptText: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    if (transcriptTurns.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Start recording to build the debate transcript.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "User 1 speaks first, then User 2.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionTitle("Transcript") }
        items(
            count = transcriptTurns.size,
            key = { index -> "turn_$index" }
        ) { index ->
            TranscriptBubble(turn = transcriptTurns[index])
        }

        if (fullTranscriptText.isNotBlank()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ExpandableTextCard(
                    title = "Full transcript (both users)",
                    text = fullTranscriptText,
                    monospace = false
                )
            }
        }
    }
}

@Composable
fun AnalysisTabContent(
    canAnalyze: Boolean,
    analysisState: com.debategpt.app.ui.viewmodel.AnalysisUiState,
    winnerState: com.debategpt.app.ui.viewmodel.WinnerUiState,
    onRunAnalysis: () -> Unit,
    onGetWinner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("Analyzer")

        if (!canAnalyze) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Add at least 2 turns (User 1 + User 2) to run analysis.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return
        }

        analysisState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = onRunAnalysis,
            modifier = Modifier.fillMaxWidth(),
            enabled = !analysisState.isLoading
        ) {
            if (analysisState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (analysisState.isLoading) "Analyzing..." else "Run Analysis")
        }

        if (analysisState.analysisSuccess) {
            Text(
                "✓ ${analysisState.sentencesAnalyzed ?: 0} sentences analyzed",
                color = MaterialTheme.colorScheme.primary
            )

            analysisState.marking?.takeIf { it.isNotEmpty() }?.let { marking ->
                SectionTitle("Marking points")
                MarkingPointsCardDynamic(marking = marking)
            }

            analysisState.stats?.takeIf { it.isNotEmpty() }?.let { stats ->
                SectionTitle("Sentence types summary")
                if (stats.size <= 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        stats.entries.forEach { (userName, userStats) ->
                            UserStatsCard(
                                title = userName,
                                stats = userStats,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        stats.entries.forEach { (userName, userStats) ->
                            UserStatsCard(
                                title = userName,
                                stats = userStats,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            if (analysisState.analysisSuccess && analysisState.analysisText.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Detailed analysis text was not returned from the server. You may still see marking and stats above.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            analysisState.analysisText?.takeIf { it.isNotBlank() }?.let { analysisText ->
                val parsed = remember(analysisText) { parseDebateFinalAnalysisText(analysisText) }

                parsed.correctedTranscript?.takeIf { it.isNotBlank() }?.let { corrected ->
                    SectionTitle("Corrected transcript")
                    ExpandableTextCard(
                        title = "Corrected transcript (readable)",
                        text = corrected,
                        monospace = false
                    )
                }

                if (parsed.sentenceItems.isNotEmpty()) {
                    SectionTitle("Sentence-wise analysis (easy to understand)")
                    parsed.sentenceItems.forEach { item ->
                        SentenceAnalysisCard(item)
                    }
                }

                // Keep the raw backend output for power users / debugging.
                ExpandableTextCard(
                    title = "Raw analysis text (same as debate_final_analysis.txt)",
                    text = analysisText,
                    monospace = true
                )
            }

            SectionTitle("Winner")
            winnerState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = onGetWinner,
                modifier = Modifier.fillMaxWidth(),
                enabled = !winnerState.isLoading
            ) {
                if (winnerState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (winnerState.isLoading) "Computing..." else "Get Winner")
            }

            winnerState.winner?.let { winner ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Winner", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("🏆 $winner", style = MaterialTheme.typography.headlineSmall)
                        winnerState.scores?.forEach { (user, score) ->
                            Text("$user: $score", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun Pill(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(container, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

@Composable
private fun ExpandableTextCard(
    title: String,
    text: String,
    monospace: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val maxLines = if (expanded) Int.MAX_VALUE else 10

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Show less" else "Show more")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                maxLines = maxLines,
                style = if (monospace) MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class ParsedFinalAnalysis(
    val correctedTranscript: String?,
    val sentenceItems: List<SentenceAnalysisItem>
)

private data class SentenceAnalysisItem(
    val index: Int,
    val correctedText: String,
    val speaker: String?,
    val sentiment: String?,
    val sentimentConfidence: Double?,
    val argumentType: String?,
    val argumentConfidence: Double?,
    val detectedBy: String?
)

private fun parseDebateFinalAnalysisText(text: String): ParsedFinalAnalysis {
    val lines = text.split("\n")

    fun indexOfLineStartsWith(prefix: String): Int =
        lines.indexOfFirst { it.trim().startsWith(prefix) }

    // ---- Corrected transcript section ----
    val correctedStart = indexOfLineStartsWith("CORRECTED TRANSCRIPT:")
    val sentenceWiseStart = indexOfLineStartsWith("SENTENCE-WISE ANALYSIS:")

    val correctedTranscript = if (correctedStart >= 0 && sentenceWiseStart > correctedStart) {
        // Skip the dashed line after the header if present
        val start = (correctedStart + 1).coerceAtMost(lines.lastIndex)
        val rawBlock = lines.subList(start, sentenceWiseStart).joinToString("\n")
        rawBlock
            .trim()
            .trimStart('-')
            .trim()
            .ifBlank { null }
    } else {
        null
    }

    // ---- Sentence-wise items ----
    val items = mutableListOf<SentenceAnalysisItem>()
    var currentIndex: Int? = null
    var correctedLines = mutableListOf<String>()
    var collectingCorrectedText = false
    var sentiment: String? = null
    var sentimentConfidence: Double? = null
    var argumentType: String? = null
    var argumentConfidence: Double? = null
    var detectedBy: String? = null

    fun flush() {
        val idx = currentIndex ?: return
        val correctedText = correctedLines.joinToString("\n").trim()
        val speaker = when {
            correctedText.contains("User 1:") -> "User 1"
            correctedText.contains("User 2:") -> "User 2"
            correctedText.contains("USER:") -> "USER"
            correctedText.contains("DEBATE GPT:") -> "DEBATE GPT"
            else -> null
        }
        if (correctedText.isNotBlank()) {
            items.add(
                SentenceAnalysisItem(
                    index = idx,
                    correctedText = correctedText,
                    speaker = speaker,
                    sentiment = sentiment,
                    sentimentConfidence = sentimentConfidence,
                    argumentType = argumentType,
                    argumentConfidence = argumentConfidence,
                    detectedBy = detectedBy
                )
            )
        }
    }

    val scanStart = if (sentenceWiseStart >= 0) sentenceWiseStart else 0
    for (i in scanStart until lines.size) {
        val raw = lines[i]
        val line = raw.trimEnd()
        val t = line.trim()

        if (t.startsWith("Sentence ") && t.endsWith(":")) {
            // New sentence block begins
            flush()

            val number = t.removePrefix("Sentence ")
                .removeSuffix(":")
                .trim()
                .toIntOrNull()

            currentIndex = number
            correctedLines = mutableListOf()
            collectingCorrectedText = false
            sentiment = null
            sentimentConfidence = null
            argumentType = null
            argumentConfidence = null
            detectedBy = null
            continue
        }

        if (currentIndex == null) continue

        if (t.startsWith("Corrected Text")) {
            collectingCorrectedText = true
            val parts = t.split(":", limit = 2)
            val after = if (parts.size == 2) parts[1].trim() else ""
            correctedLines.add(after)
            continue
        }

        if (collectingCorrectedText) {
            // Corrected text can span multiple lines until we hit "Sentiment"
            if (t.startsWith("Sentiment")) {
                collectingCorrectedText = false
                val parts = t.split(":", limit = 2)
                sentiment = if (parts.size == 2) parts[1].trim() else null
            } else if (!t.startsWith("-")) {
                // Preserve the text, ignore separator lines
                correctedLines.add(line)
            }
            continue
        }

        if (t.startsWith("Confidence")) {
            val parts = t.split(":", limit = 2)
            sentimentConfidence = if (parts.size == 2) parts[1].trim().toDoubleOrNull() else null
        } else if (t.startsWith("Argument Type")) {
            val parts = t.split(":", limit = 2)
            argumentType = if (parts.size == 2) parts[1].trim() else null
        } else if (t.startsWith("Arg Confidence")) {
            val parts = t.split(":", limit = 2)
            argumentConfidence = if (parts.size == 2) parts[1].trim().toDoubleOrNull() else null
        } else if (t.startsWith("Detected By")) {
            val parts = t.split(":", limit = 2)
            detectedBy = if (parts.size == 2) parts[1].trim() else null
        }
    }
    flush()

    return ParsedFinalAnalysis(
        correctedTranscript = correctedTranscript,
        sentenceItems = items
    )
}

@Composable
private fun SentenceAnalysisCard(item: SentenceAnalysisItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sentence ${item.index}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                item.speaker?.let { speaker ->
                    Pill(
                        text = speaker,
                        container = MaterialTheme.colorScheme.surface,
                        content = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = item.correctedText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Clear, user-friendly labels for types
            item.sentiment?.let { s ->
                val conf = item.sentimentConfidence
                KeyValueLine(
                    keyText = "Sentiment",
                    valueText = if (conf != null) "$s  (confidence ${"%.3f".format(conf)})" else s,
                    badgeColor = sentimentBadgeColor(s)
                )
            }
            item.argumentType?.let { a ->
                val conf = item.argumentConfidence
                val by = item.detectedBy
                val extra = buildString {
                    if (conf != null) append("confidence ${"%.3f".format(conf)}")
                    if (!by.isNullOrBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append("detected by $by")
                    }
                }
                KeyValueLine(
                    keyText = "Argument type",
                    valueText = if (extra.isNotBlank()) "$a  ($extra)" else a,
                    badgeColor = argumentBadgeColor(a)
                )
            }
        }
    }
}

@Composable
private fun KeyValueLine(keyText: String, valueText: String, badgeColor: androidx.compose.ui.graphics.Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(keyText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(badgeColor, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    valueText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun sentimentBadgeColor(sentiment: String): androidx.compose.ui.graphics.Color {
    return when (sentiment.uppercase()) {
        "POSITIVE" -> MaterialTheme.colorScheme.primaryContainer
        "NEGATIVE" -> MaterialTheme.colorScheme.errorContainer
        "NEUTRAL" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.outlineVariant
    }
}

@Composable
private fun argumentBadgeColor(argument: String): androidx.compose.ui.graphics.Color {
    return when (argument) {
        "Claim", "Evidence" -> MaterialTheme.colorScheme.secondaryContainer
        "Rebuttal" -> MaterialTheme.colorScheme.tertiaryContainer
        "Statement" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.outlineVariant
    }
}

@Composable
@Composable
private fun MarkingPointsCardDynamic(
    marking: Map<String, com.debategpt.app.data.MarkingPoints>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            marking.entries.forEachIndexed { index, (userName, points) ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(userName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Total: ${points.total}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                MarkingBreakdownRow("Sentiment", points.sentiment_points)
                MarkingBreakdownRow("Argument", points.argument_points)
            }
        }
    }
}

@Composable
private fun MarkingPointsCard(
    user1: com.debategpt.app.data.MarkingPoints?,
    user2: com.debategpt.app.data.MarkingPoints?
) {
    val marking = buildMap {
        user1?.let { put("User 1", it) }
        user2?.let { put("User 2", it) }
    }
    if (marking.isNotEmpty()) {
        MarkingPointsCardDynamic(marking = marking)
    }
}

@Composable
private fun MarkingBreakdownRow(label: String, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UserStatsCard(
    title: String,
    stats: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val sentimentOrder = listOf("POSITIVE", "NEGATIVE", "NEUTRAL")
    val argumentOrder = listOf("Claim", "Evidence", "Rebuttal", "Statement")

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("Sentiment", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            sentimentOrder.forEach { key ->
                StatRow(label = key, value = stats[key] ?: 0, highlight = valueBadgeColor(key))
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Argument type", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            argumentOrder.forEach { key ->
                StatRow(label = key, value = stats[key] ?: 0, highlight = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: Int, highlight: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .background(highlight, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun valueBadgeColor(label: String): androidx.compose.ui.graphics.Color {
    return when (label) {
        "POSITIVE" -> MaterialTheme.colorScheme.primaryContainer
        "NEGATIVE" -> MaterialTheme.colorScheme.errorContainer
        "NEUTRAL" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.outlineVariant
    }
}
