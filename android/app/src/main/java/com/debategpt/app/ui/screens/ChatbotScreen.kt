package com.debategpt.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.debategpt.app.ui.viewmodel.ChatMessage
import com.debategpt.app.ui.viewmodel.ChatbotViewModel
import com.debategpt.app.ui.viewmodel.AnalysisViewModel

private val inputShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    navController: NavController,
    chatbotViewModel: ChatbotViewModel = viewModel(),
    analysisViewModel: AnalysisViewModel = viewModel()
) {
    val state by chatbotViewModel.uiState.collectAsState()
    val analysisState by analysisViewModel.analysisState.collectAsState()
    val winnerState by analysisViewModel.winnerState.collectAsState()
    val listState = rememberLazyListState()
    val colorScheme = MaterialTheme.colorScheme
    var selectedTab by remember { mutableStateOf(0) } // 0 = Chat, 1 = Analysis

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Chatbot Debate", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Debate with AI",
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
            // Tabs: Chat | Analysis (same pattern as STT module)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = colorScheme.primary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Chat") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Analysis") })
            }

            when (selectedTab) {
                0 -> {
                    // Chat tab: topic, messages, input
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            OutlinedTextField(
                                value = state.topic,
                                onValueChange = { chatbotViewModel.setTopic(it) },
                                label = { Text("Debate Topic") },
                                placeholder = { Text("e.g. Should AI replace teachers?") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(
                                    selected = state.stance == "favor",
                                    onClick = { chatbotViewModel.setStance("favor") },
                                    label = { Text("Favor") }
                                )
                                FilterChip(
                                    selected = state.stance == "against",
                                    onClick = { chatbotViewModel.setStance("against") },
                                    label = { Text("Against") }
                                )
                            }

                            if (state.topic.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "Debate setup",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Topic: ${state.topic}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colorScheme.onSurface
                                        )
                                        val stanceLabel = when (state.stance) {
                                            "favor" -> "Favor"
                                            "against" -> "Against"
                                            else -> "Not selected"
                                        }
                                        Text(
                                            text = "Your stance: $stanceLabel",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(state.messages) { msg ->
                                MessageBubble(msg)
                            }
                        }

                        state.error?.let { error ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.errorContainer
                            ) {
                                Text(
                                    text = error,
                                    color = colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = colorScheme.surface,
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = state.messageInput,
                                    onValueChange = { chatbotViewModel.setMessageInput(it) },
                                    placeholder = { Text("Your argument...") },
                                    modifier = Modifier.weight(1f),
                                    enabled = !state.isLoading,
                                    shape = inputShape
                                )
                                FilledIconButton(
                                    onClick = { chatbotViewModel.sendMessage() },
                                    enabled = state.messageInput.isNotBlank() && !state.isLoading,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    if (state.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(Icons.Default.Send, contentDescription = "Send")
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Analysis tab: full analysis report (same as STT module)
                    AnalysisTabContent(
                        canAnalyze = state.messages.size >= 2,
                        analysisState = analysisState,
                        winnerState = winnerState,
                        onRunAnalysis = { analysisViewModel.analyzeChatbot() },
                        onGetWinner = { analysisViewModel.getWinnerChatbot() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.isUser
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 20.dp
            ),
            color = if (isUser) colorScheme.primaryContainer else colorScheme.surfaceVariant,
            shadowElevation = 1.dp
        ) {
            Text(
                text = msg.text,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isUser) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
            )
        }
    }
}
