package com.example.kilowatt.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LecturaRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val uid: String?
        get() = auth.currentUser?.uid

    private val facturasRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("facturas") }

    private val lecturasRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("lecturas") }

    private fun sanitizeDocId(docId: String): String = docId.trim().replace("/", "_").replace("\\", "_")

    // Guardar / Actualizar Recibo Base (Factura General)
    suspend fun guardarFactura(factura: FacturaGeneral): Pair<Boolean, String?> =
        suspendCancellableCoroutine { continuation ->
            try {
                val fRef = facturasRef
                if (fRef == null) {
                    if (continuation.isActive) continuation.resume(Pair(false, "Usuario no autenticado"))
                    return@suspendCancellableCoroutine
                }

                val docId = sanitizeDocId(factura.mesPeriodo)
                val facturaLimpia = factura.copy(mesPeriodo = factura.mesPeriodo.trim())

                fRef.document(docId)
                    .set(facturaLimpia)
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

    // Guardar / Actualizar Lectura de un submedidor
    suspend fun guardarLectura(lectura: Lectura): Pair<Boolean, String?> =
        suspendCancellableCoroutine { continuation ->
            try {
                val lRef = lecturasRef
                if (lRef == null) {
                    if (continuation.isActive) continuation.resume(Pair(false, "Usuario no autenticado"))
                    return@suspendCancellableCoroutine
                }

                val lecturaLimpia = lectura.copy(
                    mesPeriodo = lectura.mesPeriodo.trim(),
                    idLectura = sanitizeDocId(lectura.idLectura)
                )

                lRef.document(lecturaLimpia.idLectura)
                    .set(lecturaLimpia)
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

    // Escuchar todas las facturas generales guardadas
    fun obtenerFacturasFlow(): Flow<List<FacturaGeneral>> = callbackFlow {
        val fRef = facturasRef
        if (fRef == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = fRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val lista = snapshot?.toObjects<FacturaGeneral>() ?: emptyList()
            trySend(lista)
        }
        awaitClose { listener.remove() }
    }

    // Escuchar lecturas asociadas a un periodo específico
    fun obtenerLecturasPorMesFlow(mes: String): Flow<List<Lectura>> = callbackFlow {
        val lRef = lecturasRef
        if (lRef == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val mesLimpio = mes.trim()
        val listener = lRef.whereEqualTo("mesPeriodo", mesLimpio)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects<Lectura>() ?: emptyList()
                trySend(lista)
            }
        awaitClose { listener.remove() }
    }
}