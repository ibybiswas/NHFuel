package com.nh.fuel.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "expense_records")
data class ExpenseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Double,
    val date: String, // Format: YYYY-MM-DD
    val timestamp: String = "" // Entry timestamp
)

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense_records ORDER BY date DESC, id DESC")
    fun getAllExpenses(): Flow<List<ExpenseItem>>

    @Query("SELECT * FROM expense_records WHERE date = :date ORDER BY id DESC")
    fun getExpensesByDate(date: String): Flow<List<ExpenseItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(expense: ExpenseItem)

    @Delete
    suspend fun deleteExpense(expense: ExpenseItem)
}
