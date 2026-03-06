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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skrip.aichallenge.domain.invariants.Invariant
import dev.skrip.aichallenge.domain.invariants.InvariantState
import dev.skrip.aichallenge.domain.invariants.InvariantType

@Composable
fun InvariantsPanel(
    invariantState: InvariantState,
    onToggleInvariant: (String, Boolean) -> Unit,
    onResetInvariants: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Reset button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onResetInvariants,
                modifier = Modifier.height(28.dp),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text("Сброс", fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Invariants list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(invariantState.invariants) { invariant ->
                InvariantItem(
                    invariant = invariant,
                    onToggle = { isActive ->
                        onToggleInvariant(invariant.id, isActive)
                    }
                )
            }
        }

        // Stats
        Spacer(modifier = Modifier.height(12.dp))
        val activeCount = invariantState.activeInvariants.size
        val totalCount = invariantState.invariants.size
        Text(
            text = "Активно: $activeCount / $totalCount",
            fontSize = 11.sp,
            color = AppTheme.textMuted
        )
    }
}

@Composable
private fun InvariantItem(
    invariant: Invariant,
    onToggle: (Boolean) -> Unit
) {
    val typeColor = when (invariant.type) {
        InvariantType.MUST -> Color(0xFF10B981)      // Green
        InvariantType.MUST_NOT -> Color(0xFFEF4444)  // Red
        InvariantType.LIMIT -> Color(0xFFF59E0B)     // Orange
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (invariant.isActive) AppTheme.background
                else AppTheme.backgroundSecondary,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (invariant.isActive) typeColor.copy(alpha = 0.3f) else AppTheme.border,
                RoundedCornerShape(8.dp)
            )
            .clickable { onToggle(!invariant.isActive) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = invariant.isActive,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = typeColor,
                uncheckedColor = AppTheme.textMuted
            ),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Type badge
                Box(
                    modifier = Modifier
                        .background(typeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = invariant.type.label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = typeColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = invariant.rule,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (invariant.isActive) AppTheme.textPrimary else AppTheme.textMuted
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = invariant.description,
                fontSize = 11.sp,
                color = AppTheme.textSecondary
            )
        }
    }
}
