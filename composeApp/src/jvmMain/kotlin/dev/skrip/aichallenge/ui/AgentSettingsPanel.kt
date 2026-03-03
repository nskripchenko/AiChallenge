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
import dev.skrip.aichallenge.domain.model.MemoryLayer
import dev.skrip.aichallenge.domain.model.MemoryState
import dev.skrip.aichallenge.domain.model.ModelId
import dev.skrip.aichallenge.domain.model.UserProfile
import dev.skrip.aichallenge.ui.state.SessionStats

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
    totalMessages: Int,
    sessionStats: SessionStats,
    // Memory model
    memoryState: MemoryState,
    selectedMemoryLayer: MemoryLayer,
    isMemoryPanelExpanded: Boolean,
    onSelectMemoryLayer: (MemoryLayer) -> Unit,
    onToggleMemoryPanel: () -> Unit,
    onAddToWorkingMemory: (String, String) -> Unit,
    onRemoveFromWorkingMemory: (String) -> Unit,
    onClearWorkingMemory: () -> Unit,
    onUpdateProfile: (UserProfile) -> Unit,
    onAddDecision: (String, String) -> Unit,
    onRemoveDecision: (String) -> Unit,
    onAddKnowledge: (String, String, String) -> Unit,
    onRemoveKnowledge: (String) -> Unit,
    onClearLongTermMemory: () -> Unit,
    onSetShortTermLimit: (Int) -> Unit,
    // Original callbacks
    onSystemPromptChanged: (String) -> Unit,
    onModelChanged: (ModelId) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onMaxTokensChanged: (String) -> Unit,
    onHistoryTokenLimitChanged: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val historyTokenLimit = historyTokenLimitText.toIntOrNull() ?: 4000
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
            text = "Настройки",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.textPrimary,
            letterSpacing = (-0.02).sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Memory Panel - Main Feature
        MemoryPanel(
            memoryState = memoryState,
            selectedLayer = selectedMemoryLayer,
            isExpanded = isMemoryPanelExpanded,
            onSelectLayer = onSelectMemoryLayer,
            onToggleExpanded = onToggleMemoryPanel,
            onAddToWorkingMemory = onAddToWorkingMemory,
            onRemoveFromWorkingMemory = onRemoveFromWorkingMemory,
            onClearWorkingMemory = onClearWorkingMemory,
            onUpdateProfile = onUpdateProfile,
            onAddDecision = onAddDecision,
            onRemoveDecision = onRemoveDecision,
            onAddKnowledge = onAddKnowledge,
            onRemoveKnowledge = onRemoveKnowledge,
            onClearLongTermMemory = onClearLongTermMemory,
            onSetShortTermLimit = onSetShortTermLimit
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Context Card
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
                    text = "Контекст",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.textPrimary
                )
                Text(
                    text = "Очистить",
                    fontSize = 13.sp,
                    color = AppTheme.textTertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onClearHistory)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

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
                    text = "$totalMessages сообщений",
                    fontSize = 11.sp,
                    color = AppTheme.textMuted
                )
                Text(
                    text = "$estimatedHistoryTokens / $historyTokenLimit токенов",
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
                    text = "Сессия",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                StatRow("Токены", "${sessionStats.totalTokens}")
                StatRow("Стоимость", formatCost(sessionStats.totalCostUsd))
                StatRow("Ср. ответ", "%.1fs".format(sessionStats.avgResponseTimeSec))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // System prompt
        SettingSection("Системный промпт") {
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = onSystemPromptChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = { Text("Инструкции для ассистента...", color = AppTheme.textMuted, fontSize = 14.sp) },
                maxLines = 5,
                colors = textFieldSettingsColors(),
                shape = RoundedCornerShape(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Model
        SettingSection("Модель") {
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
                SettingSection("Температура") {
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
                SettingSection("Max токенов") {
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
private fun MemoryPanel(
    memoryState: MemoryState,
    selectedLayer: MemoryLayer,
    isExpanded: Boolean,
    onSelectLayer: (MemoryLayer) -> Unit,
    onToggleExpanded: () -> Unit,
    onAddToWorkingMemory: (String, String) -> Unit,
    onRemoveFromWorkingMemory: (String) -> Unit,
    onClearWorkingMemory: () -> Unit,
    onUpdateProfile: (UserProfile) -> Unit,
    onAddDecision: (String, String) -> Unit,
    onRemoveDecision: (String) -> Unit,
    onAddKnowledge: (String, String, String) -> Unit,
    onRemoveKnowledge: (String) -> Unit,
    onClearLongTermMemory: () -> Unit,
    onSetShortTermLimit: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.background, RoundedCornerShape(8.dp))
            .border(1.dp, AppTheme.accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🧠", fontSize = 16.sp)
                Text(
                    text = "Память ассистента",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.textPrimary
                )
            }
            Text(
                text = if (isExpanded) "▲" else "▼",
                fontSize = 12.sp,
                color = AppTheme.textMuted
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(16.dp))

            // Layer tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MemoryLayer.entries.forEach { layer ->
                    val isSelected = layer == selectedLayer
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AppTheme.accent else AppTheme.backgroundSecondary)
                            .clickable { onSelectLayer(layer) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = layer.icon,
                                fontSize = 16.sp
                            )
                            Text(
                                text = layer.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = if (isSelected) Color.White else AppTheme.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Layer content
            when (selectedLayer) {
                MemoryLayer.SHORT_TERM -> ShortTermMemoryContent(
                    shortTerm = memoryState.shortTerm,
                    onSetLimit = onSetShortTermLimit
                )
                MemoryLayer.WORKING -> WorkingMemoryContent(
                    working = memoryState.working,
                    onAdd = { showAddDialog = true },
                    onRemove = onRemoveFromWorkingMemory,
                    onClear = onClearWorkingMemory
                )
                MemoryLayer.LONG_TERM -> LongTermMemoryContent(
                    longTerm = memoryState.longTerm,
                    onUpdateProfile = onUpdateProfile,
                    onAddDecision = onAddDecision,
                    onRemoveDecision = onRemoveDecision,
                    onAddKnowledge = onAddKnowledge,
                    onRemoveKnowledge = onRemoveKnowledge,
                    onClear = onClearLongTermMemory
                )
            }
        }

        // Add to working memory dialog
        if (showAddDialog) {
            AddWorkingMemoryDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { label, content ->
                    onAddToWorkingMemory(label, content)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun ShortTermMemoryContent(
    shortTerm: dev.skrip.aichallenge.domain.model.ShortTermMemory,
    onSetLimit: (Int) -> Unit
) {
    var limitText by remember { mutableStateOf(shortTerm.maxMessages.toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Последние сообщения текущего диалога. Автоматически отправляются в каждом запросе.",
            fontSize = 12.sp,
            color = AppTheme.textMuted,
            lineHeight = 18.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "В памяти: ${shortTerm.recentMessages.size} сообщений",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppTheme.textSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Лимит:",
                fontSize = 12.sp,
                color = AppTheme.textTertiary
            )
            OutlinedTextField(
                value = limitText,
                onValueChange = {
                    limitText = it
                    it.toIntOrNull()?.let { limit -> onSetLimit(limit) }
                },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                colors = textFieldSettingsColors(),
                shape = RoundedCornerShape(6.dp)
            )
            Text(
                text = "сообщений",
                fontSize = 12.sp,
                color = AppTheme.textTertiary
            )
        }
    }
}

@Composable
private fun WorkingMemoryContent(
    working: dev.skrip.aichallenge.domain.model.WorkingMemory,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Данные текущей задачи. Добавляйте важную информацию, которую ассистент должен помнить.",
            fontSize = 12.sp,
            color = AppTheme.textMuted,
            lineHeight = 18.sp
        )

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppTheme.accent)
                    .clickable { onAdd() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "+ Добавить",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            if (working.items.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppTheme.backgroundSecondary)
                        .border(1.dp, AppTheme.border, RoundedCornerShape(6.dp))
                        .clickable { onClear() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Очистить",
                        fontSize = 13.sp,
                        color = AppTheme.textTertiary
                    )
                }
            }
        }

        // Items list
        if (working.items.isNotEmpty()) {
            working.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.backgroundSecondary, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.textPrimary
                        )
                        Text(
                            text = item.content,
                            fontSize = 12.sp,
                            color = AppTheme.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                    Text(
                        text = "×",
                        fontSize = 18.sp,
                        color = AppTheme.textMuted,
                        modifier = Modifier
                            .clickable { onRemove(item.id) }
                            .padding(4.dp)
                    )
                }
            }
        } else {
            Text(
                text = "Пусто. Добавьте данные для текущей задачи.",
                fontSize = 12.sp,
                color = AppTheme.textMuted,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun LongTermMemoryContent(
    longTerm: dev.skrip.aichallenge.domain.model.LongTermMemory,
    onUpdateProfile: (UserProfile) -> Unit,
    onAddDecision: (String, String) -> Unit,
    onRemoveDecision: (String) -> Unit,
    onAddKnowledge: (String, String, String) -> Unit,
    onRemoveKnowledge: (String) -> Unit,
    onClear: () -> Unit
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var showDecisionDialog by remember { mutableStateOf(false) }
    var showKnowledgeDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Постоянная память: профиль пользователя, важные решения, база знаний.",
            fontSize = 12.sp,
            color = AppTheme.textMuted,
            lineHeight = 18.sp
        )

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ActionButton(
                text = "👤 Профиль",
                isAccent = true,
                onClick = { showProfileDialog = true }
            )
            ActionButton(
                text = "+ Решение",
                onClick = { showDecisionDialog = true }
            )
            ActionButton(
                text = "+ Знание",
                onClick = { showKnowledgeDialog = true }
            )
        }

        // Profile info
        if (longTerm.profile.name.isNotBlank() || longTerm.profile.context.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.backgroundSecondary, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "👤", fontSize = 14.sp)
                Column {
                    if (longTerm.profile.name.isNotBlank()) {
                        Text(
                            text = longTerm.profile.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.textPrimary
                        )
                    }
                    if (longTerm.profile.context.isNotBlank()) {
                        Text(
                            text = longTerm.profile.context,
                            fontSize = 12.sp,
                            color = AppTheme.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Decisions
        if (longTerm.decisions.isNotEmpty()) {
            Text(
                text = "Решения (${longTerm.decisions.size}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppTheme.textTertiary
            )
            longTerm.decisions.forEach { decision ->
                MemoryItem(
                    icon = "📋",
                    title = decision.title,
                    content = decision.description,
                    onRemove = { onRemoveDecision(decision.id) }
                )
            }
        }

        // Knowledge
        if (longTerm.knowledge.isNotEmpty()) {
            Text(
                text = "Знания (${longTerm.knowledge.size}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppTheme.textTertiary
            )
            longTerm.knowledge.forEach { item ->
                MemoryItem(
                    icon = "📚",
                    title = "[${item.category}] ${item.title}",
                    content = item.content,
                    onRemove = { onRemoveKnowledge(item.id) }
                )
            }
        }

        // Clear button
        if (longTerm.profile.name.isNotBlank() || longTerm.decisions.isNotEmpty() || longTerm.knowledge.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFEE2E2))
                    .clickable { onClear() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Очистить всю долговременную память",
                    fontSize = 12.sp,
                    color = Color(0xFFDC2626)
                )
            }
        }
    }

    // Dialogs
    if (showProfileDialog) {
        ProfileDialog(
            currentProfile = longTerm.profile,
            onDismiss = { showProfileDialog = false },
            onSave = { profile ->
                onUpdateProfile(profile)
                showProfileDialog = false
            }
        )
    }

    if (showDecisionDialog) {
        DecisionDialog(
            onDismiss = { showDecisionDialog = false },
            onAdd = { title, description ->
                onAddDecision(title, description)
                showDecisionDialog = false
            }
        )
    }

    if (showKnowledgeDialog) {
        KnowledgeDialog(
            onDismiss = { showKnowledgeDialog = false },
            onAdd = { category, title, content ->
                onAddKnowledge(category, title, content)
                showKnowledgeDialog = false
            }
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    isAccent: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isAccent) AppTheme.accent else AppTheme.backgroundSecondary)
            .then(
                if (!isAccent) Modifier.border(1.dp, AppTheme.border, RoundedCornerShape(6.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isAccent) FontWeight.Medium else FontWeight.Normal,
            color = if (isAccent) Color.White else AppTheme.textSecondary
        )
    }
}

