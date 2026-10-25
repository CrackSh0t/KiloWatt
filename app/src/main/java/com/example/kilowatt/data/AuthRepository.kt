package com.example.kilowatt.data

import com.example.kilowatt.util.ISettingsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val database: AppDatabase,
    private val settingsManager: ISettingsManager,
    private val lecturaRepository: ILecturaRepository,
    private val inquilinoRepository: IInquilinoRepository,
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

    override suspend fun cerrarSesion() = withContext(Dispatchers.IO) {
        try {
            lecturaRepository.cancelarListeners()
        } catch (_: Exception) {}
        try {
            inquilinoRepository.cancelarListeners()
        } catch (_: Exception) {}
        try {
            database.clearAllTables()
        } catch (_: Exception) {}
        try {
            settingsManager.limpiarConfiguracion()
        } catch (_: Exception) {}
        try {
            auth.signOut()
        } catch (_: Exception) {}
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
