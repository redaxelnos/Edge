package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Investigation
import com.example.data.model.KpiCategory
import com.example.data.model.OptimizationTimePoint
import com.example.data.model.PilotKpi
import com.example.data.model.StakeholderInteraction
import com.example.data.model.UseCase
import com.example.ui.theme.GridAmber
import com.example.ui.theme.GridCyan
import com.example.ui.theme.GridEmerald
import com.example.ui.theme.GridGreen
import com.example.ui.theme.GridNavy
import com.example.ui.theme.GridOrange
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

enum class RechartsChartStyle(val label: String) {
    AREA_LINE("Area / Spline"),
    GROUPED_BAR("Grouped Bars")
}

enum class TimeHorizonOption(val label: String, val months: Int) {
    PILOT_6M("6-Mo Pilot", 6),
    ROLLOUT_12M("12-Mo Rollout", 12),
    SCALE_24M("24-Mo Enterprise", 24)
}

@Composable
fun BusinessCaseSection(
    investigation: Investigation?,
    useCases: List<UseCase>,
    pilotKpis: List<PilotKpi> = emptyList(),
    stakeholderInteractions: List<StakeholderInteraction> = emptyList(),
    onUpdateInvestigation: (Investigation) -> Unit = {},
    onEditInvestigation: (Investigation) -> Unit = {},
    onUpdateUseCase: (UseCase) -> Unit = {},
    onDeleteUseCase: (String) -> Unit = {},
    onNavigateToInteractions: () -> Unit = {}
) {
    if (investigation == null) {
        EmptySelectionPrompt("Select an investigation in the Projects tab to visualize labor efficiency, safety guardrails, and workflow optimization over time.")
        return
    }

    var editingUseCase by remember { mutableStateOf<UseCase?>(null) }
    var chartStyle by remember { mutableStateOf(RechartsChartStyle.AREA_LINE) }
    var horizon by remember { mutableStateOf(TimeHorizonOption.ROLLOUT_12M) }
    var showLaborSeries by remember { mutableStateOf(true) }
    var showSafetySeries by remember { mutableStateOf(true) }
    var showWorkflowSeries by remember { mutableStateOf(true) }
    var showTunerPanel by remember { mutableStateOf(false) }

    // Derive target operational gains grounded in project data, KPIs, and stakeholder interactions
    val baseLaborTarget = remember(investigation.id, stakeholderInteractions, pilotKpis, useCases) {
        val fromInteractions = stakeholderInteractions
            .filter { it.investigationId == investigation.id || it.investigationId == "GLOBAL" }
            .map { it.laborEfficiencyGainPct }
            .takeIf { it.isNotEmpty() }
            ?.average()
        val fromKpis = pilotKpis
            .filter { it.category == KpiCategory.EFFICIENCY.name }
            .map { it.currentValue }
            .takeIf { it.isNotEmpty() }
            ?.average()
        (fromInteractions ?: fromKpis ?: (18.0 + useCases.size * 4.5)).coerceIn(12.0, 65.0).toFloat()
    }

    val baseSafetyTarget = remember(investigation.id, stakeholderInteractions, investigation.saidiImprovement) {
        val fromInteractions = stakeholderInteractions
            .filter { it.investigationId == investigation.id || it.investigationId == "GLOBAL" }
            .map { it.safetyImprovementPct }
            .takeIf { it.isNotEmpty() }
            ?.average()
        (fromInteractions ?: (investigation.saidiImprovement * 1.45 + 16.0)).coerceIn(15.0, 75.0).toFloat()
    }

    val baseWorkflowTarget = remember(investigation.id, stakeholderInteractions, investigation.pilotFeasibilityScore) {
        val fromInteractions = stakeholderInteractions
            .filter { it.investigationId == investigation.id || it.investigationId == "GLOBAL" }
            .map { it.workflowOptimizationPct }
            .takeIf { it.isNotEmpty() }
            ?.average()
        (fromInteractions ?: (investigation.pilotFeasibilityScore * 0.48)).coerceIn(16.0, 78.0).toFloat()
    }

    var customLaborTarget by remember(investigation.id, baseLaborTarget) { mutableStateOf(baseLaborTarget) }
    var customSafetyTarget by remember(investigation.id, baseSafetyTarget) { mutableStateOf(baseSafetyTarget) }
    var customWorkflowTarget by remember(investigation.id, baseWorkflowTarget) { mutableStateOf(baseWorkflowTarget) }

    val timeSeriesPoints = remember(
        horizon,
        customLaborTarget,
        customSafetyTarget,
        customWorkflowTarget,
        investigation.pilotFeederSubstation
    ) {
        buildOptimizationTimeSeries(
            horizon = horizon,
            targetLabor = customLaborTarget,
            targetSafety = customSafetyTarget,
            targetWorkflow = customWorkflowTarget,
            feederName = investigation.pilotFeederSubstation.ifBlank { "Primary Pilot Feeder" }
        )
    }

    var selectedPointIndex by remember(timeSeriesPoints) {
        mutableStateOf((timeSeriesPoints.size - 1).coerceAtLeast(0))
    }

    val totalAnnualSavingsDollars = useCases.sumOf { it.projectedAnnualSavings }
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }

    editingUseCase?.let { uc ->
        UseCaseEditorDialog(
            initialUseCase = uc,
            onDismiss = { editingUseCase = null },
            onSaveEdited = { updated ->
                onUpdateUseCase(updated)
                editingUseCase = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("optimization_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
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
                            text = "OPERATIONAL EFFICIENCY, SAFETY & WORKFLOW DASHBOARD",
                            style = MaterialTheme.typography.labelSmall,
                            color = GridCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = investigation.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    FilledTonalButton(
                        onClick = { showTunerPanel = !showTunerPanel },
                        modifier = Modifier.testTag("business_case_edit_params_btn")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showTunerPanel) "Done Tuning" else "Tune Targets")
                    }
                }

                Text(
                    text = "Articulates measurable workforce augmentation (substituting broken manual steps without job loss), preventable lost-time injury guardrails, and simple integration into proven processes—without premature CAPEX/OPEX speculation prior to vendor contract negotiations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToInteractions,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GridCyan),
                        border = BorderStroke(1.dp, GridCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("open_interaction_log_from_dashboard_btn")
                    ) {
                        Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Stakeholder Rebuttals & Buy-In Log (${stakeholderInteractions.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { onEditInvestigation(investigation) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scope", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Interactive Target Tuner Panel
        if (showTunerPanel) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GridCyan.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Interactive Trajectory Calibration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {
                                customLaborTarget = baseLaborTarget
                                customSafetyTarget = baseSafetyTarget
                                customWorkflowTarget = baseWorkflowTarget
                            }
                        ) {
                            Text("Reset to Live Data")
                        }
                    }

                    Column {
                        Text(
                            text = "Target Labor Efficiency Gain: +${customLaborTarget.roundToInt()}% (Task Augmentation, Zero Job Cuts)",
                            style = MaterialTheme.typography.labelMedium,
                            color = GridCyan
                        )
                        Slider(
                            value = customLaborTarget,
                            onValueChange = { customLaborTarget = it },
                            valueRange = 5f..75f
                        )
                    }

                    Column {
                        Text(
                            text = "Preventable Injury & Lost-Time Reduction: +${customSafetyTarget.roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = GridEmerald
                        )
                        Slider(
                            value = customSafetyTarget,
                            onValueChange = { customSafetyTarget = it },
                            valueRange = 5f..80f
                        )
                    }

                    Column {
                        Text(
                            text = "Workflow Optimization & Process Simplicity: +${customWorkflowTarget.roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = GridAmber
                        )
                        Slider(
                            value = customWorkflowTarget,
                            onValueChange = { customWorkflowTarget = it },
                            valueRange = 5f..80f
                        )
                    }
                }
            }
        }

        // 4 Top Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FinancialSummaryCard(
                title = "Labor Efficiency Gain",
                value = String.format(Locale.US, "+%.1f%%", customLaborTarget),
                subtitle = "Substitutes failed manual tasks (No job loss)",
                accentColor = GridCyan,
                modifier = Modifier.weight(1f)
            )
            FinancialSummaryCard(
                title = "Safety & Injury Guardrail",
                value = String.format(Locale.US, "+%.1f%%", customSafetyTarget),
                subtitle = "Preventable lost-time injury reduction",
                accentColor = GridEmerald,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FinancialSummaryCard(
                title = "Workflow Simplicity",
                value = String.format(Locale.US, "+%.1f%%", customWorkflowTarget),
                subtitle = "Integrated into proven SCADA/Maximo steps",
                accentColor = GridAmber,
                modifier = Modifier.weight(1f)
            )
            FinancialSummaryCard(
                title = "SAIDI Reliability Gain",
                value = String.format(Locale.US, "+%.1f%%", investigation.saidiImprovement),
                subtitle = String.format(Locale.US, "Feasibility: %.0f/100 • ADKAR: %.0f%%", investigation.pilotFeasibilityScore, investigation.changeManagementScore),
                accentColor = GridGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // Recharts-Style Multi-Series Interactive Time-Series Chart Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recharts_optimization_chart_card"),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Projected Efficiency, Safety & Workflow Trajectory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap or drag across any time period to inspect telemetry breakdown and rollout milestones",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Horizon & Chart Mode Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeHorizonOption.entries.forEach { opt ->
                        FilterChip(
                            selected = horizon == opt,
                            onClick = { horizon = opt },
                            label = { Text(opt.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("horizon_chip_${opt.name}")
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    RechartsChartStyle.entries.forEach { st ->
                        AssistChip(
                            onClick = { chartStyle = st },
                            label = { Text(st.label, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    if (st == RechartsChartStyle.AREA_LINE) Icons.Default.ShowChart else Icons.Default.BarChart,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (chartStyle == st) GridCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                // Interactive Series Toggles (Recharts Legend)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SeriesToggleChip(
                        label = "Labor Efficiency (%)",
                        color = GridCyan,
                        selected = showLaborSeries,
                        onClick = {
                            if (!showLaborSeries || showSafetySeries || showWorkflowSeries) {
                                showLaborSeries = !showLaborSeries
                            }
                        }
                    )
                    SeriesToggleChip(
                        label = "Safety & Injury Guardrail (%)",
                        color = GridEmerald,
                        selected = showSafetySeries,
                        onClick = {
                            if (!showSafetySeries || showLaborSeries || showWorkflowSeries) {
                                showSafetySeries = !showSafetySeries
                            }
                        }
                    )
                    SeriesToggleChip(
                        label = "Workflow Optimization (%)",
                        color = GridAmber,
                        selected = showWorkflowSeries,
                        onClick = {
                            if (!showWorkflowSeries || showLaborSeries || showSafetySeries) {
                                showWorkflowSeries = !showWorkflowSeries
                            }
                        }
                    )
                }

                // Recharts-Style Canvas Visualization
                RechartsMultiSeriesCanvas(
                    points = timeSeriesPoints,
                    chartStyle = chartStyle,
                    showLabor = showLaborSeries,
                    showSafety = showSafetySeries,
                    showWorkflow = showWorkflowSeries,
                    selectedIndex = selectedPointIndex.coerceIn(0, (timeSeriesPoints.size - 1).coerceAtLeast(0)),
                    onSelectIndex = { selectedPointIndex = it }
                )

                // Interactive Recharts Tooltip Readout Card for Selected Time Point
                val activePoint = timeSeriesPoints.getOrNull(
                    selectedPointIndex.coerceIn(0, (timeSeriesPoints.size - 1).coerceAtLeast(0))
                )
                if (activePoint != null) {
                    RechartsActiveTooltipCard(point = activePoint)
                }
            }
        }

        // Value Stream & Process Integration Breakdown by Use Case
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Proven Workflow Integration by Use Case (${useCases.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Focuses on operational problem-solving, injury prevention, and task augmentation prior to contract pricing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (useCases.isNotEmpty() && totalAnnualSavingsDollars > 0) {
                        Text(
                            text = "${currencyFormatter.format(totalAnnualSavingsDollars)}/yr Value",
                            style = MaterialTheme.typography.labelMedium,
                            color = GridEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (useCases.isEmpty()) {
                    Text(
                        text = "No use cases adopted yet. Adopt or create operational use cases in the 'Use Cases' tab to map how each workflow integrates into existing systems.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    useCases.forEach { uc ->
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(uc.title, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Systems: ${uc.targetSystems} • ${uc.implementationWeeks} wks • Feasibility ${uc.feasibilityScore.roundToInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = GridEmerald.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${uc.riskLevel} RISK",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GridEmerald,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { editingUseCase = uc },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Use Case",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildOptimizationTimeSeries(
    horizon: TimeHorizonOption,
    targetLabor: Float,
    targetSafety: Float,
    targetWorkflow: Float,
    feederName: String
): List<OptimizationTimePoint> {
    val checkpoints = when (horizon) {
        TimeHorizonOption.PILOT_6M -> listOf(
            Triple("Month 1 (Baseline)", "M1", 0.18f to "Baseline manual workflow audit & union crew safety walkdown on $feederName."),
            Triple("Month 2 (Shadow Mode)", "M2", 0.36f to "Shadow-mode telemetry active; zero extra screens added to dispatch console."),
            Triple("Month 3 (Guardrails)", "M3", 0.56f to "Automated hot-line & arc-flash safety guardrails active to prevent lost-time injuries."),
            Triple("Month 4 (Maximo Sync)", "M4", 0.74f to "Direct work-order enrichment inside proven IBM Maximo workflow eliminates manual double-entry."),
            Triple("Month 5 (Crew Validation)", "M5", 0.89f to "Field crews validate substitution of tedious manual foot patrols without job loss."),
            Triple("Month 6 (Pilot Sign-Off)", "M6", 1.00f to "Full pilot target attainment ready for vendor scope & contract negotiations.")
        )
        TimeHorizonOption.ROLLOUT_12M -> listOf(
            Triple("Month 1 (Baseline)", "M1", 0.15f to "Initial process mapping & stakeholder rebuttal alignment across $feederName."),
            Triple("Month 3 (Shadow Pilot)", "M3", 0.35f to "Non-intrusive integration into existing SCADA/Maximo interfaces."),
            Triple("Month 6 (Safety Guardrails)", "M6", 0.62f to "Preventable switching & patrol injury guardrails validated with safety committee."),
            Triple("Month 8 (Crew Augmentation)", "M8", 0.80f to "Tedious manual triage substituted; crews reallocated to high-priority maintenance backlog."),
            Triple("Month 10 (Multi-Feeder)", "M10", 0.92f to "Expanded across regional substation feeders with unified single-pane reporting."),
            Triple("Month 12 (Target State)", "M12", 1.00f to "Sustained labor efficiency, preventable injury reduction, and workflow simplicity.")
        )
        TimeHorizonOption.SCALE_24M -> listOf(
            Triple("Q1 (Discovery)", "Q1", 0.16f to "Stakeholder rebuttal resolution & baseline safety/workflow telemetry."),
            Triple("Q2 (Feeder Pilot)", "Q2", 0.40f to "Field pilot on $feederName proves zero-clutter integration."),
            Triple("Q3 (Safety Lockouts)", "Q3", 0.64f to "Automated hazard guardrails cut preventable time-off injury exposure."),
            Triple("Q4 (Division Rollout)", "Q4", 0.82f to "Standardized across division operations prior to enterprise vendor lock-in."),
            Triple("Q6 (Fleet Integration)", "Q6", 0.95f to "Full IT/OT lakehouse & field dispatch synchronization."),
            Triple("Q8 (Enterprise Scale)", "Q8", 1.08f to "Compounded enterprise-wide labor augmentation and safety improvement.")
        )
    }

    return checkpoints.map { (period, shortLbl, progressAndNote) ->
        val (factor, note) = progressAndNote
        OptimizationTimePoint(
            periodLabel = period,
            shortLabel = shortLbl,
            laborEfficiencyGainPct = (targetLabor * factor).coerceIn(2f, 95f),
            safetyImprovementPct = (targetSafety * (factor * 1.03f)).coerceIn(3f, 98f),
            workflowOptimizationPct = (targetWorkflow * (factor * 0.98f)).coerceIn(3f, 98f),
            milestoneSummary = note
        )
    }
}

@Composable
private fun SeriesToggleChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, if (selected) color else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (selected) color else Color.Gray)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RechartsMultiSeriesCanvas(
    points: List<OptimizationTimePoint>,
    chartStyle: RechartsChartStyle,
    showLabor: Boolean,
    showSafety: Boolean,
    showWorkflow: Boolean,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    if (points.isEmpty()) return

    val maxVal = remember(points) {
        (points.maxOf {
            maxOf(it.laborEfficiencyGainPct, it.safetyImprovementPct, it.workflowOptimizationPct, 25f)
        } * 1.18f).coerceAtMost(100f)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Y-Axis labels column
            Column(
                modifier = Modifier
                    .height(210.dp)
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                listOf(1.0f, 0.75f, 0.5f, 0.25f, 0.0f).forEach { frac ->
                    Text(
                        text = "${(maxVal * frac).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Main Interactive Recharts Canvas
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(210.dp)
                    .testTag("recharts_interactive_canvas")
                    .pointerInput(points.size) {
                        detectTapGestures { offset ->
                            val stepWidth = size.width / points.size.toFloat()
                            val idx = (offset.x / stepWidth).toInt().coerceIn(0, points.size - 1)
                            onSelectIndex(idx)
                        }
                    }
                    .pointerInput(points.size) {
                        detectHorizontalDragGestures { change, _ ->
                            val stepWidth = size.width / points.size.toFloat()
                            val idx = (change.position.x / stepWidth).toInt().coerceIn(0, points.size - 1)
                            onSelectIndex(idx)
                        }
                    }
            ) {
                val count = points.size
                val slotWidth = size.width / count.toFloat()

                // Horizontal dashed gridlines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                for (i in 0..4) {
                    val y = size.height * (i / 4f)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.22f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.2f,
                        pathEffect = if (i < 4) dashEffect else null
                    )
                }

                // Highlighted active column crosshair
                val activeCenterX = slotWidth * selectedIndex + slotWidth / 2f
                drawRoundRect(
                    color = GridCyan.copy(alpha = 0.08f),
                    topLeft = Offset(slotWidth * selectedIndex + 4f, 0f),
                    size = Size((slotWidth - 8f).coerceAtLeast(8f), size.height),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawLine(
                    color = GridCyan.copy(alpha = 0.55f),
                    start = Offset(activeCenterX, 0f),
                    end = Offset(activeCenterX, size.height),
                    strokeWidth = 2f,
                    pathEffect = dashEffect
                )

                if (chartStyle == RechartsChartStyle.GROUPED_BAR) {
                    val barW = (slotWidth * 0.22f).coerceAtMost(24f)
                    points.forEachIndexed { idx, pt ->
                        val cx = slotWidth * idx + slotWidth / 2f
                        if (showLabor) {
                            val h = (pt.laborEfficiencyGainPct / maxVal).coerceIn(0.03f, 1f) * size.height
                            drawRoundRect(
                                color = GridCyan,
                                topLeft = Offset(cx - barW * 1.6f, size.height - h),
                                size = Size(barW, h),
                                cornerRadius = CornerRadius(5f, 5f)
                            )
                        }
                        if (showSafety) {
                            val h = (pt.safetyImprovementPct / maxVal).coerceIn(0.03f, 1f) * size.height
                            drawRoundRect(
                                color = GridEmerald,
                                topLeft = Offset(cx - barW * 0.5f, size.height - h),
                                size = Size(barW, h),
                                cornerRadius = CornerRadius(5f, 5f)
                            )
                        }
                        if (showWorkflow) {
                            val h = (pt.workflowOptimizationPct / maxVal).coerceIn(0.03f, 1f) * size.height
                            drawRoundRect(
                                color = GridAmber,
                                topLeft = Offset(cx + barW * 0.6f, size.height - h),
                                size = Size(barW, h),
                                cornerRadius = CornerRadius(5f, 5f)
                            )
                        }
                    }
                } else {
                    fun drawSmoothSeries(values: List<Float>, seriesColor: Color) {
                        if (values.isEmpty()) return
                        val coords = values.mapIndexed { idx, v ->
                            val x = slotWidth * idx + slotWidth / 2f
                            val y = size.height - ((v / maxVal).coerceIn(0.02f, 0.98f) * size.height)
                            Offset(x, y)
                        }

                        val linePath = Path().apply {
                            moveTo(coords.first().x, coords.first().y)
                            for (i in 0 until coords.size - 1) {
                                val p0 = coords[i]
                                val p1 = coords[i + 1]
                                val controlX1 = p0.x + (p1.x - p0.x) * 0.5f
                                val controlX2 = p0.x + (p1.x - p0.x) * 0.5f
                                cubicTo(controlX1, p0.y, controlX2, p1.y, p1.x, p1.y)
                            }
                        }

                        val areaPath = Path().apply {
                            addPath(linePath)
                            lineTo(coords.last().x, size.height)
                            lineTo(coords.first().x, size.height)
                            close()
                        }

                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(seriesColor.copy(alpha = 0.28f), seriesColor.copy(alpha = 0.01f))
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = seriesColor,
                            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        coords.forEachIndexed { idx, pt ->
                            val isSelected = idx == selectedIndex
                            drawCircle(
                                color = if (isSelected) Color.White else seriesColor,
                                radius = if (isSelected) 6.5.dp.toPx() else 4.dp.toPx(),
                                center = pt
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = seriesColor,
                                    radius = 4.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }

                    if (showWorkflow) {
                        drawSmoothSeries(points.map { it.workflowOptimizationPct }, GridAmber)
                    }
                    if (showSafety) {
                        drawSmoothSeries(points.map { it.safetyImprovementPct }, GridEmerald)
                    }
                    if (showLabor) {
                        drawSmoothSeries(points.map { it.laborEfficiencyGainPct }, GridCyan)
                    }
                }
            }
        }

        // X-Axis Interactive Time Period Selector Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            points.forEachIndexed { idx, pt ->
                val isSelected = idx == selectedIndex
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectIndex(idx) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) GridCyan.copy(alpha = 0.16f) else Color.Transparent
                ) {
                    Text(
                        text = pt.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) GridCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RechartsActiveTooltipCard(point: OptimizationTimePoint) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recharts_active_tooltip_card"),
        shape = RoundedCornerShape(14.dp),
        color = GridNavy,
        border = BorderStroke(1.dp, GridCyan.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = point.periodLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "TELEMETRY SNAPSHOT",
                    style = MaterialTheme.typography.labelSmall,
                    color = GridCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TooltipSeriesMetric(
                    label = "Labor Efficiency",
                    value = String.format(Locale.US, "+%.1f%%", point.laborEfficiencyGainPct),
                    color = GridCyan,
                    modifier = Modifier.weight(1f)
                )
                TooltipSeriesMetric(
                    label = "Safety Guardrail",
                    value = String.format(Locale.US, "+%.1f%%", point.safetyImprovementPct),
                    color = GridEmerald,
                    modifier = Modifier.weight(1f)
                )
                TooltipSeriesMetric(
                    label = "Workflow Opt.",
                    value = String.format(Locale.US, "+%.1f%%", point.workflowOptimizationPct),
                    color = GridAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = point.milestoneSummary,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun TooltipSeriesMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1
            )
        }
    }
}

@Composable
fun FinancialSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = RoundedCornerShape(3.dp),
            color = color
        ) {}
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
