package com.example.kilowatt.util

object PhoneUtils {

    /**
     * Limpia y valida un número telefónico para WhatsApp bajo el formato E.164 (por defecto +51 para Perú).
     * Retorna un Pair donde el primer valor indica si es válido (o falso si está vacío)
     * y el segundo valor es el número normalizado (ej. "+51987654321").
     */
    fun formatearYValidarWhatsApp(telefonoInput: String): Pair<Boolean, String> {
        val limpio = telefonoInput.replace(Regex("[^\\d+]"), "").trim()

        if (limpio.isEmpty()) {
            return Pair(false, "")
        }

        val normalizado = when {
            limpio.startsWith("+") -> limpio
            limpio.startsWith("51") && limpio.length == 11 -> "+$limpio"
            limpio.length == 9 && limpio.startsWith("9") -> "+51$limpio"
            else -> "+$limpio"
        }

        // Expresión regular estándar E.164: ^\+[1-9]\d{1,14}$
        val esValido = normalizado.matches(Regex("^\\+[1-9]\\d{1,14}$"))

        return Pair(esValido, if (esValido) normalizado else limpio)
    }
}
