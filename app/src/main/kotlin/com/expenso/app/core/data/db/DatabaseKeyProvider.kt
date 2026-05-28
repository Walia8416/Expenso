package com.expenso.app.core.data.db

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates and returns a stable per-install passphrase for the SQLCipher database.
 * The passphrase itself is stored in an EncryptedSharedPreferences backed by the
 * Android Keystore — so it never sits in cleartext on disk.
 */
@Singleton
class DatabaseKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun getOrCreatePassphrase(): ByteArray {
        val prefs = encryptedPrefs()
        val existing = prefs.getString(KEY_PASSPHRASE, null)
        if (existing != null) return existing.toByteArray(Charsets.UTF_8)

        val random = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val asHex = random.joinToString("") { "%02x".format(it) }
        prefs.edit().putString(KEY_PASSPHRASE, asHex).apply()
        return asHex.toByteArray(Charsets.UTF_8)
    }

    private fun encryptedPrefs() = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    companion object {
        private const val PREFS_FILE = "expenso_keys"
        private const val KEY_PASSPHRASE = "db_passphrase_hex_v1"
        private const val PASSPHRASE_BYTES = 32
    }
}
