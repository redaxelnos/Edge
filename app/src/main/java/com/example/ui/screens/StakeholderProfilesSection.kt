package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GridDomain
import com.example.data.model.InfluenceLevel
import com.example.data.model.StakeholderProfile
import com.example.data.model.StakeholderSentiment
import com.example.ui.theme.EnergyGold
import com.example.ui.theme.GridCyan
import com.example.ui.theme.GridEmerald
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StakeholderProfilesSection(
    profiles: List<StakeholderProfile>,
    onSaveProfile: (
        name: String,
        role: String,
        department: String,
        email: String,
        phone: String,
        officeLocation: String,
        preferredChannel: String,
        gridDomain: String,
        influenceLevel: String,
        influenceScore: Int,
        stance: String,
        projectResponsibilities: String,
        notes: String
    ) -> Unit,
    onUpdateProfile: (StakeholderProfile) -> Unit,
    onDeleteProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedInfluenceFilter by remember { mutableStateOf<InfluenceLevel?>(null) }
    var selectedDomainFilter by remember { mutableStateOf<GridDomain?>(null) }
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<StakeholderProfile?>(null) }

    val filteredProfiles = remember(profiles, searchQuery, selectedInfluenceFilter, selectedDomainFilter) {
        profiles.filter { profile ->
            val matchesQuery = searchQuery.isBlank() ||
                profile.name.contains(searchQuery, ignoreCase = true) ||
                profile.role.contains(searchQuery, ignoreCase = true) ||
                profile.department.contains(searchQuery, ignoreCase = true) ||
                profile.email.contains(searchQuery, ignoreCase = true) ||
                profile.projectResponsibilities.contains(searchQuery, ignoreCase = true)
            val matchesInfluence = selectedInfluenceFilter == null || profile.influenceLevel == selectedInfluenceFilter?.name
            val matchesDomain = selectedDomainFilter == null || profile.gridDomain == selectedDomainFilter?.name
            matchesQuery && matchesInfluence && matchesDomain
        }.sortedByDescending { it.influenceScore }
    }

    val execAndDecisionCount = remember(profiles) {
        profiles.count {
            it.influenceLevel == InfluenceLevel.EXECUTIVE_SPONSOR.name ||
                it.influenceLevel == InfluenceLevel.DECISION_MAKER.name ||
                it.influenceLevel == InfluenceLevel.TECHNICAL_APPROVER.name
        }
    }
    val avgInfluenceScore = remember(profiles) {
        if (profiles.isEmpty()) 0 else profiles.map { it.influenceScore }.average().roundToInt()
    }
    val positiveChampionsCount = remember(profiles) {
        profiles.count { it.stance == StakeholderSentiment.POSITIVE.name }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Utility Stakeholder Directory & Influence Matrix",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Track contact details, organizational authority, and influence levels across grid modernization projects.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                editingProfile = null
                                showAddOrEditDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GridCyan,
                                contentColor = Color(0xFF0A192F)
                            ),
                            modifier = Modifier.testTag("add_stakeholder_profile_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Profile", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StakeholderMetricPill(
                                label = "Total Profiles",
                                value = "${profiles.size} Stakeholders",
                                accentColor = GridCyan,
                                modifier = Modifier.weight(1f)
                            )
                            StakeholderMetricPill(
                                label = "Key Gatekeepers",
                                value = "$execAndDecisionCount Exec / Approvers",
                                accentColor = EnergyGold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StakeholderMetricPill(
                                label = "Avg Influence Score",
                                value = "$avgInfluenceScore / 100",
                                accentColor = Color(0xFF38BDF8),
                                modifier = Modifier.weight(1f)
                            )
                            StakeholderMetricPill(
                                label = "Supportive Champions",
                                value = "$positiveChampionsCount Positive",
                                accentColor = GridEmerald,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search name, role, department, email, or responsibilities...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = GridCyan) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stakeholder_profile_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "FILTER BY INFLUENCE LEVEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedInfluenceFilter == null,
                            onClick = { selectedInfluenceFilter = null },
                            label = { Text("All Tiers (${profiles.size})", fontSize = 11.sp) },
                            modifier = Modifier.testTag("filter_influence_all")
                        )
                        InfluenceLevel.entries.forEach { level ->
                            val count = profiles.count { it.influenceLevel == level.name }
                            FilterChip(
                                selected = selectedInfluenceFilter == level,
                                onClick = {
                                    selectedInfluenceFilter = if (selectedInfluenceFilter == level) null else level
                                },
                                label = { Text("${level.badgeText} ($count)", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = influenceColor(level).copy(alpha = 0.22f),
                                    selectedLabelColor = influenceColor(level)
                                ),
                                modifier = Modifier.testTag("filter_influence_${level.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "FILTER BY GRID DOMAIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDomainFilter == null,
                            onClick = { selectedDomainFilter = null },
                            label = { Text("All Domains", fontSize = 11.sp) },
                            modifier = Modifier.testTag("filter_profile_domain_all")
                        )
                        GridDomain.entries.forEach { domain ->
                            FilterChip(
                                selected = selectedDomainFilter == domain,
                                onClick = {
                                    selectedDomainFilter = if (selectedDomainFilter == domain) null else domain
                                },
                                label = { Text(domain.shortName, fontSize = 11.sp) },
                                modifier = Modifier.testTag("filter_profile_domain_${domain.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        if (filteredProfiles.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No matching stakeholder profiles",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Text(
                            text = "Add utility executives, engineers, or operations leads to track contact info and project influence.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        } else {
            items(filteredProfiles, key = { it.id }) { profile ->
                StakeholderProfileCard(
                    profile = profile,
                    onEdit = {
                        editingProfile = profile
                        showAddOrEditDialog = true
                    },
                    onDelete = { onDeleteProfile(profile.id) }
                )
            }
        }
    }

    if (showAddOrEditDialog) {
        StakeholderProfileDialog(
            initialProfile = editingProfile,
            onDismiss = {
                showAddOrEditDialog = false
                editingProfile = null
            },
            onConfirm = { name, role, dept, email, phone, office, channel, domain, influenceLevel, score, stance, responsibilities, notes ->
                val existing = editingProfile
                if (existing != null) {
                    onUpdateProfile(
                        existing.copy(
                            name = name,
                            role = role,
                            department = dept,
                            email = email,
                            phone = phone,
                            officeLocation = office,
                            preferredChannel = channel,
                            gridDomain = domain,
                            influenceLevel = influenceLevel,
                            influenceScore = score,
                            stance = stance,
                            projectResponsibilities = responsibilities,
                            notes = notes
                        )
                    )
                } else {
                    onSaveProfile(
                        name,
                        role,
                        dept,
                        email,
                        phone,
                        office,
                        channel,
                        domain,
                        influenceLevel,
                        score,
                        stance,
                        responsibilities,
                        notes
                    )
                }
                showAddOrEditDialog = false
                editingProfile = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StakeholderProfileCard(
    profile: StakeholderProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val influenceEnum = InfluenceLevel.entries.find { it.name == profile.influenceLevel }
        ?: InfluenceLevel.KEY_INFLUENCER
    val domainEnum = GridDomain.entries.find { it.name == profile.gridDomain }
        ?: GridDomain.DISTRIBUTION
    val stanceEnum = StakeholderSentiment.entries.find { it.name == profile.stance }
        ?: StakeholderSentiment.POSITIVE
    val tierColor = influenceColor(influenceEnum)
    val stanceColor = sentimentBadgeColor(stanceEnum)

    val initials = remember(profile.name) {
        profile.name.trim().split("\\s+".toRegex())
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "SH" }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = BorderStroke(1.dp, tierColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stakeholder_profile_card_${profile.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(tierColor.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = tierColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "${profile.role} • ${profile.department}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_stakeholder_profile_${profile.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Stakeholder Profile", tint = GridCyan)
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_stakeholder_profile_${profile.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Stakeholder Profile", tint = Color(0xFFEF4444))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BadgeChip(text = influenceEnum.label, color = tierColor)
                BadgeChip(text = "${domainEnum.code} • ${domainEnum.shortName}", color = GridCyan)
                BadgeChip(text = stanceEnum.label, color = stanceColor)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Influence Score Meter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0A192F))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = tierColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Grid Project Influence Index",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                    Text(
                        text = "${profile.influenceScore} / 100",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = tierColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (profile.influenceScore / 100f).coerceIn(0f, 1f) },
                    color = tierColor,
                    trackColor = Color(0xFF1E293B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contact Information Grid
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (profile.email.isNotBlank()) {
                    ContactInfoChip(
                        icon = Icons.Default.Email,
                        text = profile.email,
                        onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${profile.email}"))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
                if (profile.phone.isNotBlank()) {
                    ContactInfoChip(
                        icon = Icons.Default.Call,
                        text = profile.phone,
                        onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${profile.phone}"))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
                if (profile.officeLocation.isNotBlank()) {
                    ContactInfoChip(
                        icon = Icons.Default.LocationOn,
                        text = profile.officeLocation,
                        onClick = null
                    )
                }
                if (profile.preferredChannel.isNotBlank()) {
                    ContactInfoChip(
                        icon = Icons.Default.Forum,
                        text = "Prefers: ${profile.preferredChannel}",
                        onClick = null
                    )
                }
            }

            if (profile.projectResponsibilities.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "PROJECT GOVERNANCE & SIGN-OFF RESPONSIBILITIES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = EnergyGold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = profile.projectResponsibilities,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE2E8F0)
                )
            }

            if (profile.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ENGAGEMENT NOTES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = GridCyan
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = profile.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun StakeholderMetricPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF111827),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = accentColor,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BadgeChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ContactInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: (() -> Unit)?
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = GridCyan, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE2E8F0)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StakeholderProfileDialog(
    initialProfile: StakeholderProfile?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        role: String,
        department: String,
        email: String,
        phone: String,
        officeLocation: String,
        preferredChannel: String,
        gridDomain: String,
        influenceLevel: String,
        influenceScore: Int,
        stance: String,
        projectResponsibilities: String,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var role by remember { mutableStateOf(initialProfile?.role ?: "") }
    var department by remember { mutableStateOf(initialProfile?.department ?: "") }
    var email by remember { mutableStateOf(initialProfile?.email ?: "") }
    var phone by remember { mutableStateOf(initialProfile?.phone ?: "") }
    var officeLocation by remember { mutableStateOf(initialProfile?.officeLocation ?: "") }
    var preferredChannel by remember { mutableStateOf(initialProfile?.preferredChannel ?: "Email") }
    var selectedDomain by remember {
        mutableStateOf(
            GridDomain.entries.find { it.name == initialProfile?.gridDomain } ?: GridDomain.DISTRIBUTION
        )
    }
    var selectedInfluence by remember {
        mutableStateOf(
            InfluenceLevel.entries.find { it.name == initialProfile?.influenceLevel } ?: InfluenceLevel.DECISION_MAKER
        )
    }
    var influenceScore by remember { mutableFloatStateOf((initialProfile?.influenceScore ?: 85).toFloat()) }
    var selectedStance by remember {
        mutableStateOf(
            StakeholderSentiment.entries.find { it.name == initialProfile?.stance } ?: StakeholderSentiment.POSITIVE
        )
    }
    var projectResponsibilities by remember { mutableStateOf(initialProfile?.projectResponsibilities ?: "") }
    var notes by remember { mutableStateOf(initialProfile?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProfile == null) "Add Stakeholder Profile" else "Edit Stakeholder Profile",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_stakeholder_name")
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Organizational Role / Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_stakeholder_role")
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department / Business Unit *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_stakeholder_department")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Work Email") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_stakeholder_email")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone / Dispatch") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_stakeholder_phone")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = officeLocation,
                        onValueChange = { officeLocation = it },
                        label = { Text("Office / Substation") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_stakeholder_office")
                    )
                    OutlinedTextField(
                        value = preferredChannel,
                        onValueChange = { preferredChannel = it },
                        label = { Text("Preferred Channel") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_stakeholder_channel")
                    )
                }

                Text(
                    text = "Influence Level on Grid Projects",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfluenceLevel.entries.forEach { level ->
                        FilterChip(
                            selected = selectedInfluence == level,
                            onClick = { selectedInfluence = level },
                            label = { Text(level.badgeText, fontSize = 11.sp) },
                            modifier = Modifier.testTag("select_influence_${level.name.lowercase()}")
                        )
                    }
                }

                Text(
                    text = "Influence Score: ${influenceScore.roundToInt()} / 100",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Slider(
                    value = influenceScore,
                    onValueChange = { influenceScore = it },
                    valueRange = 1f..100f,
                    colors = SliderDefaults.colors(thumbColor = GridCyan, activeTrackColor = GridCyan),
                    modifier = Modifier.testTag("slider_influence_score")
                )

                Text(
                    text = "Primary Grid Domain",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GridDomain.entries.forEach { domain ->
                        FilterChip(
                            selected = selectedDomain == domain,
                            onClick = { selectedDomain = domain },
                            label = { Text(domain.shortName, fontSize = 11.sp) },
                            modifier = Modifier.testTag("select_domain_${domain.name.lowercase()}")
                        )
                    }
                }

                Text(
                    text = "Project Alignment Stance",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StakeholderSentiment.entries.forEach { stance ->
                        FilterChip(
                            selected = selectedStance == stance,
                            onClick = { selectedStance = stance },
                            label = { Text(stance.name, fontSize = 11.sp) },
                            modifier = Modifier.testTag("select_stance_${stance.name.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = projectResponsibilities,
                    onValueChange = { projectResponsibilities = it },
                    label = { Text("Project Governance & Sign-Off Responsibilities") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_stakeholder_responsibilities")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Engagement Strategy & Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_stakeholder_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && role.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            role.trim(),
                            department.trim().ifEmpty { "Grid Operations" },
                            email.trim(),
                            phone.trim(),
                            officeLocation.trim(),
                            preferredChannel.trim().ifEmpty { "Email" },
                            selectedDomain.name,
                            selectedInfluence.name,
                            influenceScore.roundToInt().coerceIn(1, 100),
                            selectedStance.name,
                            projectResponsibilities.trim(),
                            notes.trim()
                        )
                    }
                },
                enabled = name.isNotBlank() && role.isNotBlank(),
                modifier = Modifier.testTag("save_stakeholder_profile_confirm_button")
            ) {
                Text(if (initialProfile == null) "Save Profile" else "Update Profile")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_stakeholder_profile_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun influenceColor(level: InfluenceLevel): Color = when (level) {
    InfluenceLevel.EXECUTIVE_SPONSOR -> EnergyGold
    InfluenceLevel.DECISION_MAKER -> GridCyan
    InfluenceLevel.TECHNICAL_APPROVER -> Color(0xFFA855F7)
    InfluenceLevel.KEY_INFLUENCER -> GridEmerald
    InfluenceLevel.OPERATIONAL_STAKEHOLDER -> Color(0xFF38BDF8)
}

private fun sentimentBadgeColor(sentiment: StakeholderSentiment): Color = when (sentiment) {
    StakeholderSentiment.POSITIVE -> GridEmerald
    StakeholderSentiment.NEUTRAL -> GridCyan
    StakeholderSentiment.CAUTIOUS -> EnergyGold
    StakeholderSentiment.CONCERNED -> Color(0xFFEF4444)
}
