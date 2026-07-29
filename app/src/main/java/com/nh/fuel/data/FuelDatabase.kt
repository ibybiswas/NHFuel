package com.nh.fuel.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DailyFuelRecord::class], version = 4, exportSchema = false)
@TypeConverters(FuelConverters::class)
abstract class FuelDatabase : RoomDatabase() {
    abstract fun fuelDao(): FuelDao
}
