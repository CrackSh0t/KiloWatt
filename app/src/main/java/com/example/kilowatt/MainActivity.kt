package com.example.kilowatt

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kilowatt.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Abrir Registrador de Lecturas
        binding.cardRegistrarLecturas.setOnClickListener {
            val intent = Intent(this, LecturasActivity::class.java)
            startActivity(intent)
        }

        // 2. Abrir Resumen y Cobros
        binding.cardResumenCobros.setOnClickListener {
            val intent = Intent(this, ResumenCobrosActivity::class.java)
            startActivity(intent)
        }

        // 3. Abrir Configuración de Inquilinos
        binding.cardConfiguracion.setOnClickListener {
            val intent = Intent(this, ConfiguracionActivity::class.java)
            startActivity(intent)
        }
    }
}