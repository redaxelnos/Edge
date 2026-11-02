package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class InvestigationStatus(val label: String, val stepIndex: Int) {
    INVESTIGATION("Tech Investigation", 0),
    BUSINESS_CASE("Business Case & ROI", 1),
    PILOT("Push-to-Pilot & Testing", 2),
    CHANGE_MANAGEMENT("Change Management (ADKAR)", 3),
    ECOSYSTEM_ADOPTION("Ecosystem Adoption", 4)
}

enum class SystemIntegrationStatus(val label: String) {
    NOT_EVALUATED("Not Evaluated"),
    PLANNED("Architecture Planned"),
    VERIFIED("API/Interface Verified"),
    INTEGRATED("Fully Integrated")
}

enum class GridDomain(val label: String, val shortName: String, val code: String) {
    GENERATION("Generation & Bulk Energy Storage", "Generation", "GEN"),
    TRANSMISSION("Transmission Lines (69kV - 500kV)", "Transmission", "TX"),
    DISTRIBUTION("Distribution Grid & Feeders (4kV - 34.5kV)", "Distribution", "DX"),
    SUBSTATION("Substation Automation & Protection", "Substation", "SUB"),
    CUSTOMER_DER("Customer Microgrids & Distributed Energy (DER)", "Customer/DER", "DER"),
    CROSS_CUTTING("Cross-Cutting IT/OT & Cybersecurity", "IT/OT & Cyber", "IT/OT")
}

enum class StakeholderSentiment(val label: String, val score: Int) {
    POSITIVE("Supportive / High ROI", 4),
    NEUTRAL("Objective / Feasible", 3),
    CAUTIOUS("Cautious / Needs Gating", 2),
    CONCERNED("Critical / High Barrier", 1)
}

