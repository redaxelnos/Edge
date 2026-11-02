package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BuyInStatus
import com.example.data.model.GridDomain
import com.example.data.model.Investigation
import com.example.data.model.RebuttalCategory
import com.example.data.model.StakeholderInteraction
import com.example.data.model.StakeholderProfile
import com.example.ui.theme.GridAmber
import com.example.ui.theme.GridCyan
import com.example.ui.theme.GridEmerald
import com.example.ui.theme.GridGreen
import com.example.ui.theme.GridNavy
import com.example.ui.theme.GridOrange
import com.example.ui.theme.GridRed
import java.util.Locale
import kotlin.math.roundToInt

private data class RebuttalPlaybookTemplate(
    val title: String,
    val category: RebuttalCategory,
    val rebuttalText: String,
    val strategyText: String,
    val defaultLaborGain: Float,
    val defaultSafetyGain: Float,
    val defaultWorkflowGain: Float
)

private val PLAYBOOK_TEMPLATES = listOf(
    RebuttalPlaybookTemplate(
        title = "Union Crew: Job Loss vs. Task Augmentation",
        category = RebuttalCategory.WORKFORCE_AUGMENTATION,
        rebuttalText = "Field crews worry automated line/fault telemetry will be used to justify headcount reductions rather than supporting linemen.",
        strategyText = "Position technology strictly as workforce augmentation that substitutes low-yield, hazardous manual foot patrols and tedious paperwork—zero job loss, freeing senior crews for high-priority grid repairs.",
        defaultLaborGain = 32f,
        defaultSafetyGain = 40f,
        defaultWorkflowGain = 36f
    ),
    RebuttalPlaybookTemplate(
        title = "Safety Committee: Preventable Time-Off Injuries",
        category = RebuttalCategory.SAFETY_INJURY_GUARDRAILS,
        rebuttalText = "High overtime and repetitive manual switching in harsh conditions are driving preventable musculoskeletal and arc-exposure lost-time injuries.",
        strategyText = "Implement automated pre-switching safety guardrails and remote condition checks that eliminate unnecessary bucket-truck climbs and hot-stick exposure, directly cutting preventable injuries and lost workdays.",
        defaultLaborGain = 24f,
        defaultSafetyGain = 48f,
        defaultWorkflowGain = 34f
    ),
    RebuttalPlaybookTemplate(
        title = "Control Room: Tool Clutter vs. Simplicity",
        category = RebuttalCategory.PROCESS_SIMPLICITY,
        rebuttalText = "Operators reject adding another standalone dashboard or login that complicates storm restoration and increases alarm fatigue.",
        strategyText = "Eliminate standalone screens—surface validated actionable recommendations directly inside the existing SCADA/Maximo workflow, substituting 12 manual cross-checks with 1 unified guardrail.",
        defaultLaborGain = 28f,
        defaultSafetyGain = 30f,
        defaultWorkflowGain = 45f
    ),
    RebuttalPlaybookTemplate(
        title = "IT/OT & Engineering: Proven Process Fit",
        category = RebuttalCategory.PROVEN_WORKFLOW_FIT,
        rebuttalText = "Skepticism that new analytics will bypass proven protection coordination and asset management governance.",
        strategyText = "Augment the proven Maximo work-order and relay protection review process by auto-attaching verified waveform/thermal evidence to existing tickets rather than changing sign-off authority.",
        defaultLaborGain = 22f,
        defaultSafetyGain = 27f,
        defaultWorkflowGain = 39f
    )
)

