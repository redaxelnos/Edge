package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Interview
import com.example.data.model.Investigation
import com.example.data.model.PilotKpi
import com.example.data.model.UseCase
import com.example.ui.auth.AuthViewModel
import com.example.ui.theme.GridCyan
import com.example.ui.theme.GridEmerald
import com.example.ui.theme.GridNavy
import com.example.ui.viewmodel.AiActionState
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseUser
import java.util.Locale

data class NavTab(
    val title: String,
    val stepNumber: Int,
    val icon: ImageVector,
    val tag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    user: FirebaseUser
) {
    val context = LocalContext.current
    val investigations by mainViewModel.investigations.collectAsStateWithLifecycle()
    val selectedInvestigation by mainViewModel.selectedInvestigation.collectAsStateWithLifecycle()
    val interviews by mainViewModel.interviews.collectAsStateWithLifecycle()
    val useCases by mainViewModel.useCases.collectAsStateWithLifecycle()
    val pilotKpis by mainViewModel.pilotKpis.collectAsStateWithLifecycle()
    val stakeholderProfiles by mainViewModel.stakeholderProfiles.collectAsStateWithLifecycle()
    val stakeholderInteractions by mainViewModel.stakeholderInteractions.collectAsStateWithLifecycle()
    val currentTab by mainViewModel.activeTab.collectAsStateWithLifecycle()
    val generatedUseCases by mainViewModel.generatedUseCases.collectAsStateWithLifecycle()
    val isGeneratingUseCases by mainViewModel.isGeneratingUseCases.collectAsStateWithLifecycle()
    val useCaseGenerationError by mainViewModel.useCaseGenerationError.collectAsStateWithLifecycle()
    val adoptedDraftTitles by mainViewModel.adoptedDraftTitles.collectAsStateWithLifecycle()
    val quickAiState by mainViewModel.quickAiState.collectAsStateWithLifecycle()
    val thinkingAiState by mainViewModel.thinkingAiState.collectAsStateWithLifecycle()
    val imageAnalysisState by mainViewModel.imageAnalysisState.collectAsStateWithLifecycle()

    var showReportDialog by remember { mutableStateOf(false) }
    var showQuickAssistant by remember { mutableStateOf(false) }
    var showWorkflowGuide by remember { mutableStateOf(true) }
    var editingInvestigation by remember { mutableStateOf<Investigation?>(null) }

    val tabs = listOf(
        NavTab("1. Projects", 1, Icons.Default.PrecisionManufacturing, "tab_investigations"),
        NavTab("2. Stakeholders", 2, Icons.Default.Badge, "tab_stakeholder_profiles"),
        NavTab("3. Interaction Log", 3, Icons.Default.Forum, "tab_interaction_log"),
        NavTab("4. Interviews", 4, Icons.Default.RecordVoiceOver, "tab_interviews"),
        NavTab("5. Use Cases", 5, Icons.AutoMirrored.Filled.Assignment, "tab_use_cases"),
        NavTab("6. Impact Dashboard", 6, Icons.Default.Analytics, "tab_business_case"),
        NavTab("7. Integrations", 7, Icons.Default.Hub, "tab_integrations"),
        NavTab("8. Pilot Ops", 8, Icons.Default.Speed, "tab_pilot"),
        NavTab("9. Change Mgmt", 9, Icons.Default.Groups, "tab_change_mgmt"),
        NavTab("10. AI Strategy", 10, Icons.Default.AutoAwesome, "tab_ai_strategy"),
        NavTab("11. PDF Report", 11, Icons.Default.PictureAsPdf, "tab_stakeholder_report")
    )

    if (showReportDialog && selectedInvestigation != null) {
        StakeholderReportDialog(
            investigation = selectedInvestigation!!,
            interviews = interviews,
            useCases = useCases,
            pilotKpis = pilotKpis,
            onDismiss = { showReportDialog = false }
        )
    }

    if (showQuickAssistant) {
        QuickAssistantModal(
            onDismiss = { showQuickAssistant = false },
            aiState = quickAiState,
            onAskQuery = { mainViewModel.runQuickSynthesis(it) }
        )
    }

    editingInvestigation?.let { targetInv ->
        EditInvestigationDialog(
            investigation = targetInv,
            useCases = if (targetInv.id == selectedInvestigation?.id) useCases else emptyList(),
            onDismiss = { editingInvestigation = null },
            onSave = { updated ->
                mainViewModel.updateInvestigation(updated)
                editingInvestigation = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = GridCyan,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = user.displayName ?: user.email ?: "Utility Grid Modernization Suite",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showQuickAssistant = true },
                        modifier = Modifier.testTag("open_quick_assistant_button")
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Quick Grid AI Assistant",
                            tint = GridCyan
                        )
                    }
                    if (selectedInvestigation != null) {
                        IconButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier.testTag("export_pdf_report_button")
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "Export Stakeholder PDF Report",
                                tint = GridCyan
                            )
                        }
                    }
                    IconButton(
                        onClick = { authViewModel.signOut(context) },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpanded = maxWidth >= 840.dp
            Row(modifier = Modifier.fillMaxSize()) {
                if (isExpanded) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        tabs.forEachIndexed { index, tab ->
                            NavigationRailItem(
                                selected = currentTab == index,
                                onClick = { mainViewModel.setActiveTab(index) },
                                icon = { Icon(tab.icon, contentDescription = tab.title) },
                                label = { Text(tab.title, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("rail_${tab.tag}")
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (!isExpanded) {
                        PrimaryScrollableTabRow(
                            selectedTabIndex = currentTab,
                            edgePadding = 12.dp,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            tabs.forEachIndexed { index, tab ->
                                Tab(
                                    selected = currentTab == index,
                                    onClick = { mainViewModel.setActiveTab(index) },
                                    text = {
                                        Text(
                                            text = tab.title,
                                            fontWeight = if (currentTab == index) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            tab.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("scroll_${tab.tag}")
                                )
                            }
                        }
                    }

                    // Global Active Investigation Context Bar
                    ActiveInvestigationContextBar(
                        investigations = investigations,
                        selectedInvestigation = selectedInvestigation,
                        stakeholderProfilesCount = stakeholderProfiles.size,
                        interactionsCount = stakeholderInteractions.size,
                        interviewsCount = interviews.size,
                        useCasesCount = useCases.size,
                        kpisCount = pilotKpis.size,
                        showGuide = showWorkflowGuide,
                        onToggleGuide = { showWorkflowGuide = !showWorkflowGuide },
                        onSelectInvestigation = { mainViewModel.selectInvestigation(it) },
                        onEditInvestigation = { editingInvestigation = it },
                        onUpdateInvestigation = { mainViewModel.updateInvestigation(it) }
                    )

                    // Collapsible Step-by-Step UX Engagement Guide
                    AnimatedVisibility(visible = showWorkflowGuide) {
                        InteractiveWorkflowGuideCard(
                            currentTab = currentTab,
                            selectedInvestigation = selectedInvestigation,
                            stakeholderProfiles = stakeholderProfiles,
                            interactionsCount = stakeholderInteractions.size,
                            interviews = interviews,
                            useCases = useCases,
                            pilotKpis = pilotKpis,
                            onNavigateToTab = { mainViewModel.setActiveTab(it) },
                            onDismiss = { showWorkflowGuide = false }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        when (currentTab) {
                            0 -> InvestigationListSection(
                                investigations = investigations,
                                selectedInvestigation = selectedInvestigation,
                                onSelect = { mainViewModel.selectInvestigation(it.id) },
                                onCreate = { title, tech, vendor, desc, capex, opex, saidi, payback, roi, feasibility, feeder, weeks ->
                                    mainViewModel.createInvestigation(
                                        title = title,
                                        techType = tech,
                                        vendor = vendor,
                                        description = desc,
                                        capex = capex,
                                        opex = opex,
                                        saidi = saidi,
                                        payback = payback,
                                        roi = roi,
                                        feasibilityScore = feasibility,
                                        pilotFeeder = feeder,
                                        pilotWeeks = weeks
                                    )
                                },
                                onEdit = { editingInvestigation = it },
                                onDelete = { mainViewModel.deleteInvestigation(it) }
                            )
                            1 -> StakeholderProfilesSection(
                                profiles = stakeholderProfiles,
                                onSaveProfile = { name, role, dept, email, phone, office, channel, domain, infLevel, infScore, stance, resp, notes ->
                                    mainViewModel.saveStakeholderProfile(
                                        name = name,
                                        role = role,
                                        department = dept,
                                        email = email,
                                        phone = phone,
                                        officeLocation = office,
                                        preferredChannel = channel,
                                        gridDomain = domain,
                                        influenceLevel = infLevel,
                                        influenceScore = infScore,
                                        stance = stance,
                                        projectResponsibilities = resp,
                                        notes = notes
                                    )
                                },
                                onUpdateProfile = { mainViewModel.updateStakeholderProfile(it) },
                                onDeleteProfile = { mainViewModel.deleteStakeholderProfile(it) }
                            )
                            2 -> StakeholderInteractionLogSection(
                                investigation = selectedInvestigation,
                                interactions = stakeholderInteractions,
                                stakeholderProfiles = stakeholderProfiles,
                                onSaveInteraction = { invId, name, role, dept, domain, channel, dateLbl, summary, rebuttals, cat, strategy, status, laborPct, safetyPct, workflowPct ->
                                    mainViewModel.saveStakeholderInteraction(
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
                                },
                                onUpdateInteraction = { mainViewModel.updateStakeholderInteraction(it) },
                                onDeleteInteraction = { mainViewModel.deleteStakeholderInteraction(it) }
                            )
                            3 -> StakeholderInterviewsSection(
                                investigation = selectedInvestigation,
                                interviews = interviews,
                                stakeholderProfiles = stakeholderProfiles,
                                onAddInterview = { name, role, dept, domain, sentiment, notes, priority, feasibility, reqs, concerns ->
                                    mainViewModel.addInterview(
                                        stakeholderName = name,
                                        stakeholderRole = role,
                                        department = dept,
                                        gridDomain = domain,
                                        sentiment = sentiment,
                                        notes = notes,
                                        priority = priority,
                                        feasibility = feasibility,
                                        keyRequirements = reqs,
                                        integrationConcerns = concerns
                                    )
                                },
                                onUpdateInterview = { mainViewModel.updateInterview(it) },
                                onAutoCategorize = { notes, callback ->
                                    mainViewModel.autoCategorizeInterview(notes, callback)
                                },
                                onDeleteInterview = { mainViewModel.deleteInterview(it) }
                            )
                            4 -> UseCaseGeneratorSection(
                                investigation = selectedInvestigation,
                                interviews = interviews,
                                adoptedUseCases = useCases,
                                generatedDrafts = generatedUseCases,
                                adoptedDraftTitles = adoptedDraftTitles,
                                aiGenerating = isGeneratingUseCases,
                                generationError = useCaseGenerationError,
                                onGenerateAiUseCases = { domainFilter, priority, deep ->
                                    mainViewModel.generateUseCases(domainFilter, priority, deep)
                                },
                                onAdoptDraft = { mainViewModel.adoptUseCase(it) },
                                onCreateCustomUseCase = { title, desc, systems, savings, risk, feas, roi, domain, backing, weeks ->
                                    mainViewModel.addUseCase(
                                        title = title,
                                        description = desc,
                                        targetSystems = systems,
                                        projectedSavings = savings,
                                        riskLevel = risk,
                                        feasibilityScore = feas,
                                        projectedRoi = roi,
                                        gridDomain = domain,
                                        stakeholderBacking = backing,
                                        implementationWeeks = weeks
                                    )
                                },
                                onUpdateUseCase = { mainViewModel.updateUseCase(it) },
                                onDeleteUseCase = { mainViewModel.deleteUseCase(it) }
                            )
                            5 -> BusinessCaseSection(
                                investigation = selectedInvestigation,
                                useCases = useCases,
                                pilotKpis = pilotKpis,
                                stakeholderInteractions = stakeholderInteractions,
                                onUpdateInvestigation = { mainViewModel.updateInvestigation(it) },
                                onEditInvestigation = { editingInvestigation = it },
                                onUpdateUseCase = { mainViewModel.updateUseCase(it) },
                                onDeleteUseCase = { mainViewModel.deleteUseCase(it) },
                                onNavigateToInteractions = { mainViewModel.setActiveTab(2) }
                            )
                            6 -> SystemIntegrationSection(
                                investigation = selectedInvestigation,
                                onUpdateSystemStatus = { sysName, newStatus ->
                                    mainViewModel.updateIntegrationStatus(sysName, newStatus)
                                },
                                onUpdateInvestigation = { mainViewModel.updateInvestigation(it) }
                            )
                            7 -> PilotEvaluationSection(
                                investigation = selectedInvestigation,
                                pilotKpis = pilotKpis,
                                onPushToPilot = { feeder, weeks -> mainViewModel.pushToPilot(feeder, weeks) },
                                onAddKpi = { name, cat, curr, target, unit, notes ->
                                    mainViewModel.logPilotKpi(name, cat, curr, target, unit, notes)
                                },
                                onUpdateKpi = { mainViewModel.updatePilotKpi(it) },
                                onDeleteKpi = { mainViewModel.deletePilotKpi(it) },
                                onUpdateInvestigation = { mainViewModel.updateInvestigation(it) }
                            )
                            8 -> ChangeManagementSection(
                                investigation = selectedInvestigation,
                                interviews = interviews,
                                onUpdateInvestigation = { mainViewModel.updateInvestigation(it) }
                            )
                            9 -> AiStrategySection(
                                investigation = selectedInvestigation,
                                interviews = interviews,
                                useCases = useCases,
                                pilotKpis = pilotKpis,
                                thinkingAiState = thinkingAiState,
                                imageAnalysisState = imageAnalysisState,
                                onGenerateSummary = { mainViewModel.runDeepThinkingEvaluation() },
                                onAnalyzeImage = { bmp, prompt -> mainViewModel.analyzeGridImage(bmp, prompt) },
                                onSaveManualSummary = { updatedSummary ->
                                    selectedInvestigation?.let { inv ->
                                        mainViewModel.updateInvestigation(inv.copy(strategicSummary = updatedSummary))
                                    }
                                }
                            )
                            10 -> StakeholderReportSection(
                                investigation = selectedInvestigation,
                                interviews = interviews,
                                useCases = useCases,
                                pilotKpis = pilotKpis,
                                onOpenCustomizerDialog = { showReportDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiStrategySection(
    investigation: Investigation?,
    interviews: List<Interview>,
    useCases: List<UseCase>,
    pilotKpis: List<PilotKpi>,
    thinkingAiState: AiActionState,
    imageAnalysisState: AiActionState,
    onGenerateSummary: () -> Unit,
    onAnalyzeImage: (android.graphics.Bitmap, String) -> Unit,
    onSaveManualSummary: (String) -> Unit
) {
    if (investigation == null) {
        EmptySelectionPrompt("Select an investigation in the Projects tab to synthesize or edit an executive strategy.")
        return
    }

    val aiGenerating = thinkingAiState is AiActionState.Loading
    val aiStreamText = when (thinkingAiState) {
        is AiActionState.Streaming -> thinkingAiState.partialText
        is AiActionState.Success -> thinkingAiState.text
        else -> ""
    }

    var isEditingManual by remember(investigation.id) { mutableStateOf(false) }
    var showImageAnalyzer by remember { mutableStateOf(false) }
    var draftSummary by remember(investigation.id, investigation.strategicSummary) {
        mutableStateOf(investigation.strategicSummary)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GridNavy)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GridCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Executive Strategy & Synthesis Workspace",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Synthesizes live data for ${investigation.title}: ${interviews.size} stakeholder interviews, ${useCases.size} adopted use cases, and ${pilotKpis.size} measured pilot KPIs. Generate an AI assessment or directly author/refine the executive brief.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StrategyGroundingChip(
                        label = "Efficiency & Fit",
                        value = String.format(Locale.US, "+%.0f%% • %.0f/100", investigation.projectedRoi, investigation.pilotFeasibilityScore),
                        modifier = Modifier.weight(1f)
                    )
                    StrategyGroundingChip(
                        label = "SAIDI / ADKAR",
                        value = String.format(Locale.US, "+%.1f%% • %.0f%%", investigation.saidiImprovement, investigation.changeManagementScore),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onGenerateSummary,
                        enabled = !aiGenerating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GridCyan,
                            contentColor = GridNavy
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_ai_strategy_button")
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
                            Text("Synthesize with Gemini AI", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            draftSummary = aiStreamText.ifBlank { investigation.strategicSummary }
                            isEditingManual = !isEditingManual
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("toggle_manual_strategy_edit_btn")
                    ) {
                        Icon(
                            if (isEditingManual) Icons.Default.Close else Icons.Default.EditNote,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEditingManual) "Cancel" else "Edit Text")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showImageAnalyzer = !showImageAnalyzer },
                    colors = ButtonDefaults.textButtonColors(contentColor = GridCyan)
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showImageAnalyzer) "Hide Multimodal Grid Diagram Analyzer" else "Open Multimodal Grid Diagram / Photo Analyzer")
                }
            }
        }

        AnimatedVisibility(visible = showImageAnalyzer) {
            Box(modifier = Modifier.heightIn(max = 520.dp)) {
                ImageAnalyzerSection(
                    aiState = imageAnalysisState,
                    onAnalyzeImage = onAnalyzeImage
                )
            }
        }

        AnimatedVisibility(visible = isEditingManual) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GridCyan.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Customize Executive Strategic Brief",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = draftSummary,
                        onValueChange = { draftSummary = it },
                        label = { Text("Executive Summary & Governance Recommendations") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_strategy_summary_input"),
                        minLines = 6
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onSaveManualSummary(draftSummary.trim())
                                isEditingManual = false
                            },
                            modifier = Modifier.testTag("save_manual_strategy_btn")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Executive Summary")
                        }
                    }
                }
            }
        }

        val displaySummary = aiStreamText.ifBlank { investigation.strategicSummary }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Strategic Recommendation & Risk Mitigation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (displaySummary.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GridEmerald.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "GROUNDED IN LIVE PROJECT DATA",
                                style = MaterialTheme.typography.labelSmall,
                                color = GridEmerald,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = displaySummary.ifBlank {
                        "No executive summary recorded yet. Tap 'Synthesize with Gemini AI' above to generate a report grounded in your ${interviews.size} captured interviews, ${useCases.size} adopted use cases, and ${pilotKpis.size} pilot KPIs, or tap 'Edit Text' to author a custom summary."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.15f
                )
            }
        }
    }
}

@Composable
private fun StrategyGroundingChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
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

@Composable
fun EmptySelectionPrompt(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ElectricMeter,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
