package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Interview
import com.example.data.model.Investigation
import com.example.data.model.InvestigationStatus
import com.example.data.model.PilotKpi
import com.example.data.model.StakeholderProfile
import com.example.data.model.SystemIntegrationStatus
import com.example.data.model.UseCase
import com.example.ui.theme.GridCyan
import com.example.ui.theme.GridEmerald
import com.example.ui.theme.GridGreen
import com.example.ui.theme.GridNavy
import com.example.ui.theme.GridOrange
import java.util.Locale

data class NativeBarPoint(
    val label: String,
    val value: Float,
    val secondaryValue: Float = 0f
)

@Composable
fun NativeFinancialBarChart(
    points: List<NativeBarPoint>,
    primaryColor: Color,
    secondaryColor: Color,
    unitSuffix: String,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val maxVal = points.maxOf { maxOf(it.value, it.secondaryValue, 1f) } * 1.15f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val count = points.size
            val groupWidth = size.width / count.toFloat()
            val barWidth = (groupWidth * 0.28f).coerceAtMost(34f)

            // Subtle horizontal grid lines
            for (i in 1..3) {
                val y = size.height * (1f - i / 4f)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            points.forEachIndexed { idx, pt ->
                val centerX = groupWidth * idx + groupWidth / 2f
                val h1 = ((pt.value / maxVal).coerceIn(0.03f, 1f)) * size.height
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(centerX - barWidth - 3f, size.height - h1),
                    size = Size(barWidth, h1),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                val h2 = ((pt.secondaryValue / maxVal).coerceIn(0.03f, 1f)) * size.height
                drawRoundRect(
                    color = secondaryColor.copy(alpha = 0.78f),
                    topLeft = Offset(centerX + 3f, size.height - h2),
                    size = Size(barWidth, h2),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            points.forEach { pt ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%s", pt.value, unitSuffix),
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

fun Investigation.statusLabel(): String {
    return runCatching { InvestigationStatus.valueOf(status).label }.getOrDefault(status)
}

fun Investigation.activeIntegrationCount(): Int {
    val statuses = listOf(
        maximoIntegrationStatus,
        azureIntegrationStatus,
        oracleIntegrationStatus,
        snowflakeIntegrationStatus,
        copilotReportingStatus
    )
    return statuses.count {
        it == SystemIntegrationStatus.VERIFIED.name || it == SystemIntegrationStatus.INTEGRATED.name || it == SystemIntegrationStatus.PLANNED.name
    }
}

fun Investigation.isPilotStageOrBeyond(): Boolean {
    return status in listOf(
        InvestigationStatus.PILOT.name,
        InvestigationStatus.CHANGE_MANAGEMENT.name,
        InvestigationStatus.ECOSYSTEM_ADOPTION.name
    )
}

@Composable
fun ActiveInvestigationContextBar(
    investigations: List<Investigation>,
    selectedInvestigation: Investigation?,
    stakeholderProfilesCount: Int,
    interactionsCount: Int = 0,
    interviewsCount: Int,
    useCasesCount: Int,
    kpisCount: Int,
    showGuide: Boolean,
    onToggleGuide: () -> Unit,
    onSelectInvestigation: (String) -> Unit,
    onEditInvestigation: (Investigation) -> Unit,
    onUpdateInvestigation: (Investigation) -> Unit
) {
    var projectMenuExpanded by remember { mutableStateOf(false) }
    var stageMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_investigation_context_bar"),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = investigations.isNotEmpty()) {
                                projectMenuExpanded = true
                            }
                            .testTag("active_project_selector_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (selectedInvestigation != null) GridGreen else GridOrange)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "ACTIVE INVESTIGATION WORKSPACE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = selectedInvestigation?.title ?: "No Project Selected — Create or Select in Projects Tab",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (investigations.isNotEmpty()) {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Investigation",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = projectMenuExpanded,
                        onDismissRequest = { projectMenuExpanded = false }
                    ) {
                        investigations.forEach { inv ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(inv.title, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${inv.technologyType} • ${inv.vendor} • ${inv.statusLabel()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        if (inv.id == selectedInvestigation?.id) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (inv.id == selectedInvestigation?.id) GridGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    onSelectInvestigation(inv.id)
                                    projectMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (selectedInvestigation != null) {
                    FilledTonalIconButton(
                        onClick = { onEditInvestigation(selectedInvestigation) },
                        modifier = Modifier.testTag("context_bar_edit_project_btn")
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Edit All Project Attributes"
                        )
                    }
                }

                IconButton(
                    onClick = onToggleGuide,
                    modifier = Modifier.testTag("toggle_workflow_guide_btn")
                ) {
                    Icon(
                        if (showGuide) Icons.Default.Help else Icons.Default.HelpOutline,
                        contentDescription = "Toggle Step-by-Step UX Guide",
                        tint = if (showGuide) GridCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (selectedInvestigation != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        AssistChip(
                            onClick = { stageMenuExpanded = true },
                            label = {
                                Text(
                                    "Stage: ${selectedInvestigation.statusLabel()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Flag,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = GridCyan
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Change Stage",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("lifecycle_stage_chip")
                        )
                        DropdownMenu(
                            expanded = stageMenuExpanded,
                            onDismissRequest = { stageMenuExpanded = false }
                        ) {
                            InvestigationStatus.entries.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st.label) },
                                    leadingIcon = {
                                        if (st.name == selectedInvestigation.status) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = GridGreen)
                                        }
                                    },
                                    onClick = {
                                        onUpdateInvestigation(selectedInvestigation.copy(status = st.name))
                                        stageMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    LivePipelineBadge(label = "Stakeholders", count = stakeholderProfilesCount)
                    LivePipelineBadge(label = "Rebuttals & Buy-In", count = interactionsCount)
                    LivePipelineBadge(label = "Interviews", count = interviewsCount)
                    LivePipelineBadge(label = "Use Cases", count = useCasesCount)
                    LivePipelineBadge(label = "Pilot KPIs", count = kpisCount)
                }
            }
        }
    }
}

@Composable
private fun LivePipelineBadge(label: String, count: Int) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (count > 0) GridEmerald else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InteractiveWorkflowGuideCard(
    currentTab: Int,
    selectedInvestigation: Investigation?,
    stakeholderProfiles: List<StakeholderProfile>,
    interactionsCount: Int = 0,
    interviews: List<Interview>,
    useCases: List<UseCase>,
    pilotKpis: List<PilotKpi>,
    onNavigateToTab: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val stepInfo = remember(currentTab, selectedInvestigation, stakeholderProfiles.size, interactionsCount, interviews.size, useCases.size, pilotKpis.size) {
        buildWorkflowStepGuidance(
            currentTab = currentTab,
            inv = selectedInvestigation,
            profilesCount = stakeholderProfiles.size,
            interactionsCount = interactionsCount,
            interviewsCount = interviews.size,
            useCasesCount = useCases.size,
            kpisCount = pilotKpis.size
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("interactive_workflow_guide_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = GridNavy.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, GridCyan.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = GridCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stepInfo.stageTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Hide Workflow Guide",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = stepInfo.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stepInfo.liveStatusNote,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = GridEmerald,
                    modifier = Modifier.weight(1f)
                )

                if (stepInfo.nextTabIndex != null && stepInfo.nextTabLabel != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { onNavigateToTab(stepInfo.nextTabIndex) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("workflow_guide_next_step_btn")
                    ) {
                        Text(
                            text = stepInfo.nextTabLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class WorkflowStepGuidance(
    val stageTitle: String,
    val explanation: String,
    val liveStatusNote: String,
    val nextTabIndex: Int?,
    val nextTabLabel: String?
)

private fun buildWorkflowStepGuidance(
    currentTab: Int,
    inv: Investigation?,
    profilesCount: Int,
    interactionsCount: Int,
    interviewsCount: Int,
    useCasesCount: Int,
    kpisCount: Int
): WorkflowStepGuidance {
    return when (currentTab) {
        0 -> WorkflowStepGuidance(
            stageTitle = "Step 1 of 11 • Projects & Portfolio Setup",
            explanation = "Define your grid technology investigation (efficiency targets, SAIDI reliability gain, safety guardrails, vendor, and pilot scope) without premature CAPEX/OPEX speculation.",
            liveStatusNote = if (inv != null) "Active: ${inv.title} (${inv.statusLabel()})" else "Create or select a project below to begin.",
            nextTabIndex = 1,
            nextTabLabel = "Next: Stakeholders"
        )
        1 -> WorkflowStepGuidance(
            stageTitle = "Step 2 of 11 • Stakeholder Directory & Influence Mapping",
            explanation = "Register key utility personnel, their grid domain (Generation, Transmission, Distribution), organizational role, and influence level.",
            liveStatusNote = "$profilesCount stakeholder profile(s) stored in Firestore.",
            nextTabIndex = 2,
            nextTabLabel = "Next: Interaction Log"
        )
        2 -> WorkflowStepGuidance(
            stageTitle = "Step 3 of 11 • Stakeholder Interaction Log & Rebuttal Playbook",
            explanation = "Track communication history, identified stakeholder rebuttals, and strategies for achieving buy-in through workforce augmentation (zero job loss), preventable injury guardrails, and workflow simplicity.",
            liveStatusNote = "$interactionsCount interaction(s) & buy-in strategies logged in Firestore.",
            nextTabIndex = 3,
            nextTabLabel = "Next: Interviews"
        )
        3 -> WorkflowStepGuidance(
            stageTitle = "Step 4 of 11 • Stakeholder Interviews & Domain Sentiment",
            explanation = "Log field interviews, link them to registered stakeholders or custom roles, rate priority (1–5) and sentiment, and record technical requirements. Updates the Domain × Sentiment Heatmap live.",
            liveStatusNote = "$interviewsCount interview(s) captured for ${inv?.title ?: "current project"}.",
            nextTabIndex = 4,
            nextTabLabel = "Next: Use Cases"
        )
        4 -> WorkflowStepGuidance(
            stageTitle = "Step 5 of 11 • Operational Use Case Synthesis",
            explanation = "Generate AI-backed operational use cases from captured interview notes or author/edit custom use cases focused on simple integration into proven processes.",
            liveStatusNote = "$useCasesCount adopted use case(s) linked to this investigation.",
            nextTabIndex = 5,
            nextTabLabel = "Next: Impact Dashboard"
        )
        5 -> WorkflowStepGuidance(
            stageTitle = "Step 6 of 11 • Efficiency, Safety & Workflow Optimization Dashboard",
            explanation = "Visualize projected labor efficiency gains, preventable injury reduction guardrails, and workflow optimization metrics over time using the interactive multi-series chart.",
            liveStatusNote = if (inv != null) String.format(Locale.US, "SAIDI +%.1f%% • Feasibility %.0f/100 • ADKAR %.0f%%", inv.saidiImprovement, inv.pilotFeasibilityScore, inv.changeManagementScore) else "Select an investigation to view telemetry trajectory.",
            nextTabIndex = 6,
            nextTabLabel = "Next: Integrations"
        )
        6 -> WorkflowStepGuidance(
            stageTitle = "Step 7 of 11 • Enterprise IT/OT System Integrations",
            explanation = "Configure integration readiness across IBM Maximo, Azure Digital Twins, Oracle Utilities, Snowflake Grid Lakehouse, and Copilot Reporting before field deployment.",
            liveStatusNote = "${inv?.activeIntegrationCount() ?: 0} of 5 enterprise connectors active/planned.",
            nextTabIndex = 7,
            nextTabLabel = "Next: Pilot Ops"
        )
        7 -> WorkflowStepGuidance(
            stageTitle = "Step 8 of 11 • Push-to-Pilot & Live Telemetry KPIs",
            explanation = "Deploy the investigation to a target substation feeder, log and edit real-time field KPIs (current vs. target), and track measured target attainment.",
            liveStatusNote = "$kpisCount field KPI(s) recorded (${if (inv?.isPilotStageOrBeyond() == true) "Pilot Active on ${inv.pilotFeederSubstation}" else "Pre-Pilot Stage"}).",
            nextTabIndex = 8,
            nextTabLabel = "Next: Change Mgmt"
        )
        8 -> WorkflowStepGuidance(
            stageTitle = "Step 9 of 11 • ADKAR Organizational Change Management",
            explanation = "Adjust interactive ADKAR readiness sliders (Awareness, Desire, Knowledge, Ability, Reinforcement) for utility control room & field crews and persist the composite score to Firestore.",
            liveStatusNote = String.format(Locale.US, "Current ADKAR Readiness: %.1f%%", inv?.changeManagementScore ?: 0.0),
            nextTabIndex = 9,
            nextTabLabel = "Next: AI Strategy"
        )
        9 -> WorkflowStepGuidance(
            stageTitle = "Step 10 of 11 • Executive AI Strategy & Multimodal Analysis",
            explanation = "Synthesize a comprehensive executive recommendation grounded in your captured interviews, rebuttals, adopted use cases, and measured pilot KPIs.",
            liveStatusNote = if (!inv?.strategicSummary.isNullOrBlank()) "Executive summary saved (${inv?.strategicSummary?.length ?: 0} chars)." else "Ready to generate or author strategic summary.",
            nextTabIndex = 10,
            nextTabLabel = "Next: PDF Report"
        )
        else -> WorkflowStepGuidance(
            stageTitle = "Step 11 of 11 • Stakeholder Summary PDF Dossier",
            explanation = "Compile all live project metrics, stakeholder interviews, adopted use cases, and pilot KPIs into a multi-page vector PDF report for utility executives and regulators.",
            liveStatusNote = "Ready to export $interviewsCount interviews, $useCasesCount use cases, and $kpisCount KPIs.",
            nextTabIndex = null,
            nextTabLabel = null
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInvestigationDialog(
    investigation: Investigation,
    useCases: List<UseCase>,
    onDismiss: () -> Unit,
    onSave: (Investigation) -> Unit
) {
    var title by remember { mutableStateOf(investigation.title) }
    var technologyType by remember { mutableStateOf(investigation.technologyType) }
    var vendor by remember { mutableStateOf(investigation.vendor) }
    var status by remember { mutableStateOf(investigation.status) }
    var description by remember { mutableStateOf(investigation.description) }
    var capex by remember { mutableStateOf(investigation.capexEstimate.toString()) }
    var opex by remember { mutableStateOf(investigation.opexEstimate.toString()) }
    var projectedRoi by remember { mutableStateOf(investigation.projectedRoi.toString()) }
    var paybackYears by remember { mutableStateOf(investigation.paybackYears.toString()) }
    var saidiImprovement by remember { mutableStateOf(investigation.saidiImprovement.toString()) }
    var feasibilityScore by remember { mutableStateOf(investigation.pilotFeasibilityScore.toString()) }
    var pilotFeeder by remember { mutableStateOf(investigation.pilotFeederSubstation) }
    var pilotWeeks by remember { mutableStateOf(investigation.pilotDurationWeeks.toString()) }
    var pilotSuccessRate by remember { mutableStateOf(investigation.pilotSuccessRate.toString()) }
    var changeScore by remember { mutableStateOf(investigation.changeManagementScore.toString()) }
    var strategicSummary by remember { mutableStateOf(investigation.strategicSummary) }

    val techCategories = listOf("ADMS", "DERMS", "AMI 2.0", "FLISR", "DLR", "Substation Automation", "Synchrophasor / PMU", "BESS Control")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Edit Investigation Attributes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Customize technical, financial, pilot, and lifecycle parameters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Investigation Title *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_inv_title_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = technologyType,
                            onValueChange = { technologyType = it },
                            label = { Text("Technology Type *") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_inv_tech_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = vendor,
                            onValueChange = { vendor = it },
                            label = { Text("Primary Vendor *") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_inv_vendor_input"),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        techCategories.forEach { cat ->
                            FilterChip(
                                selected = technologyType.equals(cat, ignoreCase = true),
                                onClick = { technologyType = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text(
                        text = "Lifecycle Stage",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InvestigationStatus.entries.forEach { st ->
                            FilterChip(
                                selected = status == st.name,
                                onClick = { status = st.name },
                                label = { Text(st.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Technical Scope & Operational Objectives") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_inv_desc_input"),
                        minLines = 3
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Operational Efficiency, Safety Guardrails & Reliability Parameters",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = projectedRoi,
                            onValueChange = { projectedRoi = it },
                            label = { Text("Efficiency Gain (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = paybackYears,
                            onValueChange = { paybackYears = it },
                            label = { Text("Rollout Horizon (Yrs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = saidiImprovement,
                            onValueChange = { saidiImprovement = it },
                            label = { Text("SAIDI Gain (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = feasibilityScore,
                            onValueChange = { feasibilityScore = it },
                            label = { Text("Feasibility (0-100)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Field Pilot & Change Management Configuration",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = pilotFeeder,
                            onValueChange = { pilotFeeder = it },
                            label = { Text("Target Feeder / Substation") },
                            modifier = Modifier.weight(1.4f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = pilotWeeks,
                            onValueChange = { pilotWeeks = it },
                            label = { Text("Pilot Weeks") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = pilotSuccessRate,
                            onValueChange = { pilotSuccessRate = it },
                            label = { Text("Pilot Attainment (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = changeScore,
                            onValueChange = { changeScore = it },
                            label = { Text("ADKAR Score (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = strategicSummary,
                        onValueChange = { strategicSummary = it },
                        label = { Text("Executive Strategic Summary (Optional / Editable)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    investigation.copy(
                                        title = title.trim(),
                                        technologyType = technologyType.trim().ifBlank { "Grid Modernization" },
                                        vendor = vendor.trim().ifBlank { "TBD" },
                                        status = status,
                                        description = description.trim(),
                                        capexEstimate = capex.toDoubleOrNull() ?: investigation.capexEstimate,
                                        opexEstimate = opex.toDoubleOrNull() ?: investigation.opexEstimate,
                                        projectedRoi = projectedRoi.toDoubleOrNull() ?: investigation.projectedRoi,
                                        paybackYears = paybackYears.toDoubleOrNull() ?: investigation.paybackYears,
                                        saidiImprovement = saidiImprovement.toDoubleOrNull() ?: investigation.saidiImprovement,
                                        pilotFeasibilityScore = (feasibilityScore.toDoubleOrNull() ?: investigation.pilotFeasibilityScore).coerceIn(0.0, 100.0),
                                        pilotFeederSubstation = pilotFeeder.trim().ifBlank { "Substation 14 - Feeder 3" },
                                        pilotDurationWeeks = (pilotWeeks.toIntOrNull() ?: investigation.pilotDurationWeeks).coerceAtLeast(1),
                                        pilotSuccessRate = (pilotSuccessRate.toDoubleOrNull() ?: investigation.pilotSuccessRate).coerceIn(0.0, 100.0),
                                        changeManagementScore = (changeScore.toDoubleOrNull() ?: investigation.changeManagementScore).coerceIn(0.0, 100.0),
                                        strategicSummary = strategicSummary.trim()
                                    )
                                )
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.testTag("save_edited_investigation_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
