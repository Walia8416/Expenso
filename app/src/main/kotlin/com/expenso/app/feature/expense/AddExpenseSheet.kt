package com.expenso.app.feature.expense

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.upi.UpiPaymentRequest
import com.expenso.app.feature.income.AddIncomeSheet
import com.expenso.app.feature.pay.LaunchPayload
import com.expenso.app.feature.pay.PaySheetContent
import com.expenso.app.feature.pay.PayViewModel
import com.expenso.app.feature.paycontact.PayContactFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    manualRequest: UpiPaymentRequest,
    categories: List<Category>,
    lastUsedCategoryId: String?,
    selectedTabIndex: Int,
    onSelectTab: (Int) -> Unit,
    onDismiss: () -> Unit,
    onLaunchUpi: (LaunchPayload) -> Unit,
    onLaunched: (String) -> Unit,
    onCashSaved: () -> Unit,
    onIncomeSaved: () -> Unit,
    onError: (String) -> Unit,
    payVm: PayViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val payState by payVm.state.collectAsStateWithLifecycle()

    LaunchedEffect(manualRequest, selectedTabIndex) {
        if (selectedTabIndex == TAB_UPI_MANUAL) {
            val defaultCat = lastUsedCategoryId
                ?: categories.firstOrNull { it.id != "other" }?.id
                ?: categories.firstOrNull()?.id
            payVm.bindRequest(manualRequest, defaultCat)
        }
    }

    LaunchedEffect(payVm, selectedTabIndex) {
        payVm.events.collect { ev ->
            when (ev) {
                is com.expenso.app.feature.pay.PayEvent.LaunchUpi -> {
                    onLaunchUpi(
                        LaunchPayload(
                            uri = ev.intentUri,
                            targetPackage = ev.targetPackage,
                            expenseId = ev.expenseId,
                        )
                    )
                    onLaunched(ev.expenseId)
                }
                is com.expenso.app.feature.pay.PayEvent.Error -> onError(ev.message)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 12.dp,
            ) {
                Tab(
                    selected = selectedTabIndex == TAB_UPI_MANUAL,
                    onClick = { onSelectTab(TAB_UPI_MANUAL) },
                    text = { Text("UPI") },
                )
                Tab(
                    selected = selectedTabIndex == TAB_UPI_CONTACT,
                    onClick = { onSelectTab(TAB_UPI_CONTACT) },
                    text = { Text("Pay contact") },
                )
                Tab(
                    selected = selectedTabIndex == TAB_CASH_CARD,
                    onClick = { onSelectTab(TAB_CASH_CARD) },
                    text = { Text("Cash / Card") },
                )
                Tab(
                    selected = selectedTabIndex == TAB_INCOME,
                    onClick = { onSelectTab(TAB_INCOME) },
                    text = { Text("Income") },
                )
            }
            Spacer(Modifier.height(4.dp))
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tabSwap",
            ) { idx ->
                when (idx) {
                    TAB_UPI_MANUAL -> Box(modifier = Modifier.padding(top = 4.dp)) {
                        PaySheetContent(
                            state = payState,
                            categories = categories,
                            onVpaChange = payVm::setVpa,
                            onAmountChange = payVm::setAmount,
                            onNoteChange = payVm::setNote,
                            onCategorySelect = payVm::selectCategory,
                            onChangeUpiApp = payVm::chooseUpiApp,
                            onPayClicked = payVm::onPayClicked,
                        )
                    }
                    TAB_UPI_CONTACT -> Box(modifier = Modifier.padding(top = 4.dp)) {
                        PayContactFlow(
                            categories = categories,
                            lastUsedCategoryId = lastUsedCategoryId,
                            onLaunchUpi = onLaunchUpi,
                            onLaunched = onLaunched,
                            onError = onError,
                        )
                    }
                    TAB_CASH_CARD -> Box(modifier = Modifier.padding(top = 4.dp)) {
                        LogExpenseSheet(
                            categories = categories,
                            lastUsedCategoryId = lastUsedCategoryId,
                            onSaved = onCashSaved,
                        )
                    }
                    else -> Box(modifier = Modifier.padding(top = 4.dp)) {
                        AddIncomeSheet(onSaved = onIncomeSaved)
                    }
                }
            }
        }
    }
}

const val TAB_UPI_MANUAL = 0
const val TAB_UPI_CONTACT = 1
const val TAB_CASH_CARD = 2
const val TAB_INCOME = 3
