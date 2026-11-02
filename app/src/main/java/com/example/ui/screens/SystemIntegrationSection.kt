package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Investigation
import com.example.data.model.SystemIntegrationStatus
import com.example.ui.theme.GridAmber
import com.example.ui.theme.GridCyan
import com.example.ui.theme.GridEmerald
import com.example.ui.theme.GridGreen
import com.example.ui.theme.GridNavy
import com.example.ui.theme.GridOrange

data class EnterpriseConnectorSpec(
    val key: String,
    val displayName: String,
    val protocol: String,
    val description: String,
    val currentStatus: String
)

@Composable
fun SystemIntegrationSection(
    investigation: Investigation?,
    onUpdateSystemStatus: (systemName: String, newStatus: SystemIntegrationStatus) -> Unit,
    onUpdateInvestigation: (Investigation) -> Unit = {}
) {
    if (investigation == null) {
        EmptySelectionPrompt("Select an investigation in the Projects tab to configure enterprise utility system integrations.")
        return
    }

    val connectors = remember(investigation) {
        listOf(
            EnterpriseConnectorSpec(
                key = "Maximo",
                displayName = "IBM Maximo EAM & Work Management",
                protocol = "REST API / OSLC / IEC 61968",
                description = "Automated substation asset health work orders, transformer maintenance schedules, and field crew dispatch.",
                currentStatus = investigation.maximoIntegrationStatus
            ),
            EnterpriseConnectorSpec(
                key = "Azure",
                displayName = "Azure IoT Hub & Digital Twins",
                protocol = "MQTT / AMQP / DNP3 Edge Gateway",
                description = "Real-time edge telemetry ingestion, feeder digital twin state synchronization, and cloud analytics.",
                currentStatus = investigation.azureIntegrationStatus
            ),
            EnterpriseConnectorSpec(
                key = "Oracle",
                displayName = "Oracle Utilities NMS / ADMS / MDM",
                protocol = "CIM XML / SOAP / Kafka Event Bus",
                description = "Network Management System outage switching, meter data management, and customer connectivity topology.",
                currentStatus = investigation.oracleIntegrationStatus
            ),
            EnterpriseConnectorSpec(
                key = "Snowflake",
                displayName = "Snowflake Grid Data Lakehouse",
                protocol = "Snowpipe Streaming / JDBC",
                description = "High-frequency AMI interval data, synchrophasor PMU archives, and historical SAIDI/SAIFI training datasets.",
                currentStatus = investigation.snowflakeIntegrationStatus
            ),
            EnterpriseConnectorSpec(
                key = "Copilot",
                displayName = "Executive Copilot & Regulatory Reporting",
                protocol = "Graph API / Secure Webhook",
                description = "Automated PUC regulatory compliance packets, board reliability briefings, and audit logs.",
                currentStatus = investigation.copilotReportingStatus
            )
        )
    }

    val verifiedOrIntegratedCount = connectors.count {
        it.currentStatus == SystemIntegrationStatus.VERIFIED.name ||
            it.currentStatus == SystemIntegrationStatus.INTEGRATED.name
    }
    val plannedCount = connectors.count {
        it.currentStatus == SystemIntegrationStatus.PLANNED.name
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GridNavy)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "IT / OT ENTERPRISE ARCHITECTURE & TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = GridCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "System Integration Readiness: ${investigation.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select the integration maturity stage for each core utility platform (Not Evaluated → Planned → API Verified → Fully Integrated) or mark all verified for pilot cutover.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = "Verified / Integrated",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                                Text(
                                    text = "$verifiedOrIntegratedCount / ${connectors.size} Systems",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GridEmerald
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = "Architecture Planned",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                                Text(
                                    text = "$plannedCount Systems",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GridAmber
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                onUpdateInvestigation(
                                    investigation.copy(
                                        maximoIntegrationStatus = SystemIntegrationStatus.INTEGRATED.name,
                                        azureIntegrationStatus = SystemIntegrationStatus.INTEGRATED.name,
                                        oracleIntegrationStatus = SystemIntegrationStatus.VERIFIED.name,
                                        snowflakeIntegrationStatus = SystemIntegrationStatus.VERIFIED.name,
                                        copilotReportingStatus = SystemIntegrationStatus.INTEGRATED.name
                                    )
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GridCyan),
                            border = BorderStroke(1.dp, GridCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("verify_all_integrations_btn")
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark All Systems Verified for Pilot", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        items(connectors, key = { it.key }) { connector ->
            val statusEnum = runCatching { SystemIntegrationStatus.valueOf(connector.currentStatus) }
                .getOrDefault(SystemIntegrationStatus.NOT_EVALUATED)
            val badgeColor = when (statusEnum) {
                SystemIntegrationStatus.INTEGRATED -> GridGreen
                SystemIntegrationStatus.VERIFIED -> GridCyan
                SystemIntegrationStatus.PLANNED -> GridAmber
                SystemIntegrationStatus.NOT_EVALUATED -> GridOrange
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("system_card_${connector.key}"),
                shape = RoundedCornerShape(16.dp),
                border = if (statusEnum == SystemIntegrationStatus.INTEGRATED || statusEnum == SystemIntegrationStatus.VERIFIED) {
                    BorderStroke(1.5.dp, badgeColor.copy(alpha = 0.7f))
                } else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = badgeColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (statusEnum) {
                                            SystemIntegrationStatus.INTEGRATED -> Icons.Default.CheckCircle
                                            SystemIntegrationStatus.VERIFIED -> Icons.Default.Verified
                                            SystemIntegrationStatus.PLANNED -> Icons.Default.Architecture
                                            SystemIntegrationStatus.NOT_EVALUATED -> Icons.Default.Hub
                                        },
                                        contentDescription = null,
                                        tint = badgeColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = connector.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Protocol: ${connector.protocol}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = statusEnum.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = connector.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SystemIntegrationStatus.entries.forEach { st ->
                            FilterChip(
                                selected = statusEnum == st,
                                onClick = { onUpdateSystemStatus(connector.key, st) },
                                label = { Text(st.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("status_${connector.key}_${st.name}")
                            )
                        }
                    }
                }
            }
        }
    }
}
