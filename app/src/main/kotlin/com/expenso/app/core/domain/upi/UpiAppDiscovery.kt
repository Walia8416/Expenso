package com.expenso.app.core.domain.upi

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.expenso.app.core.domain.model.InstalledUpiApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Discovers installed UPI apps by resolving a canonical `upi://pay?pa=example@upi`
 * intent. Requires the matching `<queries>` entries in AndroidManifest.xml.
 */
@Singleton
class UpiAppDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun installedApps(): List<InstalledUpiApp> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PROBE_URI))
        val pm = context.packageManager
        val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        // Never advertise Expenso as a UPI target — the mediator activity
        // itself resolves this intent, which would otherwise show up in the
        // PaySheet and loop back into us.
        val self = context.packageName
        return infos
            .asSequence()
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == self) return@mapNotNull null
                val label = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                }.getOrDefault(friendlyNameFor(pkg))
                InstalledUpiApp(packageName = pkg, displayName = label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.displayName.lowercase() }
            .toList()
    }

    private fun friendlyNameFor(pkg: String): String = when (pkg) {
        "com.google.android.apps.nbu.paisa.user" -> "Google Pay"
        "com.phonepe.app", "com.phonepe.app.preprod" -> "PhonePe"
        "net.one97.paytm" -> "Paytm"
        "in.org.npci.upiapp" -> "BHIM"
        "com.whatsapp" -> "WhatsApp"
        else -> pkg
    }

    companion object {
        private const val PROBE_URI = "upi://pay?pa=example@upi&pn=Example&cu=INR"
    }
}
