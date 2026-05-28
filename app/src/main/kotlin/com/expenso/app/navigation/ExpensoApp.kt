package com.expenso.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.expenso.app.feature.history.ExpenseDetailScreen
import com.expenso.app.feature.history.HistoryScreen
import com.expenso.app.feature.insights.InsightsScreen
import com.expenso.app.feature.onboarding.OnboardingScreen
import com.expenso.app.feature.scanner.ScannerScreen
import com.expenso.app.feature.settings.CategoryManagerScreen
import com.expenso.app.feature.settings.SettingsScreen

enum class WidgetAction {
    ScanAndPay,
    LogCash,
    LogUpi,
}

@Composable
fun ExpensoApp(
    rootViewModel: RootViewModel = hiltViewModel(),
    widgetAction: WidgetAction? = null,
    onWidgetActionConsumed: () -> Unit = {},
) {
    val state by rootViewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    when (val s = state) {
        is RootState.Loading -> LoadingRoot()
        is RootState.NeedsOnboarding -> OnboardingScreen(
            onDone = { rootViewModel.completeOnboarding() }
        )
        is RootState.Ready -> MainShell(
            navController = navController,
            widgetAction = widgetAction,
            onWidgetActionConsumed = onWidgetActionConsumed,
        )
    }
}

@Composable
private fun LoadingRoot() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun MainShell(
    navController: NavHostController,
    widgetAction: WidgetAction?,
    onWidgetActionConsumed: () -> Unit,
) {
    var scannerOpenTab by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(widgetAction) {
        when (widgetAction) {
            WidgetAction.ScanAndPay -> {
                navController.navigate(Routes.SCANNER) {
                    popUpTo(Routes.SCANNER) { inclusive = true }
                    launchSingleTop = true
                }
                onWidgetActionConsumed()
            }
            WidgetAction.LogCash -> {
                scannerOpenTab = com.expenso.app.feature.expense.TAB_CASH_CARD
                navController.navigate(Routes.SCANNER) {
                    popUpTo(Routes.SCANNER) { inclusive = true }
                    launchSingleTop = true
                }
                onWidgetActionConsumed()
            }
            WidgetAction.LogUpi -> {
                // Entry point for the "Log UPI payment" shortcut / QS tile:
                // jump straight to the UPI manual-entry tab of the add sheet.
                scannerOpenTab = com.expenso.app.feature.expense.TAB_UPI_MANUAL
                navController.navigate(Routes.SCANNER) {
                    popUpTo(Routes.SCANNER) { inclusive = true }
                    launchSingleTop = true
                }
                onWidgetActionConsumed()
            }
            null -> Unit
        }
    }

    Scaffold(
        bottomBar = { ExpensoBottomBar(navController) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SCANNER,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SCANNER) {
                ScannerScreen(
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    openAddSheetTab = scannerOpenTab,
                    onAddSheetTabConsumed = { scannerOpenTab = null },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onOpenExpense = { id -> navController.navigate(Routes.expenseDetail(id)) },
                )
            }
            composable(Routes.INSIGHTS) {
                InsightsScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCategoryManager = { navController.navigate(Routes.CATEGORY_MANAGER) },
                )
            }
            composable(Routes.CATEGORY_MANAGER) {
                CategoryManagerScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.EXPENSE_DETAIL) { entry ->
                val id = entry.arguments?.getString("expenseId").orEmpty()
                ExpenseDetailScreen(
                    expenseId = id,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
