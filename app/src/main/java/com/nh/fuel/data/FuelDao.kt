package com.nh.fuel.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Query("SELECT * FROM daily_fuel_records WHERE date = :date")
    fun getRecordByDate(date: String): Flow<DailyFuelRecord?>

    @Query("SELECT * FROM daily_fuel_records ORDER BY date ASC")
    fun getAllRecords(): Flow<List<DailyFuelRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: DailyFuelRecord)
}
