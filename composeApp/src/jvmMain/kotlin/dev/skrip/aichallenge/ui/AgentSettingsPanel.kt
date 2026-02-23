package dev.skrip.aichallenge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skrip.aichallenge.domain.model.ModelId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSettingsPanel(
    systemPrompt: String,
    selectedModel: ModelId,
    temperatureText: String,
    maxTokensText: String,
    historyTokenLimitText: String,
    estimatedHistoryTokens: Int,
    historyTokensRemaining: Int,
    onSystemPromptChanged: (String) -> Unit,
    onModelChanged: (ModelId) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onMaxTokensChanged: (String) -> Unit,
    onHistoryTokenLimitChanged: (String) -> Unit,
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        Text(
            text = "Agent Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // History Token Usage Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = when {
                        usageRatio > 0.9f -> Color(0xFFFFEBEE)
                        usageRatio > 0.7f -> Color(0xFFFFF3E0)
                        else -> Color(0xFFE8F5E9)
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History Tokens",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onClearHistory,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { usageRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    usageRatio > 0.9f -> Color(0xFFF44336)
                    usageRatio > 0.7f -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                },
                trackColor = Color.White.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Used: $estimatedHistoryTokens",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "Remaining: $historyTokensRemaining",
                    fontSize = 11.sp,
                    color = if (historyTokensRemaining < 500) Color(0xFFF44336) else Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "History Token Limit",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = historyTokenLimitText,
            onValueChange = onHistoryTokenLimitChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g., 1000") },
            singleLine = true,
            supportingText = { Text("0 = no history sent") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "System Prompt",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = systemPrompt,
            onValueChange = onSystemPromptChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("Enter system prompt (optional)...") },
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Model",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
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
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ModelId.entries.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.label) },
                        onClick = {
                            onModelChanged(model)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Temperature",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = temperatureText,
                    onValueChange = onTemperatureChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0.7") },
                    singleLine = true
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Max Tokens",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = maxTokensText,
                    onValueChange = onMaxTokensChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("512") },
                    singleLine = true
                )
            }
        }
    }
}
