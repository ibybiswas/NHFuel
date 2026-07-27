// FuelDatabase.kt
package com.nh.fuel.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DailyFuelRecord::class], version = 1, exportSchema = false)
abstract class FuelDatabase : RoomDatabase() {
    abstract fun fuelDao(): FuelDao
}
