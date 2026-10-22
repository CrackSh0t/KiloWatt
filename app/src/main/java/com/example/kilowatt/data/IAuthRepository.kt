package com.example.kilowatt.data

import com.google.firebase.auth.FirebaseUser

interface IAuthRepository {
    val usuarioActual: FirebaseUser?
    fun registrarUsuario(email: String, pass: String, onResult: (Boolean, String?) -> Unit)
    fun iniciarSesion(email: String, pass: String, onResult: (Boolean, String?) -> Unit)
    fun cerrarSesion()
    fun enviarCorreoRecuperacion(email: String, onResult: (Boolean, String?) -> Unit)
}
