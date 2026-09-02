package com.nh.fuel.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@IgnoreExtraProperties
data class ActivityLogItem(
    val id: String = System.currentTimeMillis().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = "",
    val firstName: String = "",
    val userRole: String = "",
    val actionDetails: String = ""
) {
    val logText: String
        get() = "🕒 $formattedTime | $firstName ($userRole) $actionDetails"
}

object ActivityLogger {
    /**
     * Master switch for Activity & Audit Logging. Defaults to OFF (disabled) and is kept in
     * sync with the "Enable Activity & Audit Logging" toggle in Settings via [setEnabled].
     * While disabled, [log] is a no-op — no entries are written to Firestore.
     */
    @Volatile
    var isEnabled: Boolean = false
        private set

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun log(session: AppUserSession, actionDetails: String) {
        if (!isEnabled) return

        val db = FirebaseFirestore.getInstance()
        val now = System.currentTimeMillis()
        val formattedTime = SimpleDateFormat("MMM d, hh:mm a", Locale.US).format(Date(now))
        
        // Extract only the first name from displayName
        val rawName = session.displayName.trim()
        val firstName = if (rawName.contains(" ")) rawName.substringBefore(" ") else rawName

        val logItem = ActivityLogItem(
            id = now.toString(),
            timestamp = now,
            formattedTime = formattedTime,
            firstName = if (firstName.isNotBlank()) firstName else "User",
            userRole = session.role.name,
            actionDetails = actionDetails
        )

        db.collection("activity_logs")
            .document(logItem.id)
            .set(logItem)

        // Cleanup entries older than 90 days
        val ninetyDaysAgo = now - (90L * 24 * 60 * 60 * 1000)
        db.collection("activity_logs")
            .whereLessThan("timestamp", ninetyDaysAgo)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    db.collection("activity_logs").document(doc.id).delete()
                }
            }
    }
}
