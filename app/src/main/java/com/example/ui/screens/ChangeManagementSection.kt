package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Interview
import com.example.data.model.Investigation
import com.example.data.model.StakeholderSentiment
import com.example.ui.theme.GridAmber
import com.example.ui.theme.GridCyan
import com.example.ui.theme.GridEmerald
import com.example.ui.theme.GridGreen
import com.example.ui.theme.GridNavy
import com.example.ui.theme.GridOrange
import java.util.Locale
import kotlin.math.roundToInt

data class AdkarStageState(
    val letter: String,
    val title: String,
    val targetAudience: String,
    val actionPlan: String,
    val scorePercent: Int
)

@Composable
fun ChangeManagementSection(
    investigation: Investigation?,
    interviews: List<Interview> = emptyList(),
    onUpdateInvestigation: (Investigation) -> Unit = {}
) {
    if (investigation == null) {
        EmptySelectionPrompt("Select an investigation in the Projects tab to manage organizational change and training readiness.")
        return
    }

    val baseScore = investigation.changeManagementScore.roundToInt().coerceIn(0, 100)

    var stages by remember(investigation.id, baseScore) {
        mutableStateOf(
            listOf(
                AdkarStageState(
                    letter = "A",
                    title = "Awareness of Grid Modernization Need",
                    targetAudience = "Control Room Operators, Protection Engineers & Dispatchers",
                    actionPlan = "Conduct town halls on legacy outage bottlenecks, DER hosting constraints, and regulatory SAIDI targets.",
                    scorePercent = (baseScore + 6).coerceIn(0, 100)
                ),
                AdkarStageState(
                    letter = "D",
                    title = "Desire to Participate in Field Pilot",
                    targetAudience = "Union Field Crews & Substation Technicians",
                    actionPlan = "Co-design switching workflows with senior operators to reduce alarm fatigue and manual switching risk.",
                    scorePercent = (baseScore - 2).coerceIn(0, 100)
                ),
                AdkarStageState(
                    letter = "K",
                    title = "Knowledge of New Operating Procedures",
                    targetAudience = "Distribution Operations & SCADA Support",
                    actionPlan = "Deliver hands-on simulator training for ${investigation.technologyType} HMI screens and automated failover.",
                    scorePercent = baseScore.coerceIn(0, 100)
                ),
                AdkarStageState(
                    letter = "A",
                    title = "Ability to Execute in Live Grid Conditions",
                    targetAudience = "Primary Pilot Feeder Team (${investigation.pilotFeederSubstation})",
                    actionPlan = "Run shadow-mode parallel operations for 4 weeks prior to closed-loop control cutover.",
                    scorePercent = (baseScore - 4).coerceIn(0, 100)
                ),
                AdkarStageState(
                    letter = "R",
                    title = "Reinforcement & Continuous Governance",
                    targetAudience = "Utility Executive Sponsors & Regulatory Affairs",
                    actionPlan = "Publish weekly pilot reliability scorecards and recognize operator contributions to SAIDI reduction.",
                    scorePercent = baseScore.coerceIn(0, 100)
                )
            )
        )
    }

    var editingIndex by remember { mutableStateOf<Int?>(null) }
    val compositeScore = remember(stages) {
        stages.map { it.scorePercent }.average()
    }
    val hasUnsavedChanges = remember(compositeScore, investigation.changeManagementScore) {
        kotlin.math.abs(compositeScore - investigation.changeManagementScore) >= 0.5
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GridNavy)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ORGANIZATIONAL CHANGE MANAGEMENT (ADKAR)",
                                style = MaterialTheme.typography.labelSmall,
                                color = GridCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Workforce Adoption: ${investigation.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = GridEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f%% Ready", compositeScore),
                                style = MaterialTheme.typography.titleSmall,
                                color = GridEmerald,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = "Adjust the interactive ADKAR readiness sliders below or derive initial workforce sentiment from your ${interviews.size} captured stakeholder interviews.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onUpdateInvestigation(
                                    investigation.copy(
                                        changeManagementScore = Math.round(compositeScore * 10.0) / 10.0
                                    )
                                )
                            },
                            enabled = hasUnsavedChanges,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GridGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_adkar_score_btn")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (hasUnsavedChanges) String.format(Locale.US, "Save %.1f%% Score", compositeScore)
                                else "ADKAR Score Saved",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (interviews.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    val sentimentAvg = interviews.map {
                                        val s = runCatching { StakeholderSentiment.valueOf(it.sentiment) }
                                            .getOrDefault(StakeholderSentiment.NEUTRAL)
                                        when (s) {
                                            StakeholderSentiment.POSITIVE -> 92
                                            StakeholderSentiment.NEUTRAL -> 76
                                            StakeholderSentiment.CAUTIOUS -> 62
                                            StakeholderSentiment.CONCERNED -> 48
                                        }
                                    }.average().roundToInt()
                                    stages = stages.mapIndexed { idx, st ->
                                        val offset = when (idx) {
                                            0 -> 6
                                            1 -> -2
                                            2 -> 0
                                            3 -> -4
                                            else -> 2
                                        }
                                        st.copy(scorePercent = (sentimentAvg + offset).coerceIn(20, 100))
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GridCyan),
                                border = BorderStroke(1.dp, GridCyan.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("derive_adkar_from_interviews_btn")
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Derive from Interviews", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        itemsIndexed(stages) { index, stage ->
            val stageStatusLabel = when {
                stage.scorePercent >= 85 -> "Completed"
                stage.scorePercent >= 60 -> "In Progress"
                else -> "Needs Attention"
            }
            val statusColor = when {
                stage.scorePercent >= 85 -> GridGreen
                stage.scorePercent >= 60 -> GridAmber
                else -> GridOrange
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("adkar_card_$index"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = stage.letter,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stage.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { editingIndex = if (editingIndex == index) null else index },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Stage Action Plan",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = "Audience: ${stage.targetAudience}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (editingIndex == index) {
                        OutlinedTextField(
                            value = stage.targetAudience,
                            onValueChange = { updatedText ->
                                stages = stages.toMutableList().also {
                                    it[index] = stage.copy(targetAudience = updatedText)
                                }
                            },
                            label = { Text("Target Utility Audience") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = stage.actionPlan,
                            onValueChange = { updatedPlan ->
                                stages = stages.toMutableList().also {
                                    it[index] = stage.copy(actionPlan = updatedPlan)
                                }
                            },
                            label = { Text("Action Plan & Rollout Deliverables") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    } else {
                        Text(
                            text = stage.actionPlan,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = stageStatusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "Readiness: ${stage.scorePercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    Slider(
                        value = stage.scorePercent.toFloat(),
                        onValueChange = { newVal ->
                            stages = stages.toMutableList().also {
                                it[index] = stage.copy(scorePercent = newVal.roundToInt())
                            }
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("adkar_slider_$index")
                    )
                }
            }
        }
    }
}
