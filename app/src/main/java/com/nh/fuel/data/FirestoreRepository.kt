package com.nh.fuel.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    // 1. Daily Fuel Records
    fun observeAllFuelRecords(): Flow<List<DailyFuelRecord>> = callbackFlow {
        val listener = db.collection("daily_fuel_records")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepository", "Error observing fuel records: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val records = snapshot.documents.mapNotNull { doc ->
                        runCatching { doc.toObject(DailyFuelRecord::class.java) }
                            .onFailure { Log.e("FirestoreRepository", "Failed to parse fuel record ${doc.id}: ${it.message}") }
                            .getOrNull()
                    }
                    trySend(records)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveFuelRecord(record: DailyFuelRecord) {
        if (record.date.isBlank()) return
        suspendCancellableCoroutine<Unit> { cont ->
            db.collection("daily_fuel_records")
                .document(record.date)
                .set(record)
                .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
                .addOnFailureListener { e ->
                    Log.e("FirestoreRepository", "Failed to save record: ${e.localizedMessage}")
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }

    suspend fun deleteFuelRecord(date: String) {
        if (date.isBlank()) return
        suspendCancellableCoroutine<Unit> { cont ->
            db.collection("daily_fuel_records")
                .document(date)
                .delete()
                .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
                .addOnFailureListener { e ->
                    Log.e("FirestoreRepository", "Failed to delete record: ${e.localizedMessage}")
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }

    // 2. Expenses
    fun observeAllExpenses(): Flow<List<ExpenseItem>> = callbackFlow {
        val listener = db.collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // NOTE: if this keeps firing, it is almost always a Firestore
                    // security-rules permission error on the "expenses" collection
                    // specifically (check Logcat tag FirestoreRepository for the real message).
                    Log.e("FirestoreRepository", "Error observing expenses: ${error.localizedMessage}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val expenses = snapshot.documents.mapNotNull { doc ->
                        runCatching { doc.toObject(ExpenseItem::class.java) }
                            .onFailure { Log.e("FirestoreRepository", "Failed to parse expense ${doc.id}: ${it.message}") }
                            .getOrNull()
                    }
                    trySend(expenses)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveExpense(expense: ExpenseItem) {
        suspendCancellableCoroutine<Unit> { cont ->
            db.collection("expenses")
                .document(expense.id.toString())
                .set(expense)
                .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
                .addOnFailureListener { e ->
                    Log.e("FirestoreRepository", "Failed to save expense: ${e.localizedMessage}", e)
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }

    suspend fun deleteExpense(expense: ExpenseItem) {
        suspendCancellableCoroutine<Unit> { cont ->
            db.collection("expenses")
                .document(expense.id.toString())
                .delete()
                .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
                .addOnFailureListener { e ->
                    Log.e("FirestoreRepository", "Failed to delete expense: ${e.localizedMessage}", e)
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }

    // 3. Credits
    fun observeAllCredits(): Flow<List<CreditRecord>> = callbackFlow {
        val listener = db.collection("credits")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Same note as observeAllExpenses(): a recurring error here usually
                    // means the "credits" collection is denied by Firestore rules.
                    Log.e("FirestoreRepository", "Error observing credits: ${error.localizedMessage}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val credits = snapshot.documents.mapNotNull { doc ->
                        runCatching { doc.toObject(CreditRecord::class.java) }
                            .onFailure { Log.e("FirestoreRepository", "Failed to parse credit ${doc.id}: ${it.message}") }
                            .getOrNull()
                    }
                    trySend(credits)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveCredit(credit: CreditRecord) {
        suspendCancellableCoroutine<Unit> { cont ->
            db.collection("credits")
                .document(credit.id.toString())
                .set(credit)
                .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
                .addOnFailureListener { e ->
                    Log.e("FirestoreRepository", "Failed to save credit: ${e.localizedMessage}", e)
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }

    suspend fun deleteCredit(credit: CreditRecord) {
        suspendCancellableCoroutine<Unit> { cont ->
            db.collection("credits")
                .document(credit.id.toString())
                .delete()
                .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
                .addOnFailureListener { e ->
                    Log.e("FirestoreRepository", "Failed to delete credit: ${e.localizedMessage}", e)
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }
}
