package com.expenso.app.feature.upi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.R
import com.expenso.app.core.domain.upi.UpiIntentBuilder
import com.expenso.app.core.ui.theme.ExpensoTheme
import com.expenso.app.feature.pay.PaySheet
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Android chooser entry point when another app fires a `upi://pay?...`
 * intent (or shares one as plain text). Acts as a trampoline:
 *
 * 1. parse the URI via [UpiMediatorViewModel],
 * 2. render the shared [PaySheet] so the user picks a target PSP app and
 *    confirms amount/category (at which point `PayViewModel` writes the
 *    pending expense + `payment_intent` row and emits a launch event),
 * 3. fire the outbound intent pinned to the chosen package,
 * 4. [finish] so the user returns cleanly to whatever app invoked us.
 *
 * The activity lives in its own task (`taskAffinity=""` in the manifest) so
 * finishing does not drag the user into Expenso's main task when the caller
 * expected the UPI app to return them to, say, a merchant checkout.
 */
@AndroidEntryPoint
class UpiMediatorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val incoming = extractUpiUri(intent)
        if (incoming == null) {
            Toast.makeText(this, R.string.upi_invalid_request, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setContent {
            ExpensoTheme {
                MediatorContent(incomingUri = incoming, onFinish = { finish() })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // If the mediator is already on screen (singleTop) and another UPI
        // intent arrives, just re-process with the new URI. Android re-runs
        // the composable on setIntent indirectly — but to keep things simple
        // we finish and let the caller relaunch us for a fresh session.
        setIntent(intent)
        recreate()
    }

    private fun extractUpiUri(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                val data: Uri? = intent.data
                data?.takeIf { it.scheme.equals("upi", ignoreCase = true) }?.toString()
            }
            Intent.ACTION_SEND -> {
                // Browsers / chat apps often share a UPI deeplink as plain
                // text. Pull the first `upi://...` token out of whatever they
                // handed us and parse that.
                val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                UPI_URI_REGEX.find(text)?.value
            }
            else -> null
        }
    }

    companion object {
        private val UPI_URI_REGEX = Regex("upi://[^\\s]+", RegexOption.IGNORE_CASE)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediatorContent(
    incomingUri: String,
    onFinish: () -> Unit,
    vm: UpiMediatorViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(incomingUri) {
        vm.parseIncoming(incomingUri)
    }

    // Anchor the sheet on a transparent full-screen box so it floats over a
    // dimmed scrim instead of our own UI.
    Box(modifier = Modifier.fillMaxSize()) {
        val request = state.request
        val parseError = state.parseError
        when {
            request != null -> {
                PaySheet(
                    request = request,
                    categories = state.categories,
                    lastUsedCategoryId = state.lastUsedCategoryId,
                    onDismiss = onFinish,
                    onLaunched = { expenseId ->
                        // Persist via VM so ConfirmPaymentSheet can prompt next
                        // time Expenso is opened. The write runs on the VM's
                        // scope (app-lifetime DataStore), so it survives the
                        // activity finishing immediately after we dispatch.
                        vm.rememberPendingExpense(expenseId)
                        onFinish()
                    },
                    onError = { msg -> Timber.w("Mediator pay error: $msg") },
                    launchUpi = { payload ->
                        try {
                            val intent = UpiIntentBuilder.buildIntent(
                                uri = Uri.parse(payload.uri),
                                targetPackage = payload.targetPackage,
                            )
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Timber.w(e, "Mediator: no UPI app resolved")
                            Toast.makeText(
                                context,
                                context.getString(R.string.upi_no_apps_title),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                )
            }
            parseError != null -> {
                // Invalid payload — show a toast and exit rather than a dead
                // empty sheet.
                androidx.compose.runtime.LaunchedEffect(parseError) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.upi_invalid_request),
                        Toast.LENGTH_SHORT,
                    ).show()
                    onFinish()
                }
            }
        }
    }
}
