package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Interview
import com.example.data.model.Investigation
import com.example.data.model.PilotKpi
import com.example.data.model.UseCase
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportOptions(
    val includeExecutiveSummary: Boolean = true,
    val includeInterviews: Boolean = true,
    val includeUseCases: Boolean = true,
    val includePilotKpis: Boolean = true,
    val includeSystemIntegrations: Boolean = true,
    val includeChangeManagement: Boolean = true,
    val executiveNotes: String = "",
    val preparedBy: String = "Grid Modernization Engineering Team",
    val utilityName: String = "Regional Electric Utility"
)

data class GeneratedReport(
    val file: File,
    val uri: Uri,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val generatedDate: String
)

class ReportPdfGenerator(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595 // A4 standard width in points
        private const val PAGE_HEIGHT = 842 // A4 standard height in points
        private const val MARGIN_LEFT = 36f
        private const val MARGIN_RIGHT = 559f
        private const val CONTENT_WIDTH = MARGIN_RIGHT - MARGIN_LEFT // 523f
        private const val FOOTER_Y = 812f
        private const val MAX_CONTENT_Y = 790f

        // Brand colors
        private val COLOR_PRIMARY_DARK = Color.rgb(10, 25, 47) // #0A192F
        private val COLOR_CYAN = Color.rgb(0, 180, 216) // #00B4D8
        private val COLOR_GOLD = Color.rgb(245, 158, 11) // #F59E0B
        private val COLOR_EMERALD = Color.rgb(16, 185, 129) // #10B981
        private val COLOR_RED = Color.rgb(239, 68, 68) // #EF4444
        private val COLOR_SLATE_BG = Color.rgb(241, 245, 249) // #F1F5F9
        private val COLOR_CARD_BORDER = Color.rgb(203, 213, 225) // #CBD5E1
        private val COLOR_TEXT_PRIMARY = Color.rgb(15, 23, 42) // #0F172A
        private val COLOR_TEXT_MUTED = Color.rgb(100, 116, 139) // #64748B
    }

    fun generateReport(
        investigation: Investigation,
        interviews: List<Interview>,
        useCases: List<UseCase>,
        pilotKpis: List<PilotKpi>,
        options: ReportOptions
    ): GeneratedReport {
        val document = PdfDocument()
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val displayDate = SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date())
        val sanitizedTitle = investigation.title.replace(Regex("[^a-zA-Z0-9]"), "_").take(24)
        val reportFile = File(reportsDir, "GridPilot_Report_${sanitizedTitle}_$timeStamp.pdf")

        val writer = DocumentWriter(document, displayDate, investigation.title)

        // 1. Cover / Executive Title Banner
        writer.drawCoverHeader(investigation, options)

        // 2. Executive Summary & Core Financials
        if (options.includeExecutiveSummary) {
            writer.drawExecutiveSummarySection(investigation, options)
        }

        // 3. System Integration Architecture Matrix
        if (options.includeSystemIntegrations) {
            writer.drawSystemIntegrationsSection(investigation)
        }

        // 4. Stakeholder Interviews & Domain Feedback
        if (options.includeInterviews) {
            writer.drawInterviewsSection(interviews)
        }

        // 5. Synthesized Grid Technology Use Cases
        if (options.includeUseCases) {
            writer.drawUseCasesSection(useCases)
        }

        // 6. Active Pilot Performance Telemetry & KPIs
        if (options.includePilotKpis) {
            writer.drawPilotKpisSection(investigation, pilotKpis)
        }

        // 7. Change Management, ADKAR & Formal Sign-off
        if (options.includeChangeManagement) {
            writer.drawChangeManagementSection(investigation)
        }

        val totalPages = writer.finishDocument()

        FileOutputStream(reportFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, reportFile)

        return GeneratedReport(
            file = reportFile,
            uri = contentUri,
            pageCount = totalPages,
            fileSizeBytes = reportFile.length(),
            generatedDate = displayDate
        )
    }

    /**
     * Creates an Intent to view the PDF in an external PDF reader.
     */
    fun createViewIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates an Intent chooser to share the PDF with stakeholders.
     */
    fun createShareIntent(uri: Uri, subject: String): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "Attached is the GridPilot AI executive stakeholder evaluation report for: $subject")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Share Stakeholder PDF Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private inner class DocumentWriter(
        private val document: PdfDocument,
        private val dateStr: String,
        private val docTitle: String
    ) {
        private var currentPageNumber = 0
        private var currentPage: PdfDocument.Page? = null
        private var currentCanvas: Canvas? = null
        var currentY: Float = 0f

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            startNewPage()
        }

        fun startNewPage() {
            currentPage?.let {
                drawPageFooter(currentCanvas!!, currentPageNumber)
                document.finishPage(it)
            }
            currentPageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
            currentPage = document.startPage(pageInfo)
            currentCanvas = currentPage!!.canvas

            if (currentPageNumber > 1) {
                drawRunningHeader(currentCanvas!!)
                currentY = 48f
            } else {
                currentY = 0f
            }
        }

        fun ensureSpace(neededHeight: Float) {
            if (currentY + neededHeight > MAX_CONTENT_Y) {
                startNewPage()
            }
        }

        fun finishDocument(): Int {
            currentPage?.let {
                drawPageFooter(currentCanvas!!, currentPageNumber)
                document.finishPage(it)
            }
            return currentPageNumber
        }

        private fun drawRunningHeader(canvas: Canvas) {
            textPaint.color = COLOR_TEXT_MUTED
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            canvas.drawText("GRIDPILOT AI | STAKEHOLDER STRATEGIC REPORT: ${docTitle.take(40)}", MARGIN_LEFT, 28f, textPaint)

            strokePaint.color = COLOR_CARD_BORDER
            strokePaint.strokeWidth = 0.5f
            canvas.drawLine(MARGIN_LEFT, 34f, MARGIN_RIGHT, 34f, strokePaint)
        }

        private fun drawPageFooter(canvas: Canvas, pageNum: Int) {
            strokePaint.color = COLOR_CARD_BORDER
            strokePaint.strokeWidth = 0.5f
            canvas.drawLine(MARGIN_LEFT, FOOTER_Y - 8f, MARGIN_RIGHT, FOOTER_Y - 8f, strokePaint)

            textPaint.color = COLOR_TEXT_MUTED
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            canvas.drawText("CONFIDENTIAL - ELECTRIC UTILITY GRID INFRASTRUCTURE EVALUATION", MARGIN_LEFT, FOOTER_Y + 4f, textPaint)

            val pageStr = "Page $pageNum"
            val width = textPaint.measureText(pageStr)
            canvas.drawText(pageStr, MARGIN_RIGHT - width, FOOTER_Y + 4f, textPaint)
        }

        fun drawCoverHeader(investigation: Investigation, options: ReportOptions) {
            val canvas = currentCanvas!!
            // Top hero banner
            fillPaint.color = COLOR_PRIMARY_DARK
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 130f, fillPaint)

            // Cyan accent bar
            fillPaint.color = COLOR_CYAN
            canvas.drawRect(0f, 130f, PAGE_WIDTH.toFloat(), 134f, fillPaint)

            // Header badge
            fillPaint.color = COLOR_GOLD
            val badgeRect = RectF(MARGIN_LEFT, 20f, MARGIN_LEFT + 155f, 36f)
            canvas.drawRoundRect(badgeRect, 4f, 4f, fillPaint)
            textPaint.color = COLOR_PRIMARY_DARK
            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            canvas.drawText("UTILITY GRID MODERNIZATION", MARGIN_LEFT + 8f, 31.5f, textPaint)

            // Title
            textPaint.color = Color.WHITE
            textPaint.textSize = 18f
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            val title = if (investigation.title.isNotBlank()) investigation.title else "Grid Technology Investigation"
            canvas.drawText(title.take(45), MARGIN_LEFT, 62f, textPaint)

            // Subtitle
            textPaint.color = Color.rgb(203, 213, 225)
            textPaint.textSize = 9.5f
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            canvas.drawText(
                "Vendor: ${investigation.vendor.ifEmpty { "Unspecified" }}  |  Category: ${investigation.technologyType.ifEmpty { "Grid Tech" }}  |  Status: ${investigation.status}",
                MARGIN_LEFT,
                80f,
                textPaint
            )

            // Metadata row
            textPaint.color = Color.rgb(148, 163, 184)
            textPaint.textSize = 8.5f
            canvas.drawText(
                "Prepared by: ${options.preparedBy}  •  ${options.utilityName}  •  Date: $dateStr",
                MARGIN_LEFT,
                98f,
                textPaint
            )

            currentY = 150f
        }

        fun drawExecutiveSummarySection(investigation: Investigation, options: ReportOptions) {
            ensureSpace(140f)
            val canvas = currentCanvas!!

            drawSectionHeader("1. Executive Summary & Financial Feasibility")

            // Stat Cards Row
            val cardCount = 4
            val cardSpacing = 8f
            val cardWidth = (CONTENT_WIDTH - (cardSpacing * (cardCount - 1))) / cardCount
            val cardHeight = 48f

            val stats = listOf(
                Pair("CAPEX ESTIMATE", "$${String.format(Locale.US, "%,.0f", investigation.capexEstimate)}"),
                Pair("OPEX / YEAR", "$${String.format(Locale.US, "%,.0f", investigation.opexEstimate)}"),
                Pair("PROJECTED ROI", "${String.format(Locale.US, "%.1f", investigation.projectedRoi)}%"),
                Pair("PAYBACK PERIOD", "${String.format(Locale.US, "%.1f", investigation.paybackYears)} yrs")
            )

            stats.forEachIndexed { index, pair ->
                val cardLeft = MARGIN_LEFT + (index * (cardWidth + cardSpacing))
                val rect = RectF(cardLeft, currentY, cardLeft + cardWidth, currentY + cardHeight)

                fillPaint.color = COLOR_SLATE_BG
                canvas.drawRoundRect(rect, 4f, 4f, fillPaint)
                strokePaint.color = COLOR_CARD_BORDER
                strokePaint.strokeWidth = 0.8f
                canvas.drawRoundRect(rect, 4f, 4f, strokePaint)

                textPaint.color = COLOR_TEXT_MUTED
                textPaint.textSize = 7f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText(pair.first, cardLeft + 6f, currentY + 16f, textPaint)

                textPaint.color = COLOR_PRIMARY_DARK
                textPaint.textSize = 12f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText(pair.second, cardLeft + 6f, currentY + 36f, textPaint)
            }

            currentY += cardHeight + 14f

            // Description / Strategic Notes
            if (investigation.description.isNotBlank()) {
                ensureSpace(40f)
                textPaint.color = COLOR_TEXT_PRIMARY
                textPaint.textSize = 9f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                currentY = drawWrappedText(
                    "Scope Overview: ${investigation.description}",
                    MARGIN_LEFT,
                    CONTENT_WIDTH,
                    textPaint,
                    13f
                ) + 8f
            }

            // Custom executive notes if provided
            if (options.executiveNotes.isNotBlank()) {
                ensureSpace(40f)
                val noteRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 36f)
                fillPaint.color = Color.rgb(238, 242, 255)
                canvas.drawRoundRect(noteRect, 4f, 4f, fillPaint)
                strokePaint.color = Color.rgb(199, 210, 254)
                canvas.drawRoundRect(noteRect, 4f, 4f, strokePaint)

                textPaint.color = Color.rgb(67, 56, 202)
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText("EXECUTIVE REMARKS", MARGIN_LEFT + 8f, currentY + 14f, textPaint)

                textPaint.color = COLOR_TEXT_PRIMARY
                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                currentY = drawWrappedText(options.executiveNotes, MARGIN_LEFT + 8f, CONTENT_WIDTH - 16f, textPaint, 12f, startY = currentY + 26f) + 12f
            }
        }

        fun drawSystemIntegrationsSection(investigation: Investigation) {
            ensureSpace(70f)
            val canvas = currentCanvas!!
            drawSectionHeader("2. Legacy Grid Systems & Cloud Integration Matrix")

            val systems = listOf(
                Pair("Maximo (EAM)", investigation.maximoIntegrationStatus),
                Pair("Azure Cloud", investigation.azureIntegrationStatus),
                Pair("Oracle ERP", investigation.oracleIntegrationStatus),
                Pair("Snowflake", investigation.snowflakeIntegrationStatus),
                Pair("MS Copilot", investigation.copilotReportingStatus)
            )

            val itemWidth = CONTENT_WIDTH / systems.size
            val cardHeight = 36f

            systems.forEachIndexed { index, pair ->
                val left = MARGIN_LEFT + (index * itemWidth)
                val rect = RectF(left + 2f, currentY, left + itemWidth - 2f, currentY + cardHeight)

                val (badgeColor, statusColor) = when (pair.second) {
                    "INTEGRATED", "VERIFIED" -> Pair(Color.rgb(209, 250, 229), COLOR_EMERALD)
                    "PLANNED" -> Pair(Color.rgb(254, 243, 199), COLOR_GOLD)
                    else -> Pair(COLOR_SLATE_BG, COLOR_TEXT_MUTED)
                }

                fillPaint.color = badgeColor
                canvas.drawRoundRect(rect, 4f, 4f, fillPaint)
                strokePaint.color = COLOR_CARD_BORDER
                canvas.drawRoundRect(rect, 4f, 4f, strokePaint)

                textPaint.color = COLOR_TEXT_PRIMARY
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText(pair.first, left + 6f, currentY + 14f, textPaint)

                textPaint.color = statusColor
                textPaint.textSize = 7f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText(pair.second.replace("_", " "), left + 6f, currentY + 28f, textPaint)
            }

            currentY += cardHeight + 16f
        }

        fun drawInterviewsSection(interviews: List<Interview>) {
            ensureSpace(80f)
            val canvas = currentCanvas!!
            drawSectionHeader("3. Captured Stakeholder Interviews & Domain Synthesis (${interviews.size} Stakeholders)")

            if (interviews.isEmpty()) {
                textPaint.color = COLOR_TEXT_MUTED
                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                canvas.drawText("No interview sessions have been logged for this investigation yet.", MARGIN_LEFT, currentY + 12f, textPaint)
                currentY += 24f
                return
            }

            interviews.forEachIndexed { idx, interview ->
                ensureSpace(90f)

                val cardTop = currentY
                val left = MARGIN_LEFT
                val width = CONTENT_WIDTH

                // Draw Interview Card Box background
                fillPaint.color = COLOR_SLATE_BG
                val approxHeight = 70f
                val cardRect = RectF(left, cardTop, left + width, cardTop + approxHeight)
                canvas.drawRoundRect(cardRect, 4f, 4f, fillPaint)
                strokePaint.color = COLOR_CARD_BORDER
                strokePaint.strokeWidth = 0.8f
                canvas.drawRoundRect(cardRect, 4f, 4f, strokePaint)

                // Stakeholder Name and Role
                textPaint.color = COLOR_PRIMARY_DARK
                textPaint.textSize = 9.5f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                val nameRole = "${idx + 1}. ${interview.stakeholderName.ifEmpty { "Anonymous Stakeholder" }} — ${interview.stakeholderRole.ifEmpty { "Operator / Engineer" }} (${interview.department.ifEmpty { "Operations" }})"
                canvas.drawText(nameRole.take(55), left + 8f, cardTop + 16f, textPaint)

                // Domain & Sentiment badges
                textPaint.textSize = 7.5f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                val domainStr = "[${interview.gridDomain}]"
                textPaint.color = COLOR_CYAN
                canvas.drawText(domainStr, left + width - 150f, cardTop + 16f, textPaint)

                val sentimentColor = when {
                    interview.sentiment.contains("POSITIVE") -> COLOR_EMERALD
                    interview.sentiment.contains("CONCERNED") -> COLOR_RED
                    interview.sentiment.contains("CAUTIOUS") -> COLOR_GOLD
                    else -> COLOR_TEXT_MUTED
                }
                textPaint.color = sentimentColor
                canvas.drawText(interview.sentiment.take(18), left + width - 85f, cardTop + 16f, textPaint)

                // Notes / Constraints
                var textY = cardTop + 30f
                textPaint.color = COLOR_TEXT_PRIMARY
                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                if (interview.notes.isNotBlank()) {
                    textY = drawWrappedText("Notes: ${interview.notes}", left + 8f, width - 16f, textPaint, 11f, startY = textY)
                }

                if (interview.keyRequirements.isNotBlank()) {
                    textPaint.color = Color.rgb(30, 41, 59)
                    textY = drawWrappedText("Technical Reqs: ${interview.keyRequirements}", left + 8f, width - 16f, textPaint, 11f, startY = textY + 2f)
                }

                currentY = textY + 14f
            }
        }

        fun drawUseCasesSection(useCases: List<UseCase>) {
            ensureSpace(80f)
            val canvas = currentCanvas!!
            drawSectionHeader("4. Prioritized Grid Modernization Use Cases (${useCases.size} Adopted)")

            if (useCases.isEmpty()) {
                textPaint.color = COLOR_TEXT_MUTED
                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                canvas.drawText("No use cases have been generated or adopted for this technology yet.", MARGIN_LEFT, currentY + 12f, textPaint)
                currentY += 24f
                return
            }

            useCases.forEachIndexed { idx, uc ->
                ensureSpace(80f)
                val cardTop = currentY
                val left = MARGIN_LEFT
                val width = CONTENT_WIDTH

                fillPaint.color = Color.WHITE
                val cardRect = RectF(left, cardTop, left + width, cardTop + 62f)
                canvas.drawRoundRect(cardRect, 4f, 4f, fillPaint)
                strokePaint.color = COLOR_CARD_BORDER
                strokePaint.strokeWidth = 0.8f
                canvas.drawRoundRect(cardRect, 4f, 4f, strokePaint)

                // Title
                textPaint.color = COLOR_PRIMARY_DARK
                textPaint.textSize = 9.5f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText("${idx + 1}. ${uc.title.take(50)}", left + 8f, cardTop + 16f, textPaint)

                // Metrics pill: Feasibility %, ROI %, Savings
                textPaint.color = COLOR_EMERALD
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                val metricStr = "Feasibility: ${uc.feasibilityScore.toInt()}% | ROI: ${uc.projectedRoi.toInt()}% | Savings: $${String.format(Locale.US, "%,.0f", uc.projectedAnnualSavings)}/yr"
                canvas.drawText(metricStr, left + 8f, cardTop + 30f, textPaint)

                // Description & Target Systems
                textPaint.color = COLOR_TEXT_MUTED
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                var descY = cardTop + 44f
                val desc = uc.description.take(130) + if (uc.description.length > 130) "..." else ""
                descY = drawWrappedText(desc, left + 8f, width - 16f, textPaint, 10f, startY = descY)

                currentY = descY + 12f
            }
        }

        fun drawPilotKpisSection(investigation: Investigation, pilotKpis: List<PilotKpi>) {
            ensureSpace(90f)
            val canvas = currentCanvas!!
            drawSectionHeader("5. Active Pilot Telemetry & Success KPI Benchmarks")

            // Pilot Overview Box
            val pilotBoxRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 32f)
            fillPaint.color = COLOR_SLATE_BG
            canvas.drawRoundRect(pilotBoxRect, 4f, 4f, fillPaint)
            strokePaint.color = COLOR_CARD_BORDER
            canvas.drawRoundRect(pilotBoxRect, 4f, 4f, strokePaint)

            textPaint.color = COLOR_TEXT_PRIMARY
            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            val siteInfo = "Pilot Test Site: ${investigation.pilotFeederSubstation.ifEmpty { "Field Substation Beta" }} | Duration: ${investigation.pilotDurationWeeks} Weeks | Success Rate: ${String.format(Locale.US, "%.1f", investigation.pilotSuccessRate)}%"
            canvas.drawText(siteInfo, MARGIN_LEFT + 8f, currentY + 20f, textPaint)
            currentY += 40f

            if (pilotKpis.isEmpty()) {
                textPaint.color = COLOR_TEXT_MUTED
                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                canvas.drawText("No live pilot KPIs currently logged. System awaiting telemetry telemetry push.", MARGIN_LEFT, currentY + 12f, textPaint)
                currentY += 24f
                return
            }

            // Draw KPI Table Header
            val tableTop = currentY
            fillPaint.color = COLOR_PRIMARY_DARK
            canvas.drawRect(MARGIN_LEFT, tableTop, MARGIN_RIGHT, tableTop + 20f, fillPaint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            canvas.drawText("KPI METRIC", MARGIN_LEFT + 6f, tableTop + 13f, textPaint)
            canvas.drawText("CATEGORY", MARGIN_LEFT + 180f, tableTop + 13f, textPaint)
            canvas.drawText("CURRENT", MARGIN_LEFT + 280f, tableTop + 13f, textPaint)
            canvas.drawText("TARGET", MARGIN_LEFT + 360f, tableTop + 13f, textPaint)
            canvas.drawText("STATUS", MARGIN_LEFT + 440f, tableTop + 13f, textPaint)
            currentY += 20f

            // Rows
            pilotKpis.forEachIndexed { index, kpi ->
                ensureSpace(22f)
                val rowTop = currentY
                val rowHeight = 20f

                fillPaint.color = if (index % 2 == 0) Color.rgb(248, 250, 252) else Color.WHITE
                canvas.drawRect(MARGIN_LEFT, rowTop, MARGIN_RIGHT, rowTop + rowHeight, fillPaint)

                textPaint.color = COLOR_TEXT_PRIMARY
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText(kpi.metricName.take(28), MARGIN_LEFT + 6f, rowTop + 13f, textPaint)

                textPaint.color = COLOR_TEXT_MUTED
                textPaint.textSize = 7.5f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                canvas.drawText(kpi.category.replace("_", " "), MARGIN_LEFT + 180f, rowTop + 13f, textPaint)

                textPaint.color = COLOR_PRIMARY_DARK
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText("${String.format(Locale.US, "%.1f", kpi.currentValue)} ${kpi.unit}", MARGIN_LEFT + 280f, rowTop + 13f, textPaint)

                textPaint.color = COLOR_TEXT_MUTED
                textPaint.textSize = 8f
                canvas.drawText("${String.format(Locale.US, "%.1f", kpi.targetValue)} ${kpi.unit}", MARGIN_LEFT + 360f, rowTop + 13f, textPaint)

                val isAchieved = kpi.currentValue >= kpi.targetValue
                val statusText = if (isAchieved) "ACHIEVED" else "IN PROGRESS"
                val statusColor = if (isAchieved) COLOR_EMERALD else COLOR_GOLD
                textPaint.color = statusColor
                textPaint.textSize = 7.5f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText(statusText, MARGIN_LEFT + 440f, rowTop + 13f, textPaint)

                strokePaint.color = COLOR_CARD_BORDER
                strokePaint.strokeWidth = 0.5f
                canvas.drawLine(MARGIN_LEFT, rowTop + rowHeight, MARGIN_RIGHT, rowTop + rowHeight, strokePaint)

                currentY += rowHeight
            }

            currentY += 14f
        }

        fun drawChangeManagementSection(investigation: Investigation) {
            ensureSpace(120f)
            val canvas = currentCanvas!!
            drawSectionHeader("6. Change Management, ADKAR Readiness & Formal Sign-off")

            // ADKAR summary card
            val rect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 38f)
            fillPaint.color = COLOR_SLATE_BG
            canvas.drawRoundRect(rect, 4f, 4f, fillPaint)
            strokePaint.color = COLOR_CARD_BORDER
            canvas.drawRoundRect(rect, 4f, 4f, strokePaint)

            textPaint.color = COLOR_TEXT_PRIMARY
            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            val adkarInfo = "Organizational ADKAR Readiness Score: ${String.format(Locale.US, "%.1f", investigation.changeManagementScore)}/100  |  Recommendation: PROCEED TO ECOSYSTEM ADOPTION"
            canvas.drawText(adkarInfo, MARGIN_LEFT + 8f, currentY + 22f, textPaint)
            currentY += 48f

            // Stakeholder Signature blocks
            val sigWidth = (CONTENT_WIDTH - 24f) / 3
            val sigTitles = listOf(
                "VP of Electric Grid Operations",
                "Chief SCADA & IT Systems Architect",
                "Director of Asset Management"
            )

            sigTitles.forEachIndexed { index, title ->
                val left = MARGIN_LEFT + (index * (sigWidth + 12f))
                strokePaint.color = COLOR_PRIMARY_DARK
                strokePaint.strokeWidth = 1f
                canvas.drawLine(left, currentY + 28f, left + sigWidth, currentY + 28f, strokePaint)

                textPaint.color = COLOR_TEXT_MUTED
                textPaint.textSize = 7f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                canvas.drawText("Signature / Date", left, currentY + 38f, textPaint)

                textPaint.color = COLOR_TEXT_PRIMARY
                textPaint.textSize = 7.5f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText(title, left, currentY + 48f, textPaint)
            }

            currentY += 60f
        }

        private fun drawSectionHeader(title: String) {
            val canvas = currentCanvas!!
            fillPaint.color = COLOR_CYAN
            canvas.drawRect(MARGIN_LEFT, currentY, MARGIN_LEFT + 4f, currentY + 14f, fillPaint)

            textPaint.color = COLOR_PRIMARY_DARK
            textPaint.textSize = 10.5f
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            canvas.drawText(title, MARGIN_LEFT + 10f, currentY + 11.5f, textPaint)

            currentY += 22f
        }

        /**
         * Draws text wrapped to a given width and returns the updated Y position.
         */
        private fun drawWrappedText(
            text: String,
            x: Float,
            width: Float,
            paint: Paint,
            lineHeight: Float,
            startY: Float = currentY
        ): Float {
            val words = text.split(" ")
            var curLine = ""
            var y = startY
            val canvas = currentCanvas!!

            for (word in words) {
                val testLine = if (curLine.isEmpty()) word else "$curLine $word"
                if (paint.measureText(testLine) > width) {
                    if (curLine.isNotEmpty()) {
                        canvas.drawText(curLine, x, y, paint)
                        y += lineHeight
                    }
                    curLine = word
                } else {
                    curLine = testLine
                }
            }
            if (curLine.isNotEmpty()) {
                canvas.drawText(curLine, x, y, paint)
                y += lineHeight
            }
            return y
        }
    }
}
