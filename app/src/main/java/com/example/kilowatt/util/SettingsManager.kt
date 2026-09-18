package com.example.kilowatt.util

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kilowatt_prefs", Context.MODE_PRIVATE)

    var nombrePropietario: String
        get() = prefs.getString("propietario_nombre", "") ?: ""
        set(value) {
            prefs.edit().putString("propietario_nombre", value).apply()
        }

    var numeroPago: String
        get() = prefs.getString("pago_numero", "") ?: ""
        set(value) {
            prefs.edit().putString("pago_numero", value).apply()
        }

    var diaLimitePago: Int
        get() = prefs.getInt("pago_dia_limite", 5)
        set(value) {
            prefs.edit().putInt("pago_dia_limite", value).apply()
        }

    var bancoYapePlin: String
        get() = prefs.getString("banco_yape_plin", "Yape/Plin") ?: "Yape/Plin"
        set(value) {
            prefs.edit().putString("banco_yape_plin", value).apply()
        }
}
