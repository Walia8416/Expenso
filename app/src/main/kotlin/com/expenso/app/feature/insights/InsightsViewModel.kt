package com.expenso.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.core.data.db.dao.CategoryTotal
import com.expenso.app.core.data.db.dao.DailyCategoryTotal
import com.expenso.app.core.data.db.dao.DailyTotal
import com.expenso.app.core.data.db.dao.LifestyleTotal
import com.expenso.app.core.data.db.dao.MerchantTotal
import com.expenso.app.core.data.db.dao.PaymentMethodTotal
import com.expenso.app.core.data.db.dao.SourceTotal
import com.expenso.app.core.data.repository.CategoryRepository
import com.expenso.app.core.data.repository.ExpenseRepository
import com.expenso.app.core.data.repository.IncomeRepository
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.model.Expense
import com.expenso.app.core.domain.model.Income
import com.expenso.app.core.domain.model.LifestyleGroup
import com.expenso.app.core.domain.model.PaymentMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.max
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class InsightsPeriod(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
}

enum class CompareChartMode { PAIRED_BARS, OVERLAY }

data class CategorySlice(
    val category: Category,
    val totalMinor: Long,
    val count: Int,
    val fraction: Float,
)

data class LifestyleSlice(
    val group: LifestyleGroup,
    val totalMinor: Long,
    val count: Int,
    val fraction: Float,
)

data class MethodSlice(
    val method: PaymentMethod,
    val totalMinor: Long,
    val count: Int,
    val fraction: Float,
)

data class IncomeSourceSlice(
    val source: String,
    val totalMinor: Long,
    val count: Int,
    val fraction: Float,
)

data class DailyPoint(
    val date: LocalDate,
    val totalMinor: Long,
    val count: Int,
)

data class StackedDayPoint(
    val date: LocalDate,
    val totalMinor: Long,
    val segments: List<Segment>,
) {
    data class Segment(
        val categoryId: String,
        val amountMinor: Long,
    )
}

enum class InsightMilestoneType { PEAK, SPIKE, BIGGEST_EXPENSE, INCOME_DAY }

data class InsightMilestone(
    val date: LocalDate,
    val type: InsightMilestoneType,
    val label: String,
)

data class CompareBucket(
    val label: String,
    val leftTotalMinor: Long,
    val rightTotalMinor: Long,
)

data class VarianceContribution(
    val label: String,
    val deltaMinor: Long,
)

data class StressItem(
    val label: String,
    val score: Int,
    val hint: String,
)

data class EventExplainer(
    val title: String,
    val detail: String,
)

enum class RecommendationPriority { HIGH, MEDIUM, LOW }

data class RecommendationItem(
    val title: String,
    val insight: String,
    val action: String,
    val priority: RecommendationPriority,
)

