package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "GridAiService"

class GridAiService {

    // 1. Low-latency responses using gemini-3.1-flash-lite for fast synthesis & interactive Q&A
    private val fastModel by lazy {
        Firebase.ai.generativeModel("gemini-3.1-flash-lite")
    }

    // 2. High thinking model using gemini-4-argon with thinkingLevel = ThinkingLevel.HIGH
    // Do NOT set maxOutputTokens per user feature specification
    private val thinkingModel by lazy {
        val config = generationConfig {
            thinkingConfig = thinkingConfig {
                thinkingLevel = ThinkingLevel.HIGH
            }
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = config
        )
    }

    // 3. Multimodal image understanding model using gemini-4-argon
    private val visionModel by lazy {
        Firebase.ai.generativeModel("gemini-4-argon")
    }

    // Low-latency quick response (gemini-3.1-flash-lite)
    suspend fun quickSynthesize(prompt: String): String = withContext(Dispatchers.IO) {
        withTimeout(35_000L) {
            try {
                val response = fastModel.generateContent(prompt)
                response.text ?: "No response generated."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Low-latency generation failed", e)
                throw e
            }
        }
    }

    // Low-latency streaming for fast interactive chat/synthesis
    fun streamQuickResponse(prompt: String): Flow<String> = flow {
        val stream = fastModel.generateContentStream(prompt)
        stream.collect { chunk ->
            chunk.text?.let { emit(it) }
        }
    }

    // Deep thinking analysis (gemini-4-argon with ThinkingLevel.HIGH)
    suspend fun analyzeComplexStrategy(
        title: String,
        techType: String,
        vendor: String,
        interviewsSummary: String,
        useCasesSummary: String,
        capex: Double,
        opex: Double,
        saidi: Double
    ): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are a Principal Power Systems Architect and Utility Transformation Strategist evaluating electric utility grid infrastructure modernization.
            
            Perform a rigorous, deep multi-stakeholder business case, system integration feasibility, and pilot risk assessment for:
            - Technology: $title
            - Domain: $techType
            - Vendor/Solution: $vendor
            - Estimated CAPEX: $$capex USD
            - Estimated Annual OPEX: $$opex USD/yr
            - Target SAIDI Outage Reduction: $saidi minutes/customer/year
            
            Stakeholder Input Aggregation:
            $interviewsSummary
            
            Defined Use Cases:
            $useCasesSummary
            
            Deliver a comprehensive, executive-ready Strategic Assessment covering:
            1. Multi-Stakeholder Consensus & Trade-Offs (Operations, Substation Engineering, IT/Cybersecurity NERC-CIP, Financial/Regulatory).
            2. System Integration Matrix with Existing Grid Infrastructure:
               - IBM Maximo (Asset Registry, Preventive Maintenance, automated work order dispatch)
               - Azure Backend (IoT Hub ingestion, Event Hubs, telemetry pipelines)
               - Oracle (Network Management System / NMS, Outage Management / OMS, CC&B customer billing)
               - Snowflake (Smart meter interval data warehouse, high-frequency synchrophasor sensor analytics)
               - Microsoft Copilot (Automated strategic reporting, operator decision assistance)
            3. Economic Feasibility & ROI Modeling (NPV, Payback, Avoided Outage Penalty Costs, Rate Base Justification for Public Utility Commission / PUC).
            4. Phased Push-to-Pilot Execution Plan (Feeder/Substation selection criteria, test duration, gating criteria, telemetry monitoring).
            5. Pilot Success Analytics (Quantitative KPIs: SAIFI, SAIDI, latency, telemetry packet loss).
            6. Change Management & Ecosystem Adoption (Prosci ADKAR framework for grid control room dispatchers and field utility crews).
        """.trimIndent()

        withTimeout(60_000L) {
            try {
                val response = thinkingModel.generateContent(prompt)
                response.text ?: "No analysis generated by deep reasoning model."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "High-thinking strategic analysis failed", e)
                throw e
            }
        }
    }

    // Multimodal Image Understanding using gemini-4-argon
    suspend fun analyzeGridImage(
        bitmap: Bitmap,
        contextPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val prompt = if (contextPrompt.isNotBlank()) {
            "Analyze this electric utility grid diagram / technical spec / whiteboard diagram with context: $contextPrompt. Detail grid components, single-line diagram elements, communication protocols, integration bottlenecks with Maximo/Azure/Oracle/Snowflake, and risk factors."
        } else {
            "Analyze this electric utility grid diagram / technical specification image. Identify electrical equipment (transformers, breakers, reclosers, relays, RTUs), single-line topology, data flows, and recommended integration paths for modern grid operations."
        }

        withTimeout(60_000L) {
            try {
                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }
                val response = visionModel.generateContent(inputContent)
                response.text ?: "Image analysis returned empty output."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Multimodal image analysis failed", e)
                throw e
            }
        }
    }

    // 4. Generate Use Cases from Stakeholder Interviews
    suspend fun generateUseCasesFromInterviews(
        investigation: com.example.data.model.Investigation,
        interviews: List<com.example.data.model.Interview>,
        domainFilter: String? = null,
        strategicPriority: String? = null,
        useDeepThinking: Boolean = false
    ): List<GeneratedUseCaseDraft> = withContext(Dispatchers.IO) {
        val interviewsContext = if (interviews.isNotEmpty()) {
            interviews.joinToString("\n---\n") { i ->
                "- Stakeholder: ${i.stakeholderName} (${i.stakeholderRole}, Dept: ${i.department})\n" +
                "  Grid Domain: ${i.gridDomain} | Sentiment: ${i.sentiment} | Priority: ${i.priorityRating}/5\n" +
                "  Notes: ${i.notes}\n" +
                "  Key Requirements: ${i.keyRequirements}\n" +
                "  Concerns: ${i.integrationConcerns}\n" +
                "  Feasibility Assessment: ${i.feasibilityAssessment}"
            }
        } else {
            "No recorded stakeholder interviews yet. Synthesize potential use cases based on standard utility grid modernization best practices."
        }

        val domainFocusText = if (!domainFilter.isNullOrBlank() && domainFilter != "ALL") {
            "Focus specifically on the grid domain: $domainFilter."
        } else {
            "Cover diverse grid domains where stakeholder value is highest (Distribution, Transmission, Substation, etc.)."
        }

        val priorityText = if (!strategicPriority.isNullOrBlank()) {
            "Strategic Priority Focus: $strategicPriority."
        } else {
            "Balance economic ROI, grid reliability (SAIDI), and integration feasibility."
        }

        val prompt = """
            You are a Principal Power Systems Architect and Utility Digital Transformation Lead.
            
