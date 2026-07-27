package com.nh.fuel.sync

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.Gson
import com.nh.fuel.data.DailyFuelRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Collections

class DriveSyncEngine(private val context: Context) {

    private val gson = Gson()

    private fun getDriveService(): Drive? {
        val account: GoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account.account
        }

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("NH Fuel").build()
    }

    /**
     * Serializes the DailyFuelRecord to JSON and updates or creates `nh_fuel_backup.json` in Google Drive AppData folder.
     */
    suspend fun uploadToDrive(record: DailyFuelRecord): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService() ?: return@withContext false

            val jsonContent = gson.toJson(record)
            val fileName = "nh_fuel_${record.date}.json"

            // Search for existing file in Drive AppData folder
            val query = service.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName'")
                .execute()

            val mediaContent = ByteArrayContent.fromString("application/json", jsonContent)

            if (query.files.isNullOrEmpty()) {
                val fileMetaData = com.google.api.services.drive.model.File().apply {
                    name = fileName
                    parents = Collections.singletonList("appDataFolder")
                }
                service.files().create(fileMetaData, mediaContent).execute()
            } else {
                val fileId = query.files[0].id
                service.files().update(fileId, null, mediaContent).execute()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Downloads the latest JSON record for a given date from Google Drive AppData folder.
     */
    suspend fun downloadFromDrive(date: String): DailyFuelRecord? = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService() ?: return@withContext null
            val fileName = "nh_fuel_$date.json"

            val query = service.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName'")
                .execute()

            if (query.files.isNullOrEmpty()) return@withContext null

            val fileId = query.files[0].id
            val outputStream = ByteArrayOutputStream()
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream)

            val jsonString = outputStream.toString("UTF-8")
            gson.fromJson(jsonString, DailyFuelRecord::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
