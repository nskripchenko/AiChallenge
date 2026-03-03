package dev.skrip.aichallenge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.window.Dialog
import dev.skrip.aichallenge.domain.model.ModelId
import dev.skrip.aichallenge.ui.state.SessionStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    systemPrompt: String,
    selectedModel: ModelId,
    temperatureText: String,
    maxTokensText: String,
    historyTokenLimitText: String,
    estimatedHistoryTokens: Int,
    totalMessages: Int,
    sessionStats: SessionStats,
    onSystemPromptChanged: (String) -> Unit,
    onModelChanged: (ModelId) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onMaxTokensChanged: (String) -> Unit,
    onHistoryTokenLimitChanged: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClearAllMemory: () -> Unit,
    onDismiss: () -> Unit
) {
    var modelExpanded by remember { mutableStateOf(false) }
    val historyTokenLimit = historyTokenLimitText.toIntOrNull() ?: 4000
    val usageRatio = if (historyTokenLimit > 0) {
        (estimatedHistoryTokens.toFloat() / historyTokenLimit).coerceIn(0f, 1f)
    } else 0f

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(480.dp)
                .background(AppTheme.background, RoundedCornerShape(16.dp))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.textPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onDismiss() }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "✕",
                        fontSize = 18.sp,
                        color = AppTheme.textMuted
                    )
                }
            }

            // Model selection
            SettingSection("Model") {
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedModel.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false }
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
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Temperature and Max tokens
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SettingSection("Temperature") {
                        OutlinedTextField(
                            value = temperatureText,
                            onValueChange = onTemperatureChanged,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("0.7", color = AppTheme.textMuted, fontSize = 14.sp) },
                            singleLine = true,
                            colors = textFieldColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    SettingSection("Max Tokens") {
                        OutlinedTextField(
                            value = maxTokensText,
                            onValueChange = onMaxTokensChanged,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("512", color = AppTheme.textMuted, fontSize = 14.sp) },
                            singleLine = true,
                            colors = textFieldColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // History token limit
            SettingSection("History Token Limit") {
                OutlinedTextField(
                    value = historyTokenLimitText,
                    onValueChange = onHistoryTokenLimitChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("4000", color = AppTheme.textMuted, fontSize = 14.sp) },
                    singleLine = true,
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // System prompt
            SettingSection("System Prompt") {
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = onSystemPromptChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("Instructions for the assistant...", color = AppTheme.textMuted, fontSize = 14.sp) },
                    maxLines = 6,
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Context usage card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.backgroundSecondary, RoundedCornerShape(8.dp))
                    .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Context Usage",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.textPrimary
                    )
                    Text(
                        text = "Clear",
                        fontSize = 13.sp,
                        color = AppTheme.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onClearHistory)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AppTheme.border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(usageRatio)
                            .height(6.dp)
                            .background(
                                if (usageRatio > 0.8f) Color(0xFFEF4444)
                                else if (usageRatio > 0.6f) Color(0xFFF59E0B)
                                else AppTheme.accent
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$totalMessages messages",
                        fontSize = 12.sp,
                        color = AppTheme.textMuted
                    )
                    Text(
                        text = "$estimatedHistoryTokens / $historyTokenLimit tokens",
                        fontSize = 12.sp,
                        color = AppTheme.textMuted
                    )
                }
            }

            // Session stats
            if (sessionStats.exchangeCount > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.backgroundSecondary, RoundedCornerShape(8.dp))
                        .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Session Stats",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.textPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    StatRow("Total Tokens", "${sessionStats.totalTokens}")
                    StatRow("Cost", formatCost(sessionStats.totalCostUsd))
                    StatRow("Avg Response", "%.1fs".format(sessionStats.avgResponseTimeSec))
                    StatRow("Exchanges", "${sessionStats.exchangeCount}")
                }
            }

            // Clear all memory
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Clear All",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFDC2626),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Clears chat history, notes, and profile",
                    fontSize = 12.sp,
                    color = AppTheme.textMuted,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFDC2626))
                        .clickable { onClearAllMemory() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Clear All Memory",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
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
