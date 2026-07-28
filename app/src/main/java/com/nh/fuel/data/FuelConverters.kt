package com.nh.fuel.data

import androidx.room.TypeConverter
import com.google.gson.Gson

class FuelConverters {
    private val gson = Gson()

    // DayShift Converters
    @TypeConverter
    fun fromDayShift(shift: DayShift): String = gson.toJson(shift)

    @TypeConverter
    fun toDayShift(json: String): DayShift = gson.fromJson(json, DayShift::class.java)

    // RefillEvent Converters
    @TypeConverter
    fun fromRefillEvent(event: RefillEvent): String = gson.toJson(event)

    @TypeConverter
    fun toRefillEvent(json: String): RefillEvent = gson.fromJson(json, RefillEvent::class.java)
}
