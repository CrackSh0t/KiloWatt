package com.example.kilowatt.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class InquilinoRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val uid: String?
        get() = auth.currentUser?.uid

    private val inquilinosRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("inquilinos") }

    private val submedidoresRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("submedidores") }

    private val perfilRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("perfil") }

    // Guardar Inquilino y Submedidor (Suspend)
    suspend fun guardarInquilinoYSubmedidor(
        nombreCompleto: String,
        telefono: String,
        nombreEspacio: String,
        esAreaComun: Boolean,
        pagaAreaComun: Boolean,
    ): Pair<Boolean, String?> = suspendCancellableCoroutine { continuation ->
        try {
            val inqRef = inquilinosRef
            val subRef = submedidoresRef

            if (inqRef == null || subRef == null) {
                if (continuation.isActive) continuation.resume(Pair(false, "Usuario no autenticado"))
                return@suspendCancellableCoroutine
            }

            val batch = db.batch()
            val idSubmedidor = subRef.document().id

            if (esAreaComun) {
                val nuevoSubmedidor = Submedidor(
                    idSubmedidor = idSubmedidor,
                    nombreEspacio = nombreEspacio,
                    esAreaComun = true,
                    pagaAreaComun = false,
                    idInquilinoTitular = null,
                )
                batch.set(subRef.document(idSubmedidor), nuevoSubmedidor)
            } else {
                val idInquilino = inqRef.document().id
                val nuevoInquilino = Inquilino(
                    idInquilino = idInquilino,
                    nombreCompleto = nombreCompleto,
                    telefonoWhatsapp = telefono,
                )
                val nuevoSubmedidor = Submedidor(
                    idSubmedidor = idSubmedidor,
                    nombreEspacio = nombreEspacio,
                    esAreaComun = false,
                    pagaAreaComun = pagaAreaComun,
                    idInquilinoTitular = idInquilino,
                )
                batch.set(inqRef.document(idInquilino), nuevoInquilino)
                batch.set(subRef.document(idSubmedidor), nuevoSubmedidor)
            }

            batch.commit()
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Pair(true, null))
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) continuation.resume(Pair(false, e.localizedMessage))
                }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resume(Pair(false, e.localizedMessage))
        }
    }

    // Escuchar la lista de submedidores en tiempo real
    fun obtenerSubmedidoresFlow(): Flow<List<Submedidor>> = callbackFlow {
        val subRef = submedidoresRef
        if (subRef == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = subRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val lista = snapshot?.toObjects<Submedidor>() ?: emptyList()
            trySend(lista)
        }
        awaitClose { listener.remove() }
    }

    // Escuchar la lista de inquilinos en tiempo real
    fun obtenerInquilinosFlow(): Flow<List<Inquilino>> = callbackFlow {
        val inqRef = inquilinosRef
        if (inqRef == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = inqRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val lista = snapshot?.toObjects<Inquilino>() ?: emptyList()
            trySend(lista)
        }
        awaitClose { listener.remove() }
    }

    // Actualizar cambios en Inquilino y Submedidor
    fun actualizarInquilinoYSubmedidor(
        inquilino: Inquilino?,
        submedidor: Submedidor,
        onComplete: (Boolean, String?) -> Unit,
    ) {
        try {
            val inqRef = inquilinosRef
            val subRef = submedidoresRef

            if (subRef == null) {
                onComplete(false, "Usuario no autenticado")
                return
            }

            val batch = db.batch()
            batch.set(subRef.document(submedidor.idSubmedidor), submedidor)
            if (inquilino != null && inqRef != null && inquilino.idInquilino.isNotEmpty()) {
                batch.set(inqRef.document(inquilino.idInquilino), inquilino)
            }

            batch.commit()
                .addOnSuccessListener { onComplete(true, null) }
                .addOnFailureListener { e -> onComplete(false, e.localizedMessage) }
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage)
        }
    }

    // Eliminar registros
    fun eliminarInquilinoYSubmedidor(
        inquilino: Inquilino?,
        submedidor: Submedidor,
        onComplete: (Boolean, String?) -> Unit,
    ) {
        try {
            val inqRef = inquilinosRef
            val subRef = submedidoresRef

            if (subRef == null) {
                onComplete(false, "Usuario no autenticado")
                return
            }

            val batch = db.batch()
            batch.delete(subRef.document(submedidor.idSubmedidor))
            if (inquilino != null && inqRef != null && inquilino.idInquilino.isNotEmpty()) {
                batch.delete(inqRef.document(inquilino.idInquilino))
            }

            batch.commit()
                .addOnSuccessListener { onComplete(true, null) }
                .addOnFailureListener { e -> onComplete(false, e.localizedMessage) }
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage)
        }
    }

    // Guardar perfil del propietario en Firestore (Suspend)
    suspend fun guardarPerfilPropietario(nombre: String, numero: String, diaLimite: Int): Pair<Boolean, String?> =
        suspendCancellableCoroutine { continuation ->
            try {
                val pRef = perfilRef
                if (pRef == null) {
                    if (continuation.isActive) continuation.resume(Pair(false, "Usuario no autenticado"))
                    return@suspendCancellableCoroutine
                }

                val perfil = mapOf(
                    "nombrePropietario" to nombre,
                    "numeroPago" to numero,
                    "diaLimitePago" to diaLimite,
                )
                pRef.document("datos_pago")
                    .set(perfil)
                    .addOnSuccessListener {
                        if (continuation.isActive) continuation.resume(Pair(true, null))
                    }
                    .addOnFailureListener { e ->
                        if (continuation.isActive) continuation.resume(Pair(false, e.localizedMessage))
                    }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(Pair(false, e.localizedMessage))
            }
        }

    // Obtener perfil del propietario en tiempo real
    fun obtenerPerfilPropietarioFlow(): Flow<Map<String, Any>?> = callbackFlow {
        val pRef = perfilRef
        if (pRef == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = pRef.document("datos_pago").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.data)
        }
        awaitClose { listener.remove() }
    }
}