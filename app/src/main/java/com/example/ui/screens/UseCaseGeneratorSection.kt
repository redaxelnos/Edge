package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.GeneratedUseCaseDraft
import com.example.data.model.GridDomain
import com.example.data.model.Interview
import com.example.data.model.Investigation
import com.example.data.model.UseCase
import com.example.ui.theme.GridAmber
import com.example.ui.theme.GridCyan
import com.example.ui.theme.GridEmerald
import com.example.ui.theme.GridGreen
import com.example.ui.theme.GridNavy
import com.example.ui.theme.GridOrange
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UseCaseGeneratorSection(
    investigation: Investigation?,
    interviews: List<Interview>,
    adoptedUseCases: List<UseCase>,
    generatedDrafts: List<GeneratedUseCaseDraft>,
    adoptedDraftTitles: Set<String>,
    aiGenerating: Boolean,
    generationError: String?,
    onGenerateAiUseCases: (domainFilter: String?, priority: String?, deepThinking: Boolean) -> Unit,
    onAdoptDraft: (GeneratedUseCaseDraft) -> Unit,
    onCreateCustomUseCase: (
        title: String,
        description: String,
        targetSystems: String,
        projectedSavings: Double,
        riskLevel: String,
        feasibilityScore: Double,
        projectedRoi: Double,
        gridDomain: String,
        stakeholderBacking: String,
        implementationWeeks: Int
    ) -> Unit,
    onUpdateUseCase: (UseCase) -> Unit = {},
    onDeleteUseCase: (String) -> Unit
) {
    if (investigation == null) {
        EmptySelectionPrompt("Select an investigation in the Projects tab to generate and customize operational use cases.")
        return
    }

    var showEditorDialog by remember { mutableStateOf(false) }
    var editingUseCase by remember { mutableStateOf<UseCase?>(null) }
    var deepThinking by remember { mutableStateOf(false) }

    val distinctTargetSystems = remember(adoptedUseCases) {
        adoptedUseCases
            .flatMap { it.targetSystems.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }
    val avgTimelineWeeks = remember(adoptedUseCases) {
        if (adoptedUseCases.isNotEmpty()) adoptedUseCases.map { it.implementationWeeks }.average() else 0.0
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingUseCase = null
                    showEditorDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Custom Use Case") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_custom_use_case_fab")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GridNavy)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GridCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Operational Use Case Synthesis & Customization",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Synthesizes ${interviews.size} stakeholder interviews into structured grid use cases with estimated OPEX savings, target IT/OT systems, and deployment complexity—or create and edit custom use cases.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        val totalSavings = adoptedUseCases.sumOf { it.projectedAnnualSavings }
                        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
                            maximumFractionDigits = 0
                        }

                        // 2x2 Responsive Live Summary Grid (No hardcoded "5 Systems")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            UseCaseSummaryBadge(
                                label = "Adopted Use Cases",
                                value = "${adoptedUseCases.size} Active",
                                modifier = Modifier.weight(1f)
                            )
                            UseCaseSummaryBadge(
                                label = "Est. Annual Savings",
                                value = "${currencyFormatter.format(totalSavings)}/yr",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            UseCaseSummaryBadge(
                                label = "Linked IT/OT Systems",
                                value = "${distinctTargetSystems.size} Systems",
                                modifier = Modifier.weight(1f)
                            )
                            UseCaseSummaryBadge(
                                label = "Avg Rollout Timeline",
                                value = if (adoptedUseCases.isNotEmpty()) String.format(Locale.US, "%.1f Weeks", avgTimelineWeeks) else "Not Set",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = deepThinking,
                                    onCheckedChange = { deepThinking = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Deep Engineering Synthesis",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onGenerateAiUseCases(null, null, deepThinking) },
                                enabled = !aiGenerating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GridCyan,
                                    contentColor = GridNavy
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_use_cases_button")
                            ) {
                                if (aiGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = GridNavy
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Synthesizing...")
                                } else {
                                    Icon(Icons.Default.Bolt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate AI Use Cases", fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    editingUseCase = null
                                    showEditorDialog = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("header_add_custom_use_case_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Custom")
                            }
                        }

                        if (!generationError.isNullOrBlank()) {
                            Text(
                                text = generationError,
                                style = MaterialTheme.typography.bodySmall,
                                color = GridOrange
                            )
                        }
                    }
                }
            }

            if (generatedDrafts.isNotEmpty()) {
                item {
                    Text(
                        text = "AI-Recommended Use Cases (Review & Adopt)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(generatedDrafts) { draft ->
                    val isAdopted = draft.title in adoptedDraftTitles
                    SuggestedUseCaseDraftCard(
                        draft = draft,
                        isAdopted = isAdopted,
                        onAdopt = { onAdoptDraft(draft) }
                    )
                }
            }

            item {
                Text(
                    text = "Adopted Operational Use Cases (${adoptedUseCases.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (adoptedUseCases.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Adopted Use Cases Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Generate AI recommendations from your interviews or tap 'Add Custom Use Case' to define your own.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(adoptedUseCases, key = { it.id }) { uc ->
                    AdoptedUseCaseCard(
                        useCase = uc,
                        onEdit = {
                            editingUseCase = uc
                            showEditorDialog = true
                        },
                        onDelete = { onDeleteUseCase(uc.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showEditorDialog) {
        UseCaseEditorDialog(
            initialUseCase = editingUseCase,
            onDismiss = {
                showEditorDialog = false
                editingUseCase = null
            },
            onSaveNew = { title, desc, systems, savings, risk, feas, roi, domain, backing, weeks ->
                onCreateCustomUseCase(title, desc, systems, savings, risk, feas, roi, domain, backing, weeks)
                showEditorDialog = false
            },
            onSaveEdited = { updated ->
                onUpdateUseCase(updated)
                showEditorDialog = false
                editingUseCase = null
            }
        )
    }
}

@Composable
private fun UseCaseSummaryBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestedUseCaseDraftCard(
    draft: GeneratedUseCaseDraft,
    isAdopted: Boolean,
    onAdopt: () -> Unit
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, GridCyan)
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
                Surface(
                    color = GridCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "AI SUGGESTION • FEASIBILITY ${draft.feasibilityScore.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = GridCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "${currencyFormatter.format(draft.projectedAnnualSavings)}/yr",
                    style = MaterialTheme.typography.titleSmall,
                    color = GridGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = draft.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = draft.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "ROI: ${draft.projectedRoi}% • Risk: ${draft.riskLevel} • Timeline: ${draft.implementationWeeks} wks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                draft.targetSystems.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { sys ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(sys, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Button(
                onClick = onAdopt,
                enabled = !isAdopted,
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("adopt_use_case_${draft.title.take(10)}")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isAdopted) "Adopted" else "Adopt into Investigation")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdoptedUseCaseCard(
    useCase: UseCase,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    val riskColor = when (useCase.riskLevel.uppercase()) {
        "LOW" -> GridGreen
        "HIGH" -> GridOrange
        else -> GridAmber
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("adopted_use_case_${useCase.id}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = riskColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${useCase.riskLevel} Risk",
                                style = MaterialTheme.typography.labelSmall,
                                color = riskColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = GridEmerald.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${useCase.implementationWeeks} Wks Rollout",
                                style = MaterialTheme.typography.labelSmall,
                                color = GridEmerald,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = useCase.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_use_case_${useCase.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Use Case",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_use_case_${useCase.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Use Case",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Text(
                text = useCase.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Feasibility & ROI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.0f/100 • %.1f%% ROI", useCase.feasibilityScore, useCase.projectedRoi),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = GridGreen.copy(alpha = 0.1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Est. Annual OPEX Savings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${currencyFormatter.format(useCase.projectedAnnualSavings)}/yr",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GridGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val systemsList = useCase.targetSystems.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (systemsList.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    systemsList.forEach { sys ->
                        AssistChip(
                            onClick = onEdit,
                            label = { Text(sys, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UseCaseEditorDialog(
    initialUseCase: UseCase?,
    onDismiss: () -> Unit,
    onSaveNew: (
        title: String,
        description: String,
        targetSystems: String,
        projectedSavings: Double,
        riskLevel: String,
        feasibilityScore: Double,
        projectedRoi: Double,
        gridDomain: String,
        stakeholderBacking: String,
        implementationWeeks: Int
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onSaveEdited: (UseCase) -> Unit
) {
    var title by remember { mutableStateOf(initialUseCase?.title ?: "") }
    var description by remember { mutableStateOf(initialUseCase?.description ?: "") }
    var stakeholderBacking by remember { mutableStateOf(initialUseCase?.stakeholderBacking ?: "Distribution Operations & Protection Engineering") }
    var riskLevel by remember { mutableStateOf(initialUseCase?.riskLevel ?: "MEDIUM") }
    var weeks by remember { mutableStateOf((initialUseCase?.implementationWeeks ?: 14).toString()) }
    var savings by remember { mutableStateOf((initialUseCase?.projectedAnnualSavings ?: 420000.0).toLong().toString()) }
    var feasibility by remember { mutableStateOf((initialUseCase?.feasibilityScore ?: 85.0).toString()) }
    var roi by remember { mutableStateOf((initialUseCase?.projectedRoi ?: 42.0).toString()) }
    var gridDomain by remember { mutableStateOf(initialUseCase?.gridDomain ?: GridDomain.DISTRIBUTION.name) }
    var customSystemsText by remember {
        mutableStateOf(initialUseCase?.targetSystems ?: "SCADA/EMS, ADMS, IBM Maximo")
    }

    val standardSystems = listOf("SCADA/EMS", "ADMS", "IBM Maximo", "Azure Digital Twins", "Oracle Utilities", "Snowflake", "DERMS", "GIS")
    val riskOptions = listOf("LOW", "MEDIUM", "HIGH")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp),
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
                    Text(
                        text = if (initialUseCase == null) "Create Custom Operational Use Case" else "Edit Operational Use Case",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                        label = { Text("Use Case Title *") },
                        placeholder = { Text("e.g., Automated FLISR Feeder Reconfiguration") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("use_case_title_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Technical Description & Workflow *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("use_case_desc_input"),
                        minLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = savings,
                            onValueChange = { savings = it },
                            label = { Text("Est. Annual Savings (\$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("use_case_savings_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = weeks,
                            onValueChange = { weeks = it },
                            label = { Text("Rollout (Weeks)") },
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
                            value = feasibility,
                            onValueChange = { feasibility = it },
                            label = { Text("Feasibility (0-100)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = roi,
                            onValueChange = { roi = it },
                            label = { Text("Projected ROI (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Text("Deployment Risk Level", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        riskOptions.forEach { r ->
                            FilterChip(
                                selected = riskLevel.equals(r, ignoreCase = true),
                                onClick = { riskLevel = r },
                                label = { Text(r) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = stakeholderBacking,
                        onValueChange = { stakeholderBacking = it },
                        label = { Text("Sponsoring Stakeholders / Divisions") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = customSystemsText,
                        onValueChange = { customSystemsText = it },
                        label = { Text("Target IT/OT Systems (Comma-separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = "Tap to toggle standard utility systems:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val currentList = customSystemsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        standardSystems.forEach { sys ->
                            val isSelected = currentList.any { it.equals(sys, ignoreCase = true) }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val updated = if (isSelected) {
                                        currentList.filterNot { it.equals(sys, ignoreCase = true) }
                                    } else {
                                        currentList + sys
                                    }
                                    customSystemsText = updated.joinToString(", ")
                                },
                                label = { Text(sys, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

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
                            if (title.isNotBlank() && description.isNotBlank()) {
                                val cleanedSystems = customSystemsText.trim().ifBlank { "SCADA/EMS, ADMS" }
                                if (initialUseCase == null) {
                                    onSaveNew(
                                        title.trim(),
                                        description.trim(),
                                        cleanedSystems,
                                        savings.toDoubleOrNull() ?: 250000.0,
                                        riskLevel,
                                        (feasibility.toDoubleOrNull() ?: 85.0).coerceIn(0.0, 100.0),
                                        roi.toDoubleOrNull() ?: 35.0,
                                        gridDomain,
                                        stakeholderBacking.trim(),
                                        (weeks.toIntOrNull() ?: 14).coerceAtLeast(1)
                                    )
                                } else {
                                    onSaveEdited(
                                        initialUseCase.copy(
                                            title = title.trim(),
                                            description = description.trim(),
                                            targetSystems = cleanedSystems,
                                            projectedAnnualSavings = savings.toDoubleOrNull() ?: initialUseCase.projectedAnnualSavings,
                                            riskLevel = riskLevel,
                                            feasibilityScore = (feasibility.toDoubleOrNull() ?: initialUseCase.feasibilityScore).coerceIn(0.0, 100.0),
                                            projectedRoi = roi.toDoubleOrNull() ?: initialUseCase.projectedRoi,
                                            gridDomain = gridDomain,
                                            stakeholderBacking = stakeholderBacking.trim(),
                                            implementationWeeks = (weeks.toIntOrNull() ?: initialUseCase.implementationWeeks).coerceAtLeast(1)
                                        )
                                    )
                                }
                            }
                        },
                        enabled = title.isNotBlank() && description.isNotBlank(),
                        modifier = Modifier.testTag("save_use_case_btn")
                    ) {
                        Text(if (initialUseCase == null) "Create Use Case" else "Save Changes")
                    }
                }
            }
        }
    }
}
