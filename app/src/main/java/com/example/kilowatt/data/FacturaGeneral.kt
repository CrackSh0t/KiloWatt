package com.example.kilowatt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "factura_general")
data class FacturaGeneral(
    @PrimaryKey(autoGenerate = true)
    val idFactura: Int = 0,
    val mesPeriodo: String,       // Ej: "2026-08"
    val kwhTotalesRecibo: Double, // Ej: 571.0
    val montoTotalSoles: Double   // Ej: 465.10
)
