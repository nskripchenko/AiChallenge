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
import dev.skrip.aichallenge.domain.model.ModelId
import dev.skrip.aichallenge.ui.state.SessionStats

// Design System SettingsColors
private object SettingsColors {
    val background = Color.White
    val backgroundSecondary = Color(0xFFFAFAFA)

    val textPrimary = Color(0xFF0A0A0A)
    val textSecondary = Color(0xFF404040)
    val textTertiary = Color(0xFF737373)
    val textMuted = Color(0xFFA3A3A3)

    val border = Color(0xFFE5E5E5)
    val accent = Color(0xFF18181B)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSettingsPanel(
    systemPrompt: String,
    selectedModel: ModelId,
    temperatureText: String,
    maxTokensText: String,
    historyTokenLimitText: String,
    keepRecentMessagesText: String,
    estimatedHistoryTokens: Int,
    historyTokensRemaining: Int,
    hasSummary: Boolean,
    summarizedCount: Int,
    totalMessages: Int,
    keepRecentMessages: Int,
    sessionStats: SessionStats,
    onSystemPromptChanged: (String) -> Unit,
    onModelChanged: (ModelId) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onMaxTokensChanged: (String) -> Unit,
    onHistoryTokenLimitChanged: (String) -> Unit,
    onKeepRecentMessagesChanged: (String) -> Unit,
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
            .background(SettingsColors.backgroundSecondary)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "Settings",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = SettingsColors.textPrimary,
            letterSpacing = (-0.02).sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Context Card - Messages Overview
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SettingsColors.background, RoundedCornerShape(8.dp))
                .border(1.dp, SettingsColors.border, RoundedCornerShape(8.dp))
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
                    color = SettingsColors.textPrimary
                )
                Text(
                    text = "Clear",
                    fontSize = 13.sp,
                    color = SettingsColors.textTertiary,
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
                        .background(SettingsColors.border)
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
                                .background(SettingsColors.accent)
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
                            color = SettingsColors.textMuted
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
                                .background(SettingsColors.accent, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "$recentCount recent",
                            fontSize = 11.sp,
                            color = SettingsColors.textMuted
                        )
                    }
                }
            } else {
                Text(
                    text = "No messages yet",
                    fontSize = 12.sp,
                    color = SettingsColors.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Token Usage Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SettingsColors.background, RoundedCornerShape(8.dp))
                .border(1.dp, SettingsColors.border, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Tokens",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = SettingsColors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Token progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SettingsColors.border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(usageRatio)
                        .height(4.dp)
                        .background(SettingsColors.accent)
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
                    color = SettingsColors.textMuted
                )
                Text(
                    text = "$historyTokensRemaining remaining",
                    fontSize = 11.sp,
                    color = SettingsColors.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Session Stats Card
        if (sessionStats.exchangeCount > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SettingsColors.background, RoundedCornerShape(8.dp))
                    .border(1.dp, SettingsColors.border, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Session",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SettingsColors.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                StatRow("Tokens", "${sessionStats.totalTokens}")
                StatRow("Cost", formatCost(sessionStats.totalCostUsd))
                StatRow("Avg response", "%.1fs".format(sessionStats.avgResponseTimeSec))
                StatRow("Messages", "${sessionStats.exchangeCount}")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Settings Fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SettingSection("History limit") {
                    OutlinedTextField(
                        value = historyTokenLimitText,
                        onValueChange = onHistoryTokenLimitChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("1000", color = SettingsColors.textMuted, fontSize = 14.sp) },
                        singleLine = true,
                        colors = textFieldSettingsColors(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                SettingSection("Keep recent") {
                    OutlinedTextField(
                        value = keepRecentMessagesText,
                        onValueChange = onKeepRecentMessagesChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("10", color = SettingsColors.textMuted, fontSize = 14.sp) },
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
                placeholder = { Text("Optional instructions...", color = SettingsColors.textMuted, fontSize = 14.sp) },
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
                                    color = SettingsColors.textSecondary
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
                        placeholder = { Text("0.7", color = SettingsColors.textMuted, fontSize = 14.sp) },
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
                        placeholder = { Text("512", color = SettingsColors.textMuted, fontSize = 14.sp) },
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
private fun SettingSection(
    label: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = SettingsColors.textTertiary,
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
            color = SettingsColors.textTertiary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = SettingsColors.textPrimary
        )
    }
}

@Composable
private fun textFieldSettingsColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SettingsColors.accent,
    unfocusedBorderColor = SettingsColors.border,
    cursorColor = SettingsColors.accent,
    focusedTextColor = SettingsColors.textSecondary,
    unfocusedTextColor = SettingsColors.textSecondary
)

private fun formatCost(cost: Double): String {
    return when {
        cost < 0.001 -> "<$0.001"
        else -> "$${String.format("%.3f", cost)}"
    }
}
