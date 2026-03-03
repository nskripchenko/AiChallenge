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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.skrip.aichallenge.domain.model.Currency
import dev.skrip.aichallenge.domain.model.DefaultProfile
import dev.skrip.aichallenge.domain.model.InvestmentHorizon
import dev.skrip.aichallenge.domain.model.MemoryState
import dev.skrip.aichallenge.domain.model.RiskLevel
import dev.skrip.aichallenge.domain.model.UserProfile

@Composable
fun ProfilePanel(
    memoryState: MemoryState,
    onUpdateProfile: (UserProfile) -> Unit,
    onAddToWorkingMemory: (String, String) -> Unit,
    onRemoveFromWorkingMemory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(AppTheme.backgroundSecondary)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.textPrimary
            )
            Text(
                text = "Edit",
                fontSize = 12.sp,
                color = AppTheme.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { showProfileDialog = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Card
        ProfileCard(profile = memoryState.longTerm.profile)

        Spacer(modifier = Modifier.height(20.dp))

        // Notes Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notes",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppTheme.textTertiary
            )
            Text(
                text = "+",
                fontSize = 16.sp,
                color = AppTheme.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { showAddNoteDialog = true }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (memoryState.working.items.isEmpty()) {
            Text(
                text = "No notes yet",
                fontSize = 12.sp,
                color = AppTheme.textMuted,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                memoryState.working.items.forEach { item ->
                    NoteCard(
                        label = item.label,
                        content = item.content,
                        onRemove = { onRemoveFromWorkingMemory(item.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Disclaimer
        Text(
            text = "Just for fun. Not financial advice.",
            fontSize = 10.sp,
            color = AppTheme.textMuted,
            modifier = Modifier.padding(top = 16.dp)
        )
    }

    if (showProfileDialog) {
        ProfileDialog(
            currentProfile = memoryState.longTerm.profile,
            onDismiss = { showProfileDialog = false },
            onSave = { profile ->
                onUpdateProfile(profile)
                showProfileDialog = false
            }
        )
    }

    if (showAddNoteDialog) {
        AddNoteDialog(
            onDismiss = { showAddNoteDialog = false },
            onAdd = { label, content ->
                onAddToWorkingMemory(label, content)
                showAddNoteDialog = false
            }
        )
    }
}

@Composable
private fun ProfileCard(profile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppTheme.background)
            .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Deposit
        ProfileRow(
            label = "Deposit",
            value = if (profile.depositAmount > 0) profile.formattedDeposit else "—"
        )

        // Target
        ProfileRow(
            label = "Target",
            value = profile.formattedTarget
        )

        // Risk
        ProfileRow(
            label = "Risk",
            value = profile.riskLevel.label
        )

        // Horizon
        ProfileRow(
            label = "Horizon",
            value = profile.investmentHorizon.label
        )
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
private fun NoteCard(
    label: String,
    content: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(AppTheme.background)
            .border(1.dp, AppTheme.border, RoundedCornerShape(6.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppTheme.textPrimary
            )
            if (content.isNotBlank()) {
                Text(
                    text = content,
                    fontSize = 11.sp,
                    color = AppTheme.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = "x",
            fontSize = 12.sp,
            color = AppTheme.textMuted,
            modifier = Modifier
                .clickable { onRemove() }
                .padding(4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var depositAmountText by remember {
        mutableStateOf(
            if (currentProfile.depositAmount > 0)
                currentProfile.depositAmount.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }
            else ""
        )
    }
    var depositCurrency by remember { mutableStateOf(currentProfile.depositCurrency) }
    var targetPercentText by remember { mutableStateOf(currentProfile.targetPercent.toInt().toString()) }
    var targetDaysText by remember { mutableStateOf(currentProfile.targetDays.toString()) }
    var riskLevel by remember { mutableStateOf(currentProfile.riskLevel) }
    var investmentHorizon by remember { mutableStateOf(currentProfile.investmentHorizon) }

    var currencyExpanded by remember { mutableStateOf(false) }
    var riskExpanded by remember { mutableStateOf(false) }
    var horizonExpanded by remember { mutableStateOf(false) }

    val depositError = depositAmountText.isNotBlank() && depositAmountText.toDoubleOrNull() == null
    val targetPercentError = targetPercentText.isNotBlank() && targetPercentText.toDoubleOrNull() == null
    val targetDaysError = targetDaysText.isNotBlank() && targetDaysText.toIntOrNull() == null
    val canSave = !depositError && !targetPercentError && !targetDaysError

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(480.dp)
                .background(AppTheme.background, RoundedCornerShape(12.dp))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Profile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.textPrimary
                )
                Text(
                    text = "Reset",
                    fontSize = 12.sp,
                    color = AppTheme.accent,
                    modifier = Modifier
                        .clickable {
                            depositAmountText = DefaultProfile.value.depositAmount.toLong().toString()
                            depositCurrency = DefaultProfile.value.depositCurrency
                            targetPercentText = DefaultProfile.value.targetPercent.toInt().toString()
                            targetDaysText = DefaultProfile.value.targetDays.toString()
                            riskLevel = DefaultProfile.value.riskLevel
                            investmentHorizon = DefaultProfile.value.investmentHorizon
                        }
                        .padding(8.dp)
                )
            }

            // Deposit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = depositAmountText,
                    onValueChange = { depositAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Deposit", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = depositError,
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = it },
                    modifier = Modifier.width(120.dp)
                ) {
                    OutlinedTextField(
                        value = depositCurrency.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        Currency.entries.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text("${currency.symbol} ${currency.label}", fontSize = 14.sp) },
                                onClick = {
                                    depositCurrency = currency
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = targetPercentText,
                    onValueChange = { targetPercentText = it.filter { c -> c.isDigit() } },
                    label = { Text("Target %", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = targetPercentError,
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = targetDaysText,
                    onValueChange = { targetDaysText = it.filter { c -> c.isDigit() } },
                    label = { Text("Days", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = targetDaysError,
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Risk Level
            ExposedDropdownMenuBox(
                expanded = riskExpanded,
                onExpandedChange = { riskExpanded = it }
            ) {
                OutlinedTextField(
                    value = riskLevel.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Risk", fontSize = 13.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = riskExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = riskExpanded,
                    onDismissRequest = { riskExpanded = false }
                ) {
                    RiskLevel.entries.forEach { risk ->
                        DropdownMenuItem(
                            text = { Text(risk.label, fontSize = 14.sp) },
                            onClick = {
                                riskLevel = risk
                                riskExpanded = false
                            }
                        )
                    }
                }
            }

            // Investment Horizon
            ExposedDropdownMenuBox(
                expanded = horizonExpanded,
                onExpandedChange = { horizonExpanded = it }
            ) {
                OutlinedTextField(
                    value = investmentHorizon.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Horizon", fontSize = 13.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = horizonExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = horizonExpanded,
                    onDismissRequest = { horizonExpanded = false }
                ) {
                    InvestmentHorizon.entries.forEach { horizon ->
                        DropdownMenuItem(
                            text = { Text(horizon.label, fontSize = 14.sp) },
                            onClick = {
                                investmentHorizon = horizon
                                horizonExpanded = false
                            }
                        )
                    }
                }
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Cancel",
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
                        .background(if (canSave) AppTheme.accent else AppTheme.border)
                        .clickable(enabled = canSave) {
                            onSave(
                                UserProfile(
                                    name = currentProfile.name,
                                    riskLevel = riskLevel,
                                    depositAmount = depositAmountText.toDoubleOrNull() ?: 0.0,
                                    depositCurrency = depositCurrency,
                                    targetPercent = targetPercentText.toDoubleOrNull() ?: 5.0,
                                    targetDays = targetDaysText.toIntOrNull() ?: 3,
                                    investmentHorizon = investmentHorizon
                                )
                            )
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Save",
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
private fun AddNoteDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .background(AppTheme.background, RoundedCornerShape(12.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add Note",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.textPrimary
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Title", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors(),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4,
                colors = textFieldColors(),
                shape = RoundedCornerShape(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Cancel",
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
                        .background(if (label.isNotBlank()) AppTheme.accent else AppTheme.border)
                        .clickable(enabled = label.isNotBlank()) { onAdd(label, content) }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Add",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}
