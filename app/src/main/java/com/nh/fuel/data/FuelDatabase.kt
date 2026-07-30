package com.nh.fuel.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [DailyFuelRecord::class, ExpenseItem::class, CreditRecord::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FuelDatabase : RoomDatabase() {
    abstract fun fuelDao(): FuelDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun creditDao(): CreditDao
}
