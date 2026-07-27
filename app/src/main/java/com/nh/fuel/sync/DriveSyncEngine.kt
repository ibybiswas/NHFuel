package com.nh.fuel.sync

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.nh.fuel.data.DailyFuelRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Collections

class DriveSyncEngine(private val context: Context) {

    private fun getDriveService(): Drive? {
        val account: GoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account.account
        }

        return Drive.Builder(
            NetHttpTransport(),
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

            val jsonContent = JSONObject().apply {
                put("date", record.date)
                put("petrolTotal", record.petrolTotal)
                put("petrolRefill", record.petrolRefill)
                put("petrolShortage", record.petrolShortage)
                put("dieselTotal", record.dieselTotal)
                put("dieselRefill", record.dieselRefill)
                put("dieselShortage", record.dieselShortage)

                // NPD1
                put("npd1PetrolN2Open", record.npd1PetrolN2Open)
                put("npd1PetrolN2Close", record.npd1PetrolN2Close)
                put("npd1PetrolN3Open", record.npd1PetrolN3Open)
                put("npd1PetrolN3Close", record.npd1PetrolN3Close)
                put("npd1DieselN1Open", record.npd1DieselN1Open)
                put("npd1DieselN1Close", record.npd1DieselN1Close)
                put("npd1DieselN4Open", record.npd1DieselN4Open)
                put("npd1DieselN4Close", record.npd1DieselN4Close)

                // NPD2
                put("npd2PetrolN2Open", record.npd2PetrolN2Open)
                put("npd2PetrolN2Close", record.npd2PetrolN2Close)
                put("npd2PetrolN3Open", record.npd2PetrolN3Open)
                put("npd2PetrolN3Close", record.npd2PetrolN3Close)
                put("npd2DieselN1Open", record.npd2DieselN1Open)
                put("npd2DieselN1Close", record.npd2DieselN1Close)
                put("npd2DieselN4Open", record.npd2DieselN4Open)
                put("npd2DieselN4Close", record.npd2DieselN4Close)
            }.toString()

            val fileName = "nh_fuel_${record.date}.json"

            // Search for existing file
            val query = service.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName'")
                .execute()

            val mediaContent = ByteArrayContent.fromString("application/json", jsonContent)

            if (query.files.isNullOrEmpty()) {
                // File does not exist, create new
                val fileMetaData = com.google.api.services.drive.model.File().apply {
                    name = fileName
                    parents = Collections.singletonList("appDataFolder")
                }
                service.files().create(fileMetaData, mediaContent).execute()
            } else {
                // File exists, update it
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
     * Downloads the latest JSON file for the given date from Google Drive AppData folder.
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
            val json = JSONObject(jsonString)

            DailyFuelRecord(
                date = json.optString("date", date),
                petrolTotal = json.optDouble("petrolTotal", 0.0),
                petrolRefill = json.optDouble("petrolRefill", 0.0),
                petrolShortage = json.optDouble("petrolShortage", 0.0),
                dieselTotal = json.optDouble("dieselTotal", 0.0),
                dieselRefill = json.optDouble("dieselRefill", 0.0),
                dieselShortage = json.optDouble("dieselShortage", 0.0),

                npd1PetrolN2Open = json.optDouble("npd1PetrolN2Open", 0.0),
                npd1PetrolN2Close = json.optDouble("npd1PetrolN2Close", 0.0),
                npd1PetrolN3Open = json.optDouble("npd1PetrolN3Open", 0.0),
                npd1PetrolN3Close = json.optDouble("npd1PetrolN3Close", 0.0),
                npd1DieselN1Open = json.optDouble("npd1DieselN1Open", 0.0),
                npd1DieselN1Close = json.optDouble("npd1DieselN1Close", 0.0),
                npd1DieselN4Open = json.optDouble("npd1DieselN4Open", 0.0),
                npd1DieselN4Close = json.optDouble("npd1DieselN4Close", 0.0),

                npd2PetrolN2Open = json.optDouble("npd2PetrolN2Open", 0.0),
                npd2PetrolN2Close = json.optDouble("npd2PetrolN2Close", 0.0),
                npd2PetrolN3Open = json.optDouble("npd2PetrolN3Open", 0.0),
                npd2PetrolN3Close = json.optDouble("npd2PetrolN3Close", 0.0),
                npd2DieselN1Open = json.optDouble("npd2DieselN1Open", 0.0),
                npd2DieselN1Close = json.optDouble("npd2DieselN1Close", 0.0),
                npd2DieselN4Open = json.optDouble("npd2DieselN4Open", 0.0),
                npd2DieselN4Close = json.optDouble("npd2DieselN4Close", 0.0)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
