package com.example.kilowatt

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lanzar la pantalla de configuración para probarla directamente
        val intent = Intent(this, ConfiguracionActivity::class.java)
        startActivity(intent)
        finish() // Cierra MainActivity para que te quede en Configuración
    }
}