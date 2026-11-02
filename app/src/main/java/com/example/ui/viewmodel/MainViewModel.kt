package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeneratedUseCaseDraft
import com.example.ai.GridAiService
import com.example.data.model.Interview
import com.example.data.model.Investigation
import com.example.data.model.InvestigationStatus
import com.example.data.model.PilotKpi
import com.example.data.model.StakeholderInteraction
import com.example.data.model.StakeholderProfile
import com.example.data.model.SystemIntegrationStatus
import com.example.data.model.UseCase
import com.example.data.repository.GridRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

sealed interface AiActionState {
    data object Idle : AiActionState
    data object Loading : AiActionState
    data class Streaming(val partialText: String) : AiActionState
    data class Success(val text: String) : AiActionState
    data class Error(val message: String) : AiActionState
}

class MainViewModel(
    private val repository: GridRepository,
    private val aiService: GridAiService = GridAiService()
) : ViewModel() {

    constructor(context: Context) : this(
        GridRepository(context),
        GridAiService()
    )

    private val _investigations = MutableStateFlow<List<Investigation>>(emptyList())
    val investigations: StateFlow<List<Investigation>> = _investigations.asStateFlow()

    private val _selectedInvestigationId = MutableStateFlow<String?>(null)
    val selectedInvestigationId: StateFlow<String?> = _selectedInvestigationId.asStateFlow()

    private val _selectedInvestigation = MutableStateFlow<Investigation?>(null)
    val selectedInvestigation: StateFlow<Investigation?> = _selectedInvestigation.asStateFlow()

    private val _interviews = MutableStateFlow<List<Interview>>(emptyList())
    val interviews: StateFlow<List<Interview>> = _interviews.asStateFlow()

    private val _useCases = MutableStateFlow<List<UseCase>>(emptyList())
    val useCases: StateFlow<List<UseCase>> = _useCases.asStateFlow()

    private val _pilotKpis = MutableStateFlow<List<PilotKpi>>(emptyList())
    val pilotKpis: StateFlow<List<PilotKpi>> = _pilotKpis.asStateFlow()

    private val _stakeholderProfiles = MutableStateFlow<List<StakeholderProfile>>(emptyList())
    val stakeholderProfiles: StateFlow<List<StakeholderProfile>> = _stakeholderProfiles.asStateFlow()

    private val _stakeholderInteractions = MutableStateFlow<List<StakeholderInteraction>>(emptyList())
    val stakeholderInteractions: StateFlow<List<StakeholderInteraction>> = _stakeholderInteractions.asStateFlow()

    private val _generatedUseCases = MutableStateFlow<List<GeneratedUseCaseDraft>>(emptyList())
    val generatedUseCases: StateFlow<List<GeneratedUseCaseDraft>> = _generatedUseCases.asStateFlow()

    private val _isGeneratingUseCases = MutableStateFlow(false)
    val isGeneratingUseCases: StateFlow<Boolean> = _isGeneratingUseCases.asStateFlow()

    private val _useCaseGenerationError = MutableStateFlow<String?>(null)
    val useCaseGenerationError: StateFlow<String?> = _useCaseGenerationError.asStateFlow()

    private val _adoptedDraftTitles = MutableStateFlow<Set<String>>(emptySet())
    val adoptedDraftTitles: StateFlow<Set<String>> = _adoptedDraftTitles.asStateFlow()

    private val _quickAiState = MutableStateFlow<AiActionState>(AiActionState.Idle)
    val quickAiState: StateFlow<AiActionState> = _quickAiState.asStateFlow()

    private val _thinkingAiState = MutableStateFlow<AiActionState>(AiActionState.Idle)
    val thinkingAiState: StateFlow<AiActionState> = _thinkingAiState.asStateFlow()

    private val _imageAnalysisState = MutableStateFlow<AiActionState>(AiActionState.Idle)
    val imageAnalysisState: StateFlow<AiActionState> = _imageAnalysisState.asStateFlow()

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private var interviewObserveJob: Job? = null
    private var useCaseObserveJob: Job? = null
    private var pilotKpisObserveJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                repository.seedDefaultDataIfEmpty()
            } catch (e: Exception) {
                Log.w(TAG, "Seed default data notice: ${e.message}")
            }

            launch {
                repository.observeStakeholderProfiles()
                    .catch { error ->
                        Log.e(TAG, "Error observing stakeholder profiles", error)
                    }
                    .collect { profiles ->
                        _stakeholderProfiles.value = profiles
                    }
            }

            launch {
                repository.observeStakeholderInteractions()
                    .catch { error ->
                        Log.e(TAG, "Error observing stakeholder interactions", error)
                    }
                    .collect { interactions ->
                        _stakeholderInteractions.value = interactions
                    }
            }

            repository.observeInvestigations()
                .catch { error ->
                    Log.e(TAG, "Error observing investigations", error)
                }
                .collect { list ->
                    _investigations.value = list
                    if (_selectedInvestigationId.value == null && list.isNotEmpty()) {
                        selectInvestigation(list.first().id)
                    } else if (_selectedInvestigationId.value != null) {
                        _selectedInvestigation.value = list.find { it.id == _selectedInvestigationId.value }
                    }
                }
        }
    }

    fun selectInvestigation(id: String) {
        _selectedInvestigationId.value = id
        _selectedInvestigation.value = _investigations.value.find { it.id == id }
        _generatedUseCases.value = emptyList()
        _adoptedDraftTitles.value = emptySet()
        _useCaseGenerationError.value = null

        interviewObserveJob?.cancel()
        interviewObserveJob = viewModelScope.launch {
            repository.observeInterviews(id)
                .catch { Log.e(TAG, "Error observing interviews", it) }
                .collect { _interviews.value = it }
        }

        useCaseObserveJob?.cancel()
        useCaseObserveJob = viewModelScope.launch {
            repository.observeUseCases(id)
                .catch { Log.e(TAG, "Error observing use cases", it) }
                .collect { _useCases.value = it }
        }

        pilotKpisObserveJob?.cancel()
        pilotKpisObserveJob = viewModelScope.launch {
            repository.observePilotKpis(id)
                .catch { Log.e(TAG, "Error observing pilot KPIs", it) }
                .collect { _pilotKpis.value = it }
        }
    }

    fun setActiveTab(tabIndex: Int) {
        _activeTab.value = tabIndex
    }

    fun createInvestigation(
        title: String,
        techType: String,
        vendor: String,
        description: String,
        capex: Double,
        opex: Double,
        saidi: Double,
        payback: Double = 2.5,
        roi: Double = 35.0,
        feasibilityScore: Double = 80.0,
        pilotFeeder: String = "Substation 14 - Feeder 3",
        pilotWeeks: Int = 12,
        onComplete: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.createInvestigation(
                title = title,
                technologyType = techType,
                vendor = vendor,
                description = description,
                capexEstimate = capex,
                opexEstimate = opex,
                saidiImprovement = saidi,
                paybackYears = payback,
                projectedRoi = roi,
                pilotFeasibilityScore = feasibilityScore,
                pilotFeederSubstation = pilotFeeder,
                pilotDurationWeeks = pilotWeeks
            )
            result.onSuccess { id ->
                selectInvestigation(id)
                onComplete(id)
            }
        }
    }

    fun updateInvestigation(investigation: Investigation) {
        viewModelScope.launch {
            repository.updateInvestigation(investigation)
            _selectedInvestigation.value = investigation
        }
    }

    fun deleteInvestigation(id: String) {
        viewModelScope.launch {
            repository.deleteInvestigation(id)
            if (_selectedInvestigationId.value == id) {
                val next = _investigations.value.firstOrNull { it.id != id }
                if (next != null) {
                    selectInvestigation(next.id)
                } else {
                    _selectedInvestigationId.value = null
                    _selectedInvestigation.value = null
                    _interviews.value = emptyList()
                    _useCases.value = emptyList()
                    _pilotKpis.value = emptyList()
                }
            }
        }
    }

    fun addInterview(
        stakeholderName: String,
        stakeholderRole: String,
        department: String,
        gridDomain: String = "DISTRIBUTION",
        sentiment: String = "POSITIVE",
        notes: String,
        priority: Int,
        feasibility: String,
        keyRequirements: String,
        integrationConcerns: String
    ) {
        val invId = _selectedInvestigationId.value ?: return
        viewModelScope.launch {
            repository.addInterview(
                investigationId = invId,
                stakeholderName = stakeholderName,
                stakeholderRole = stakeholderRole,
                department = department,
                gridDomain = gridDomain,
                sentiment = sentiment,
                notes = notes,
                priorityRating = priority,
                feasibilityAssessment = feasibility,
                keyRequirements = keyRequirements,
                integrationConcerns = integrationConcerns
            )
        }
    }

    fun autoCategorizeInterview(
        notes: String,
        onResult: (domain: String, sentiment: String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val prompt = """
                    Classify these electric utility grid stakeholder interview notes:
                    Notes: "$notes"
                    
                    Respond with ONLY 2 uppercase words separated by a comma:
                    DOMAIN,SENTIMENT
                    Valid DOMAIN options: GENERATION, TRANSMISSION, DISTRIBUTION, SUBSTATION, CUSTOMER_DER, CROSS_CUTTING
                    Valid SENTIMENT options: POSITIVE, NEUTRAL, CAUTIOUS, CONCERNED
                    
                    Example output:
                    TRANSMISSION,POSITIVE
                """.trimIndent()
                val response = aiService.quickSynthesize(prompt).trim()
                val parts = response.split(",").map { it.trim().uppercase() }
                if (parts.size >= 2) {
                    onResult(parts[0], parts[1])
                }
            } catch (e: Exception) {
                Log.w(TAG, "Auto-categorize failed: ${e.message}")
            }
        }
    }

    fun updateInterview(interview: Interview) {
        viewModelScope.launch {
            repository.updateInterview(interview)
        }
    }

    fun deleteInterview(interviewId: String) {
        val invId = _selectedInvestigationId.value ?: return
        viewModelScope.launch {
            repository.deleteInterview(invId, interviewId)
        }
    }

    fun saveStakeholderProfile(
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
    ) {
        viewModelScope.launch {
            repository.saveStakeholderProfile(
                name = name,
                role = role,
                department = department,
                email = email,
                phone = phone,
                officeLocation = officeLocation,
                preferredChannel = preferredChannel,
                gridDomain = gridDomain,
                influenceLevel = influenceLevel,
                influenceScore = influenceScore,
                stance = stance,
                projectResponsibilities = projectResponsibilities,
                notes = notes
            )
        }
    }

    fun updateStakeholderProfile(profile: StakeholderProfile) {
        viewModelScope.launch {
            repository.updateStakeholderProfile(profile)
        }
    }

    fun deleteStakeholderProfile(profileId: String) {
        viewModelScope.launch {
            repository.deleteStakeholderProfile(profileId)
        }
    }

    fun saveStakeholderInteraction(
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
        laborEfficiencyGainPct: Double = 18.0,
        safetyImprovementPct: Double = 24.0,
        workflowOptimizationPct: Double = 30.0
    ) {
        val resolvedInvId = investigationId.ifBlank { _selectedInvestigationId.value ?: "GLOBAL" }
        viewModelScope.launch {
            repository.saveStakeholderInteraction(
                investigationId = resolvedInvId,
                stakeholderName = stakeholderName,
                stakeholderRole = stakeholderRole,
                department = department,
                gridDomain = gridDomain,
                communicationChannel = communicationChannel,
                interactionDateLabel = interactionDateLabel,
                communicationSummary = communicationSummary,
                identifiedRebuttals = identifiedRebuttals,
                rebuttalCategory = rebuttalCategory,
                buyInStrategy = buyInStrategy,
                buyInStatus = buyInStatus,
                laborEfficiencyGainPct = laborEfficiencyGainPct,
                safetyImprovementPct = safetyImprovementPct,
                workflowOptimizationPct = workflowOptimizationPct
            )
        }
    }

    fun updateStakeholderInteraction(interaction: StakeholderInteraction) {
        viewModelScope.launch {
            repository.updateStakeholderInteraction(interaction)
        }
    }

    fun deleteStakeholderInteraction(interactionId: String) {
        viewModelScope.launch {
            repository.deleteStakeholderInteraction(interactionId)
        }
    }

    fun addUseCase(
        title: String,
        description: String,
        targetSystems: String,
        projectedSavings: Double,
        riskLevel: String,
        feasibilityScore: Double = 85.0,
        projectedRoi: Double = 35.0,
        gridDomain: String = "DISTRIBUTION",
        stakeholderBacking: String = "",
        implementationWeeks: Int = 14
    ) {
        val invId = _selectedInvestigationId.value ?: return
        viewModelScope.launch {
            repository.addUseCase(
                investigationId = invId,
                title = title,
                description = description,
                targetSystems = targetSystems,
                projectedAnnualSavings = projectedSavings,
                riskLevel = riskLevel,
                feasibilityScore = feasibilityScore,
                projectedRoi = projectedRoi,
                gridDomain = gridDomain,
                stakeholderBacking = stakeholderBacking,
                implementationWeeks = implementationWeeks
            )
        }
    }

    fun updateUseCase(useCase: UseCase) {
        viewModelScope.launch {
            repository.updateUseCase(useCase)
        }
    }

    fun deleteUseCase(useCaseId: String) {
        val invId = _selectedInvestigationId.value ?: return
        viewModelScope.launch {
            repository.deleteUseCase(invId, useCaseId)
        }
    }

    fun generateUseCases(
        domainFilter: String? = null,
        strategicPriority: String? = null,
        useDeepThinking: Boolean = false
    ) {
        val current = _selectedInvestigation.value ?: return
        viewModelScope.launch {
            _isGeneratingUseCases.value = true
            _useCaseGenerationError.value = null
            try {
                val results = aiService.generateUseCasesFromInterviews(
                    investigation = current,
                    interviews = _interviews.value,
                    domainFilter = domainFilter,
                    strategicPriority = strategicPriority,
                    useDeepThinking = useDeepThinking
                )
                _generatedUseCases.value = results
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Generate use cases error", e)
                _useCaseGenerationError.value = e.localizedMessage ?: "Failed to generate use cases."
            } finally {
                _isGeneratingUseCases.value = false
            }
        }
    }

    fun adoptUseCase(draft: GeneratedUseCaseDraft) {
        val invId = _selectedInvestigationId.value ?: return
        viewModelScope.launch {
            repository.addUseCase(
                investigationId = invId,
                title = draft.title,
                description = draft.description,
                targetSystems = draft.targetSystems,
                projectedAnnualSavings = draft.projectedAnnualSavings,
                riskLevel = draft.riskLevel,
                feasibilityScore = draft.feasibilityScore,
                projectedRoi = draft.projectedRoi,
                gridDomain = draft.gridDomain,
                stakeholderBacking = draft.stakeholderBacking,
                implementationWeeks = draft.implementationWeeks
            )
            _adoptedDraftTitles.value = _adoptedDraftTitles.value + draft.title
        }
    }


    fun updateIntegrationStatus(
        systemName: String,
        newStatus: SystemIntegrationStatus
    ) {
        val current = _selectedInvestigation.value ?: return
        val updated = when (systemName) {
            "Maximo" -> current.copy(maximoIntegrationStatus = newStatus.name)
            "Azure" -> current.copy(azureIntegrationStatus = newStatus.name)
            "Oracle" -> current.copy(oracleIntegrationStatus = newStatus.name)
            "Snowflake" -> current.copy(snowflakeIntegrationStatus = newStatus.name)
            "Copilot" -> current.copy(copilotReportingStatus = newStatus.name)
            else -> current
        }
        updateInvestigation(updated)
    }

    fun pushToPilot(feederSubstation: String, durationWeeks: Int) {
        val current = _selectedInvestigation.value ?: return
        val updated = current.copy(
            status = InvestigationStatus.PILOT.name,
            pilotFeederSubstation = feederSubstation,
            pilotDurationWeeks = durationWeeks,
            pilotFeasibilityScore = 88.0
        )
        updateInvestigation(updated)
    }

    fun recordPilotResults(successRate: Double, notes: String) {
        val current = _selectedInvestigation.value ?: return
        val updated = current.copy(
            pilotSuccessRate = successRate,
            strategicSummary = if (current.strategicSummary.isNotBlank()) "${current.strategicSummary}\n\nPilot Outcome: $notes" else "Pilot Outcome: $notes"
        )
        updateInvestigation(updated)
    }

    fun logPilotKpi(
        metricName: String,
        category: String,
        currentValue: Double,
        targetValue: Double,
        unit: String,
        notes: String
    ) {
        val invId = _selectedInvestigationId.value ?: return
        viewModelScope.launch {
            repository.logPilotKpi(
                investigationId = invId,
                metricName = metricName,
                category = category,
                currentValue = currentValue,
                targetValue = targetValue,
                unit = unit,
                notes = notes
            )
        }
    }

    fun updatePilotKpi(kpi: PilotKpi) {
        viewModelScope.launch {
            repository.updatePilotKpi(kpi)
        }
    }

    fun deletePilotKpi(kpiId: String) {
        val invId = _selectedInvestigationId.value ?: return
        viewModelScope.launch {
            repository.deletePilotKpi(invId, kpiId)
        }
    }

    fun advanceToChangeManagement(adkarScore: Double) {
        val current = _selectedInvestigation.value ?: return
        val updated = current.copy(
            status = InvestigationStatus.CHANGE_MANAGEMENT.name,
            changeManagementScore = adkarScore
        )
        updateInvestigation(updated)
    }

    fun advanceToEcosystemAdoption() {
        val current = _selectedInvestigation.value ?: return
        val updated = current.copy(
            status = InvestigationStatus.ECOSYSTEM_ADOPTION.name
        )
        updateInvestigation(updated)
    }

    // 1. Fast synthesis with low-latency model: gemini-3.1-flash-lite
    fun runQuickSynthesis(userPrompt: String) {
        viewModelScope.launch {
            _quickAiState.value = AiActionState.Loading
            try {
                val current = _selectedInvestigation.value
                val context = if (current != null) {
                    "Context: Tech = ${current.title}, Vendor = ${current.vendor}, Status = ${current.status}. User query: $userPrompt"
                } else {
                    userPrompt
                }
                val result = aiService.quickSynthesize(context)
                _quickAiState.value = AiActionState.Success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Quick synthesis error", e)
                _quickAiState.value = AiActionState.Error(e.localizedMessage ?: "Quick synthesis failed.")
            }
        }
    }

    // 2. High thinking strategic evaluation: gemini-4-argon with ThinkingLevel.HIGH
    fun runDeepThinkingEvaluation() {
        val current = _selectedInvestigation.value ?: return
        val interviewText = _interviews.value.joinToString("\n---\n") {
            "Stakeholder: ${it.stakeholderName} (${it.stakeholderRole}, Dept: ${it.department})\nGrid Domain: ${it.gridDomain} | Sentiment: ${it.sentiment}\nPriority: ${it.priorityRating}/5\nNotes: ${it.notes}\nRequirements: ${it.keyRequirements}\nConcerns: ${it.integrationConcerns}"
        }
        val useCaseText = _useCases.value.joinToString("\n---\n") {
            "Use Case: ${it.title} | Systems: ${it.targetSystems} | Annual Savings: $${it.projectedAnnualSavings} | Risk: ${it.riskLevel}\n${it.description}"
        }

        viewModelScope.launch {
            _thinkingAiState.value = AiActionState.Loading
            try {
                val result = aiService.analyzeComplexStrategy(
                    title = current.title,
                    techType = current.technologyType,
                    vendor = current.vendor,
                    interviewsSummary = interviewText.ifBlank { "No specific stakeholder interview records provided yet." },
                    useCasesSummary = useCaseText.ifBlank { "No specific use cases documented yet." },
                    capex = current.capexEstimate,
                    opex = current.opexEstimate,
                    saidi = current.saidiImprovement
                )
                _thinkingAiState.value = AiActionState.Success(result)
                // Also update strategic summary in document
                updateInvestigation(current.copy(strategicSummary = result))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "High thinking analysis error", e)
                _thinkingAiState.value = AiActionState.Error(e.localizedMessage ?: "High thinking analysis failed.")
            }
        }
    }

    // 3. Multimodal image understanding: gemini-4-argon
    fun analyzeGridImage(bitmap: Bitmap, prompt: String) {
        viewModelScope.launch {
            _imageAnalysisState.value = AiActionState.Loading
            try {
                val result = aiService.analyzeGridImage(bitmap, prompt)
                _imageAnalysisState.value = AiActionState.Success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image analysis error", e)
                _imageAnalysisState.value = AiActionState.Error(e.localizedMessage ?: "Image analysis failed.")
            }
        }
    }

    fun clearAiStates() {
        _quickAiState.value = AiActionState.Idle
        _thinkingAiState.value = AiActionState.Idle
        _imageAnalysisState.value = AiActionState.Idle
    }
}