@Composable
private fun MemoryItem(
    icon: String,
    title: String,
    content: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.backgroundSecondary, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$icon $title",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppTheme.textPrimary
            )
            if (content.isNotBlank()) {
                Text(
                    text = content,
                    fontSize = 11.sp,
                    color = AppTheme.textSecondary,
                    lineHeight = 16.sp
                )
            }
        }
        Text(
            text = "×",
            fontSize = 16.sp,
            color = AppTheme.textMuted,
            modifier = Modifier
                .clickable(onClick = onRemove)
                .padding(4.dp)
        )
    }
}

@Composable
private fun AddWorkingMemoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .background(AppTheme.background, RoundedCornerShape(12.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Добавить в рабочую память",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.textPrimary
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Название", fontSize = 13.sp) },
                placeholder = { Text("Например: Текущая задача", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldSettingsColors(),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Содержимое", fontSize = 13.sp) },
                placeholder = { Text("Что нужно запомнить...", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4,
                colors = textFieldSettingsColors(),
                shape = RoundedCornerShape(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Отмена",
                    fontSize = 14.sp,
                    color = AppTheme.textTertiary,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (label.isNotBlank() && content.isNotBlank()) AppTheme.accent else AppTheme.border)
                        .clickable(enabled = label.isNotBlank() && content.isNotBlank()) {
                            onAdd(label, content)
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Добавить",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(currentProfile.name) }
    var context by remember { mutableStateOf(currentProfile.context) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .background(AppTheme.background, RoundedCornerShape(12.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Профиль пользователя",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.textPrimary
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldSettingsColors(),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = context,
                onValueChange = { context = it },
                label = { Text("О себе", fontSize = 13.sp) },
                placeholder = { Text("Кто вы, чем занимаетесь, предпочтения...", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5,
                colors = textFieldSettingsColors(),
                shape = RoundedCornerShape(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Отмена",
                    fontSize = 14.sp,
                    color = AppTheme.textTertiary,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppTheme.accent)
                        .clickable { onSave(UserProfile(name = name, context = context)) }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Сохранить",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DecisionDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .background(AppTheme.background, RoundedCornerShape(12.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Добавить решение",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.textPrimary
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название решения", fontSize = 13.sp) },
                placeholder = { Text("Например: Использовать Kotlin", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldSettingsColors(),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание", fontSize = 13.sp) },
                placeholder = { Text("Почему принято это решение...", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4,
                colors = textFieldSettingsColors(),
                shape = RoundedCornerShape(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Отмена",
                    fontSize = 14.sp,
                    color = AppTheme.textTertiary,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (title.isNotBlank()) AppTheme.accent else AppTheme.border)
                        .clickable(enabled = title.isNotBlank()) {
                            onAdd(title, description)
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Добавить",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun KnowledgeDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var category by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .background(AppTheme.background, RoundedCornerShape(12.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Добавить знание",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.textPrimary
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Категория", fontSize = 13.sp) },
                placeholder = { Text("Например: Технологии", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldSettingsColors(),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название", fontSize = 13.sp) },
                placeholder = { Text("Например: API ключ", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldSettingsColors(),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Содержимое", fontSize = 13.sp) },
                placeholder = { Text("Информация...", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4,
                colors = textFieldSettingsColors(),
                shape = RoundedCornerShape(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Отмена",
                    fontSize = 14.sp,
                    color = AppTheme.textTertiary,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (title.isNotBlank() && category.isNotBlank()) AppTheme.accent else AppTheme.border)
                        .clickable(enabled = title.isNotBlank() && category.isNotBlank()) {
                            onAdd(category, title, content)
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Добавить",
                        fontSize = 14.sp,
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

@Composable
private fun textFieldSettingsColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppTheme.accent,
    unfocusedBorderColor = AppTheme.border,
    cursorColor = AppTheme.accent,
    focusedTextColor = AppTheme.textSecondary,
    unfocusedTextColor = AppTheme.textSecondary
)
