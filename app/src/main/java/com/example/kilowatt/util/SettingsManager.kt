package com.example.kilowatt.util

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) : ISettingsManager {
    private val prefs: SharedPreferences = context.getSharedPreferences("kilowatt_prefs", Context.MODE_PRIVATE)

    override var nombrePropietario: String
        get() = prefs.getString("propietario_nombre", "") ?: ""
        set(value) {
            prefs.edit().putString("propietario_nombre", value).apply()
        }

    override var numeroPago: String
        get() = prefs.getString("pago_numero", "") ?: ""
        set(value) {
            prefs.edit().putString("pago_numero", value).apply()
        }

    override var diaLimitePago: Int
        get() = prefs.getInt("pago_dia_limite", 5)
        set(value) {
            prefs.edit().putInt("pago_dia_limite", value).apply()
        }

    var bancoYapePlin: String
        get() = prefs.getString("banco_yape_plin", "Yape/Plin") ?: "Yape/Plin"
        set(value) {
            prefs.edit().putString("banco_yape_plin", value).apply()
        }

    override fun limpiarConfiguracion() {
        prefs.edit().clear().apply()
    }
}
