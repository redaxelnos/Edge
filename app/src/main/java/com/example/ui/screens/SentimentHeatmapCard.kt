package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GridDomain
import com.example.data.model.Interview
import com.example.data.model.StakeholderSentiment
import com.example.ui.theme.EnergyGold
import com.example.ui.theme.GridCyan
import com.example.ui.theme.ReliabilityGreen
import com.example.ui.theme.RiskRed
import java.util.Locale

data class HeatmapCellData(
    val domain: GridDomain,
    val sentiment: StakeholderSentiment,
    val count: Int,
    val avgPriority: Double,
    val matchingInterviews: List<Interview>
)

@Composable
fun StakeholderSentimentHeatmapCard(
    interviews: List<Interview>,
    selectedDomainFilter: String?,
    selectedSentimentFilter: String?,
    onSelectCellFilter: (domain: String?, sentiment: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var inspectedCell by remember { mutableStateOf<HeatmapCellData?>(null) }

    val domains = GridDomain.entries
    val sentiments = StakeholderSentiment.entries
    val maxCellCount = remember(interviews) {
        var max = 1
        for (d in domains) {
            for (s in sentiments) {
                val c = interviews.count { it.gridDomain == d.name && it.sentiment == s.name }
                if (c > max) max = c
            }
        }
        max
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sentiment_heatmap_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Sentiment Heatmap",
                        tint = GridCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Grid Domain × Stakeholder Sentiment Heatmap",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Tap any cell to inspect or filter interviews by domain & sentiment",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                if (selectedDomainFilter != null || selectedSentimentFilter != null) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable {
                                inspectedCell = null
                                onSelectCellFilter(null, null)
                            }
                            .padding(4.dp)
                            .testTag("heatmap_reset_filter_button")
                    ) {
                        Text(
                            text = "Reset Filter",
                            style = MaterialTheme.typography.labelSmall,
                            color = GridCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(min = 440.dp)
                ) {
                    // Column Headers (Sentiments)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(108.dp))
                        sentiments.forEach { sentiment ->
                            val shortLabel = when (sentiment) {
                                StakeholderSentiment.POSITIVE -> "Supportive"
                                StakeholderSentiment.NEUTRAL -> "Objective"
                                StakeholderSentiment.CAUTIOUS -> "Cautious"
                                StakeholderSentiment.CONCERNED -> "Critical"
                            }
                            val headerColor = sentimentBaseColor(sentiment)
                            Text(
                                text = shortLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = headerColor,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "Avg",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Matrix Rows (Grid Domains)
                    domains.forEach { domain ->
                        val domainInterviews = interviews.filter { it.gridDomain == domain.name }
                        val domainAvgScore = if (domainInterviews.isNotEmpty()) {
                            domainInterviews.map {
                                runCatching { StakeholderSentiment.valueOf(it.sentiment).score }.getOrDefault(3)
                            }.average()
                        } else {
                            0.0
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Domain Row Label
                            Column(
                                modifier = Modifier
                                    .width(108.dp)
                                    .padding(end = 6.dp)
                            ) {
                                Text(
                                    text = domain.shortName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 1
                                )
                        Text(
                            text = "${domainInterviews.size} notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }

                    // Heatmap Cells across Sentiments
                    sentiments.forEach { sentiment ->
                        val matching = domainInterviews.filter { it.sentiment == sentiment.name }
                        val count = matching.size
                        val avgPrio = if (matching.isNotEmpty()) matching.map { it.priorityRating }.average() else 0.0
                        val intensity = if (count == 0) 0f else (0.28f + 0.72f * (count.toFloat() / maxCellCount.toFloat())).coerceIn(0.28f, 1f)
                        val baseColor = sentimentBaseColor(sentiment)
                        val cellBgColor = if (count == 0) {
                            Color(0xFF1E293B).copy(alpha = 0.45f)
                        } else {
                            baseColor.copy(alpha = intensity)
                        }

                        val isSelected = selectedDomainFilter == domain.name && selectedSentimentFilter == sentiment.name

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .padding(horizontal = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellBgColor)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.White else baseColor.copy(alpha = if (count > 0) 0.5f else 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    val cellData = HeatmapCellData(
                                        domain = domain,
                                        sentiment = sentiment,
                                        count = count,
                                        avgPriority = avgPrio,
                                        matchingInterviews = matching
                                    )
                                    inspectedCell = cellData
                                    if (isSelected) {
                                        onSelectCellFilter(null, null)
                                    } else {
                                        onSelectCellFilter(domain.name, sentiment.name)
                                    }
                                }
                                .testTag("heatmap_cell_${domain.code}_${sentiment.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (count > 0) Color.White else Color(0xFF64748B)
                                )
                                if (count > 0) {
                                    Text(
                                        text = "P${String.format(Locale.US, "%.1f", avgPrio)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }

                    // Domain Sentiment Score Pill (1.0 - 4.0 scale)
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(48.dp)
                            .padding(start = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (domainInterviews.isNotEmpty()) {
                            val scoreColor = when {
                                domainAvgScore >= 3.3 -> ReliabilityGreen
                                domainAvgScore >= 2.5 -> GridCyan
                                domainAvgScore >= 1.8 -> EnergyGold
                                else -> RiskRed
                            }
                            Surface(
                                color = scoreColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", domainAvgScore),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = scoreColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "—",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Color Scale Legend Canvas Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sentiment Gradient:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                ) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                RiskRed,
                                EnergyGold,
                                GridCyan,
                                ReliabilityGreen
                            )
                        ),
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Critical (1.0) → Supportive (4.0)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }

            // Selected Cell Details Tooltip Banner
            inspectedCell?.let { cell ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("heatmap_cell_detail_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(sentimentBaseColor(cell.sentiment), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${cell.domain.label} • ${cell.sentiment.label}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            if (cell.count > 0) {
                                val names = cell.matchingInterviews.joinToString(", ") {
                                    it.stakeholderName.ifBlank { it.stakeholderRole.ifBlank { "Stakeholder" } }
                                }
                                Text(
                                    text = "${cell.count} interview(s) | Avg Priority: ${String.format(Locale.US, "%.1f", cell.avgPriority)}/5 | Stakeholders: $names",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            } else {
                                Text(
                                    text = "No interviews captured in this domain/sentiment cell yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun sentimentBaseColor(sentiment: StakeholderSentiment): Color {
    return when (sentiment) {
        StakeholderSentiment.POSITIVE -> ReliabilityGreen
        StakeholderSentiment.NEUTRAL -> GridCyan
        StakeholderSentiment.CAUTIOUS -> EnergyGold
        StakeholderSentiment.CONCERNED -> RiskRed
    }
}
