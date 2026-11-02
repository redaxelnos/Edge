package com.example.util

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Investigation
import com.example.data.model.InvestigationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReportPdfGeneratorTest {

    private lateinit var context: Context
    private lateinit var generator: ReportPdfGenerator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        generator = ReportPdfGenerator(context)
    }

    @Test
    fun testReportOptions_defaultConfiguration() {
        val options = ReportOptions()
        assertTrue("Executive summary should be enabled by default", options.includeExecutiveSummary)
        assertTrue("Interviews should be enabled by default", options.includeInterviews)
        assertTrue("Use cases should be enabled by default", options.includeUseCases)
        assertTrue("Pilot KPIs should be enabled by default", options.includePilotKpis)
        assertTrue("System integrations should be enabled by default", options.includeSystemIntegrations)
        assertTrue("Change management should be enabled by default", options.includeChangeManagement)
        assertEquals("Regional Electric Utility", options.utilityName)
        assertEquals("Grid Modernization Engineering Team", options.preparedBy)
    }

    @Test
    fun testCreateViewIntent_configuresPdfMimeTypeAndFlags() {
        val testUri = Uri.parse("content://com.aistudio.gridpilot.qzrnke.fileprovider/reports/test.pdf")
        val viewIntent = generator.createViewIntent(testUri)

        assertEquals("application/pdf", viewIntent.type)
        assertEquals(testUri, viewIntent.data)
        assertTrue("Must grant read URI permission", (viewIntent.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
    }

    @Test
    fun testCreateShareIntent_configuresStreamAndSubject() {
        val testUri = Uri.parse("content://com.aistudio.gridpilot.qzrnke.fileprovider/reports/grid_report.pdf")
        val title = "Substation Dynamic Line Rating"
        val chooserIntent = generator.createShareIntent(testUri, title)

        assertNotNull(chooserIntent)
        assertEquals(android.content.Intent.ACTION_CHOOSER, chooserIntent.action)
    }
}
