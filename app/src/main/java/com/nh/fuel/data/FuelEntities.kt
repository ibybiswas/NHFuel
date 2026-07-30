package com.nh.fuel.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.max

data class RefillEvent(
    val amount: Double = 0.0,
    val timestamp: String = ""
)

data class TestingEvent(
    val petrolTestingAmount: Double = 0.0,
    val dieselTestingAmount: Double = 0.0,
    val timestamp: String = ""
)

data class NozzleShift(
    val open: Double = 0.0,
    val close: Double = 0.0,
    val testing: Double = 0.0
) {
    val isValid: Boolean get() = close >= open || close == 0.0
    // Gross meter difference
    val grossSale: Double get() = if (close >= open && close > 0.0) close - open else 0.0
    // Net sale after deducting fuel returned to tank during testing
    val sale: Double get() = max(0.0, grossSale - testing)
    val isClosed: Boolean get() = close > 0.0 && close >= open
}

data class DispenserShift(
    val petrolN2: NozzleShift = NozzleShift(),
    val petrolN3: NozzleShift = NozzleShift(),
    val dieselN1: NozzleShift = NozzleShift(),
    val dieselN4: NozzleShift = NozzleShift(),

    val cashCollected: Double = 0.0,
    val digitalCollected: Double = 0.0,

    val lastTestingEvent: TestingEvent = TestingEvent()
) {
    val petrolSale: Double get() = petrolN2.sale + petrolN3.sale
    val dieselSale: Double get() = dieselN1.sale + dieselN4.sale
    val isShiftComplete: Boolean
        get() = petrolN2.isClosed && petrolN3.isClosed && dieselN1.isClosed && dieselN4.isClosed

    val totalCollected: Double get() = cashCollected + digitalCollected

    fun getRevenue(petrolPrice: Double, dieselPrice: Double): Double {
        return (petrolSale * petrolPrice) + (dieselSale * dieselPrice)
    }

    fun getMismatch(petrolPrice: Double, dieselPrice: Double): Double {
        return totalCollected - getRevenue(petrolPrice, dieselPrice)
    }
}

data class DayShift(
    val shiftNumber: Int,
    val mpd1: DispenserShift = DispenserShift(),
    val mpd2: DispenserShift = DispenserShift()
) {
    val petrolSale: Double get() = mpd1.petrolSale + mpd2.petrolSale
    val dieselSale: Double get() = mpd1.dieselSale + mpd2.dieselSale
    val totalPetrolTesting: Double get() = mpd1.petrolN2.testing + mpd1.petrolN3.testing + mpd2.petrolN2.testing + mpd2.petrolN3.testing
    val totalDieselTesting: Double get() = mpd1.dieselN1.testing + mpd1.dieselN4.testing + mpd2.dieselN1.testing + mpd2.dieselN4.testing

    val isComplete: Boolean get() = mpd1.isShiftComplete && mpd2.isShiftComplete

    val totalCashCollected: Double get() = mpd1.cashCollected + mpd2.cashCollected
    val totalDigitalCollected: Double get() = mpd1.digitalCollected + mpd2.digitalCollected
    val totalCollected: Double get() = totalCashCollected + totalDigitalCollected

    fun getRevenue(petrolPrice: Double, dieselPrice: Double): Double {
        return mpd1.getRevenue(petrolPrice, dieselPrice) + mpd2.getRevenue(petrolPrice, dieselPrice)
    }

    fun getMismatch(petrolPrice: Double, dieselPrice: Double): Double {
        return totalCollected - getRevenue(petrolPrice, dieselPrice)
    }
}

@Entity(tableName = "daily_fuel_records")
data class DailyFuelRecord(
    @PrimaryKey val date: String,
    
    val petrolTotal: Double = 0.0,
    val petrolRefill: Double = 0.0,
    val petrolVariation: Double = 0.0,
    val lastPetrolRefill: RefillEvent = RefillEvent(),
    val lastPetrolVariationAmount: Double = 0.0,
    val lastPetrolVariationTime: String = "",
    val lastPetrolDipAmount: Double = 0.0,
    val lastPetrolDipTime: String = "",

    val dieselTotal: Double = 0.0,
    val dieselRefill: Double = 0.0,
    val dieselVariation: Double = 0.0,
    val lastDieselRefill: RefillEvent = RefillEvent(),
    val lastDieselVariationAmount: Double = 0.0,
    val lastDieselVariationTime: String = "",
    val lastDieselDipAmount: Double = 0.0,
    val lastDieselDipTime: String = "",

    val petrolPrice: Double = 100.0,
    val dieselPrice: Double = 90.0,

    val shift1: DayShift = DayShift(1),
    val shift2: DayShift = DayShift(2),
    val shift3: DayShift = DayShift(3)
) {
    val totalPetrolSell: Double get() = shift1.petrolSale + shift2.petrolSale + shift3.petrolSale
    val totalDieselSell: Double get() = shift1.dieselSale + shift2.dieselSale + shift3.dieselSale

    val totalPetrolTesting: Double get() = shift1.totalPetrolTesting + shift2.totalPetrolTesting + shift3.totalPetrolTesting
    val totalDieselTesting: Double get() = shift1.totalDieselTesting + shift2.totalDieselTesting + shift3.totalDieselTesting

    // Tank stock deductions apply to Net Sales only (Testing fuel returned to tank is NOT deducted)
    val currentPetrolStorage: Double 
        get() = max(0.0, (petrolTotal + petrolRefill) + petrolVariation - totalPetrolSell)
    val currentDieselStorage: Double 
        get() = max(0.0, (dieselTotal + dieselRefill) + dieselVariation - totalDieselSell)

    fun getPetrolAmount(litres: Double): Double = litres * petrolPrice
    fun getDieselAmount(litres: Double): Double = litres * dieselPrice

    val totalPetrolRevenue: Double get() = getPetrolAmount(totalPetrolSell)
    val totalDieselRevenue: Double get() = getDieselAmount(totalDieselSell)
    val grandTotalRevenue: Double get() = totalPetrolRevenue + totalDieselRevenue

    val dailyCashCollected: Double get() = shift1.totalCashCollected + shift2.totalCashCollected + shift3.totalCashCollected
    val dailyDigitalCollected: Double get() = shift1.totalDigitalCollected + shift2.totalDigitalCollected + shift3.totalDigitalCollected
    val dailyTotalCollected: Double get() = dailyCashCollected + dailyDigitalCollected
    val dailyMismatch: Double get() = dailyTotalCollected - grandTotalRevenue
}
