package com.nh.fuel.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditDao {
    @Query("SELECT * FROM credit_records ORDER BY id DESC")
    fun getAllCredits(): Flow<List<CreditRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(credit: CreditRecord)

    @Delete
    suspend fun deleteCredit(credit: CreditRecord)
}
