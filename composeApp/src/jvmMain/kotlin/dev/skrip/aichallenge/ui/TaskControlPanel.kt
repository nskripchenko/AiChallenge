package dev.skrip.aichallenge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skrip.aichallenge.domain.statemachine.ExpectedAction
import dev.skrip.aichallenge.domain.statemachine.TaskPhase
import dev.skrip.aichallenge.domain.statemachine.TaskState

@Composable
fun TaskControlPanel(
    taskState: TaskState?,
    taskError: String?,
    onApprovePlan: () -> Unit,
    onRejectPlan: (String) -> Unit,
    onApproveStep: () -> Unit,
    onApproveValidation: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Only show panel when there's an active task (not idle, not null)
    val hasActiveTask = taskState != null && taskState.currentPhase !is TaskPhase.Idle

    if (!hasActiveTask && taskError == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.backgroundSecondary)
            .padding(12.dp)
    ) {
        // Header
        Text(
            text = "Task Progress",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppTheme.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Error message
        taskError?.let { error ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEE2E2), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = error,
                    fontSize = 12.sp,
                    color = Color(0xFFDC2626)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (taskState != null && taskState.currentPhase !is TaskPhase.Idle) {
            TaskStateView(
                state = taskState,
                onApprovePlan = onApprovePlan,
                onRejectPlan = onRejectPlan,
                onApproveStep = onApproveStep,
                onApproveValidation = onApproveValidation,
                onPause = onPause,
                onResume = onResume,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun TaskStateView(
    state: TaskState,
    onApprovePlan: () -> Unit,
    onRejectPlan: (String) -> Unit,
    onApproveStep: () -> Unit,
    onApproveValidation: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Phase indicator
        PhaseIndicator(phase = state.currentPhase)

        // Current step description
        Text(
            text = state.currentStepDescription,
            fontSize = 12.sp,
            color = AppTheme.textSecondary
        )

        // Progress bar (for executing phase)
        val executingPhase = state.currentPhase as? TaskPhase.Executing
        if (executingPhase != null) {
            Column {
                val phase = executingPhase
                LinearProgressIndicator(
                    progress = { phase.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AppTheme.accent,
                    trackColor = AppTheme.border
                )
                Text(
                    text = "${phase.completedSteps.size}/${phase.totalSteps} steps",
                    fontSize = 11.sp,
                    color = AppTheme.textMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Expected action
        ExpectedActionView(action = state.expectedAction)

        // Action buttons based on phase
        ActionButtons(
            phase = state.currentPhase,
            onApprovePlan = onApprovePlan,
            onRejectPlan = onRejectPlan,
            onApproveStep = onApproveStep,
            onApproveValidation = onApproveValidation,
            onPause = onPause,
            onResume = onResume,
            onCancel = onCancel
        )
    }
}

@Composable
private fun PhaseIndicator(phase: TaskPhase) {
    val phases = listOf("clarifying", "planning", "executing", "validating", "completed")
    val currentIndex = phases.indexOf(phase.name).takeIf { it >= 0 } ?: -1
    val pausedPhase = phase as? TaskPhase.Paused

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        phases.forEachIndexed { index, phaseName ->
            val isActive = index == currentIndex
            val isCompleted = index < currentIndex

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = when {
                                pausedPhase != null && phaseName == pausedPhase.previousPhase -> Color(0xFFF59E0B)
                                isActive -> AppTheme.accent
                                isCompleted -> Color(0xFF10B981)
                                else -> AppTheme.border
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCompleted) "v" else "${index + 1}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive || isCompleted || pausedPhase != null) Color.White else AppTheme.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = phaseName.replaceFirstChar { it.uppercase() },
                    fontSize = 9.sp,
                    color = if (isActive) AppTheme.accent else AppTheme.textMuted,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                )
            }

            if (index < phases.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(2.dp)
                        .background(
                            if (isCompleted) Color(0xFF10B981) else AppTheme.border
                        )
                )
            }
        }
    }
}

@Composable
private fun ExpectedActionView(action: ExpectedAction) {
    val (icon, text, color) = when (action) {
        is ExpectedAction.UserInput -> Triple("?", action.prompt, Color(0xFF3B82F6))
        is ExpectedAction.Approval -> Triple("!", action.description, Color(0xFFF59E0B))
        is ExpectedAction.SystemAction -> Triple("...", action.description, AppTheme.textMuted)
        is ExpectedAction.None -> Triple("v", "Task complete", Color(0xFF10B981))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = text,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ActionButtons(
    phase: TaskPhase,
    onApprovePlan: () -> Unit,
    onRejectPlan: (String) -> Unit,
    onApproveStep: () -> Unit,
    onApproveValidation: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Phase-specific buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (phase) {
                is TaskPhase.Planning -> {
                    if (phase.planSteps.isNotEmpty() && !phase.isApproved) {
                        Button(
                            onClick = onApprovePlan,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Approve Plan", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onRejectPlan("Needs changes") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reject", fontSize = 12.sp, color = Color(0xFFEF4444))
                        }
                    }
                }

                is TaskPhase.Executing -> {
                    // Show Approve button when current step is completed but not approved
                    if (!phase.isStepApproved && phase.completedSteps.size > phase.currentStepIndex) {
                        Button(
                            onClick = onApproveStep,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Approve Step", fontSize = 12.sp)
                        }
                    }
                }

                is TaskPhase.Validating -> {
                    if (phase.validationChecks.isNotEmpty() && !phase.isApproved) {
                        Button(
                            onClick = onApproveValidation,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Approve & Complete", fontSize = 12.sp)
                        }
                    }
                }

                is TaskPhase.Paused -> {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.accent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Resume", fontSize = 12.sp)
                    }
                }

                else -> {}
            }
        }

        // Pause/Cancel buttons for active phases (not Paused, not Completed, not Idle)
        if (phase !is TaskPhase.Paused &&
            phase !is TaskPhase.Completed &&
            phase !is TaskPhase.Idle) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pause", fontSize = 12.sp, color = Color(0xFFF59E0B))
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", fontSize = 12.sp, color = Color(0xFFEF4444))
                }
            }
        }

        // Cancel button for Paused state
        if (phase is TaskPhase.Paused) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel Task", fontSize = 12.sp, color = Color(0xFFEF4444))
            }
        }
    }
}
