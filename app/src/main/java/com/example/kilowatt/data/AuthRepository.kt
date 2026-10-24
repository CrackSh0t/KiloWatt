package com.example.kilowatt.data

import com.example.kilowatt.util.ISettingsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val database: AppDatabase,
    private val settingsManager: ISettingsManager,
) : IAuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override val usuarioActual: FirebaseUser?
        get() = auth.currentUser

    override fun registrarUsuario(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

    override fun iniciarSesion(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

    override fun cerrarSesion() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                database.clearAllTables()
            } catch (_: Exception) {}
            try {
                settingsManager.limpiarConfiguracion()
            } catch (_: Exception) {}
            auth.signOut()
        }
    }

    override fun enviarCorreoRecuperacion(email: String, onResult: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }
}