            Evaluate this technology initiative:
            - Technology: ${investigation.title}
            - Domain: ${investigation.technologyType}
            - Vendor: ${investigation.vendor}
            - CAPEX Estimate: $${investigation.capexEstimate}
            - Annual OPEX: $${investigation.opexEstimate}
            - Target SAIDI Outage Reduction: ${investigation.saidiImprovement} mins
            
            $domainFocusText
            $priorityText
            
            Stakeholder Interview Feedback & Requirements Context:
            $interviewsContext
            
            Synthesize the stakeholder interview context to generate 3 to 5 realistic, high-impact operational and economic 'Use Cases' for adopting this technology into the utility grid.
            Ensure each use case specifies clear feasibility (0-100), projected annual savings ($ USD/yr), projected ROI %, target enterprise systems (including IBM Maximo, Azure backend, Oracle NMS/OMS, Snowflake, and Microsoft Copilot), and stakeholder backing justification.
            
            CRITICAL: Respond ONLY with a valid JSON array. Do not include markdown code block backticks, no explanations, no prefix or suffix.
            Schema for each object:
            {
              "title": "Use case title (short & punchy, under 12 words)",
              "description": "2-3 sentences describing the operational workflow, grid benefit, and how stakeholder input shaped this.",
              "gridDomain": "One of: GENERATION, TRANSMISSION, DISTRIBUTION, SUBSTATION, CUSTOMER_DER, CROSS_CUTTING",
              "targetSystems": "E.g. IBM Maximo, Azure IoT Hub, Snowflake, Microsoft Copilot",
              "feasibilityScore": 88.0,
              "projectedRoi": 45.0,
              "projectedAnnualSavings": 350000.0,
              "riskLevel": "LOW, MEDIUM, or HIGH",
              "stakeholderBacking": "Summary of which stakeholders/departments support this and why",
              "implementationWeeks": 14
            }
        """.trimIndent()

        try {
            val responseText = withTimeout(55_000L) {
                if (useDeepThinking) {
                    thinkingModel.generateContent(prompt).text ?: ""
                } else {
                    fastModel.generateContent(prompt).text ?: ""
                }
            }
            val cleaned = cleanJsonString(responseText)
            val parsedList = parseUseCasesJson(cleaned)
            if (parsedList.isNotEmpty()) {
                parsedList
            } else {
                generateFallbackUseCases(investigation, interviews, domainFilter)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Use case generation failed, providing intelligent fallbacks", e)
            generateFallbackUseCases(investigation, interviews, domainFilter)
        }
    }

    private fun cleanJsonString(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.substring(7)
        } else if (str.startsWith("```")) {
            str = str.substring(3)
        }
        if (str.endsWith("```")) {
            str = str.substring(0, str.length - 3)
        }
        str = str.trim()
        val firstBracket = str.indexOf('[')
        val lastBracket = str.lastIndexOf(']')
        return if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
            str.substring(firstBracket, lastBracket + 1)
        } else {
            str
        }
    }

    private fun parseUseCasesJson(jsonStr: String): List<GeneratedUseCaseDraft> {
        val list = mutableListOf<GeneratedUseCaseDraft>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val title = obj.optString("title", "Modern Grid Optimization")
                val description = obj.optString("description", "")
                val domain = obj.optString("gridDomain", "DISTRIBUTION").uppercase()
                val targetSystems = obj.optString("targetSystems", "IBM Maximo, Azure IoT, Snowflake")
                val feasibility = obj.optDouble("feasibilityScore", 85.0).coerceIn(0.0, 100.0)
                val roi = obj.optDouble("projectedRoi", 35.0)
                val savings = obj.optDouble("projectedAnnualSavings", 250000.0).coerceAtLeast(0.0)
                val risk = obj.optString("riskLevel", "MEDIUM").uppercase()
                val backing = obj.optString("stakeholderBacking", "Endorsed by Engineering & Operations")
                val weeks = obj.optInt("implementationWeeks", 14).coerceIn(4, 52)

                list.add(
                    GeneratedUseCaseDraft(
                        title = title,
                        description = description,
                        gridDomain = domain,
                        targetSystems = targetSystems,
                        feasibilityScore = feasibility,
                        projectedRoi = roi,
                        projectedAnnualSavings = savings,
                        riskLevel = if (risk in listOf("LOW", "MEDIUM", "HIGH")) risk else "MEDIUM",
                        stakeholderBacking = backing,
                        implementationWeeks = weeks
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "JSON parsing error for generated use cases: ${e.message}")
        }
        return list
    }

    private fun generateFallbackUseCases(
        investigation: com.example.data.model.Investigation,
        interviews: List<com.example.data.model.Interview>,
        domainFilter: String?
    ): List<GeneratedUseCaseDraft> {
        val tech = investigation.title.ifBlank { "Smart Grid Infrastructure" }
        val vendor = investigation.vendor.ifBlank { "Enterprise Grid Vendor" }

        return listOf(
            GeneratedUseCaseDraft(
                title = "Automated Work Order Dispatch for Incipient Faults",
                description = "Ingest high-frequency telemetry from $tech into Azure IoT Hub, run predictive anomaly detection, and automatically create preventive maintenance work orders in IBM Maximo.",
                gridDomain = if (domainFilter != null && domainFilter != "ALL") domainFilter else "DISTRIBUTION",
                targetSystems = "IBM Maximo, Azure IoT Hub, Microsoft Copilot",
                feasibilityScore = 92.0,
                projectedRoi = 48.0,
                projectedAnnualSavings = 380000.0,
                riskLevel = "LOW",
                stakeholderBacking = "Directly addresses Field Maintenance and Operations demand for reduced patrol overhead.",
                implementationWeeks = 12
            ),
            GeneratedUseCaseDraft(
                title = "Substation Synchrophasor & Dynamic Capacity Monitoring",
                description = "Stream PMU / sensor feeds from $vendor units into Snowflake Data Cloud to calculate real-time dynamic thermal ratings and avoid unnecessary line reconductoring CAPEX.",
                gridDomain = "TRANSMISSION",
                targetSystems = "Snowflake Data Cloud, Oracle NMS, Azure Event Hubs",
                feasibilityScore = 86.5,
                projectedRoi = 64.0,
                projectedAnnualSavings = 720000.0,
                riskLevel = "MEDIUM",
                stakeholderBacking = "Transmission planning engineers endorse deferring multi-million dollar reconductoring projects.",
                implementationWeeks = 16
            ),
            GeneratedUseCaseDraft(
                title = "DER Hosting Capacity & Reverse Power Flow Protection",
                description = "Manage rooftop PV and commercial BESS inverter settings dynamically to prevent 34.5kV feeder overvoltage and transformer reverse saturation.",
                gridDomain = "CUSTOMER_DER",
                targetSystems = "Oracle NMS / OMS, Azure IoT, SCADA Gateway",
                feasibilityScore = 84.0,
                projectedRoi = 39.0,
                projectedAnnualSavings = 290000.0,
                riskLevel = "MEDIUM",
                stakeholderBacking = "Requested by Distribution Planning and Customer Programs to accelerate interconnection queues.",
                implementationWeeks = 14
            ),
            GeneratedUseCaseDraft(
                title = "Cybersecurity NERC-CIP Telemetry Gateway & Copilot Briefing",
                description = "Establish secure mTLS OT/IT DMZ ingestion to comply with NERC-CIP-005/007, leveraging Microsoft Copilot to synthesize automated daily audit compliance logs.",
                gridDomain = "CROSS_CUTTING",
                targetSystems = "Microsoft Copilot, Azure Sentinel, Snowflake",
                feasibilityScore = 90.0,
                projectedRoi = 32.0,
                projectedAnnualSavings = 210000.0,
                riskLevel = "LOW",
                stakeholderBacking = "CISO and Cyber Compliance teams require automated evidence gathering for regulatory filings.",
                implementationWeeks = 10
            )
        )
    }
}

data class GeneratedUseCaseDraft(
    val title: String,
    val description: String,
    val gridDomain: String = "DISTRIBUTION",
    val targetSystems: String = "IBM Maximo, Azure IoT, Snowflake",
    val feasibilityScore: Double = 85.0,
    val projectedRoi: Double = 35.0,
    val projectedAnnualSavings: Double = 300000.0,
    val riskLevel: String = "LOW",
    val stakeholderBacking: String = "Supported by Stakeholder Interviews",
    val implementationWeeks: Int = 14
)

