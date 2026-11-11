package com.guanjiazhushou.assistant.presentation.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.model.RoomListFilter
import com.guanjiazhushou.assistant.domain.model.StatisticsScope
import com.guanjiazhushou.assistant.presentation.common.MetricCard
import com.guanjiazhushou.assistant.presentation.common.formatPercent
import com.guanjiazhushou.assistant.presentation.common.formatRatioPercent
import com.guanjiazhushou.assistant.presentation.common.livingStatusColor
import com.guanjiazhushou.assistant.presentation.common.livingStatusColorMap
import com.guanjiazhushou.assistant.ui.theme.AppThemeExtras
import com.guanjiazhushou.assistant.ui.theme.SemanticColors
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.RowChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.Pie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onDrillDown: (RoomListFilter, StatisticsScope) -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AppThemeExtras.palette
    val snapshot = uiState.snapshot
    val statusColorMap = remember(uiState.livingStatusSettings, palette.id) {
        palette.livingStatusColorMap(uiState.livingStatusSettings)
    }
    val statisticsCardColors = CardDefaults.cardColors(containerColor = palette.surface)
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("数据统计") })
        },
    ) { padding ->
        if (uiState.isLoading || snapshot == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    StatisticsScope.entries.forEachIndexed { index, scope ->
                        SegmentedButton(
                            selected = uiState.scope == scope,
                            onClick = { viewModel.setScope(scope) },
                            shape = SegmentedButtonDefaults.itemShape(index, StatisticsScope.entries.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = palette.primary,
                                activeContentColor = palette.surface,
                                inactiveContainerColor = palette.surface,
                                inactiveContentColor = palette.textSecondary,
                                activeBorderColor = palette.primary,
                                inactiveBorderColor = palette.surfaceVariant,
                            ),
                        ) {
                            Text(scope.label)
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        MetricCard(
                            title = RoomListFilter.TOTAL.title,
                            value = snapshot.totalRooms.toString(),
                            onClick = { onDrillDown(RoomListFilter.TOTAL, uiState.scope) },
                        )
                    }
                    item {
                        MetricCard(
                            title = "出租率",
                            value = formatPercent(snapshot.rentalRate),
                            onClick = { onDrillDown(RoomListFilter.RENTED, uiState.scope) },
                        )
                    }
                    item {
                        MetricCard(
                            title = RoomListFilter.ARREARS.title,
                            value = snapshot.arrearsCount.toString(),
                            onClick = { onDrillDown(RoomListFilter.ARREARS, uiState.scope) },
                        )
                    }
                    item {
                        MetricCard(
                            title = "入住率",
                            value = formatPercent(snapshot.occupancyRate),
                            onClick = { onDrillDown(RoomListFilter.OCCUPIED, uiState.scope) },
                        )
                    }
                    item {
                        MetricCard(
                            title = "收缴率",
                            value = formatPercent(snapshot.collectionRate),
                            onClick = { onDrillDown(RoomListFilter.COLLECTED, uiState.scope) },
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = statisticsCardColors,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "居住状态分布",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary,
                        )
                        val totalLiving = snapshot.livingStatusDistribution.sumOf { it.count }.coerceAtLeast(1)
                        snapshot.livingStatusDistribution.forEach { item ->
                            val color = statusColorMap.livingStatusColor(item.statusKey)
                            val percent = item.count.toFloat() / totalLiving
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = item.label,
                                    modifier = Modifier.weight(0.35f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary,
                                )
                                LinearProgressIndicator(
                                    progress = { percent },
                                    modifier = Modifier
                                        .weight(0.45f)
                                        .height(8.dp),
                                    color = color,
                                    trackColor = palette.collectionProgressTrack,
                                    strokeCap = StrokeCap.Round,
                                )
                                Text(
                                    text = formatRatioPercent(percent),
                                    modifier = Modifier
                                        .weight(0.2f)
                                        .padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip,
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = statisticsCardColors,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "缴费分布",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        )
                        val pieData = snapshot.paymentTypeDistribution
                            .filter { it.count > 0 }
                            .map { item ->
                                val color = when (item.type) {
                                    PaymentType.ARREARS -> SemanticColors.Error
                                    PaymentType.PREPAID -> SemanticColors.Success
                                    PaymentType.NORMAL -> SemanticColors.Neutral
                                }
                                Pie(
                                    label = item.type.label,
                                    data = item.count.toDouble(),
                                    color = color,
                                    selectedColor = color,
                                    style = Pie.Style.Stroke(width = 36.dp),
                                )
                            }
                        if (pieData.isNotEmpty()) {
                            val pieStrokeWidth = 36.dp
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(modifier = Modifier.size(200.dp)) {
                                    val strokePx = pieStrokeWidth.toPx()
                                    val radius = (size.minDimension / 2f) - strokePx / 2f
                                    drawCircle(
                                        color = palette.collectionProgressFill,
                                        radius = radius,
                                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                                    )
                                }
                                PieChart(
                                    modifier = Modifier.size(200.dp),
                                    data = pieData,
                                    style = Pie.Style.Stroke(width = pieStrokeWidth),
                                    spaceDegree = 2f,
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = snapshot.totalRooms.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary,
                                    )
                                    Text(
                                        text = "总户数",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary,
                                    )
                                }
                            }
                            pieData.forEach { slice ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = slice.label.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textPrimary,
                                    )
                                    Text(
                                        text = "${slice.data.toInt()} 户",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary,
                                    )
                                }
                            }
                        } else {
                            Text(text = "暂无缴费数据", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = statisticsCardColors,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "特征画像",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary,
                        )
                        val barData = snapshot.featureTagProfile
                            .filter { it.count > 0 }
                            .take(5)
                            .mapIndexed { index, item ->
                                Bars(
                                    label = item.name,
                                    values = listOf(
                                        Bars.Data(
                                            label = item.name,
                                            value = item.count.toDouble(),
                                            color = SolidColor(
                                                palette.chartColors[index % palette.chartColors.size],
                                            ),
                                        ),
                                    ),
                                )
                            }
                        if (barData.isNotEmpty()) {
                            RowChart(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                data = barData,
                                barProperties = BarProperties(
                                    spacing = 4.dp,
                                    thickness = 16.dp,
                                ),
                            )
                        } else {
                            Text(text = "暂无特征数据", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
