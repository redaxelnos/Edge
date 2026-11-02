package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Interview
import com.example.data.model.Investigation
import com.example.data.model.InvestigationStatus
import com.example.data.model.PilotKpi
import com.example.data.model.StakeholderInteraction
import com.example.data.model.StakeholderProfile
import com.example.data.model.SystemIntegrationStatus
import com.example.data.model.UseCase
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GridRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeInvestigations(): Flow<List<Investigation>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/investigations"
        emitAll(
            db.collection("users").document(uid).collection("investigations")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Investigation::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getInvestigationById(id: String): Result<Investigation> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("investigations").document(id)
        return try {
            val snapshot = docRef.get().await()
            val inv = snapshot.toObject(Investigation::class.java)
            if (inv != null) {
                Result.success(inv)
            } else {
                Result.failure(NoSuchElementException("Investigation not found"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createInvestigation(
        title: String,
        technologyType: String,
        vendor: String,
        description: String,
        capexEstimate: Double,
        opexEstimate: Double,
        saidiImprovement: Double = 0.0,
        paybackYears: Double = 2.5,
        projectedRoi: Double = 35.0,
        pilotFeasibilityScore: Double = 80.0,
        pilotFeederSubstation: String = "Substation 14 - Feeder 3",
        pilotDurationWeeks: Int = 12
    ): Result<String> {
        val uid = requireUserId()
        val invId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("investigations").document(invId)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "title" to title,
            "technologyType" to technologyType,
            "vendor" to vendor,
            "status" to InvestigationStatus.INVESTIGATION.name,
            "description" to description,
            "capexEstimate" to capexEstimate,
            "opexEstimate" to opexEstimate,
            "projectedRoi" to projectedRoi,
            "paybackYears" to paybackYears,
            "saidiImprovement" to saidiImprovement,
            "pilotFeasibilityScore" to pilotFeasibilityScore,
            "maximoIntegrationStatus" to SystemIntegrationStatus.NOT_EVALUATED.name,
            "azureIntegrationStatus" to SystemIntegrationStatus.NOT_EVALUATED.name,
            "oracleIntegrationStatus" to SystemIntegrationStatus.NOT_EVALUATED.name,
            "snowflakeIntegrationStatus" to SystemIntegrationStatus.NOT_EVALUATED.name,
            "copilotReportingStatus" to SystemIntegrationStatus.NOT_EVALUATED.name,
            "pilotFeederSubstation" to pilotFeederSubstation,
            "pilotDurationWeeks" to pilotDurationWeeks,
            "pilotSuccessRate" to 0.0,
            "changeManagementScore" to 0.0,
            "strategicSummary" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(invId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateInvestigation(investigation: Investigation): Result<Unit> {
        val uid = requireUserId()
        require(investigation.id.isNotBlank()) { "Investigation ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("investigations").document(investigation.id)
        val updates = hashMapOf<String, Any>(
            "title" to investigation.title,
            "technologyType" to investigation.technologyType,
            "vendor" to investigation.vendor,
            "status" to investigation.status,
            "description" to investigation.description,
            "capexEstimate" to investigation.capexEstimate,
            "opexEstimate" to investigation.opexEstimate,
            "projectedRoi" to investigation.projectedRoi,
            "paybackYears" to investigation.paybackYears,
            "saidiImprovement" to investigation.saidiImprovement,
            "pilotFeasibilityScore" to investigation.pilotFeasibilityScore,
            "maximoIntegrationStatus" to investigation.maximoIntegrationStatus,
            "azureIntegrationStatus" to investigation.azureIntegrationStatus,
            "oracleIntegrationStatus" to investigation.oracleIntegrationStatus,
            "snowflakeIntegrationStatus" to investigation.snowflakeIntegrationStatus,
            "copilotReportingStatus" to investigation.copilotReportingStatus,
            "pilotFeederSubstation" to investigation.pilotFeederSubstation,
            "pilotDurationWeeks" to investigation.pilotDurationWeeks,
            "pilotSuccessRate" to investigation.pilotSuccessRate,
            "changeManagementScore" to investigation.changeManagementScore,
            "strategicSummary" to investigation.strategicSummary,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteInvestigation(id: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("investigations").document(id)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeInterviews(investigationId: String): Flow<List<Interview>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/investigations/$investigationId/interviews"
        emitAll(
            db.collection("users").document(uid).collection("investigations").document(investigationId)
                .collection("interviews")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Interview::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addInterview(
        investigationId: String,
        stakeholderName: String,
        stakeholderRole: String,
        department: String,
        gridDomain: String = "DISTRIBUTION",
        sentiment: String = "POSITIVE",
        notes: String,
        priorityRating: Int,
        feasibilityAssessment: String,
        keyRequirements: String,
        integrationConcerns: String
    ): Result<String> {
        val uid = requireUserId()
        val interviewId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("investigations").document(investigationId)
            .collection("interviews").document(interviewId)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "investigationId" to investigationId,
            "stakeholderName" to stakeholderName,
            "stakeholderRole" to stakeholderRole,
            "department" to department,
            "gridDomain" to gridDomain,
            "sentiment" to sentiment,
            "notes" to notes,
            "priorityRating" to priorityRating,
            "feasibilityAssessment" to feasibilityAssessment,
            "keyRequirements" to keyRequirements,
            "integrationConcerns" to integrationConcerns,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(interviewId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateInterview(interview: Interview): Result<Unit> {
        val uid = requireUserId()
        require(interview.id.isNotBlank()) { "Interview ID must not be blank" }
        require(interview.investigationId.isNotBlank()) { "Investigation ID must not be blank" }
        val docRef = db.collection("users").document(uid)
            .collection("investigations").document(interview.investigationId)
            .collection("interviews").document(interview.id)
        val updates = hashMapOf<String, Any>(
            "stakeholderName" to interview.stakeholderName,
            "stakeholderRole" to interview.stakeholderRole,
            "department" to interview.department,
            "gridDomain" to interview.gridDomain,
            "sentiment" to interview.sentiment,
            "notes" to interview.notes,
            "priorityRating" to interview.priorityRating.coerceIn(1, 5),
            "feasibilityAssessment" to interview.feasibilityAssessment,
            "keyRequirements" to interview.keyRequirements,
            "integrationConcerns" to interview.integrationConcerns,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteInterview(investigationId: String, interviewId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("investigations").document(investigationId)
            .collection("interviews").document(interviewId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUseCases(investigationId: String): Flow<List<UseCase>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/investigations/$investigationId/use_cases"
        emitAll(
            db.collection("users").document(uid).collection("investigations").document(investigationId)
                .collection("use_cases")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(UseCase::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addUseCase(
        investigationId: String,
        title: String,
        description: String,
        targetSystems: String,
        projectedAnnualSavings: Double,
        riskLevel: String,
        feasibilityScore: Double = 80.0,
        projectedRoi: Double = 30.0,
        gridDomain: String = "DISTRIBUTION",
        stakeholderBacking: String = "",
        implementationWeeks: Int = 16
    ): Result<String> {
        val uid = requireUserId()
        val useCaseId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("investigations").document(investigationId)
            .collection("use_cases").document(useCaseId)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "investigationId" to investigationId,
            "title" to title,
            "description" to description,
            "targetSystems" to targetSystems,
            "projectedAnnualSavings" to projectedAnnualSavings,
            "feasibilityScore" to feasibilityScore,
            "projectedRoi" to projectedRoi,
            "gridDomain" to gridDomain,
            "riskLevel" to riskLevel,
            "stakeholderBacking" to stakeholderBacking,
            "implementationWeeks" to implementationWeeks,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(useCaseId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateUseCase(useCase: UseCase): Result<Unit> {
        val uid = requireUserId()
        require(useCase.id.isNotBlank()) { "UseCase ID must not be blank" }
        require(useCase.investigationId.isNotBlank()) { "Investigation ID must not be blank" }
        val docRef = db.collection("users").document(uid)
            .collection("investigations").document(useCase.investigationId)
            .collection("use_cases").document(useCase.id)
        val updates = hashMapOf<String, Any>(
            "title" to useCase.title,
            "description" to useCase.description,
            "targetSystems" to useCase.targetSystems,
            "projectedAnnualSavings" to useCase.projectedAnnualSavings.coerceAtLeast(0.0),
            "feasibilityScore" to useCase.feasibilityScore.coerceIn(0.0, 100.0),
            "projectedRoi" to useCase.projectedRoi,
            "gridDomain" to useCase.gridDomain,
            "riskLevel" to useCase.riskLevel,
            "stakeholderBacking" to useCase.stakeholderBacking,
            "implementationWeeks" to useCase.implementationWeeks.coerceIn(0, 520),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteUseCase(investigationId: String, useCaseId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("investigations").document(investigationId)
            .collection("use_cases").document(useCaseId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observePilotKpis(investigationId: String): Flow<List<PilotKpi>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/investigations/$investigationId/pilot_kpis"
        emitAll(
            db.collection("users").document(uid).collection("investigations").document(investigationId)
                .collection("pilot_kpis")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(PilotKpi::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun logPilotKpi(
        investigationId: String,
        metricName: String,
        category: String,
        currentValue: Double,
        targetValue: Double,
        unit: String,
        notes: String
    ): Result<String> {
        val uid = requireUserId()
        val kpiId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("investigations").document(investigationId)
            .collection("pilot_kpis").document(kpiId)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "investigationId" to investigationId,
            "metricName" to metricName,
            "category" to category,
            "currentValue" to currentValue,
            "targetValue" to targetValue,
            "unit" to unit,
            "notes" to notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(kpiId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePilotKpi(kpi: PilotKpi): Result<Unit> {
        val uid = requireUserId()
        require(kpi.id.isNotBlank()) { "PilotKpi ID must not be blank" }
        require(kpi.investigationId.isNotBlank()) { "Investigation ID must not be blank" }
        val docRef = db.collection("users").document(uid)
            .collection("investigations").document(kpi.investigationId)
            .collection("pilot_kpis").document(kpi.id)
        val updates = hashMapOf<String, Any>(
            "metricName" to kpi.metricName,
            "category" to kpi.category,
            "currentValue" to kpi.currentValue,
            "targetValue" to kpi.targetValue,
            "unit" to kpi.unit,
            "notes" to kpi.notes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePilotKpi(investigationId: String, kpiId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("investigations").document(investigationId)
            .collection("pilot_kpis").document(kpiId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeStakeholderProfiles(): Flow<List<StakeholderProfile>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/stakeholder_profiles"
        emitAll(
            db.collection("users").document(uid).collection("stakeholder_profiles")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(StakeholderProfile::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveStakeholderProfile(
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
    ): Result<String> {
        val uid = requireUserId()
        val profileId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("stakeholder_profiles").document(profileId)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "name" to name,
            "role" to role,
            "department" to department,
            "email" to email,
            "phone" to phone,
            "officeLocation" to officeLocation,
            "preferredChannel" to preferredChannel,
            "gridDomain" to gridDomain,
            "influenceLevel" to influenceLevel,
            "influenceScore" to influenceScore,
            "stance" to stance,
            "projectResponsibilities" to projectResponsibilities,
            "notes" to notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(profileId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateStakeholderProfile(profile: StakeholderProfile): Result<Unit> {
        val uid = requireUserId()
        require(profile.id.isNotBlank()) { "StakeholderProfile ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("stakeholder_profiles").document(profile.id)
        val updates = hashMapOf<String, Any>(
            "name" to profile.name,
            "role" to profile.role,
            "department" to profile.department,
            "email" to profile.email,
            "phone" to profile.phone,
            "officeLocation" to profile.officeLocation,
            "preferredChannel" to profile.preferredChannel,
            "gridDomain" to profile.gridDomain,
            "influenceLevel" to profile.influenceLevel,
            "influenceScore" to profile.influenceScore,
            "stance" to profile.stance,
            "projectResponsibilities" to profile.projectResponsibilities,
            "notes" to profile.notes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteStakeholderProfile(profileId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("stakeholder_profiles").document(profileId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeStakeholderInteractions(): Flow<List<StakeholderInteraction>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/stakeholder_interactions"
        emitAll(
            db.collection("users").document(uid).collection("stakeholder_interactions")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(StakeholderInteraction::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveStakeholderInteraction(
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
    ): Result<String> {
        val uid = requireUserId()
        val interactionId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("stakeholder_interactions").document(interactionId)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "investigationId" to investigationId.ifBlank { "GLOBAL" },
            "stakeholderName" to stakeholderName,
            "stakeholderRole" to stakeholderRole,
            "department" to department,
            "gridDomain" to gridDomain,
            "communicationChannel" to communicationChannel,
            "interactionDateLabel" to interactionDateLabel,
            "communicationSummary" to communicationSummary,
            "identifiedRebuttals" to identifiedRebuttals,
            "rebuttalCategory" to rebuttalCategory,
            "buyInStrategy" to buyInStrategy,
            "buyInStatus" to buyInStatus,
            "laborEfficiencyGainPct" to laborEfficiencyGainPct.coerceIn(0.0, 100.0),
            "safetyImprovementPct" to safetyImprovementPct.coerceIn(0.0, 100.0),
            "workflowOptimizationPct" to workflowOptimizationPct.coerceIn(0.0, 100.0),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(interactionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateStakeholderInteraction(interaction: StakeholderInteraction): Result<Unit> {
        val uid = requireUserId()
        require(interaction.id.isNotBlank()) { "StakeholderInteraction ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("stakeholder_interactions").document(interaction.id)
        val updates = hashMapOf<String, Any>(
            "investigationId" to interaction.investigationId.ifBlank { "GLOBAL" },
            "stakeholderName" to interaction.stakeholderName,
            "stakeholderRole" to interaction.stakeholderRole,
            "department" to interaction.department,
            "gridDomain" to interaction.gridDomain,
            "communicationChannel" to interaction.communicationChannel,
            "interactionDateLabel" to interaction.interactionDateLabel,
            "communicationSummary" to interaction.communicationSummary,
            "identifiedRebuttals" to interaction.identifiedRebuttals,
            "rebuttalCategory" to interaction.rebuttalCategory,
            "buyInStrategy" to interaction.buyInStrategy,
            "buyInStatus" to interaction.buyInStatus,
            "laborEfficiencyGainPct" to interaction.laborEfficiencyGainPct.coerceIn(0.0, 100.0),
            "safetyImprovementPct" to interaction.safetyImprovementPct.coerceIn(0.0, 100.0),
            "workflowOptimizationPct" to interaction.workflowOptimizationPct.coerceIn(0.0, 100.0),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteStakeholderInteraction(interactionId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("stakeholder_interactions").document(interactionId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultStakeholderInteractionsIfEmpty(defaultInvestigationId: String = "GLOBAL") {
        val uid = requireUserId()
        val interactionsRef = db.collection("users").document(uid).collection("stakeholder_interactions")
        val existingInteractions = interactionsRef.limit(1).get().await()
        if (!existingInteractions.isEmpty) return

        saveStakeholderInteraction(
            investigationId = defaultInvestigationId,
            stakeholderName = "Dave Kowalski",
            stakeholderRole = "Transmission Line Superintendent",
            department = "Field Maintenance & Patrol",
            gridDomain = "TRANSMISSION",
            communicationChannel = "Field Crew Safety Stand-Down",
            interactionDateLabel = "2025-02-14",
            communicationSummary = "Reviewed automated line sensor triage and Maximo dispatch integration with senior linemen and union stewards at North Regional Yard.",
            identifiedRebuttals = "Crews expressed concern that automated fault/spark detection is a precursor to headcount reduction and that adding another tablet app in bucket trucks creates distraction.",
            rebuttalCategory = "WORKFORCE_AUGMENTATION",
            buyInStrategy = "Position sensors strictly as workforce augmentation that substitutes tedious, low-yield manual foot patrols in hazardous terrain—zero job loss. Integrate alerts directly into existing Maximo work orders so no new app is required.",
            buyInStatus = "BUY_IN_ACHIEVED",
            laborEfficiencyGainPct = 32.0,
            safetyImprovementPct = 41.0,
            workflowOptimizationPct = 38.0
        )

        saveStakeholderInteraction(
            investigationId = defaultInvestigationId,
            stakeholderName = "Marcus Vance",
            stakeholderRole = "Director of Grid Operations",
            department = "System Control Center",
            gridDomain = "DISTRIBUTION",
            communicationChannel = "Control Room Operations Sync",
            interactionDateLabel = "2025-02-17",
            communicationSummary = "Walked through automated DER curtailment and Volt/VAR switching guardrails during peak feeder loading windows.",
            identifiedRebuttals = "Control room dispatchers pushed back against 'black-box' automated switching and feared alarm flood during storm restoration events.",
            rebuttalCategory = "PROCESS_SIMPLICITY",
            buyInStrategy = "Embed a single-click human-in-the-loop supervisory approval inside the existing SCADA HMI rather than a separate console, substituting 14 manual spreadsheet calculations with 1 verified telemetry check.",
            buyInStatus = "PILOT_VALIDATION",
            laborEfficiencyGainPct = 26.0,
            safetyImprovementPct = 29.0,
            workflowOptimizationPct = 44.0
        )

        saveStakeholderInteraction(
            investigationId = defaultInvestigationId,
            stakeholderName = "Elena Rostova",
            stakeholderRole = "Chief Information Security Officer (CISO)",
            department = "IT & OT Cybersecurity",
            gridDomain = "CROSS_CUTTING",
            communicationChannel = "Architecture Governance Review",
            interactionDateLabel = "2025-02-19",
            communicationSummary = "Evaluated zero-trust mTLS gateway between substation RTUs, Azure IoT Hub, and Snowflake telemetry lakehouse.",
            identifiedRebuttals = "Objected to direct cellular backhaul from distribution sensors bypassing the OT DMZ and increasing NERC-CIP audit scope.",
            rebuttalCategory = "PROVEN_WORKFLOW_FIT",
            buyInStrategy = "Route all sensor payloads through the existing NERC-CIP certified substation gateway and pre-approved DMZ proxy, simplifying compliance audits rather than introducing a parallel security stack.",
            buyInStatus = "STRATEGY_IN_PROGRESS",
            laborEfficiencyGainPct = 19.0,
            safetyImprovementPct = 22.0,
            workflowOptimizationPct = 31.0
        )

        saveStakeholderInteraction(
            investigationId = defaultInvestigationId,
            stakeholderName = "Dr. Aris Thorne",
            stakeholderRole = "Principal Protection & Control Engineer",
            department = "Substation Engineering",
            gridDomain = "SUBSTATION",
            communicationChannel = "Relay Lab Technical Deep-Dive",
            interactionDateLabel = "2025-02-21",
            communicationSummary = "Demonstrated arc-flash and high-impedance fault interlock guardrails designed to prevent preventable crew injuries and lost-time incidents.",
            identifiedRebuttals = "Raised skepticism that predictive health scores could trigger premature relay trips or unnecessary de-energization during summer load peaks.",
            rebuttalCategory = "SAFETY_INJURY_GUARDRAILS",
            buyInStrategy = "Configure predictive alerts as non-tripping safety guardrails that automatically lock out hazardous hot-line reclose attempts when field crews are badged into the substation zone, directly cutting preventable lost-time injuries.",
            buyInStatus = "BUY_IN_ACHIEVED",
            laborEfficiencyGainPct = 24.0,
            safetyImprovementPct = 48.0,
            workflowOptimizationPct = 35.0
        )
    }

    suspend fun seedDefaultStakeholderProfilesIfEmpty() {
        val uid = requireUserId()
        val profilesRef = db.collection("users").document(uid).collection("stakeholder_profiles")
        val existingProfiles = profilesRef.limit(1).get().await()
        if (!existingProfiles.isEmpty) return

        saveStakeholderProfile(
            name = "Victoria Sterling",
            role = "SVP of Grid Modernization & Capital Delivery",
            department = "Executive Operations",
            email = "v.sterling@gridutility.com",
            phone = "+1 (555) 214-8900",
            officeLocation = "Corporate HQ - Floor 14, Suite 1402",
            preferredChannel = "Executive Briefing",
            gridDomain = "DISTRIBUTION",
            influenceLevel = "EXECUTIVE_SPONSOR",
            influenceScore = 96,
            stance = "POSITIVE",
            projectResponsibilities = "Chairs Capital Investment Committee; final sign-off on Phase 2 ADMS/DERMS rate-case filing and CapEx allocation > $1M.",
            notes = "Prioritizes SAIDI/SAIFI regulatory targets and demonstrable customer outage reductions within 24 months."
        )
        saveStakeholderProfile(
            name = "Marcus Vance",
            role = "Director of Grid Operations",
            department = "System Control Center",
            email = "m.vance@gridutility.com",
            phone = "+1 (555) 390-4412",
            officeLocation = "Primary Grid Control Center - Bldg B",
            preferredChannel = "Teams / Dispatch Line",
            gridDomain = "DISTRIBUTION",
            influenceLevel = "DECISION_MAKER",
            influenceScore = 91,
            stance = "POSITIVE",
            projectResponsibilities = "Approves operational cutover to Feeder 3 pilot; oversees real-time SCADA/ADMS dispatch workflows.",
            notes = "Strong advocate for automated DER curtailment signals as long as control room alarm fatigue is prevented."
        )
        saveStakeholderProfile(
            name = "Elena Rostova",
            role = "Chief Information Security Officer (CISO)",
            department = "IT & OT Cybersecurity",
            email = "e.rostova@gridutility.com",
            phone = "+1 (555) 892-1104",
            officeLocation = "Security Operations Center (SOC) - West Wing",
            preferredChannel = "Email",
            gridDomain = "CROSS_CUTTING",
            influenceLevel = "TECHNICAL_APPROVER",
            influenceScore = 94,
            stance = "CAUTIOUS",
            projectResponsibilities = "Mandatory NERC-CIP-005/007 architecture gatekeeper; approves cloud telemetry interconnect and HSM certificate provisioning.",
            notes = "Requires complete DMZ network segmentation review and zero-trust mTLS attestation before field cutover."
        )
        saveStakeholderProfile(
            name = "Dave Kowalski",
            role = "Transmission Line Superintendent",
            department = "Field Maintenance & Patrol",
            email = "d.kowalski@gridutility.com",
            phone = "+1 (555) 604-7731",
            officeLocation = "North Regional Service Yard - Bay 4",
            preferredChannel = "Phone / Field Radio",
            gridDomain = "TRANSMISSION",
            influenceLevel = "KEY_INFLUENCER",
            influenceScore = 82,
            stance = "POSITIVE",
            projectResponsibilities = "Leads 69kV-230kV line maintenance crews; validates IBM Maximo automated work-order dispatch accuracy in the field.",
            notes = "Enthusiastic champion for acoustic/optical spark sensors that eliminate manual foot patrols in High Fire-Threat Districts."
        )
        saveStakeholderProfile(
            name = "Dr. Aris Thorne",
            role = "Principal Protection & Control Engineer",
            department = "Substation Engineering",
            email = "a.thorne@gridutility.com",
            phone = "+1 (555) 481-3320",
            officeLocation = "Engineering Technical Lab - Room 308",
            preferredChannel = "Teams / Technical Memo",
            gridDomain = "SUBSTATION",
            influenceLevel = "TECHNICAL_APPROVER",
            influenceScore = 87,
            stance = "NEUTRAL",
            projectResponsibilities = "Signs off on IEC 61850 GOOSE relay settings, protection coordination studies, and substation RTU firmware changes.",
            notes = "Data-driven evaluator; requires hardware-in-the-loop (HIL) relay lab test logs prior to energizing pilot feeders."
        )
    }

    suspend fun seedDefaultDataIfEmpty() {
        seedDefaultStakeholderProfilesIfEmpty()
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("investigations")
        val existing = collectionRef.limit(1).get().await()
        if (!existing.isEmpty) {
            val firstInvId = existing.documents.firstOrNull()?.id ?: "GLOBAL"
            seedDefaultStakeholderInteractionsIfEmpty(firstInvId)
            return
        }

        // Seed 1: DERMS Integration with Azure and Snowflake
        val inv1Result = createInvestigation(
            title = "Grid-Edge DERMS & Active Network Management",
            technologyType = "ADMS / DERMS",
            vendor = "Schneider Electric & Microsoft",
            description = "High-penetration solar PV and battery energy storage management to prevent reverse power flow and feeder overvoltage.",
            capexEstimate = 1250000.0,
            opexEstimate = 85000.0,
            saidiImprovement = 14.5,
            paybackYears = 3.2,
            projectedRoi = 38.0
        )
        inv1Result.getOrNull()?.let { invId ->
            addInterview(
                investigationId = invId,
                stakeholderName = "Marcus Vance",
                stakeholderRole = "Director of Grid Operations",
                department = "System Control Center",
                gridDomain = "DISTRIBUTION",
                sentiment = "POSITIVE",
                notes = "Operators need automated DER curtailment signals mapped into SCADA without adding manual dispatch workload.",
                priorityRating = 5,
                feasibilityAssessment = "High feasibility if REST API or DNP3 interface is standardized.",
                keyRequirements = "Real-time 4-second telemetry, automatic volt/VAR optimization.",
                integrationConcerns = "Must integrate bidirectional streaming with Azure IoT Hub and Snowflake sensor lake."
            )
            addInterview(
                investigationId = invId,
                stakeholderName = "Elena Rostova",
                stakeholderRole = "Chief Information Security Officer (CISO)",
                department = "IT & OT Cybersecurity",
                gridDomain = "CROSS_CUTTING",
                sentiment = "CAUTIOUS",
                notes = "NERC-CIP compliance mandatory. Edge devices must use mTLS and HSM-backed certs.",
                priorityRating = 5,
                feasibilityAssessment = "Requires isolated OT DMZ proxy.",
                keyRequirements = "NERC-CIP-005/007 compliance, RBAC, encrypted telemetry in transit and at rest.",
                integrationConcerns = "Cloud endpoints on Azure must adhere to FedRAMP/NERC-CIP standards."
            )
            addUseCase(
                investigationId = invId,
                title = "Feeder Thermal Constraint Relief",
                description = "Dynamically modulate commercial battery storage discharge to shave peak loading on Substation Transformer Bank 2.",
                targetSystems = "Azure IoT Hub, Snowflake Data Cloud, SCADA",
                projectedAnnualSavings = 420000.0,
                riskLevel = "MEDIUM",
                feasibilityScore = 88.0,
                projectedRoi = 38.0,
                gridDomain = "DISTRIBUTION",
                stakeholderBacking = "Supported by System Control Center (Marcus Vance)",
                implementationWeeks = 14
            )
            logPilotKpi(
                investigationId = invId,
                metricName = "Feeder Peak Load Efficiency Gain",
                category = "EFFICIENCY",
                currentValue = 14.8,
                targetValue = 12.0,
                unit = "%",
                notes = "Measured 14.8% peak load shaving across Substation Feeder 3 with battery storage dispatch."
            )
            logPilotKpi(
                investigationId = invId,
                metricName = "SCADA Telemetry Availability",
                category = "UPTIME",
                currentValue = 99.98,
                targetValue = 99.90,
                unit = "%",
                notes = "4-second SCADA MQTT telemetry link sustained over 21-day continuous test period."
            )
            logPilotKpi(
                investigationId = invId,
                metricName = "Volt/VAR Feeder Deviation",
                category = "POWER_QUALITY",
                currentValue = 1.6,
                targetValue = 2.5,
                unit = "%",
                notes = "Volt/VAR optimization held feeder voltage strictly within ANSI C84.1 Range A (±5%)."
            )
        }

        // Seed 2: Wildfire Early Spark Detection & Maximo Work Order Automation
        val inv2Result = createInvestigation(
            title = "Substation Asset Health & Wildfire Spark Monitoring",
            technologyType = "Grid Edge Sensors & Predictive AI",
            vendor = "Siemens Energy & IBM Maximo",
            description = "Optical and acoustic corona discharge sensors on 69kV transmission lines with automated Maximo maintenance ticket generation.",
            capexEstimate = 780000.0,
            opexEstimate = 45000.0,
            saidiImprovement = 28.0,
            paybackYears = 2.1,
            projectedRoi = 52.0
        )
        inv2Result.getOrNull()?.let { invId ->
            addInterview(
                investigationId = invId,
                stakeholderName = "Dave Kowalski",
                stakeholderRole = "Transmission Line Superintendent",
                department = "Field Maintenance",
                gridDomain = "TRANSMISSION",
                sentiment = "POSITIVE",
                notes = "Field crews currently spend hundreds of hours on manual foot patrols. Automated Maximo work order triggers would slash triage time.",
                priorityRating = 5,
                feasibilityAssessment = "Very high return, crews are eager for predictive alerts.",
                keyRequirements = "GPS coordinate precision within 10 meters, automatic priority severity scoring in Maximo.",
                integrationConcerns = "Integration with IBM Maximo REST API for automated PM/CM work order generation."
            )
            addUseCase(
                investigationId = invId,
                title = "High-Impedance Arcing Fault Prevention",
                description = "Detect incipient insulator breakdown before conductor drops to ground in High Fire-Threat Districts (HFTD).",
                targetSystems = "IBM Maximo, Oracle NMS, Microsoft Copilot",
                projectedAnnualSavings = 950000.0,
                riskLevel = "LOW",
                feasibilityScore = 94.0,
                projectedRoi = 52.0,
                gridDomain = "TRANSMISSION",
                stakeholderBacking = "Field Maintenance & Transmission Line Crews (Dave Kowalski)",
                implementationWeeks = 12
            )
            logPilotKpi(
                investigationId = invId,
                metricName = "Automated Maximo Ticket Dispatch",
                category = "SAFETY_COMPLIANCE",
                currentValue = 100.0,
                targetValue = 95.0,
                unit = "%",
                notes = "All 18 incipient corona discharge events generated valid automated work orders in IBM Maximo."
            )
            logPilotKpi(
                investigationId = invId,
                metricName = "Transmission SAIDI Mitigation",
                category = "RELIABILITY",
                currentValue = 28.5,
                targetValue = 20.0,
                unit = "min",
                notes = "Averted high-impedance conductor fault saved estimated 28.5 minutes of customer outage time."
            )
            logPilotKpi(
                investigationId = invId,
                metricName = "Sensor Cluster Telemetry Uptime",
                category = "UPTIME",
                currentValue = 99.95,
                targetValue = 99.50,
                unit = "%",
                notes = "Solar-powered line sensor mesh maintained continuous Azure IoT link through severe storm conditions."
            )
        }

        val defaultInvId = inv1Result.getOrNull() ?: inv2Result.getOrNull() ?: "GLOBAL"
        seedDefaultStakeholderInteractionsIfEmpty(defaultInvId)
    }
}
