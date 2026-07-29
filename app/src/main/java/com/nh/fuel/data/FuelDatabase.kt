package com.nh.fuel.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DailyFuelRecord::class, ExpenseItem::class], version = 5, exportSchema = false)
@TypeConverters(FuelConverters::class)
abstract class FuelDatabase : RoomDatabase() {
    abstract fun fuelDao(): FuelDao
    abstract fun expenseDao(): ExpenseDao
}