data class InsightsUiState(
    val period: InsightsPeriod = InsightsPeriod.MONTH,
    val anchor: LocalDate = LocalDate.now(),
    val label: String = "",
    val totalMinor: Long = 0L,
    val previousTotalMinor: Long = 0L,
    val deltaPct: Float = 0f,
    val incomeTotalMinor: Long = 0L,
    val previousIncomeTotalMinor: Long = 0L,
    val incomeDeltaPct: Float = 0f,
    val netMinor: Long = 0L,
    val previousNetMinor: Long = 0L,
    val savingsRatePct: Float = 0f,
    val dailyAvgMinor: Long = 0L,
    val daysElapsed: Int = 0,
    val biggestTxnMinor: Long = 0L,
    val expenseCount: Int = 0,
    val categorySlices: List<CategorySlice> = emptyList(),
    val lifestyleSlices: List<LifestyleSlice> = emptyList(),
    val methodSlices: List<MethodSlice> = emptyList(),
    val incomeSourceSlices: List<IncomeSourceSlice> = emptyList(),
    val dailyPoints: List<DailyPoint> = emptyList(),
    val stackedPoints: List<StackedDayPoint> = emptyList(),
    val topMerchants: List<MerchantTotal> = emptyList(),
    val topCategory: CategorySlice? = null,
    val lifestyleFilter: LifestyleGroup? = null,
    val categories: List<Category> = emptyList(),
    val expensesInRange: List<Expense> = emptyList(),
    val incomeInRange: List<Income> = emptyList(),
    val trendMilestones: List<InsightMilestone> = emptyList(),
    val recommendations: List<RecommendationItem> = emptyList(),
    val compareLeftMonth: LocalDate = LocalDate.now().minusMonths(1).withDayOfMonth(1),
    val compareRightMonth: LocalDate = LocalDate.now().withDayOfMonth(1),
    val compareChartMode: CompareChartMode = CompareChartMode.PAIRED_BARS,
    val compareBuckets: List<CompareBucket> = emptyList(),
    val varianceContributions: List<VarianceContribution> = emptyList(),
    val stressItems: List<StressItem> = emptyList(),
    val eventExplainers: List<EventExplainer> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val weekFields = WeekFields.of(Locale.getDefault())

    private data class PeriodConfig(
        val period: InsightsPeriod,
        val anchor: LocalDate,
        val lifestyleFilter: LifestyleGroup?,
        val compareLeftMonth: LocalDate,
        val compareRightMonth: LocalDate,
        val compareMode: CompareChartMode,
    )

    private val _period = MutableStateFlow(
        PeriodConfig(
            period = InsightsPeriod.MONTH,
            anchor = LocalDate.now(),
            lifestyleFilter = null,
            compareLeftMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1),
            compareRightMonth = LocalDate.now().withDayOfMonth(1),
            compareMode = CompareChartMode.PAIRED_BARS,
        )
    )

    private data class GroupA(
        val total: Long,
        val prevTotal: Long,
        val catTotals: List<CategoryTotal>,
        val merchants: List<MerchantTotal>,
        val cats: List<Category>,
    )

    private data class GroupB(
        val daily: List<DailyTotal>,
        val lifestyle: List<LifestyleTotal>,
        val methods: List<PaymentMethodTotal>,
        val expenseCount: Int,
        val biggest: Long,
    )

    private data class IncomeAgg(
        val income: Long,
        val prevIncome: Long,
        val incomeSources: List<SourceTotal>,
        val incomeList: List<Income>,
    )

    private data class GroupC(
        val stacked: List<DailyCategoryTotal>,
        val incomeAgg: IncomeAgg,
        val expenses: List<Expense>,
    )

    private data class CompareGroup(
        val leftDaily: List<DailyTotal>,
        val rightDaily: List<DailyTotal>,
        val leftCategories: List<CategoryTotal>,
        val rightCategories: List<CategoryTotal>,
    )

    val state: StateFlow<InsightsUiState> = _period.flatMapLatest { cfg ->
        val bounds = bounds(cfg.period, cfg.anchor)
        val prev = previousBounds(cfg.period, cfg.anchor)
        val tzOffset = zone.rules.getOffset(java.time.Instant.now()).totalSeconds * 1000L

        val groupA = combine(
            expenseRepository.observeTotalSpentMinor(bounds.first, bounds.second),
            expenseRepository.observeTotalSpentMinor(prev.first, prev.second),
            expenseRepository.observeCategoryTotals(bounds.first, bounds.second),
            expenseRepository.observeTopMerchants(bounds.first, bounds.second, 5),
            categoryRepository.observeAll(),
        ) { total, prevTotal, catTotals, merchants, cats ->
            GroupA(total, prevTotal, catTotals, merchants, cats)
        }

        val groupB = combine(
            expenseRepository.observeDailyTotals(bounds.first, bounds.second, tzOffset),
            expenseRepository.observeLifestyleTotals(bounds.first, bounds.second),
            expenseRepository.observePaymentMethodTotals(bounds.first, bounds.second),
            expenseRepository.observeExpenseCount(bounds.first, bounds.second),
            expenseRepository.observeBiggestTxn(bounds.first, bounds.second),
        ) { daily, lifestyle, methods, count, biggest ->
            GroupB(daily, lifestyle, methods, count, biggest)
        }

        val incomeAggFlow = combine(
            incomeRepository.observeTotalInRange(bounds.first, bounds.second),
            incomeRepository.observeTotalInRange(prev.first, prev.second),
            incomeRepository.observeSourceTotals(bounds.first, bounds.second),
            incomeRepository.observeInRange(bounds.first, bounds.second),
        ) { income, prevIncome, sources, list ->
            IncomeAgg(income, prevIncome, sources, list)
        }

        val groupC = combine(
            expenseRepository.observeDailyCategoryTotals(bounds.first, bounds.second, tzOffset),
            incomeAggFlow,
            expenseRepository.observeInRange(bounds.first, bounds.second),
        ) { stacked, incomeAgg, expenses ->
            GroupC(stacked, incomeAgg, expenses)
        }

        val compareLeftBounds = bounds(InsightsPeriod.MONTH, cfg.compareLeftMonth)
        val compareRightBounds = bounds(InsightsPeriod.MONTH, cfg.compareRightMonth)
        val compareGroup = combine(
            expenseRepository.observeDailyTotals(compareLeftBounds.first, compareLeftBounds.second, tzOffset),
            expenseRepository.observeDailyTotals(compareRightBounds.first, compareRightBounds.second, tzOffset),
            expenseRepository.observeCategoryTotals(compareLeftBounds.first, compareLeftBounds.second),
            expenseRepository.observeCategoryTotals(compareRightBounds.first, compareRightBounds.second),
        ) { leftDaily, rightDaily, leftCats, rightCats ->
            CompareGroup(leftDaily, rightDaily, leftCats, rightCats)
        }

        combine(groupA, groupB, groupC, compareGroup) { a, b, c, cmp ->
            val daysElapsed = computeDaysElapsed(cfg.period, cfg.anchor)
            val avg = if (daysElapsed <= 0) 0L else a.total / daysElapsed

            val slices = buildCategorySlices(a.catTotals, a.cats, a.total)
            val dailyPoints = buildDailyPoints(b.daily, cfg.period, cfg.anchor, tzOffset)
            val stackedPoints = buildStackedPoints(
                c.stacked,
                a.cats,
                cfg.period,
                cfg.anchor,
                tzOffset,
                cfg.lifestyleFilter,
            )
            val lifestyleSlices = buildLifestyleSlices(b.lifestyle, a.total)
            val methodSlices = buildMethodSlices(b.methods, a.total)
            val incomeSources = buildIncomeSlices(c.incomeAgg.incomeSources, c.incomeAgg.income)
            val delta = pctDelta(a.total, a.prevTotal)
            val incomeDelta = pctDelta(c.incomeAgg.income, c.incomeAgg.prevIncome)
            val net = c.incomeAgg.income - a.total
            val prevNet = c.incomeAgg.prevIncome - a.prevTotal
            val savingsRate = if (c.incomeAgg.income <= 0L) 0f
            else ((net.toDouble() / c.incomeAgg.income.toDouble()) * 100.0).toFloat().coerceIn(-999f, 999f)
            val milestones = buildTrendMilestones(
                points = stackedPoints,
                expenses = c.expenses,
                incomes = c.incomeAgg.incomeList,
                period = cfg.period,
            )
            val recos = buildRecommendations(
                totalMinor = a.total,
                deltaPct = delta,
                topCategory = slices.firstOrNull(),
                topLifestyle = lifestyleSlices.maxByOrNull { it.totalMinor },
                savingsRatePct = savingsRate,
                netMinor = net,
                peakMilestone = milestones.firstOrNull { it.type == InsightMilestoneType.PEAK },
                compareLeftMonth = cfg.compareLeftMonth,
                compareRightMonth = cfg.compareRightMonth,
            )
            val compareBuckets = buildCompareBuckets(
                leftDaily = cmp.leftDaily,
                rightDaily = cmp.rightDaily,
                leftMonth = cfg.compareLeftMonth,
                rightMonth = cfg.compareRightMonth,
                tzOffset = tzOffset,
            )
            val variance = buildVarianceContributions(cmp.leftCategories, cmp.rightCategories, a.cats)
            val stress = buildStressItems(variance)
            val events = buildEventExplainers(compareBuckets)

            InsightsUiState(
                period = cfg.period,
                anchor = cfg.anchor,
                label = formatLabel(cfg.period, cfg.anchor),
                totalMinor = a.total,
                previousTotalMinor = a.prevTotal,
                deltaPct = delta,
                incomeTotalMinor = c.incomeAgg.income,
                previousIncomeTotalMinor = c.incomeAgg.prevIncome,
                incomeDeltaPct = incomeDelta,
                netMinor = net,
                previousNetMinor = prevNet,
                savingsRatePct = savingsRate,
                dailyAvgMinor = avg,
                daysElapsed = daysElapsed,
                biggestTxnMinor = b.biggest,
                expenseCount = b.expenseCount,
                categorySlices = slices,
                lifestyleSlices = lifestyleSlices,
                methodSlices = methodSlices,
                incomeSourceSlices = incomeSources,
                dailyPoints = dailyPoints,
                stackedPoints = stackedPoints,
                topMerchants = a.merchants,
                topCategory = slices.firstOrNull(),
                lifestyleFilter = cfg.lifestyleFilter,
                categories = a.cats,
                expensesInRange = c.expenses,
                incomeInRange = c.incomeAgg.incomeList,
                trendMilestones = milestones,
                recommendations = recos,
                compareLeftMonth = cfg.compareLeftMonth,
                compareRightMonth = cfg.compareRightMonth,
                compareChartMode = cfg.compareMode,
                compareBuckets = compareBuckets,
                varianceContributions = variance.take(6),
                stressItems = stress.take(4),
                eventExplainers = events.take(4),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    fun setPeriod(period: InsightsPeriod) {
        _period.value = _period.value.copy(period = period)
    }

    fun previous() {
        val cfg = _period.value
        _period.value = cfg.copy(anchor = shift(cfg.period, cfg.anchor, -1))
    }

    fun next() {
        val cfg = _period.value
        _period.value = cfg.copy(anchor = shift(cfg.period, cfg.anchor, +1))
    }

    fun resetToNow() {
        _period.value = _period.value.copy(anchor = LocalDate.now())
    }

    fun setLifestyleFilter(group: LifestyleGroup?) {
        _period.value = _period.value.copy(lifestyleFilter = group)
    }

    fun shiftCompareLeft(months: Long) {
        val cfg = _period.value
        _period.value = cfg.copy(compareLeftMonth = cfg.compareLeftMonth.plusMonths(months).withDayOfMonth(1))
    }

    fun shiftCompareRight(months: Long) {
        val cfg = _period.value
        _period.value = cfg.copy(compareRightMonth = cfg.compareRightMonth.plusMonths(months).withDayOfMonth(1))
    }

    fun swapCompareMonths() {
        val cfg = _period.value
        _period.value = cfg.copy(compareLeftMonth = cfg.compareRightMonth, compareRightMonth = cfg.compareLeftMonth)
    }

    fun setCompareMode(mode: CompareChartMode) {
        _period.value = _period.value.copy(compareMode = mode)
    }

    private fun pctDelta(current: Long, previous: Long): Float = when {
        previous <= 0L -> if (current > 0L) 100f else 0f
        else -> ((current - previous).toFloat() / previous.toFloat()) * 100f
    }

    private fun shift(period: InsightsPeriod, anchor: LocalDate, amount: Long): LocalDate =
        when (period) {
            InsightsPeriod.WEEK -> anchor.plusWeeks(amount)
            InsightsPeriod.MONTH -> anchor.plusMonths(amount)
            InsightsPeriod.YEAR -> anchor.plusYears(amount)
        }

    private fun bounds(period: InsightsPeriod, anchor: LocalDate): Pair<Long, Long> {
        val (startDate, endDateExclusive) = periodBounds(period, anchor)
        val s = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val e = endDateExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
        return s to e
    }

    private fun periodBounds(
        period: InsightsPeriod,
        anchor: LocalDate,
    ): Pair<LocalDate, LocalDate> = when (period) {
        InsightsPeriod.WEEK -> {
            val start = anchor.with(TemporalAdjusters.previousOrSame(weekFields.firstDayOfWeek))
            start to start.plusWeeks(1)
        }
        InsightsPeriod.MONTH -> {
            val start = anchor.withDayOfMonth(1)
            start to start.plusMonths(1)
        }
        InsightsPeriod.YEAR -> {
            val start = anchor.withDayOfYear(1)
            start to start.plusYears(1)
        }
    }

    private fun previousBounds(period: InsightsPeriod, anchor: LocalDate): Pair<Long, Long> =
        bounds(period, shift(period, anchor, -1))

    private fun computeDaysElapsed(period: InsightsPeriod, anchor: LocalDate): Int {
        val today = LocalDate.now()
        val (startDate, endDateExclusive) = periodBounds(period, anchor)
        return when {
            today.isBefore(startDate) -> 0
            !today.isBefore(endDateExclusive) ->
                (endDateExclusive.toEpochDay() - startDate.toEpochDay()).toInt()
            else -> (today.toEpochDay() - startDate.toEpochDay() + 1).toInt()
        }.coerceAtLeast(1)
    }

    private fun formatLabel(period: InsightsPeriod, anchor: LocalDate): String =
        when (period) {
            InsightsPeriod.WEEK -> {
                val start = anchor.with(TemporalAdjusters.previousOrSame(weekFields.firstDayOfWeek))
                val end = start.plusDays(6)
                val sameMonth = start.month == end.month
                val startFmt = java.time.format.DateTimeFormatter.ofPattern("MMM d")
                val endFmt = java.time.format.DateTimeFormatter.ofPattern("MMM d")
                if (sameMonth) "${start.format(startFmt)} \u2013 ${end.dayOfMonth}"
                else "${start.format(startFmt)} \u2013 ${end.format(endFmt)}"
            }
            InsightsPeriod.MONTH ->
                anchor.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
            InsightsPeriod.YEAR -> anchor.year.toString()
        }

    private fun buildCategorySlices(
        totals: List<CategoryTotal>,
        cats: List<Category>,
        sumMinor: Long,
    ): List<CategorySlice> {
        if (totals.isEmpty() || sumMinor <= 0L) return emptyList()
        val catMap = cats.associateBy { it.id }
        return totals.mapNotNull { t ->
            val c = catMap[t.categoryId] ?: return@mapNotNull null
            CategorySlice(
                category = c,
                totalMinor = t.total,
                count = t.count,
                fraction = (t.total.toDouble() / sumMinor.toDouble()).toFloat(),
            )
        }
    }

    private fun buildLifestyleSlices(
        totals: List<LifestyleTotal>,
        sumMinor: Long,
    ): List<LifestyleSlice> {
        if (totals.isEmpty() || sumMinor <= 0L) return emptyList()
        return totals.map { t ->
            LifestyleSlice(
                group = LifestyleGroup.fromName(t.lifestyleGroup),
                totalMinor = t.total,
                count = t.count,
                fraction = (t.total.toDouble() / sumMinor.toDouble()).toFloat(),
            )
        }
    }

    private fun buildMethodSlices(
        totals: List<PaymentMethodTotal>,
        sumMinor: Long,
    ): List<MethodSlice> {
        if (totals.isEmpty() || sumMinor <= 0L) return emptyList()
        return totals.map { t ->
            MethodSlice(
                method = PaymentMethod.fromName(t.paymentMethod),
                totalMinor = t.total,
                count = t.count,
                fraction = (t.total.toDouble() / sumMinor.toDouble()).toFloat(),
            )
        }
    }

    private fun buildIncomeSlices(
        totals: List<SourceTotal>,
        sumMinor: Long,
    ): List<IncomeSourceSlice> {
        if (totals.isEmpty() || sumMinor <= 0L) return emptyList()
        return totals.map { t ->
            IncomeSourceSlice(
                source = t.source,
                totalMinor = t.total,
                count = t.count,
                fraction = (t.total.toDouble() / sumMinor.toDouble()).toFloat(),
            )
        }
    }

    private fun buildDailyPoints(
        daily: List<DailyTotal>,
        period: InsightsPeriod,
        anchor: LocalDate,
        tzOffset: Long,
    ): List<DailyPoint> {
        val (startDate, endDateExclusive) = periodBounds(period, anchor)
        val dayMap = daily.associateBy { it.dayIndex }
        val points = mutableListOf<DailyPoint>()
        var cursor = startDate
        while (cursor.isBefore(endDateExclusive)) {
            val dayIndex = (cursor.atStartOfDay(zone).toInstant().toEpochMilli() + tzOffset) / 86_400_000L
            val entry = dayMap[dayIndex]
            points.add(
                DailyPoint(
                    date = cursor,
                    totalMinor = entry?.total ?: 0L,
                    count = entry?.count ?: 0,
                )
            )
            cursor = cursor.plusDays(1)
        }
        return points
    }

    private fun buildStackedPoints(
        daily: List<DailyCategoryTotal>,
        cats: List<Category>,
        period: InsightsPeriod,
        anchor: LocalDate,
        tzOffset: Long,
        lifestyleFilter: LifestyleGroup?,
    ): List<StackedDayPoint> {
        val (startDate, endDateExclusive) = periodBounds(period, anchor)
        val catMap = cats.associateBy { it.id }

        // Attach each raw daily entry to the LocalDate it represents, after
        // applying the lifestyle filter. Later we group into week/month buckets
        // so Month view shows weekly bars and Year view shows monthly bars.
        val dayToDate: (Long) -> LocalDate = { idx ->
            LocalDate.ofEpochDay(idx - (tzOffset / 86_400_000L))
        }
        val flattened = daily
            .filter { e ->
                lifestyleFilter == null ||
                    catMap[e.categoryId]?.lifestyleGroup == lifestyleFilter
            }
            .map { e -> dayToDate(e.dayIndex) to e }

        val bucketOf: (LocalDate) -> LocalDate = when (period) {
            InsightsPeriod.WEEK -> { d -> d }
            InsightsPeriod.MONTH -> { d ->
                d.with(TemporalAdjusters.previousOrSame(weekFields.firstDayOfWeek))
            }
            InsightsPeriod.YEAR -> { d -> d.withDayOfMonth(1) }
        }
        val nextBucket: (LocalDate) -> LocalDate = when (period) {
            InsightsPeriod.WEEK -> { d -> d.plusDays(1) }
            InsightsPeriod.MONTH -> { d -> d.plusWeeks(1) }
            InsightsPeriod.YEAR -> { d -> d.plusMonths(1) }
        }

        val byBucket: Map<LocalDate, List<DailyCategoryTotal>> = flattened
            .groupBy({ bucketOf(it.first) }, { it.second })

        val points = mutableListOf<StackedDayPoint>()
        var cursor = bucketOf(startDate)
        while (cursor.isBefore(endDateExclusive)) {
            val entries = byBucket[cursor].orEmpty()
            val segments = entries
                .groupBy { it.categoryId }
                .map { (catId, rows) -> catId to rows.sumOf { it.total } }
                .sortedByDescending { it.second }
                .map { (catId, total) ->
                    StackedDayPoint.Segment(categoryId = catId, amountMinor = total)
                }
            val total = segments.sumOf { it.amountMinor }
            points.add(
                StackedDayPoint(
                    date = cursor,
                    totalMinor = total,
                    segments = segments,
                )
            )
            cursor = nextBucket(cursor)
        }
        return points
    }

    private fun bucketForDate(period: InsightsPeriod, date: LocalDate): LocalDate = when (period) {
        InsightsPeriod.WEEK -> date
        InsightsPeriod.MONTH -> date.with(TemporalAdjusters.previousOrSame(weekFields.firstDayOfWeek))
        InsightsPeriod.YEAR -> date.withDayOfMonth(1)
    }

    private fun median(values: List<Long>): Long {
        if (values.isEmpty()) return 0L
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2 else sorted[mid]
    }

    private fun buildTrendMilestones(
        points: List<StackedDayPoint>,
        expenses: List<Expense>,
        incomes: List<Income>,
        period: InsightsPeriod,
    ): List<InsightMilestone> {
        if (points.isEmpty()) return emptyList()
        val out = mutableListOf<InsightMilestone>()
        val peak = points.maxByOrNull { it.totalMinor }?.takeIf { it.totalMinor > 0L }
        if (peak != null) {
            out += InsightMilestone(peak.date, InsightMilestoneType.PEAK, "Peak spend")
        }
        val nonZero = points.map { it.totalMinor }.filter { it > 0L }
        val med = median(nonZero)
        if (med > 0L) {
            val threshold = max((med * 18) / 10, med + 1)
            points.filter { it.totalMinor >= threshold }
                .take(3)
                .forEach { out += InsightMilestone(it.date, InsightMilestoneType.SPIKE, "Spike") }
        }
        val biggestExpense = expenses.maxByOrNull { it.amountMinor }
        if (biggestExpense != null) {
            val d = java.time.Instant.ofEpochMilli(biggestExpense.createdAt).atZone(zone).toLocalDate()
            out += InsightMilestone(bucketForDate(period, d), InsightMilestoneType.BIGGEST_EXPENSE, "Biggest txn")
        }
        incomes.sortedByDescending { it.amountMinor }.take(2).forEach { income ->
            val d = java.time.Instant.ofEpochMilli(income.recordedAt).atZone(zone).toLocalDate()
            out += InsightMilestone(bucketForDate(period, d), InsightMilestoneType.INCOME_DAY, "Income")
        }
        return out.distinctBy { it.date to it.type }
    }

    private fun buildRecommendations(
        totalMinor: Long,
        deltaPct: Float,
        topCategory: CategorySlice?,
        topLifestyle: LifestyleSlice?,
        savingsRatePct: Float,
        netMinor: Long,
        peakMilestone: InsightMilestone?,
        compareLeftMonth: LocalDate,
        compareRightMonth: LocalDate,
    ): List<RecommendationItem> {
        val lines = mutableListOf<RecommendationItem>()
        lines += when {
            deltaPct > 5f -> RecommendationItem(
                title = "Spend acceleration",
                insight = "Spending is up ${"%.0f".format(deltaPct)}% vs previous period.",
                action = "Audit top variable categories for this month.",
                priority = RecommendationPriority.HIGH,
            )
            deltaPct < -5f -> RecommendationItem(
                title = "Spend improvement",
                insight = "Spending is down ${"%.0f".format(-deltaPct)}% vs previous period.",
                action = "Lock this pattern as your monthly baseline.",
                priority = RecommendationPriority.LOW,
            )
            else -> RecommendationItem(
                title = "Stable spending",
                insight = "Spending is stable vs previous period.",
                action = "Look for optimizations in top category concentration.",
                priority = RecommendationPriority.MEDIUM,
            )
        }
        if (topCategory != null && totalMinor > 0L) {
            val pct = ((topCategory.totalMinor.toDouble() / totalMinor.toDouble()) * 100).toInt()
            lines += RecommendationItem(
                title = "Category concentration",
                insight = "${topCategory.category.emoji} ${topCategory.category.name} contributes $pct% of spend.",
                action = "Set a soft cap for this category in ${compareRightMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }}.",
                priority = if (pct >= 35) RecommendationPriority.HIGH else RecommendationPriority.MEDIUM,
            )
        }
        if (topLifestyle != null && totalMinor > 0L) {
            val pct = ((topLifestyle.totalMinor.toDouble() / totalMinor.toDouble()) * 100).toInt()
            lines += RecommendationItem(
                title = "Lifestyle skew",
                insight = "${topLifestyle.group.displayName} makes up $pct% of this period's expenses.",
                action = "Rebalance discretionary spend across weeks.",
                priority = if (pct >= 45) RecommendationPriority.HIGH else RecommendationPriority.MEDIUM,
            )
        }
        if (netMinor != 0L) {
            lines += if (netMinor >= 0L) RecommendationItem(
                title = "Savings health",
                insight = "Net is positive with ${"%.0f".format(savingsRatePct)}% savings.",
                action = "Move part of surplus to a fixed monthly investment bucket.",
                priority = RecommendationPriority.LOW,
            )
            else RecommendationItem(
                title = "Net deficit alert",
                insight = "Net is negative in the current period.",
                action = "Cut top discretionary spend categories this week.",
                priority = RecommendationPriority.HIGH,
            )
        }
        if (peakMilestone != null) {
            lines += RecommendationItem(
                title = "Spike detected",
                insight = "Busiest spend window was around ${peakMilestone.date}.",
                action = "Compare ${compareLeftMonth.month.name.lowercase()} vs ${compareRightMonth.month.name.lowercase()} buckets in Compare chart.",
                priority = RecommendationPriority.MEDIUM,
            )
        }
        return lines.take(4)
    }

    private fun buildCompareBuckets(
        leftDaily: List<DailyTotal>,
        rightDaily: List<DailyTotal>,
        leftMonth: LocalDate,
        rightMonth: LocalDate,
        tzOffset: Long,
    ): List<CompareBucket> {
        val left = buildDailyPoints(leftDaily, InsightsPeriod.MONTH, leftMonth, tzOffset)
        val right = buildDailyPoints(rightDaily, InsightsPeriod.MONTH, rightMonth, tzOffset)
        val leftWeekly = left.groupBy { it.date.with(TemporalAdjusters.previousOrSame(weekFields.firstDayOfWeek)) }
            .toSortedMap()
            .values.map { rows -> rows.sumOf { it.totalMinor } }
        val rightWeekly = right.groupBy { it.date.with(TemporalAdjusters.previousOrSame(weekFields.firstDayOfWeek)) }
            .toSortedMap()
            .values.map { rows -> rows.sumOf { it.totalMinor } }
        val n = maxOf(leftWeekly.size, rightWeekly.size)
        return (0 until n).map { i ->
            CompareBucket(
                label = "W${i + 1}",
                leftTotalMinor = leftWeekly.getOrElse(i) { 0L },
                rightTotalMinor = rightWeekly.getOrElse(i) { 0L },
            )
        }
    }

    private fun buildVarianceContributions(
        left: List<CategoryTotal>,
        right: List<CategoryTotal>,
        categories: List<Category>,
    ): List<VarianceContribution> {
        val leftMap = left.associate { it.categoryId to it.total }
        val rightMap = right.associate { it.categoryId to it.total }
        val catMap = categories.associateBy { it.id }
        return (leftMap.keys + rightMap.keys).toSet()
            .map { id ->
                val delta = (rightMap[id] ?: 0L) - (leftMap[id] ?: 0L)
                val label = catMap[id]?.let { "${it.emoji} ${it.name}" } ?: id
                VarianceContribution(label, delta)
            }
            .sortedByDescending { kotlin.math.abs(it.deltaMinor) }
    }

    private fun buildStressItems(variance: List<VarianceContribution>): List<StressItem> {
        return variance.take(6).map { v ->
            val abs = kotlin.math.abs(v.deltaMinor)
            val score = when {
                abs > 200_000L -> 90
                abs > 100_000L -> 70
                abs > 50_000L -> 50
                else -> 30
            }
            val hint = if (v.deltaMinor > 0) "Rising spend pressure" else "Improving trend"
            StressItem(v.label, score, hint)
        }
    }

    private fun buildEventExplainers(compareBuckets: List<CompareBucket>): List<EventExplainer> {
        val deltas = compareBuckets.map { it.rightTotalMinor - it.leftTotalMinor }
        val med = median(deltas.map { kotlin.math.abs(it) })
        if (med <= 0L) return emptyList()
        return compareBuckets.mapNotNull { b ->
            val d = b.rightTotalMinor - b.leftTotalMinor
            if (kotlin.math.abs(d) >= med * 2) {
                val trend = if (d > 0) "up" else "down"
                EventExplainer(
                    title = "${b.label} moved $trend sharply",
                    detail = "Week delta vs comparison month: ₹${kotlin.math.abs(d) / 100}.",
                )
            } else null
        }
    }

    @Suppress("unused")
    private fun startOfWeek(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
}
