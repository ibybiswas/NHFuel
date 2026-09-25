package com.nh.fuel.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties

/** Litres and rupees are kept to 2 decimals; removes floating-point noise (91.10000000000014 -> 91.1). */
fun round2(value: Double): Double =
    java.math.BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()

@IgnoreExtraProperties
data class RefillEvent(
    val amount: Double = 0.0,
    val timestamp: String = ""
) {
    /** Returns this event with `amount` snapped to 2 decimals (fixes legacy Firestore data). */
    fun normalizeRounding(): RefillEvent = copy(amount = round2(amount))
}

@IgnoreExtraProperties
data class TestingEvent(
    val petrolTestingAmount: Double = 0.0,
    val dieselTestingAmount: Double = 0.0,
    val timestamp: String = ""
) {
    fun normalizeRounding(): TestingEvent = copy(
        petrolTestingAmount = round2(petrolTestingAmount),
        dieselTestingAmount = round2(dieselTestingAmount)
    )
}

@IgnoreExtraProperties
data class NozzleShift(
    val open: Double = 0.0,
    val close: Double = 0.0,
    val testing: Double = 0.0,
    val isReset: Boolean = false,              // <--- Red Reset Badge Flag
    val originalOpenBeforeReset: Double = 0.0  // <--- Restores original open reading on Undo
) {
    val isValid: Boolean get() = close >= open || close == 0.0
    val grossSale: Double get() = if (close >= open && close > 0.0) round2(close - open) else 0.0
    // Testing fuel is dispensed through the meter but returned to the tank, so it must always be
    // subtracted in full. Do NOT clamp per nozzle: a nozzle with less meter movement than its test
    // litres (or not yet closed) would otherwise silently "lose" part of the test amount.
    val sale: Double get() = round2(grossSale - testing)
    val isClosed: Boolean get() = close > 0.0 && close >= open

    /** Returns this reading with every stored litre value snapped to 2 decimals. */
    fun normalizeRounding(): NozzleShift = copy(
        open = round2(open),
        close = round2(close),
        testing = round2(testing),
        originalOpenBeforeReset = round2(originalOpenBeforeReset)
    )
}

@IgnoreExtraProperties
data class DispenserShift(
    val petrolN2: NozzleShift = NozzleShift(),
    val petrolN3: NozzleShift = NozzleShift(),
    val dieselN1: NozzleShift = NozzleShift(),
    val dieselN4: NozzleShift = NozzleShift(),
    val cashCollected: Double = 0.0,
    val digitalCollected: Double = 0.0,
    val creditCollected: Double = 0.0,
    val lastTestingEvent: TestingEvent = TestingEvent()
) {
    val petrolSale: Double get() = round2(petrolN2.sale + petrolN3.sale)
    val dieselSale: Double get() = round2(dieselN1.sale + dieselN4.sale)
    val isShiftComplete: Boolean
        get() = petrolN2.isClosed && petrolN3.isClosed && dieselN1.isClosed && dieselN4.isClosed
    val totalCollected: Double get() = round2(cashCollected + digitalCollected + creditCollected)

    fun getRevenue(petrolPrice: Double, dieselPrice: Double): Double {
        return round2((petrolSale * petrolPrice) + (dieselSale * dieselPrice))
    }

    fun getMismatch(petrolPrice: Double, dieselPrice: Double): Double {
        return round2(totalCollected - getRevenue(petrolPrice, dieselPrice))
    }

    fun normalizeRounding(): DispenserShift = copy(
        petrolN2 = petrolN2.normalizeRounding(),
        petrolN3 = petrolN3.normalizeRounding(),
        dieselN1 = dieselN1.normalizeRounding(),
        dieselN4 = dieselN4.normalizeRounding(),
        cashCollected = round2(cashCollected),
        digitalCollected = round2(digitalCollected),
        creditCollected = round2(creditCollected),
        lastTestingEvent = lastTestingEvent.normalizeRounding()
    )
}

@IgnoreExtraProperties
data class DayShift(
    val shiftNumber: Int = 1,
    val mpd1: DispenserShift = DispenserShift(),
    val mpd2: DispenserShift = DispenserShift()
) {
    val petrolSale: Double get() = round2(mpd1.petrolSale + mpd2.petrolSale)
    val dieselSale: Double get() = round2(mpd1.dieselSale + mpd2.dieselSale)
    val totalPetrolTesting: Double get() = round2(mpd1.petrolN2.testing + mpd1.petrolN3.testing + mpd2.petrolN2.testing + mpd2.petrolN3.testing)
    val totalDieselTesting: Double get() = round2(mpd1.dieselN1.testing + mpd1.dieselN4.testing + mpd2.dieselN1.testing + mpd2.dieselN4.testing)
    val isComplete: Boolean get() = mpd1.isShiftComplete && mpd2.isShiftComplete
    val totalCashCollected: Double get() = round2(mpd1.cashCollected + mpd2.cashCollected)
    val totalDigitalCollected: Double get() = round2(mpd1.digitalCollected + mpd2.digitalCollected)
    val totalCreditCollected: Double get() = round2(mpd1.creditCollected + mpd2.creditCollected)
    val totalCollected: Double get() = round2(totalCashCollected + totalDigitalCollected + totalCreditCollected)

    fun getRevenue(petrolPrice: Double, dieselPrice: Double): Double {
        return round2(mpd1.getRevenue(petrolPrice, dieselPrice) + mpd2.getRevenue(petrolPrice, dieselPrice))
    }

    fun getMismatch(petrolPrice: Double, dieselPrice: Double): Double {
        return round2(totalCollected - getRevenue(petrolPrice, dieselPrice))
    }

    fun normalizeRounding(): DayShift = copy(
        mpd1 = mpd1.normalizeRounding(),
        mpd2 = mpd2.normalizeRounding()
    )
}
