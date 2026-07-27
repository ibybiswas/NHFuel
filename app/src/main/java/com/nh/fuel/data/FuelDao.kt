// FuelDao.kt
package com.nh.fuel.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Query("SELECT * FROM daily_fuel_records WHERE date = :date")
    fun getRecordByDate(date: String): Flow<DailyFuelRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: DailyFuelRecord)
}

// FuelDatabase.kt
package com.nh.fuel.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DailyFuelRecord::class], version = 1, exportSchema = false)
abstract class FuelDatabase : RoomDatabase() {
    abstract fun fuelDao(): FuelDao
}
