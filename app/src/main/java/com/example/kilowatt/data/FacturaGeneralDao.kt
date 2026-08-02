package com.example.kilowatt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FacturaGeneralDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarFactura(factura: FacturaGeneral)

    @Query("SELECT * FROM factura_general WHERE mesPeriodo = :mes LIMIT 1")
    suspend fun obtenerFacturaPorMes(mes: String): FacturaGeneral?
}