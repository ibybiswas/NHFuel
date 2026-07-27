package com.nh.fuel.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_fuel_records")
data class DailyFuelRecord(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    
    // Tank Storage
    val petrolTotal: Double = 0.0,
    val petrolRefill: Double = 0.0,
    val petrolShortage: Double = 0.0,
    
    val dieselTotal: Double = 0.0,
    val dieselRefill: Double = 0.0,
    val dieselShortage: Double = 0.0,

    // NPD 1 Nozzles
    val npd1PetrolN2Open: Double = 0.0,
    val npd1PetrolN2Close: Double = 0.0,
    val npd1PetrolN3Open: Double = 0.0,
    val npd1PetrolN3Close: Double = 0.0,
    
    val npd1DieselN1Open: Double = 0.0,
    val npd1DieselN1Close: Double = 0.0,
    val npd1DieselN4Open: Double = 0.0,
    val npd1DieselN4Close: Double = 0.0,

    // NPD 2 Nozzles
    val npd2PetrolN2Open: Double = 0.0,
    val npd2PetrolN2Close: Double = 0.0,
    val npd2PetrolN3Open: Double = 0.0,
    val npd2PetrolN3Close: Double = 0.0,
    
    val npd2DieselN1Open: Double = 0.0,
    val npd2DieselN1Close: Double = 0.0,
    val npd2DieselN4Open: Double = 0.0,
    val npd2DieselN4Close: Double = 0.0
) {
    // Current Storage Calculations
    val currentPetrolStorage: Double get() = petrolTotal + petrolRefill - petrolShortage
    val currentDieselStorage: Double get() = dieselTotal + dieselRefill - dieselShortage

    // Individual Nozzle Sales Calculations
    private fun calcSale(open: Double, close: Double) = if (close >= open) close - open else 0.0

    val npd1PetrolSell: Double get() = calcSale(npd1PetrolN2Open, npd1PetrolN2Close) + calcSale(npd1PetrolN3Open, npd1PetrolN3Close)
    val npd1DieselSell: Double get() = calcSale(npd1DieselN1Open, npd1DieselN1Close) + calcSale(npd1DieselN4Open, npd1DieselN4Close)

    val npd2PetrolSell: Double get() = calcSale(npd2PetrolN2Open, npd2PetrolN2Close) + calcSale(npd2PetrolN3Open, npd2PetrolN3Close)
    val npd2DieselSell: Double get() = calcSale(npd2DieselN1Open, npd2DieselN1Close) + calcSale(npd2DieselN4Open, npd2DieselN4Close)

    // Total Daily Sales Across Dispensers
    val totalPetrolSell: Double get() = npd1PetrolSell + npd2PetrolSell
    val totalDieselSell: Double get() = npd1DieselSell + npd2DieselSell
}
