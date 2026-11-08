package com.example.kilowatt.data

import com.example.kilowatt.util.ISettingsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import android.util.Log
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

    override suspend fun cerrarSesion() {
        withContext(Dispatchers.IO) {
            var firstFailure: Exception? = null
            try {
                lecturaRepository.cancelarListeners()
            } catch (e: Exception) {
                Log.e("AuthRepository", "No se pudieron cancelar listeners de lecturas", e)
                firstFailure = e
            }
            try {
                inquilinoRepository.cancelarListeners()
            } catch (e: Exception) {
                Log.e("AuthRepository", "No se pudieron cancelar listeners de inquilinos", e)
                if (firstFailure == null) firstFailure = e
            }
            try {
                database.clearAllTables()
            } catch (e: Exception) {
                Log.e("AuthRepository", "No se pudo limpiar Room durante el cierre de sesión", e)
                if (firstFailure == null) firstFailure = e
            }
            try {
                settingsManager.limpiarConfiguracion()
            } catch (e: Exception) {
                Log.e("AuthRepository", "No se pudo limpiar la configuración durante el cierre de sesión", e)
                if (firstFailure == null) firstFailure = e
            }
            try {
                auth.signOut()
            } catch (e: Exception) {
                Log.e("AuthRepository", "No se pudo cerrar la sesión de Firebase", e)
                if (firstFailure == null) firstFailure = e
            }
            firstFailure?.let { throw it }
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
