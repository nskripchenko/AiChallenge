package dev.skrip.aichallenge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skrip.aichallenge.domain.model.ContextStrategy
import dev.skrip.aichallenge.domain.model.ModelId
import dev.skrip.aichallenge.ui.state.Fact
import dev.skrip.aichallenge.ui.state.SessionStats

// Use unified AppTheme from DesignSystem.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSettingsPanel(
    systemPrompt: String,
    selectedModel: ModelId,
    temperatureText: String,
    maxTokensText: String,
    historyTokenLimitText: String,
    keepRecentMessagesText: String,
    windowSizeText: String,
    estimatedHistoryTokens: Int,
    historyTokensRemaining: Int,
    hasSummary: Boolean,
    summarizedCount: Int,
    totalMessages: Int,
    keepRecentMessages: Int,
    sessionStats: SessionStats,
    // Strategy-specific
    currentStrategy: ContextStrategy,
    facts: List<Fact>,
    isExtractingFacts: Boolean,
    factsUpdatedCount: Int,
    onSystemPromptChanged: (String) -> Unit,
    onModelChanged: (ModelId) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onMaxTokensChanged: (String) -> Unit,
    onHistoryTokenLimitChanged: (String) -> Unit,
    onKeepRecentMessagesChanged: (String) -> Unit,
    onWindowSizeChanged: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val historyTokenLimit = historyTokenLimitText.toIntOrNull() ?: 1000
    val usageRatio = if (historyTokenLimit > 0) {
        (estimatedHistoryTokens.toFloat() / historyTokenLimit).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(AppTheme.backgroundSecondary)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "Settings",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.textPrimary,
            letterSpacing = (-0.02).sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Context Card - Messages Overview
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.background, RoundedCornerShape(8.dp))
                .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Context",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.textPrimary
                )
                Text(
                    text = "Clear",
                    fontSize = 13.sp,
                    color = AppTheme.textTertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onClearHistory)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Context progress bar (summarized + recent)
            val recentCount = (totalMessages - summarizedCount).coerceAtLeast(0)
            val maxDisplayMessages = keepRecentMessages * 3 // Show capacity for ~3x keepRecent
            val displayTotal = maxOf(totalMessages, maxDisplayMessages)

            if (totalMessages > 0) {
                // Segmented progress bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AppTheme.border)
                ) {
                    // Summarized portion (darker)
                    if (summarizedCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(summarizedCount.toFloat() / displayTotal)
                                .fillMaxHeight()
                                .background(Color(0xFF737373))
                        )
                    }
                    // Recent portion (accent)
                    if (recentCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(recentCount.toFloat() / displayTotal)
                                .fillMaxHeight()
                                .background(AppTheme.accent)
                        )
                    }
                    // Empty space
                    val emptyWeight = (displayTotal - totalMessages).toFloat() / displayTotal
                    if (emptyWeight > 0) {
                        Spacer(modifier = Modifier.weight(emptyWeight))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summarized legend
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF737373), RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "$summarizedCount summarized",
                            fontSize = 11.sp,
                            color = AppTheme.textMuted
                        )
                    }
                    // Recent legend
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AppTheme.accent, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "$recentCount recent",
                            fontSize = 11.sp,
                            color = AppTheme.textMuted
                        )
                    }
                }
            } else {
                Text(
                    text = "No messages yet",
                    fontSize = 12.sp,
                    color = AppTheme.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Token Usage Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.background, RoundedCornerShape(8.dp))
                .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Tokens",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppTheme.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Token progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppTheme.border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(usageRatio)
                        .height(4.dp)
                        .background(AppTheme.accent)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$estimatedHistoryTokens used",
                    fontSize = 11.sp,
                    color = AppTheme.textMuted
                )
                Text(
                    text = "$historyTokensRemaining remaining",
                    fontSize = 11.sp,
                    color = AppTheme.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Session Stats Card
        if (sessionStats.exchangeCount > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.background, RoundedCornerShape(8.dp))
                    .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Session",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                StatRow("Tokens", "${sessionStats.totalTokens}")
                StatRow("Cost", formatCost(sessionStats.totalCostUsd))
                StatRow("Avg response", "%.1fs".format(sessionStats.avgResponseTimeSec))
                StatRow("Messages", "${sessionStats.exchangeCount}")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Facts Panel (only for STICKY_FACTS strategy)
        if (currentStrategy == ContextStrategy.STICKY_FACTS) {
            FactsPanel(
                facts = facts,
                isExtractingFacts = isExtractingFacts,
                factsUpdatedCount = factsUpdatedCount
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Strategy-specific settings
        when (currentStrategy) {
            ContextStrategy.SLIDING_WINDOW -> {
                SettingSection("Window size (messages)") {
                    OutlinedTextField(
                        value = windowSizeText,
                        onValueChange = onWindowSizeChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("10", color = AppTheme.textMuted, fontSize = 14.sp) },
                        singleLine = true,
                        colors = textFieldSettingsColors(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
            ContextStrategy.STICKY_FACTS, ContextStrategy.BRANCHING -> {
                SettingSection("Keep recent (messages)") {
                    OutlinedTextField(
                        value = keepRecentMessagesText,
                        onValueChange = onKeepRecentMessagesChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("10", color = AppTheme.textMuted, fontSize = 14.sp) },
                        singleLine = true,
                        colors = textFieldSettingsColors(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingSection("System prompt") {
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = onSystemPromptChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = { Text("Optional instructions...", color = AppTheme.textMuted, fontSize = 14.sp) },
                maxLines = 5,
                colors = textFieldSettingsColors(),
                shape = RoundedCornerShape(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingSection("Model") {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedModel.label,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    colors = textFieldSettingsColors(),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ModelId.entries.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    model.label,
                                    fontSize = 14.sp,
                                    color = AppTheme.textSecondary
                                )
                            },
                            onClick = {
                                onModelChanged(model)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SettingSection("Temperature") {
                    OutlinedTextField(
                        value = temperatureText,
                        onValueChange = onTemperatureChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0.7", color = AppTheme.textMuted, fontSize = 14.sp) },
                        singleLine = true,
                        colors = textFieldSettingsColors(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                SettingSection("Max tokens") {
                    OutlinedTextField(
                        value = maxTokensText,
                        onValueChange = onMaxTokensChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("512", color = AppTheme.textMuted, fontSize = 14.sp) },
                        singleLine = true,
                        colors = textFieldSettingsColors(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FactsPanel(
    facts: List<Fact>,
    isExtractingFacts: Boolean,
    factsUpdatedCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.background, RoundedCornerShape(8.dp))
            .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Sticky Facts",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.textPrimary
                )
                if (isExtractingFacts) {
                    Text(
                        text = "extracting...",
                        fontSize = 11.sp,
                        color = AppTheme.textMuted
                    )
                } else if (factsUpdatedCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "updated",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }
            Text(
                text = "${facts.size} facts",
                fontSize = 11.sp,
                color = AppTheme.textMuted
            )
        }

        if (isExtractingFacts) {
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = AppTheme.accent,
                trackColor = AppTheme.border
            )
        }

        if (facts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                facts.forEach { fact ->
                    FactItem(fact)
                }
            }
        } else if (!isExtractingFacts) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Facts will be extracted automatically from conversation",
                fontSize = 12.sp,
                color = AppTheme.textMuted
            )
        }
    }
}

@Composable
private fun FactItem(fact: Fact) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.backgroundSecondary, RoundedCornerShape(6.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = fact.key,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = AppTheme.textPrimary
        )
        Text(
            text = ":",
            fontSize = 12.sp,
            color = AppTheme.textMuted
        )
        Text(
            text = fact.value,
            fontSize = 12.sp,
            color = AppTheme.textSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SettingSection(
    label: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppTheme.textTertiary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = AppTheme.textTertiary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppTheme.textPrimary
        )
    }
}

@Composable
private fun textFieldSettingsColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppTheme.accent,
    unfocusedBorderColor = AppTheme.border,
    cursorColor = AppTheme.accent,
    focusedTextColor = AppTheme.textSecondary,
    unfocusedTextColor = AppTheme.textSecondary
)
