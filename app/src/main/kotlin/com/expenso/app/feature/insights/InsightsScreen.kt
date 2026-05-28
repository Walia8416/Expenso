package com.expenso.app.feature.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Expand
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.R
import com.expenso.app.core.data.db.dao.MerchantTotal
import com.expenso.app.core.ui.components.LottieLoop
import com.expenso.app.core.ui.components.formatInrCompact
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun InsightsScreen(vm: InsightsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var visibleIndex by remember { mutableStateOf(0) }
    var dayDrilldown by remember { mutableStateOf<LocalDate?>(null) }
    var categoryDrilldown by remember { mutableStateOf<CategorySlice?>(null) }
    var trendExpanded by rememberSaveable { mutableStateOf(false) }
    var lifestyleExpanded by rememberSaveable { mutableStateOf(false) }
    var methodsExpanded by rememberSaveable { mutableStateOf(false) }
    var incomeExpanded by rememberSaveable { mutableStateOf(false) }
    var merchantsExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.period, state.anchor) {
        visibleIndex = 0
        for (i in 1..9) {
            delay(60)
            visibleIndex = i
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HeroCard(
                    total = state.totalMinor,
                    label = state.label,
                    deltaPct = state.deltaPct,
                    period = state.period,
                )
            }

            item {
                PeriodSwitcher(
                    period = state.period,
                    label = state.label,
                    onPeriodChange = vm::setPeriod,
                    onPrev = vm::previous,
                    onNext = vm::next,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            item {
                StaggeredIn(visible = visibleIndex >= 1) {
                    BalanceCard(
                        incomeMinor = state.incomeTotalMinor,
                        spentMinor = state.totalMinor,
                        netMinor = state.netMinor,
                        savingsRatePct = state.savingsRatePct,
                        incomeDeltaPct = state.incomeDeltaPct,
                        spentDeltaPct = state.deltaPct,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            item {
                StaggeredIn(visible = visibleIndex >= 1) {
                    CompareHeader(
                        left = state.compareLeftMonth,
                        right = state.compareRightMonth,
                        mode = state.compareChartMode,
                        onLeftPrev = { vm.shiftCompareLeft(-1) },
                        onLeftNext = { vm.shiftCompareLeft(1) },
                        onRightPrev = { vm.shiftCompareRight(-1) },
                        onRightNext = { vm.shiftCompareRight(1) },
                        onSwap = vm::swapCompareMonths,
                        onMode = vm::setCompareMode,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (state.compareBuckets.isNotEmpty()) {
                item {
                    MonthCompareChart(
                        buckets = state.compareBuckets,
                        mode = state.compareChartMode,
                        leftLabel = state.compareLeftMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                        rightLabel = state.compareRightMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            item {
                StaggeredIn(visible = visibleIndex >= 2) {
                    KpiRow(state = state)
                }
            }

            item {
                StaggeredIn(visible = visibleIndex >= 3) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionTitle(
                                when (state.period) {
                                    InsightsPeriod.WEEK -> "Daily trend"
                                    InsightsPeriod.MONTH -> "Weekly trend"
                                    InsightsPeriod.YEAR -> "Monthly trend"
                                }
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { trendExpanded = true }) {
                                Icon(Icons.Rounded.Expand, contentDescription = "Expand trend")
                            }
                            val lf = state.lifestyleFilter
                            if (lf != null) {
                                Text(
                                    lf.displayName,
                                    modifier = Modifier
                                        .background(
                                            lifestyleColor(lf).copy(alpha = 0.18f),
                                            RoundedCornerShape(100),
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = lifestyleColor(lf),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        LifestyleFilterChips(
                            selected = state.lifestyleFilter,
                            onSelect = vm::setLifestyleFilter,
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(20.dp),
                                )
                                .padding(14.dp),
                        ) {
                            StackedBarChart(
                                points = state.stackedPoints,
                                categories = state.categories,
                                lifestyleFilter = state.lifestyleFilter,
                                milestones = state.trendMilestones,
                                onDayTap = { date -> dayDrilldown = date },
                                period = state.period,
                            )
                        }
                    }
                }
            }

            if (state.recommendations.isNotEmpty()) {
                item {
                    StaggeredIn(visible = visibleIndex >= 4) {
                        InsightsRecommendationsCard(
                            recommendations = state.recommendations,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }

            if (state.varianceContributions.isNotEmpty()) {
                item {
                    AnalysisCard(
                        title = "Variance decomposition",
                        lines = state.varianceContributions.map {
                            "${it.label}: ${if (it.deltaMinor >= 0) "+" else "-"}${formatInrCompact(kotlin.math.abs(it.deltaMinor))}"
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (state.stressItems.isNotEmpty()) {
                item {
                    AnalysisCard(
                        title = "Budget stress radar",
                        lines = state.stressItems.map { "${it.label} • ${it.score}/100 • ${it.hint}" },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (state.eventExplainers.isNotEmpty()) {
                item {
                    AnalysisCard(
                        title = "Anomaly explainers",
                        lines = state.eventExplainers.map { "${it.title}: ${it.detail}" },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (state.categorySlices.isNotEmpty()) {
                item {
                    StaggeredIn(visible = visibleIndex >= 5) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionTitle("Category breakdown")
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(20.dp),
                                    )
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CategoryDonut(
                                    slices = state.categorySlices,
                                    totalMinor = state.totalMinor,
                                )
                            }
                        }
                    }
                }
                items(state.categorySlices.take(8), key = { "leg-" + it.category.id }) { slice ->
                    StaggeredIn(visible = true) {
                        CategoryLegendRow(
                            slice = slice,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clickable { categoryDrilldown = slice },
                        )
                    }
                }
            }

            if (state.lifestyleSlices.isNotEmpty()) {
                item {
                    StaggeredIn(visible = visibleIndex >= 6) {
                        CollapsibleSection(
                            title = "Lifestyle buckets",
                            expanded = lifestyleExpanded,
                            onToggle = { lifestyleExpanded = !lifestyleExpanded },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            LifestyleRings(slices = state.lifestyleSlices)
                        }
                    }
                }
            }

            if (state.methodSlices.isNotEmpty()) {
                item {
                    StaggeredIn(visible = visibleIndex >= 7) {
                        CollapsibleSection(
                            title = "Where the money went",
                            expanded = methodsExpanded,
                            onToggle = { methodsExpanded = !methodsExpanded },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            MethodBar(slices = state.methodSlices, total = state.totalMinor)
                        }
                    }
                }
            }

            if (state.incomeSourceSlices.isNotEmpty()) {
                item {
                    StaggeredIn(visible = visibleIndex >= 8) {
                        CollapsibleSection(
                            title = "Income sources",
                            expanded = incomeExpanded,
                            onToggle = { incomeExpanded = !incomeExpanded },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            IncomeSourceList(slices = state.incomeSourceSlices)
                        }
                    }
                }
            }

            if (state.topMerchants.isNotEmpty()) {
                item {
                    StaggeredIn(visible = visibleIndex >= 9) {
                        CollapsibleSection(
                            title = "Top merchants",
                            expanded = merchantsExpanded,
                            onToggle = { merchantsExpanded = !merchantsExpanded },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.topMerchants.forEach { m ->
                                    MerchantRow(m)
                                }
                            }
                        }
                    }
                }
            }

            if (state.totalMinor == 0L && state.incomeTotalMinor == 0L) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        LottieLoop(
                            res = R.raw.insights_empty,
                            modifier = Modifier.size(200.dp),
                        )
                        Text(
                            "No activity yet in this period",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Log an expense or add income to see beautiful insights.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }

        val day = dayDrilldown
        if (day != null) {
            DayBreakdownSheet(
                date = day,
                period = state.period,
                expenses = state.expensesInRange,
                onDismiss = { dayDrilldown = null },
                onOpenExpense = { /* handled elsewhere */ },
            )
        }

        val catSlice = categoryDrilldown
        if (catSlice != null) {
            CategoryDrilldownSheet(
                slice = catSlice,
                expensesInCategory = state.expensesInRange.filter {
                    it.category.id == catSlice.category.id
                },
                onDismiss = { categoryDrilldown = null },
                onOpenExpense = { /* handled elsewhere */ },
            )
        }
        if (trendExpanded) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { trendExpanded = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(0.9f),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("Expanded trend")
                            Spacer(Modifier.weight(1f))
                            Text(
                                state.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        StackedBarChart(
                            points = state.stackedPoints,
                            categories = state.categories,
                            lifestyleFilter = state.lifestyleFilter,
                            milestones = state.trendMilestones,
                            onDayTap = { date -> dayDrilldown = date },
                            period = state.period,
                            expanded = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsRecommendationsCard(recommendations: List<RecommendationItem>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        SectionTitle("Insights for you")
        Spacer(Modifier.height(8.dp))
        recommendations.forEach { rec ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
            ) {
                Text(
                    "${rec.priority.name} • ${rec.title}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(rec.insight, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Action: ${rec.action}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CompareHeader(
    left: LocalDate,
    right: LocalDate,
    mode: CompareChartMode,
    onLeftPrev: () -> Unit,
    onLeftNext: () -> Unit,
    onRightPrev: () -> Unit,
    onRightNext: () -> Unit,
    onSwap: () -> Unit,
    onMode: (CompareChartMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fmt = DateTimeFormatter.ofPattern("MMM yyyy")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        SectionTitle("Compare two months")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLeftPrev) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous left month") }
            Text(left.format(fmt), modifier = Modifier.weight(1f))
            IconButton(onClick = onLeftNext) { Icon(Icons.Rounded.ChevronRight, contentDescription = "Next left month") }
            IconButton(onClick = onSwap) { Icon(Icons.Rounded.SwapHoriz, contentDescription = "Swap months") }
            IconButton(onClick = onRightPrev) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous right month") }
            Text(right.format(fmt), modifier = Modifier.weight(1f))
            IconButton(onClick = onRightNext) { Icon(Icons.Rounded.ChevronRight, contentDescription = "Next right month") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Paired bars", selected = mode == CompareChartMode.PAIRED_BARS) { onMode(CompareChartMode.PAIRED_BARS) }
            Chip("Overlay", selected = mode == CompareChartMode.OVERLAY) { onMode(CompareChartMode.OVERLAY) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(100),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun AnalysisCard(title: String, lines: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        SectionTitle(title)
        Spacer(Modifier.height(6.dp))
        lines.take(5).forEach { line ->
            Text("• $line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(title)
            Spacer(Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun HeroCard(
    total: Long,
    label: String,
    deltaPct: Float,
    period: InsightsPeriod,
) {
    val brush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
        )
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush)
            .padding(horizontal = 20.dp, vertical = 28.dp),
    ) {
        Text(
            "Spent this ${period.label.lowercase()}",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.9f),
        )
        Spacer(Modifier.height(6.dp))
        com.expenso.app.core.ui.components.AnimatedRupee(
            amountMinor = total,
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
            color = Color.White,
            compact = total >= 100_000_00L,
        )
        Spacer(Modifier.height(6.dp))
        val deltaText = when {
            deltaPct > 1f -> "\u25B2 ${"%.0f".format(deltaPct)}% vs previous"
            deltaPct < -1f -> "\u25BC ${"%.0f".format(-deltaPct)}% vs previous"
            else -> "About the same as previous"
        }
        Text(
            deltaText,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(100))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun KpiRow(state: InsightsUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard(
            title = "Daily avg",
            value = formatInrCompact(state.dailyAvgMinor),
            subtitle = "${state.daysElapsed} days",
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            title = "Transactions",
            value = state.expenseCount.toString(),
            subtitle = if (state.topCategory != null)
                "Top: ${state.topCategory.category.emoji} ${state.topCategory.category.name}"
            else null,
            accent = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            title = "Biggest",
            value = formatInrCompact(state.biggestTxnMinor),
            subtitle = "single txn",
            accent = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MethodBar(slices: List<MethodSlice>, total: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(100),
                    ),
            ) {
                slices.forEach { slice ->
                    Box(
                        modifier = Modifier
                            .weight(slice.fraction.coerceAtLeast(0.02f))
                            .fillMaxSize()
                            .background(methodColor(slice.method.name)),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            slices.forEach { slice ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(methodColor(slice.method.name), CircleShape),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        slice.method.displayName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        formatInrCompact(slice.totalMinor),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "${(slice.fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeSourceList(slices: List<IncomeSourceSlice>) {
    val accent = Color(0xFF14B886)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        slices.forEach { slice ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(accent, CircleShape),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    slice.source,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    formatInrCompact(slice.totalMinor),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    "${(slice.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun methodColor(name: String): Color = when (name) {
    "UPI" -> Color(0xFF6A4FE2)
    "CASH" -> Color(0xFF2DAE85)
    "CARD" -> Color(0xFFE26A4F)
    else -> Color(0xFF9A9A93)
}

@Composable
private fun SectionTitle(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun StaggeredIn(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 }),
    ) { content() }
}

@Composable
private fun CategoryLegendRow(slice: CategorySlice, modifier: Modifier = Modifier) {
    val color = runCatching {
        val h = slice.category.colorHex.removePrefix("#")
        val v = if (h.length == 6) "FF$h" else h
        Color(v.toLong(16))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            "${slice.category.emoji} ${slice.category.name}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatInrCompact(slice.totalMinor),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Text(
                "${(slice.fraction * 100).toInt()}% \u2022 ${slice.count} txn",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MerchantRow(m: MerchantTotal, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                m.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                m.vpa,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatInrCompact(m.total),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Text(
                "${m.count} txn",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
