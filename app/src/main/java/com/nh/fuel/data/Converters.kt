package com.nh.fuel.data

import androidx.room.TypeConverter
import com.google.gson.Gson

class Converters {
    private val gson = Gson()

    // DayShift Converters
    @TypeConverter
    fun fromDayShift(shift: DayShift?): String = gson.toJson(shift)

    @TypeConverter
    fun toDayShift(json: String?): DayShift =
        if (json.isNullOrEmpty()) DayShift(1) else gson.fromJson(json, DayShift::class.java)

    // RefillEvent Converters
    @TypeConverter
    fun fromRefillEvent(refill: RefillEvent?): String = gson.toJson(refill)

    @TypeConverter
    fun toRefillEvent(json: String?): RefillEvent =
        if (json.isNullOrEmpty()) RefillEvent() else gson.fromJson(json, RefillEvent::class.java)

    // CreditFuelType Converters
    @TypeConverter
    fun fromCreditFuelType(value: CreditFuelType?): String = value?.name ?: CreditFuelType.PETROL.name

    @TypeConverter
    fun toCreditFuelType(value: String?): CreditFuelType =
        try {
            if (value.isNullOrEmpty()) CreditFuelType.PETROL else CreditFuelType.valueOf(value)
        } catch (e: Exception) {
            CreditFuelType.PETROL
        }
}
