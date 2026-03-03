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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.skrip.aichallenge.domain.model.MemoryState

@Composable
fun MemoryDialog(
    memoryState: MemoryState,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(500.dp)
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
                    text = "Memory",
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
                        text = "x",
                        fontSize = 18.sp,
                        color = AppTheme.textMuted
                    )
                }
            }

            // Short-term memory
            MemorySection("Short-term (dialog)") {
                val count = memoryState.shortTerm.recentMessages.size
                val max = memoryState.shortTerm.maxMessages
                Text(
                    text = "$count / $max messages in context",
                    fontSize = 13.sp,
                    color = AppTheme.textSecondary
                )
            }

            // Working memory
            MemorySection("Working (notes)") {
                val items = memoryState.working.items
                if (items.isEmpty()) {
                    Text(
                        text = "No notes",
                        fontSize = 13.sp,
                        color = AppTheme.textMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.forEach { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppTheme.backgroundSecondary, RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = item.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppTheme.textPrimary
                                )
                                Text(
                                    text = item.content,
                                    fontSize = 12.sp,
                                    color = AppTheme.textSecondary,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }

            // Long-term memory (profile)
            MemorySection("Long-term (profile)") {
                val profile = memoryState.longTerm.profile
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (profile.depositAmount > 0) {
                        MemoryRow("Deposit", profile.formattedDeposit)
                    }
                    MemoryRow("Target", profile.formattedTarget)
                    MemoryRow("Risk", profile.riskLevel.label)
                    MemoryRow("Horizon", profile.investmentHorizon.label)
                }
            }

            // Decisions (if any)
            val decisions = memoryState.longTerm.decisions
            if (decisions.isNotEmpty()) {
                MemorySection("Decisions") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        decisions.takeLast(5).forEach { decision ->
                            Text(
                                text = "- ${decision.title}",
                                fontSize = 12.sp,
                                color = AppTheme.textSecondary
                            )
                        }
                    }
                }
            }

            // Knowledge (if any)
            val knowledge = memoryState.longTerm.knowledge
            if (knowledge.isNotEmpty()) {
                MemorySection("Knowledge") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        knowledge.takeLast(5).forEach { item ->
                            Text(
                                text = "- ${item.title}",
                                fontSize = 12.sp,
                                color = AppTheme.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemorySection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.backgroundSecondary, RoundedCornerShape(8.dp))
            .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppTheme.textPrimary,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        content()
    }
}

@Composable
private fun MemoryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppTheme.textTertiary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = AppTheme.textPrimary
        )
    }
}
