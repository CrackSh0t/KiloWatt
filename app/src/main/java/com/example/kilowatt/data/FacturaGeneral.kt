package com.example.kilowatt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "factura_general")
data class FacturaGeneral(
    @PrimaryKey val idFactura: String = "",
    val mesPeriodo: String = "",
    val kwhTotalesRecibo: Double = 0.0,
    val montoTotalSoles: Double = 0.0
)
