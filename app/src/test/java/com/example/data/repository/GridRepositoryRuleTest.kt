package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GridRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createInvestigation_authenticatedUser_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = GridRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createInvestigation(
                title = "Smart Recloser Automation",
                technologyType = "Distribution Automation",
                vendor = "Eaton",
                description = "Automated loop restoration system",
                capexEstimate = 320000.0,
                opexEstimate = 18000.0,
                saidiImprovement = 12.0
            )
        }
        assertTrue("Expected creation to succeed", result.isSuccess)
        val id = result.getOrThrow()
        assertTrue(id.isNotBlank())
    }

    @Test
    fun observeInvestigations_authenticatedOwner_returnsCreatedInvestigation() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = GridRepository(firestore)
        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createInvestigation(
                title = "Dynamic Line Rating",
                technologyType = "Transmission Sensors",
                vendor = "LineVision",
                description = "Real-time thermal capacity monitoring",
                capexEstimate = 450000.0,
                opexEstimate = 22000.0
            )
        }
        val id = createResult.getOrThrow()

        val list = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeInvestigations().first { items -> items.any { it.id == id } }
        }
        assertTrue(list.any { it.id == id })
    }

    @Test
    fun unauthenticatedAccess_failsWithIllegalStateException() = runBlocking {
        auth.signOut()
        val repository = GridRepository(firestore)

        try {
            repository.createInvestigation(
                title = "Unauth Project",
                technologyType = "ADMS",
                vendor = "Unknown",
                description = "Should fail before writing",
                capexEstimate = 10000.0,
                opexEstimate = 1000.0
            )
            fail("Expected exception for unauthenticated user")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("User must be signed in") == true)
        }
    }

    @Test
    fun logAndObservePilotKpi_authenticatedUser_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = GridRepository(firestore)

        val invResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createInvestigation(
                title = "Smart Substation Bus",
                technologyType = "Substation Automation",
                vendor = "ABB",
                description = "Bus protection modernization",
                capexEstimate = 200000.0,
                opexEstimate = 10000.0
            )
        }
        val invId = invResult.getOrThrow()

        val kpiResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.logPilotKpi(
                investigationId = invId,
                metricName = "Inrush Current Efficiency",
                category = "EFFICIENCY",
                currentValue = 15.5,
                targetValue = 12.0,
                unit = "%",
                notes = "Measured 15.5% efficiency improvement during peak testing."
            )
        }
        assertTrue("Expected KPI logging to succeed but got ${kpiResult.exceptionOrNull()}", kpiResult.isSuccess)
        val kpiId = kpiResult.getOrThrow()
        assertTrue(kpiId.isNotBlank())

        val kpis = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePilotKpis(invId).first { items -> items.any { it.id == kpiId } }
        }
        val loggedKpi = kpis.first { it.id == kpiId }
        assertEquals("Inrush Current Efficiency", loggedKpi.metricName)
        assertEquals(15.5, loggedKpi.currentValue, 0.001)
        assertEquals("EFFICIENCY", loggedKpi.category)
    }

    @Test
    fun saveUpdateAndObserveStakeholderProfile_authenticatedUser_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = GridRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveStakeholderProfile(
                name = "Elena Rostova",
                role = "Chief Information Security Officer",
                department = "IT & OT Cybersecurity",
                email = "e.rostova@gridutility.com",
                phone = "+1 (555) 892-1104",
                officeLocation = "SOC West Wing",
                preferredChannel = "Email",
                gridDomain = "CROSS_CUTTING",
                influenceLevel = "TECHNICAL_APPROVER",
                influenceScore = 94,
                stance = "CAUTIOUS",
                projectResponsibilities = "NERC-CIP compliance sign-off",
                notes = "Requires mTLS and DMZ segmentation."
            )
        }
        assertTrue("Expected profile save to succeed but got ${saveResult.exceptionOrNull()}", saveResult.isSuccess)
        val profileId = saveResult.getOrThrow()
        assertTrue(profileId.isNotBlank())

        val profiles = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeStakeholderProfiles().first { items -> items.any { it.id == profileId } }
        }
        val saved = profiles.first { it.id == profileId }
        assertEquals("Elena Rostova", saved.name)
        assertEquals("TECHNICAL_APPROVER", saved.influenceLevel)
        assertEquals(94, saved.influenceScore)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateStakeholderProfile(
                saved.copy(
                    influenceScore = 97,
                    stance = "POSITIVE"
                )
            )
        }
        assertTrue("Expected profile update to succeed", updateResult.isSuccess)

        val updatedProfiles = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeStakeholderProfiles().first { items ->
                items.any { it.id == profileId && it.influenceScore == 97 }
            }
        }
        val updated = updatedProfiles.first { it.id == profileId }
        assertEquals(97, updated.influenceScore)
        assertEquals("POSITIVE", updated.stance)
    }

    @Test
    fun saveUpdateAndObserveStakeholderInteraction_authenticatedUser_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = GridRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveStakeholderInteraction(
                investigationId = "GLOBAL",
                stakeholderName = "Dave Kowalski",
                stakeholderRole = "Transmission Line Superintendent",
                department = "Field Maintenance",
                gridDomain = "TRANSMISSION",
                communicationChannel = "Field Crew Safety Stand-Down",
                interactionDateLabel = "2025-02-20",
                communicationSummary = "Reviewed automated line sensor triage and Maximo work order triggers.",
                identifiedRebuttals = "Concerned about job displacement and extra mobile apps in bucket trucks.",
                rebuttalCategory = "WORKFORCE_AUGMENTATION",
                buyInStrategy = "Substitute tedious manual patrols in High Fire-Threat Districts and embed directly into Maximo.",
                buyInStatus = "REBUTTAL_IDENTIFIED",
                laborEfficiencyGainPct = 32.0,
                safetyImprovementPct = 41.0,
                workflowOptimizationPct = 38.0
            )
        }
        assertTrue("Expected interaction save to succeed but got ${saveResult.exceptionOrNull()}", saveResult.isSuccess)
        val interactionId = saveResult.getOrThrow()
        assertTrue(interactionId.isNotBlank())

        val interactions = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeStakeholderInteractions().first { items -> items.any { it.id == interactionId } }
        }
        val saved = interactions.first { it.id == interactionId }
        assertEquals("Dave Kowalski", saved.stakeholderName)
        assertEquals("REBUTTAL_IDENTIFIED", saved.buyInStatus)
        assertEquals(32.0, saved.laborEfficiencyGainPct, 0.01)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateStakeholderInteraction(
                saved.copy(
                    buyInStatus = "BUY_IN_ACHIEVED",
                    safetyImprovementPct = 45.0
                )
            )
        }
        assertTrue("Expected interaction update to succeed", updateResult.isSuccess)

        val updatedList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeStakeholderInteractions().first { items ->
                items.any { it.id == interactionId && it.buyInStatus == "BUY_IN_ACHIEVED" }
            }
        }
        val updated = updatedList.first { it.id == interactionId }
        assertEquals("BUY_IN_ACHIEVED", updated.buyInStatus)
        assertEquals(45.0, updated.safetyImprovementPct, 0.01)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}


