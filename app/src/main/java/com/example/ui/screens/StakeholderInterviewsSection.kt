package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GridDomain
import com.example.data.model.Interview
import com.example.data.model.Investigation
import com.example.data.model.StakeholderProfile
import com.example.data.model.StakeholderSentiment
import com.example.ui.theme.GridAmber
import com.example.ui.theme.GridCyan
import com.example.ui.theme.GridEmerald
import com.example.ui.theme.GridGreen
import com.example.ui.theme.GridNavy
import com.example.ui.theme.GridOrange
import com.example.ui.theme.GridRed
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StakeholderInterviewsSection(
    investigation: Investigation?,
    interviews: List<Interview>,
    stakeholderProfiles: List<StakeholderProfile> = emptyList(),
    onAddInterview: (
        name: String,
        role: String,
        dept: String,
        domain: String,
        sentiment: String,
        notes: String,
        priority: Int,
        feasibility: String,
        keyRequirements: String,
        integrationConcerns: String
    ) -> Unit,
    onUpdateInterview: (Interview) -> Unit = {},
    onAutoCategorize: (notes: String, onResult: (String, String) -> Unit) -> Unit = { _, _ -> },
    onDeleteInterview: (String) -> Unit
) {
    if (investigation == null) {
        EmptySelectionPrompt("Select an investigation in the Projects tab to capture and analyze stakeholder interviews.")
        return
    }

    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingInterview by remember { mutableStateOf<Interview?>(null) }
    var selectedDomainFilter by remember { mutableStateOf<String?>(null) }
    var selectedSentimentFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredInterviews = remember(interviews, selectedDomainFilter, selectedSentimentFilter, searchQuery) {
        interviews.filter { interview ->
            val domainMatch = selectedDomainFilter == null || interview.gridDomain == selectedDomainFilter
            val sentimentMatch = selectedSentimentFilter == null || interview.sentiment == selectedSentimentFilter
            val queryMatch = searchQuery.isBlank() ||
                interview.stakeholderName.contains(searchQuery, ignoreCase = true) ||
                interview.stakeholderRole.contains(searchQuery, ignoreCase = true) ||
                interview.department.contains(searchQuery, ignoreCase = true) ||
                interview.notes.contains(searchQuery, ignoreCase = true) ||
                interview.keyRequirements.contains(searchQuery, ignoreCase = true)
            domainMatch && sentimentMatch && queryMatch
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingInterview = null
                    showAddOrEditDialog = true
                },
                icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) },
                text = { Text("Log Interview") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_interview_fab")
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
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "STAKEHOLDER DISCOVERY & DOMAIN SENTIMENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = GridCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Interviews for ${investigation.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        val avgPriority = if (interviews.isNotEmpty()) {
                            interviews.map { it.priorityRating }.average()
                        } else 0.0
                        val highPriorityCount = interviews.count { it.priorityRating >= 4 }

                        // 2x2 Responsive Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InterviewStatBadge(
                                label = "Captured Sessions",
                                value = "${interviews.size} Interviews",
                                modifier = Modifier.weight(1f)
                            )
                            InterviewStatBadge(
                                label = "Avg Urgency Rating",
                                value = if (interviews.isNotEmpty()) String.format(Locale.US, "%.1f / 5.0", avgPriority) else "No Ratings",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InterviewStatBadge(
                                label = "High Urgency (P4-P5)",
                                value = "$highPriorityCount Sessions",
                                modifier = Modifier.weight(1f)
                            )
                            InterviewStatBadge(
                                label = "Directory Profiles",
                                value = "${stakeholderProfiles.size} Available",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                StakeholderSentimentHeatmapCard(
                    interviews = interviews,
                    selectedDomainFilter = selectedDomainFilter,
                    selectedSentimentFilter = selectedSentimentFilter,
                    onSelectCellFilter = { domain, sentiment ->
                        selectedDomainFilter = domain
                        selectedSentimentFilter = sentiment
                    }
                )
            }

            if (interviews.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search interviews by stakeholder, role, department, or transcript...") },
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
                            .testTag("search_interviews_input")
                    )
                }
            }

            if (selectedDomainFilter != null || selectedSentimentFilter != null) {
                item {
                    Surface(
                        color = GridCyan.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GridCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val domainText = selectedDomainFilter?.let {
                                runCatching { GridDomain.valueOf(it).shortName }.getOrDefault(it)
                            } ?: "All Domains"
                            val sentimentText = selectedSentimentFilter?.let {
                                runCatching { StakeholderSentiment.valueOf(it).label }.getOrDefault(it)
                            } ?: "All Sentiments"

                            Text(
                                text = "Filtered by Heatmap: $domainText • $sentimentText (${filteredInterviews.size} shown)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    selectedDomainFilter = null
                                    selectedSentimentFilter = null
                                },
                                modifier = Modifier.testTag("clear_heatmap_filter_button")
                            ) {
                                Icon(Icons.Default.FilterAltOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }
                    }
                }
            }

            if (interviews.isEmpty()) {
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
                                Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Stakeholder Interviews Logged Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Log Interview' to capture field notes from Control Room Operators, Protection Engineers, or IT/OT Leads, or link directly from your Stakeholder Directory.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredInterviews, key = { it.id }) { interview ->
                    InterviewCard(
                        interview = interview,
                        onEdit = {
                            editingInterview = interview
                            showAddOrEditDialog = true
                        },
                        onDelete = { onDeleteInterview(interview.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showAddOrEditDialog) {
        InterviewEditorDialog(
            initialInterview = editingInterview,
            stakeholderProfiles = stakeholderProfiles,
            onAutoCategorize = onAutoCategorize,
            onDismiss = {
                showAddOrEditDialog = false
                editingInterview = null
            },
            onSaveNew = { name, role, dept, domain, sentiment, notes, priority, feasibility, reqs, concerns ->
                onAddInterview(name, role, dept, domain, sentiment, notes, priority, feasibility, reqs, concerns)
                showAddOrEditDialog = false
            },
            onSaveEdited = { updated ->
                onUpdateInterview(updated)
                showAddOrEditDialog = false
                editingInterview = null
            }
        )
    }
}

@Composable
private fun InterviewStatBadge(
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
fun InterviewCard(
    interview: Interview,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sentimentEnum = runCatching { StakeholderSentiment.valueOf(interview.sentiment) }
        .getOrDefault(StakeholderSentiment.POSITIVE)

    val domainEnum = runCatching { GridDomain.valueOf(interview.gridDomain) }
        .getOrDefault(GridDomain.DISTRIBUTION)

    val sentimentColor = when (sentimentEnum) {
        StakeholderSentiment.POSITIVE -> GridGreen
        StakeholderSentiment.NEUTRAL -> GridCyan
        StakeholderSentiment.CAUTIOUS -> GridAmber
        StakeholderSentiment.CONCERNED -> GridRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("interview_card_${interview.id}"),
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
                    Text(
                        text = interview.stakeholderName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${interview.stakeholderRole} • ${interview.department}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = GridCyan.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = domainEnum.shortName,
                                style = MaterialTheme.typography.labelSmall,
                                color = GridCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = sentimentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = sentimentEnum.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = sentimentColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Priority ${interview.priorityRating}/5",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (interview.feasibilityAssessment.isNotBlank()) {
                            Surface(
                                color = GridEmerald.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = interview.feasibilityAssessment,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GridEmerald,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_interview_${interview.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Interview Attributes",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_interview_${interview.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Interview",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Text(
                text = interview.notes,
                style = MaterialTheme.typography.bodyMedium
            )

            if (interview.keyRequirements.isNotBlank() || interview.integrationConcerns.isNotBlank()) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (interview.keyRequirements.isNotBlank()) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Key Technical Requirements",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GridCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = interview.keyRequirements,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    if (interview.integrationConcerns.isNotBlank()) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = GridOrange.copy(alpha = 0.1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Integration & Cyber Concerns",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GridOrange,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = interview.integrationConcerns,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InterviewEditorDialog(
    initialInterview: Interview?,
    stakeholderProfiles: List<StakeholderProfile>,
    onAutoCategorize: (notes: String, onResult: (String, String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSaveNew: (
        name: String,
        role: String,
        dept: String,
        domain: String,
        sentiment: String,
        notes: String,
        priority: Int,
        feasibility: String,
        keyRequirements: String,
        integrationConcerns: String
    ) -> Unit,
    onSaveEdited: (Interview) -> Unit
) {
    var name by remember { mutableStateOf(initialInterview?.stakeholderName ?: "") }
    var role by remember { mutableStateOf(initialInterview?.stakeholderRole ?: "Control Room Operator") }
    var dept by remember { mutableStateOf(initialInterview?.department ?: "Grid Operations") }
    var gridDomain by remember { mutableStateOf(initialInterview?.gridDomain ?: GridDomain.DISTRIBUTION.name) }
    var sentiment by remember { mutableStateOf(initialInterview?.sentiment ?: StakeholderSentiment.POSITIVE.name) }
    var notes by remember { mutableStateOf(initialInterview?.notes ?: "") }
    var priority by remember { mutableIntStateOf(initialInterview?.priorityRating ?: 4) }
    var feasibility by remember { mutableStateOf(initialInterview?.feasibilityAssessment ?: "High — Ready for Pilot") }
    var keyRequirements by remember { mutableStateOf(initialInterview?.keyRequirements ?: "") }
    var integrationConcerns by remember { mutableStateOf(initialInterview?.integrationConcerns ?: "") }

    val roleSuggestions = listOf(
        "Control Room Operator",
        "Substation Protection Engineer",
        "Distribution Planning Lead",
        "VP of Grid Modernization",
        "IT/OT Cybersecurity Architect",
        "Field Crew Supervisor"
    )
    val feasibilityOptions = listOf(
        "High — Ready for Pilot",
        "Moderate — Needs API Gating",
        "Requires Telecom / RTU Upgrade",
        "Regulatory Approval Needed"
    )

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
                    Text(
                        text = if (initialInterview == null) "Log Stakeholder Interview" else "Edit Stakeholder Interview",
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
                    if (stakeholderProfiles.isNotEmpty()) {
                        Text(
                            text = "Quick-Fill from Stakeholder Directory:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = GridCyan
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
                                        name = profile.name
                                        role = profile.role
                                        dept = profile.department
                                        gridDomain = profile.gridDomain
                                        sentiment = profile.stance
                                    },
                                    label = { Text("${profile.name} (${profile.role})") },
                                    leadingIcon = {
                                        Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Stakeholder Full Name *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_interview_name"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = role,
                            onValueChange = { role = it },
                            label = { Text("Organizational Role (Custom) *") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_interview_role"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = dept,
                            onValueChange = { dept = it },
                            label = { Text("Department / Division *") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_interview_dept"),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        roleSuggestions.forEach { r ->
                            FilterChip(
                                selected = role.equals(r, ignoreCase = true),
                                onClick = { role = r },
                                label = { Text(r, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grid Domain", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        if (notes.length > 15) {
                            TextButton(
                                onClick = {
                                    onAutoCategorize(notes) { dom, sent ->
                                        gridDomain = dom
                                        sentiment = sent
                                    }
                                }
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Classify Domain & Sentiment", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GridDomain.entries.forEach { domainOption ->
                            FilterChip(
                                selected = gridDomain == domainOption.name,
                                onClick = { gridDomain = domainOption.name },
                                label = { Text(domainOption.shortName, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("select_domain_${domainOption.code}")
                            )
                        }
                    }

                    Text("Stakeholder Sentiment", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StakeholderSentiment.entries.forEach { s ->
                            FilterChip(
                                selected = sentiment == s.name,
                                onClick = { sentiment = s.name },
                                label = { Text(s.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Urgency / Priority Rating: $priority / 5",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { priority = star },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (star <= priority) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Priority $star",
                                        tint = if (star <= priority) GridAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = feasibility,
                        onValueChange = { feasibility = it },
                        label = { Text("Technical Feasibility Assessment") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        feasibilityOptions.forEach { opt ->
                            FilterChip(
                                selected = feasibility.equals(opt, ignoreCase = true),
                                onClick = { feasibility = opt },
                                label = { Text(opt, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Interview Field Notes & Operational Observations *") },
                        placeholder = { Text("Describe operational bottlenecks, SCADA/OMS switching challenges, or field crew feedback...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_interview_notes"),
                        minLines = 3
                    )

                    OutlinedTextField(
                        value = keyRequirements,
                        onValueChange = { keyRequirements = it },
                        label = { Text("Key Technical Requirements (Editable)") },
                        placeholder = { Text("e.g., Sub-second DNP3 polling, automated feeder fault isolation") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = integrationConcerns,
                        onValueChange = { integrationConcerns = it },
                        label = { Text("Integration & Cybersecurity Concerns (Editable)") },
                        placeholder = { Text("e.g., Legacy RTU firmware compatibility, NERC CIP firewall rules") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
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
                            if (name.isNotBlank() && notes.isNotBlank()) {
                                if (initialInterview == null) {
                                    onSaveNew(
                                        name.trim(),
                                        role.trim().ifBlank { "Utility Stakeholder" },
                                        dept.trim().ifBlank { "Grid Operations" },
                                        gridDomain,
                                        sentiment,
                                        notes.trim(),
                                        priority,
                                        feasibility.trim().ifBlank { "High — Ready for Pilot" },
                                        keyRequirements.trim(),
                                        integrationConcerns.trim()
                                    )
                                } else {
                                    onSaveEdited(
                                        initialInterview.copy(
                                            stakeholderName = name.trim(),
                                            stakeholderRole = role.trim().ifBlank { "Utility Stakeholder" },
                                            department = dept.trim().ifBlank { "Grid Operations" },
                                            gridDomain = gridDomain,
                                            sentiment = sentiment,
                                            notes = notes.trim(),
                                            priorityRating = priority,
                                            feasibilityAssessment = feasibility.trim().ifBlank { "High — Ready for Pilot" },
                                            keyRequirements = keyRequirements.trim(),
                                            integrationConcerns = integrationConcerns.trim()
                                        )
                                    )
                                }
                            }
                        },
                        enabled = name.isNotBlank() && notes.isNotBlank(),
                        modifier = Modifier.testTag("confirm_add_interview")
                    ) {
                        Text(if (initialInterview == null) "Save Interview" else "Update Interview")
                    }
                }
            }
        }
    }
}
