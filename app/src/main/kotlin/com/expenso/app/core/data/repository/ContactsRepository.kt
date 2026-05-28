package com.expenso.app.core.data.repository

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceContact(
    val lookupKey: String,
    val displayName: String,
    val phoneNumber: String,
    val normalizedPhone: String,
    val photoUri: Uri?,
)

@Singleton
class ContactsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Reads contacts with at least one phone number. Multiple numbers for
     * the same contact produce multiple rows; the UI can de-duplicate by
     * lookupKey if desired.
     */
    suspend fun loadContactsWithPhones(query: String? = null): List<DeviceContact> =
        withContext(Dispatchers.IO) {
            val cr = context.contentResolver
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            )
            val selection = if (query.isNullOrBlank()) null
            else "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = if (query.isNullOrBlank()) null else arrayOf("%${query.trim()}%")
            val sort = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"

            val items = mutableListOf<DeviceContact>()
            cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sort,
            )?.use { c ->
                val iLookup = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                val iName = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val iNumber = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val iNormalized = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
                val iPhoto = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val seen = mutableSetOf<String>()
                while (c.moveToNext()) {
                    val lookup = c.getString(iLookup) ?: continue
                    val name = c.getString(iName) ?: continue
                    val rawNumber = c.getString(iNumber)?.replace("\\s".toRegex(), "") ?: continue
                    val normalized = c.getString(iNormalized)?.takeIf { it.isNotBlank() } ?: rawNumber
                    val key = "$lookup|$normalized"
                    if (!seen.add(key)) continue
                    val photo = c.getString(iPhoto)?.let(Uri::parse)
                    items += DeviceContact(
                        lookupKey = lookup,
                        displayName = name,
                        phoneNumber = rawNumber,
                        normalizedPhone = normalized,
                        photoUri = photo,
                    )
                }
            }
            items
        }
}
