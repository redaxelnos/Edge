package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Interview
import com.example.data.model.Investigation
import com.example.data.model.PilotKpi
import com.example.data.model.UseCase
import com.example.ui.theme.EnergyGold
import com.example.ui.theme.GridCyan
import com.example.ui.theme.GridDarkNavy
import com.example.ui.theme.GridElectricBlue
import com.example.ui.theme.GridEmerald
import com.example.util.GeneratedReport
import com.example.util.ReportOptions
import com.example.util.ReportPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StakeholderReportDialog(
    investigation: Investigation?,
    interviews: List<Interview>,
    useCases: List<UseCase>,
    pilotKpis: List<PilotKpi>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pdfGenerator = remember { ReportPdfGenerator(context) }

    var utilityName by remember { mutableStateOf("Regional Electric Utility") }
    var preparedBy by remember { mutableStateOf("Grid Modernization Task Force") }
    var executiveNotes by remember { mutableStateOf("") }

    var includeExecutiveSummary by remember { mutableStateOf(true) }
    var includeInterviews by remember { mutableStateOf(true) }
    var includeUseCases by remember { mutableStateOf(true) }
    var includePilotKpis by remember { mutableStateOf(true) }
    var includeSystemIntegrations by remember { mutableStateOf(true) }
    var includeChangeManagement by remember { mutableStateOf(true) }

    var isGenerating by remember { mutableStateOf(false) }
    var generatedReport by remember { mutableStateOf<GeneratedReport?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = GridDarkNavy,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF0F172A), CircleShape)
                                .border(1.dp, GridCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = GridCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Stakeholder Summary Report",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Compile Interviews, Use Cases & Pilot Telemetry into PDF",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_report_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (investigation == null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Please select or create a grid technology investigation before compiling reports.",
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    return@Column
                }

                // Technology Scope Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = EnergyGold, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = investigation.title.ifEmpty { "Grid Technology Investigation" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Vendor: ${investigation.vendor.ifEmpty { "N/A" }}  •  Status: ${investigation.status}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GridCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Report Sections Selection
                Text(
                    text = "REPORT SECTIONS INCLUDED",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = GridCyan
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        ReportSectionCheckItem(
                            label = "Executive Financial Summary & Payback",
                            subtitle = "CapEx, OpEx, Projected ROI, and SAIDI Delta",
                            checked = includeExecutiveSummary,
                            onCheckedChange = { includeExecutiveSummary = it },
                            testTag = "checkbox_executive_summary"
                        )
                        HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 4.dp))

                        ReportSectionCheckItem(
                            label = "Stakeholder Interviews & Sentiment Matrix",
                            subtitle = "${interviews.size} logged sessions with requirements and concerns",
                            checked = includeInterviews,
                            onCheckedChange = { includeInterviews = it },
                            testTag = "checkbox_interviews"
                        )
                        HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 4.dp))

                        ReportSectionCheckItem(
                            label = "Synthesized Grid Use Cases",
                            subtitle = "${useCases.size} adopted cases with feasibility & annual savings",
                            checked = includeUseCases,
                            onCheckedChange = { includeUseCases = it },
                            testTag = "checkbox_use_cases"
                        )
                        HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 4.dp))

                        ReportSectionCheckItem(
                            label = "Active Pilot Performance & KPI Telemetry",
                            subtitle = "${pilotKpis.size} metrics tracking Target vs Current values",
                            checked = includePilotKpis,
                            onCheckedChange = { includePilotKpis = it },
                            testTag = "checkbox_pilot_kpis"
                        )
                        HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 4.dp))

                        ReportSectionCheckItem(
                            label = "Legacy System Integration Matrix",
                            subtitle = "Maximo, Azure Cloud, Oracle, Snowflake & MS Copilot readiness",
                            checked = includeSystemIntegrations,
                            onCheckedChange = { includeSystemIntegrations = it },
                            testTag = "checkbox_system_integrations"
                        )
                        HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 4.dp))

                        ReportSectionCheckItem(
                            label = "Change Management & Stakeholder Sign-Off",
                            subtitle = "ADKAR adoption score and formal signature blocks",
                            checked = includeChangeManagement,
                            onCheckedChange = { includeChangeManagement = it },
                            testTag = "checkbox_change_management"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Metadata Form
                Text(
                    text = "REPORT DOSSIER METADATA",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = GridCyan
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = utilityName,
                    onValueChange = { utilityName = it },
                    label = { Text("Utility / Organization Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GridCyan,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = GridCyan,
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_utility_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = preparedBy,
                    onValueChange = { preparedBy = it },
                    label = { Text("Prepared By / Author") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GridCyan,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = GridCyan,
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_prepared_by")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = executiveNotes,
                    onValueChange = { executiveNotes = it },
                    label = { Text("Executive Remarks / Strategic Notes (Optional)") },
                    placeholder = { Text("e.g. Recommended for Stage 2 pilot deployment pending cybersecurity sign-off.", color = Color(0xFF64748B)) },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GridCyan,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = GridCyan,
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_executive_notes")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Error message
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFFCA5A5),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Generated Report Success Block
                if (generatedReport != null) {
                    val report = generatedReport!!
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GridEmerald, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PDF Report Compiled Successfully!",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${report.file.name} • ${report.pageCount} Pages • ${(report.fileSizeBytes / 1024)} KB",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA7F3D0)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            val intent = pdfGenerator.createViewIntent(report.uri)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No PDF viewer found on device: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GridEmerald),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_view_pdf")
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open PDF", color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val intent = pdfGenerator.createShareIntent(report.uri, investigation.title)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Unable to share report: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GridCyan),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_share_pdf")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share", color = Color(0xFF0A192F), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Generate Button
                Button(
                    onClick = {
                        isGenerating = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val options = ReportOptions(
                                    includeExecutiveSummary = includeExecutiveSummary,
                                    includeInterviews = includeInterviews,
                                    includeUseCases = includeUseCases,
                                    includePilotKpis = includePilotKpis,
                                    includeSystemIntegrations = includeSystemIntegrations,
                                    includeChangeManagement = includeChangeManagement,
                                    executiveNotes = executiveNotes.trim(),
                                    preparedBy = preparedBy.trim().ifEmpty { "Grid Modernization Engineering Team" },
                                    utilityName = utilityName.trim().ifEmpty { "Regional Electric Utility" }
                                )

                                val result = withContext(Dispatchers.IO) {
                                    pdfGenerator.generateReport(
                                        investigation = investigation,
                                        interviews = interviews,
                                        useCases = useCases,
                                        pilotKpis = pilotKpis,
                                        options = options
                                    )
                                }
                                generatedReport = result
                                Toast.makeText(context, "Report generated: ${result.pageCount} pages", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                errorMessage = "Failed to generate PDF: ${e.message}"
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GridElectricBlue,
                        disabledContainerColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_generate_pdf")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compiling Grid Report...", color = Color.White)
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (generatedReport == null) "Generate Stakeholder PDF Report" else "Re-Generate PDF Report",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportSectionCheckItem(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = GridCyan,
                uncheckedColor = Color(0xFF64748B),
                checkmarkColor = Color(0xFF0A192F)
            ),
            modifier = Modifier.testTag(testTag)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
