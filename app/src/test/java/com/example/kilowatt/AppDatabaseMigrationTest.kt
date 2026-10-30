package com.example.kilowatt

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kilowatt.data.Inquilino
import com.example.kilowatt.data.MIGRATION_5_6
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class AppDatabaseMigrationTest {

    @Test
    fun migration5to6_versionesCorrectas() {
        assertEquals(5, MIGRATION_5_6.startVersion)
        assertEquals(6, MIGRATION_5_6.endVersion)
    }

    @Test
    fun migration5to6_ejecutaSentenciaAlterTableConColumnaNumeroPersonas() {
        val sentenciasEjecutadas = mutableListOf<String>()
        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                sentenciasEjecutadas.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        MIGRATION_5_6.migrate(fakeDb)

        assertEquals(1, sentenciasEjecutadas.size)
        assertTrue(
            sentenciasEjecutadas[0].contains("ALTER TABLE inquilinos ADD COLUMN numeroPersonas INTEGER NOT NULL DEFAULT 1")
        )
    }

    @Test
    fun inquilino_valorPorDefectoNumeroPersonas_esUno() {
        val inquilino = Inquilino(idInquilino = "inq1", nombreCompleto = "Test", telefonoWhatsapp = "999999999")
        assertEquals(1, inquilino.numeroPersonas)
    }
}