@IgnoreExtraProperties
data class Investigation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val technologyType: String = "",
    val vendor: String = "",
    val status: String = InvestigationStatus.INVESTIGATION.name,
    val description: String = "",
    val capexEstimate: Double = 0.0,
    val opexEstimate: Double = 0.0,
    val projectedRoi: Double = 0.0,
    val paybackYears: Double = 0.0,
    val saidiImprovement: Double = 0.0,
    val pilotFeasibilityScore: Double = 0.0,
    val maximoIntegrationStatus: String = SystemIntegrationStatus.NOT_EVALUATED.name,
    val azureIntegrationStatus: String = SystemIntegrationStatus.NOT_EVALUATED.name,
    val oracleIntegrationStatus: String = SystemIntegrationStatus.NOT_EVALUATED.name,
    val snowflakeIntegrationStatus: String = SystemIntegrationStatus.NOT_EVALUATED.name,
    val copilotReportingStatus: String = SystemIntegrationStatus.NOT_EVALUATED.name,
    val pilotFeederSubstation: String = "",
    val pilotDurationWeeks: Int = 12,
    val pilotSuccessRate: Double = 0.0,
    val changeManagementScore: Double = 0.0,
    val strategicSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Interview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val investigationId: String = "",
    val stakeholderName: String = "",
    val stakeholderRole: String = "",
    val department: String = "",
    val gridDomain: String = GridDomain.DISTRIBUTION.name,
    val sentiment: String = StakeholderSentiment.POSITIVE.name,
    val notes: String = "",
    val priorityRating: Int = 3,
    val feasibilityAssessment: String = "",
    val keyRequirements: String = "",
    val integrationConcerns: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UseCase(
    @DocumentId val id: String = "",
    val userId: String = "",
    val investigationId: String = "",
    val title: String = "",
    val description: String = "",
    val targetSystems: String = "",
    val projectedAnnualSavings: Double = 0.0,
    val feasibilityScore: Double = 80.0,
    val projectedRoi: Double = 30.0,
    val gridDomain: String = GridDomain.DISTRIBUTION.name,
    val riskLevel: String = "MEDIUM",
    val stakeholderBacking: String = "",
    val implementationWeeks: Int = 16,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class KpiCategory(val label: String, val defaultUnit: String) {
    EFFICIENCY("Efficiency Gains", "%"),
    UPTIME("Uptime & Availability", "%"),
    RELIABILITY("Reliability & Outage Delta", "min"),
    POWER_QUALITY("Power Quality & Voltage", "V"),
    COST_REDUCTION("Operational Cost Savings", "$/mo"),
    SAFETY_COMPLIANCE("Safety & Work Orders", "events")
}

@IgnoreExtraProperties
data class PilotKpi(
    @DocumentId val id: String = "",
    val userId: String = "",
    val investigationId: String = "",
    val metricName: String = "",
    val category: String = KpiCategory.EFFICIENCY.name,
    val currentValue: Double = 0.0,
    val targetValue: Double = 0.0,
    val unit: String = "%",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class InfluenceLevel(val label: String, val badgeText: String, val description: String, val tierWeight: Int) {
    EXECUTIVE_SPONSOR("Executive Sponsor", "Exec Sponsor", "C-Suite / VP budget authority & strategic sign-off", 5),
    DECISION_MAKER("Decision Maker", "Decision Maker", "Director / Governance voting authority on pilot & rollout", 4),
    TECHNICAL_APPROVER("Technical Approver", "Tech Approver", "Principal Engineer / CISO architectural & compliance gatekeeper", 4),
    KEY_INFLUENCER("Key Influencer", "Key Influencer", "Subject Matter Expert / Operations lead shaping requirements", 3),
    OPERATIONAL_STAKEHOLDER("Operational Stakeholder", "Operational", "Field Superintendent / Operator impacted by day-to-day workflow", 2)
}

@IgnoreExtraProperties
data class StakeholderProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val role: String = "",
    val department: String = "",
    val email: String = "",
    val phone: String = "",
    val officeLocation: String = "",
    val preferredChannel: String = "Email",
    val gridDomain: String = GridDomain.DISTRIBUTION.name,
    val influenceLevel: String = InfluenceLevel.KEY_INFLUENCER.name,
    val influenceScore: Int = 80,
    val stance: String = StakeholderSentiment.POSITIVE.name,
    val projectResponsibilities: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class BuyInStatus(val label: String, val stepOrder: Int) {
    INITIAL_OUTREACH("Initial Outreach", 1),
    REBUTTAL_IDENTIFIED("Rebuttal Identified", 2),
    STRATEGY_IN_PROGRESS("Strategy In Progress", 3),
    PILOT_VALIDATION("Pilot Validation", 4),
    BUY_IN_ACHIEVED("Buy-In Achieved", 5)
}

enum class RebuttalCategory(val label: String, val shortLabel: String) {
    WORKFORCE_AUGMENTATION("Workforce Augmentation (No Job Loss)", "Job Augmentation"),
    SAFETY_INJURY_GUARDRAILS("Preventable Injury & Time-Off Guardrails", "Safety Guardrails"),
    PROCESS_SIMPLICITY("Process Simplicity vs. Tool Clutter", "Process Simplicity"),
    PROVEN_WORKFLOW_FIT("Integration into Proven Workflows", "Proven Workflow Fit"),
    OPERATIONAL_TRUST("Field Reliability & Operator Trust", "Operational Trust")
}

@IgnoreExtraProperties
data class StakeholderInteraction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val investigationId: String = "",
    val stakeholderName: String = "",
    val stakeholderRole: String = "",
    val department: String = "",
    val gridDomain: String = GridDomain.DISTRIBUTION.name,
    val communicationChannel: String = "Working Session",
    val interactionDateLabel: String = "2025-02-20",
    val communicationSummary: String = "",
    val identifiedRebuttals: String = "",
    val rebuttalCategory: String = RebuttalCategory.WORKFORCE_AUGMENTATION.name,
    val buyInStrategy: String = "",
    val buyInStatus: String = BuyInStatus.REBUTTAL_IDENTIFIED.name,
    val laborEfficiencyGainPct: Double = 18.0,
    val safetyImprovementPct: Double = 24.0,
    val workflowOptimizationPct: Double = 30.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class OptimizationTimePoint(
    val periodLabel: String,
    val shortLabel: String,
    val laborEfficiencyGainPct: Float,
    val safetyImprovementPct: Float,
    val workflowOptimizationPct: Float,
    val milestoneSummary: String
)



