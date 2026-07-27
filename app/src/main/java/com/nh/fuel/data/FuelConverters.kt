package com.nh.fuel.data

import androidx.room.TypeConverter
import com.google.gson.Gson

class FuelConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromDayShift(shift: DayShift): String = gson.toJson(shift)

    @TypeConverter
    fun toDayShift(json: String): DayShift = gson.fromJson(json, DayShift::class.java)
}