@Composable
fun StakeholderInteractionLogSection(
    investigation: Investigation?,
    interactions: List<StakeholderInteraction>,
    stakeholderProfiles: List<StakeholderProfile>,
    onSaveInteraction: (
        investigationId: String,
        stakeholderName: String,
        stakeholderRole: String,
        department: String,
        gridDomain: String,
        communicationChannel: String,
        interactionDateLabel: String,
        communicationSummary: String,
        identifiedRebuttals: String,
        rebuttalCategory: String,
        buyInStrategy: String,
        buyInStatus: String,
        laborEfficiencyGainPct: Double,
        safetyImprovementPct: Double,
        workflowOptimizationPct: Double
    ) -> Unit,
    onUpdateInteraction: (StakeholderInteraction) -> Unit,
    onDeleteInteraction: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingInteraction by remember { mutableStateOf<StakeholderInteraction?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<BuyInStatus?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<RebuttalCategory?>(null) }
    var filterToActiveInvestigation by remember { mutableStateOf(false) }

    val filteredInteractions = remember(
        interactions,
        searchQuery,
        selectedStatusFilter,
        selectedCategoryFilter,
        filterToActiveInvestigation,
        investigation?.id
    ) {
        interactions.filter { item ->
            val matchesInv = !filterToActiveInvestigation ||
                investigation == null ||
                item.investigationId == investigation.id ||
                item.investigationId == "GLOBAL"
            val matchesStatus = selectedStatusFilter == null || item.buyInStatus == selectedStatusFilter?.name
            val matchesCategory = selectedCategoryFilter == null || item.rebuttalCategory == selectedCategoryFilter?.name
            val matchesSearch = searchQuery.isBlank() ||
                item.stakeholderName.contains(searchQuery, ignoreCase = true) ||
                item.stakeholderRole.contains(searchQuery, ignoreCase = true) ||
                item.department.contains(searchQuery, ignoreCase = true) ||
                item.communicationSummary.contains(searchQuery, ignoreCase = true) ||
                item.identifiedRebuttals.contains(searchQuery, ignoreCase = true) ||
                item.buyInStrategy.contains(searchQuery, ignoreCase = true)
            matchesInv && matchesStatus && matchesCategory && matchesSearch
        }
    }

    val totalCount = interactions.size
    val buyInAchievedCount = interactions.count {
        it.buyInStatus == BuyInStatus.BUY_IN_ACHIEVED.name || it.buyInStatus == BuyInStatus.PILOT_VALIDATION.name
    }
    val activeRebuttalsCount = interactions.count {
        it.buyInStatus == BuyInStatus.REBUTTAL_IDENTIFIED.name || it.buyInStatus == BuyInStatus.STRATEGY_IN_PROGRESS.name
    }
    val avgLaborGain = if (interactions.isNotEmpty()) interactions.map { it.laborEfficiencyGainPct }.average() else 26.0
    val avgSafetyGain = if (interactions.isNotEmpty()) interactions.map { it.safetyImprovementPct }.average() else 35.0

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.AddComment, contentDescription = null) },
                text = { Text("Log Interaction & Rebuttal") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_stakeholder_interaction_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("stakeholder_interaction_log_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                InteractionLogHeroCard(
                    totalCount = totalCount,
                    buyInAchievedCount = buyInAchievedCount,
                    activeRebuttalsCount = activeRebuttalsCount,
                    avgLaborGain = avgLaborGain,
                    avgSafetyGain = avgSafetyGain,
                    activeInvestigationTitle = investigation?.title
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, GridEmerald.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = GridEmerald,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Buy-In Doctrine: Augmentation, Safety Guardrails & Process Simplicity",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Use this single communication log to address difficult stakeholder rebuttals early: articulate how technology substitutes unsuccessful, tedious manual steps without job loss, enforces safety guardrails against preventable lost-time injuries, and integrates into proven workflows without adding clutter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search rebuttals, stakeholder names, buy-in strategies, or channels...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("interaction_search_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (investigation != null) {
                            FilterChip(
                                selected = filterToActiveInvestigation,
                                onClick = { filterToActiveInvestigation = !filterToActiveInvestigation },
                                label = { Text("Active Project Only") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.FilterAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }

                        FilterChip(
                            selected = selectedStatusFilter == null && selectedCategoryFilter == null,
                            onClick = {
                                selectedStatusFilter = null
                                selectedCategoryFilter = null
                            },
                            label = { Text("All (${interactions.size})") }
                        )

                        BuyInStatus.entries.forEach { st ->
                            val count = interactions.count { it.buyInStatus == st.name }
                            FilterChip(
                                selected = selectedStatusFilter == st,
                                onClick = {
                                    selectedStatusFilter = if (selectedStatusFilter == st) null else st
                                },
                                label = { Text("${st.label} ($count)") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RebuttalCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = {
                                    selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                                },
                                label = { Text(cat.shortLabel, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            if (filteredInteractions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Forum,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = GridCyan
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Stakeholder Interactions Match Filter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Log a stakeholder communication session, rebuttal, and buy-in strategy to track alignment across utility operations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.testTag("empty_log_interaction_btn")
                            ) {
                                Icon(Icons.Default.AddComment, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log First Interaction")
                            }
                        }
                    }
                }
            } else {
                items(filteredInteractions, key = { it.id }) { interaction ->
                    StakeholderInteractionCard(
                        interaction = interaction,
                        onAdvanceStatus = { nextStatus ->
                            onUpdateInteraction(interaction.copy(buyInStatus = nextStatus.name))
                        },
                        onEdit = { editingInteraction = interaction },
                        onDelete = { onDeleteInteraction(interaction.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(76.dp)) }
        }
    }

    if (showAddDialog) {
        StakeholderInteractionDialog(
            initialInteraction = null,
            activeInvestigationId = investigation?.id ?: "GLOBAL",
            stakeholderProfiles = stakeholderProfiles,
            onDismiss = { showAddDialog = false },
            onConfirm = { invId, name, role, dept, domain, channel, dateLbl, summary, rebuttals, cat, strategy, status, laborPct, safetyPct, workflowPct ->
                onSaveInteraction(
                    invId,
                    name,
                    role,
                    dept,
                    domain,
                    channel,
                    dateLbl,
                    summary,
                    rebuttals,
                    cat,
                    strategy,
                    status,
                    laborPct,
                    safetyPct,
                    workflowPct
                )
                showAddDialog = false
            }
        )
    }

    editingInteraction?.let { existing ->
        StakeholderInteractionDialog(
            initialInteraction = existing,
            activeInvestigationId = existing.investigationId,
            stakeholderProfiles = stakeholderProfiles,
            onDismiss = { editingInteraction = null },
            onConfirm = { invId, name, role, dept, domain, channel, dateLbl, summary, rebuttals, cat, strategy, status, laborPct, safetyPct, workflowPct ->
                onUpdateInteraction(
                    existing.copy(
                        investigationId = invId,
                        stakeholderName = name,
                        stakeholderRole = role,
                        department = dept,
                        gridDomain = domain,
                        communicationChannel = channel,
                        interactionDateLabel = dateLbl,
                        communicationSummary = summary,
                        identifiedRebuttals = rebuttals,
                        rebuttalCategory = cat,
                        buyInStrategy = strategy,
                        buyInStatus = status,
                        laborEfficiencyGainPct = laborPct,
                        safetyImprovementPct = safetyPct,
                        workflowOptimizationPct = workflowPct
                    )
                )
                editingInteraction = null
            }
        )
    }
}

@Composable
private fun InteractionLogHeroCard(
    totalCount: Int,
    buyInAchievedCount: Int,
    activeRebuttalsCount: Int,
    avgLaborGain: Double,
    avgSafetyGain: Double,
    activeInvestigationTitle: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F2942), GridNavy)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STAKEHOLDER INTERACTION & REBUTTAL LOG",
                            style = MaterialTheme.typography.labelSmall,
                            color = GridCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Communication History & Buy-In Strategy Hub",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GridEmerald.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "$buyInAchievedCount/$totalCount Aligned",
                            style = MaterialTheme.typography.labelSmall,
                            color = GridEmerald,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                if (!activeInvestigationTitle.isNullOrBlank()) {
                    Text(
                        text = "Active Program Context: $activeInvestigationTitle",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InteractionStatPill(
                        label = "Logged Sessions",
                        value = "$totalCount Logs",
                        accent = GridCyan,
                        modifier = Modifier.weight(1f)
                    )
                    InteractionStatPill(
                        label = "Active Rebuttals",
                        value = "$activeRebuttalsCount Open",
                        accent = GridAmber,
                        modifier = Modifier.weight(1f)
                    )
                    InteractionStatPill(
                        label = "Avg Labor Gain",
                        value = String.format(Locale.US, "+%.0f%%", avgLaborGain),
                        accent = GridEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    InteractionStatPill(
                        label = "Injury Guardrail",
                        value = String.format(Locale.US, "+%.0f%%", avgSafetyGain),
                        accent = GridGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractionStatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = FontWeight.ExtraBold
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
fun StakeholderInteractionCard(
    interaction: StakeholderInteraction,
    onAdvanceStatus: (BuyInStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }

    val statusEnum = runCatching { BuyInStatus.valueOf(interaction.buyInStatus) }
        .getOrDefault(BuyInStatus.REBUTTAL_IDENTIFIED)
    val categoryEnum = runCatching { RebuttalCategory.valueOf(interaction.rebuttalCategory) }
        .getOrDefault(RebuttalCategory.WORKFORCE_AUGMENTATION)
    val domainEnum = runCatching { GridDomain.valueOf(interaction.gridDomain) }
        .getOrDefault(GridDomain.DISTRIBUTION)

    val statusColor = when (statusEnum) {
        BuyInStatus.INITIAL_OUTREACH -> GridCyan
        BuyInStatus.REBUTTAL_IDENTIFIED -> GridOrange
        BuyInStatus.STRATEGY_IN_PROGRESS -> GridAmber
        BuyInStatus.PILOT_VALIDATION -> GridEmerald
        BuyInStatus.BUY_IN_ACHIEVED -> GridGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("interaction_card_${interaction.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Box {
                            Surface(
                                color = statusColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable { statusMenuExpanded = true }
                                    .testTag("interaction_status_badge_${interaction.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Text(
                                        text = statusEnum.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Update Buy-In Status",
                                        tint = statusColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = statusMenuExpanded,
                                onDismissRequest = { statusMenuExpanded = false }
                            ) {
                                BuyInStatus.entries.forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text("Step ${st.stepOrder}: ${st.label}") },
                                        onClick = {
                                            onAdvanceStatus(st)
                                            statusMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = domainEnum.shortName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${interaction.communicationChannel} • ${interaction.interactionDateLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = interaction.stakeholderName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${interaction.stakeholderRole}${if (interaction.department.isNotBlank()) " • ${interaction.department}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_interaction_${interaction.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Interaction",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_interaction_${interaction.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Interaction",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 1. Communication Summary
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "COMMUNICATION HISTORY LOG",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = interaction.communicationSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 2. Identified Rebuttals Callout
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = GridOrange.copy(alpha = 0.09f),
                border = BorderStroke(1.dp, GridOrange.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ReportProblem,
                                contentDescription = null,
                                tint = GridOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "IDENTIFIED REBUTTAL / FRICTION POINT",
                                style = MaterialTheme.typography.labelSmall,
                                color = GridOrange,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = categoryEnum.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = GridOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = interaction.identifiedRebuttals,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 3. Strategy for Achieving Buy-In Callout
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = GridEmerald.copy(alpha = 0.09f),
                border = BorderStroke(1.dp, GridEmerald.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Handshake,
                            contentDescription = null,
                            tint = GridEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STRATEGY FOR BUY-IN & PROVEN PROCESS INTEGRATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = GridEmerald,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = interaction.buyInStrategy,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 4. Quantified Articulated Data Points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArticulatedMetricBadge(
                    label = "Labor Efficiency",
                    value = String.format(Locale.US, "+%.0f%%", interaction.laborEfficiencyGainPct),
                    color = GridCyan,
                    modifier = Modifier.weight(1f)
                )
                ArticulatedMetricBadge(
                    label = "Safety Guardrail",
                    value = String.format(Locale.US, "+%.0f%%", interaction.safetyImprovementPct),
                    color = GridEmerald,
                    modifier = Modifier.weight(1f)
                )
                ArticulatedMetricBadge(
                    label = "Workflow Simplicity",
                    value = String.format(Locale.US, "+%.0f%%", interaction.workflowOptimizationPct),
                    color = GridAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ArticulatedMetricBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StakeholderInteractionDialog(
    initialInteraction: StakeholderInteraction?,
    activeInvestigationId: String,
    stakeholderProfiles: List<StakeholderProfile>,
    onDismiss: () -> Unit,
    onConfirm: (
        investigationId: String,
        stakeholderName: String,
        stakeholderRole: String,
        department: String,
        gridDomain: String,
        communicationChannel: String,
        interactionDateLabel: String,
        communicationSummary: String,
        identifiedRebuttals: String,
        rebuttalCategory: String,
        buyInStrategy: String,
        buyInStatus: String,
        laborEfficiencyGainPct: Double,
        safetyImprovementPct: Double,
        workflowOptimizationPct: Double
    ) -> Unit
) {
    var stakeholderName by remember { mutableStateOf(initialInteraction?.stakeholderName ?: "") }
    var stakeholderRole by remember { mutableStateOf(initialInteraction?.stakeholderRole ?: "") }
    var department by remember { mutableStateOf(initialInteraction?.department ?: "") }
    var gridDomain by remember { mutableStateOf(initialInteraction?.gridDomain ?: GridDomain.DISTRIBUTION.name) }
    var communicationChannel by remember { mutableStateOf(initialInteraction?.communicationChannel ?: "Field Crew Safety Stand-Down") }
    var interactionDateLabel by remember { mutableStateOf(initialInteraction?.interactionDateLabel ?: "2025-02-24") }
    var communicationSummary by remember { mutableStateOf(initialInteraction?.communicationSummary ?: "") }
    var identifiedRebuttals by remember { mutableStateOf(initialInteraction?.identifiedRebuttals ?: "") }
    var rebuttalCategory by remember { mutableStateOf(initialInteraction?.rebuttalCategory ?: RebuttalCategory.WORKFORCE_AUGMENTATION.name) }
    var buyInStrategy by remember { mutableStateOf(initialInteraction?.buyInStrategy ?: "") }
    var buyInStatus by remember { mutableStateOf(initialInteraction?.buyInStatus ?: BuyInStatus.REBUTTAL_IDENTIFIED.name) }
    var laborGain by remember { mutableStateOf((initialInteraction?.laborEfficiencyGainPct ?: 25.0).toFloat()) }
    var safetyGain by remember { mutableStateOf((initialInteraction?.safetyImprovementPct ?: 35.0).toFloat()) }
    var workflowGain by remember { mutableStateOf((initialInteraction?.workflowOptimizationPct ?: 38.0).toFloat()) }

    val channelOptions = listOf(
        "Field Crew Safety Stand-Down",
        "Control Room Sync",
        "Union Steward Briefing",
        "Architecture Review",
        "Relay Lab Walkthrough",
        "Executive Alignment"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 740.dp),
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
                            text = if (initialInteraction == null) "Log Stakeholder Interaction & Rebuttal" else "Edit Stakeholder Interaction Log",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track communication history, rebuttals, and workforce-augmentation buy-in strategies",
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
                    if (stakeholderProfiles.isNotEmpty()) {
                        Text(
                            text = "Quick-Fill from Stakeholder Directory:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            stakeholderProfiles.forEach { profile ->
                                AssistChip(
                                    onClick = {
                                        stakeholderName = profile.name
                                        stakeholderRole = profile.role
                                        department = profile.department
                                        gridDomain = profile.gridDomain
                                    },
                                    label = { Text("${profile.name} (${profile.role.take(18)})") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = "Quick-Insert Rebuttal & Buy-In Playbook Preset:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GridEmerald
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PLAYBOOK_TEMPLATES.forEach { tpl ->
                            FilterChip(
                                selected = rebuttalCategory == tpl.category.name && identifiedRebuttals == tpl.rebuttalText,
                                onClick = {
                                    rebuttalCategory = tpl.category.name
                                    identifiedRebuttals = tpl.rebuttalText
                                    buyInStrategy = tpl.strategyText
                                    laborGain = tpl.defaultLaborGain
                                    safetyGain = tpl.defaultSafetyGain
                                    workflowGain = tpl.defaultWorkflowGain
                                    if (communicationSummary.isBlank()) {
                                        communicationSummary = "Reviewed operational workflow integration, safety guardrails, and task substitution metrics with stakeholder team."
                                    }
                                },
                                label = { Text(tpl.title, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = stakeholderName,
                            onValueChange = { stakeholderName = it },
                            label = { Text("Stakeholder Name *") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("interaction_stakeholder_name_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = stakeholderRole,
                            onValueChange = { stakeholderRole = it },
                            label = { Text("Role / Title *") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("interaction_stakeholder_role_input"),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text("Department / Crew") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = interactionDateLabel,
                            onValueChange = { interactionDateLabel = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                    }

                    Text("Communication Channel", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        channelOptions.forEach { ch ->
                            FilterChip(
                                selected = communicationChannel == ch,
                                onClick = { communicationChannel = ch },
                                label = { Text(ch, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text("Grid Domain", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GridDomain.entries.forEach { dom ->
                            FilterChip(
                                selected = gridDomain == dom.name,
                                onClick = { gridDomain = dom.name },
                                label = { Text(dom.shortName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = communicationSummary,
                        onValueChange = { communicationSummary = it },
                        label = { Text("Communication History & Discussion Summary *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("interaction_summary_input"),
                        minLines = 2
                    )

                    Text("Rebuttal Theme", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RebuttalCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = rebuttalCategory == cat.name,
                                onClick = { rebuttalCategory = cat.name },
                                label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = identifiedRebuttals,
                        onValueChange = { identifiedRebuttals = it },
                        label = { Text("Identified Rebuttals / Stakeholder Objections *") },
                        placeholder = { Text("e.g., Fear of job loss, alarm clutter, or safety skepticism...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("interaction_rebuttal_input"),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = buyInStrategy,
                        onValueChange = { buyInStrategy = it },
                        label = { Text("Strategy for Achieving Buy-In & Simple Integration *") },
                        placeholder = { Text("e.g., Substitute broken manual steps without job cuts; add preventable injury guardrails...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("interaction_strategy_input"),
                        minLines = 2
                    )

                    Text("Buy-In Status", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BuyInStatus.entries.forEach { st ->
                            FilterChip(
                                selected = buyInStatus == st.name,
                                onClick = { buyInStatus = st.name },
                                label = { Text(st.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Articulated Operational Gains Discussed",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Column {
                        Text(
                            text = "Projected Labor Efficiency Gain: +${laborGain.roundToInt()}% (Task Augmentation, Zero Job Loss)",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = laborGain,
                            onValueChange = { laborGain = it },
                            valueRange = 0f..80f
                        )
                    }

                    Column {
                        Text(
                            text = "Preventable Injury & Time-Off Safety Guardrail: +${safetyGain.roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = safetyGain,
                            onValueChange = { safetyGain = it },
                            valueRange = 0f..80f
                        )
                    }

                    Column {
                        Text(
                            text = "Workflow Simplicity & Proven Process Integration: +${workflowGain.roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = workflowGain,
                            onValueChange = { workflowGain = it },
                            valueRange = 0f..80f
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                val isValid = stakeholderName.isNotBlank() &&
                    stakeholderRole.isNotBlank() &&
                    communicationSummary.isNotBlank() &&
                    identifiedRebuttals.isNotBlank() &&
                    buyInStrategy.isNotBlank()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isValid) {
                                onConfirm(
                                    activeInvestigationId.ifBlank { "GLOBAL" },
                                    stakeholderName.trim(),
                                    stakeholderRole.trim(),
                                    department.trim(),
                                    gridDomain,
                                    communicationChannel.trim().ifBlank { "Working Session" },
                                    interactionDateLabel.trim().ifBlank { "2025-02-24" },
                                    communicationSummary.trim(),
                                    identifiedRebuttals.trim(),
                                    rebuttalCategory,
                                    buyInStrategy.trim(),
                                    buyInStatus,
                                    laborGain.roundToInt().toDouble(),
                                    safetyGain.roundToInt().toDouble(),
                                    workflowGain.roundToInt().toDouble()
                                )
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier.testTag("confirm_save_interaction_btn")
                    ) {
                        Text(if (initialInteraction == null) "Save Interaction Log" else "Update Interaction Log")
                    }
                }
            }
        }
    }
}
