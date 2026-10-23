package com.example.kilowatt.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import javax.inject.Inject

class LecturaRepository @Inject constructor(
    private val lecturaDao: LecturaDao,
    private val facturaDao: FacturaGeneralDao,
) : ILecturaRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val uid: String?
        get() = auth.currentUser?.uid

    private val facturasRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("facturas") }

    private val lecturasRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("lecturas") }

    private fun sanitizeDocId(docId: String): String = docId.trim().replace("/", "_").replace("\\", "_")

    // Guardar / Actualizar Recibo Base (Factura General) - SSOT: Local Room primero, luego Firestore
    override suspend fun guardarFactura(factura: FacturaGeneral): Pair<Boolean, String?> {
        try {
            facturaDao.insertarFactura(factura)
        } catch (_: Exception) {
            // Continuar con sincronización remota aunque falle localmente
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                val fRef = facturasRef
                if (fRef == null) {
                    if (continuation.isActive) continuation.resume(Pair(true, null))
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
                        if (continuation.isActive) continuation.resume(Pair(true, "Guardado localmente. Error en nube: ${e.localizedMessage}"))
                    }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(Pair(false, e.localizedMessage))
            }
        }
    }

    // Guardar / Actualizar Lectura de un submedidor - SSOT: Local Room primero, luego Firestore
    override suspend fun guardarLectura(lectura: Lectura): Pair<Boolean, String?> {
        try {
            lecturaDao.insertarLectura(lectura)
        } catch (_: Exception) {}

        return suspendCancellableCoroutine { continuation ->
            try {
                val lRef = lecturasRef
                if (lRef == null) {
                    if (continuation.isActive) continuation.resume(Pair(true, null))
                    return@suspendCancellableCoroutine
                }

                val lecturaLimpia = lectura.copy(
                    mesPeriodo = lectura.mesPeriodo.trim(),
                    idLectura = sanitizeDocId(lectura.idLectura),
                )

                lRef.document(lecturaLimpia.idLectura)
                    .set(lecturaLimpia)
                    .addOnSuccessListener {
                        if (continuation.isActive) continuation.resume(Pair(true, null))
                    }
                    .addOnFailureListener { e ->
                        if (continuation.isActive) continuation.resume(Pair(true, "Guardado localmente. Error en nube: ${e.localizedMessage}"))
                    }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(Pair(false, e.localizedMessage))
            }
        }
    }

    // Actualizar el estado y auditoría de pago - SSOT: Local Room primero, luego Firestore
    override suspend fun actualizarEstadoPago(
        idLectura: String,
        nuevoEstado: String,
        fechaPago: Long?,
        metodoPago: String?,
    ): Pair<Boolean, String?> {
        try {
            lecturaDao.actualizarEstadoPago(idLectura, nuevoEstado, fechaPago, metodoPago)
        } catch (_: Exception) {}

        return suspendCancellableCoroutine { continuation ->
            try {
                val lRef = lecturasRef
                if (lRef == null) {
                    if (continuation.isActive) continuation.resume(Pair(true, null))
                    return@suspendCancellableCoroutine
                }

                val docId = sanitizeDocId(idLectura)
                val updates = mapOf(
                    "estadoPago" to nuevoEstado,
                    "fechaPago" to fechaPago,
                    "metodoPago" to metodoPago,
                )

                lRef.document(docId)
                    .update(updates)
                    .addOnSuccessListener {
                        if (continuation.isActive) continuation.resume(Pair(true, null))
                    }
                    .addOnFailureListener { e ->
                        if (continuation.isActive) continuation.resume(Pair(true, "Actualizado localmente. Error en nube: ${e.localizedMessage}"))
                    }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(Pair(false, e.localizedMessage))
            }
        }
    }

    // SSOT: Facturas desde Room DAO + Sincronización background con Firestore
    override fun obtenerFacturasFlow(): Flow<List<FacturaGeneral>> {
        sincronizarFacturasRemotas()
        return facturaDao.obtenerTodasLasFacturas()
    }

    // SSOT: Lecturas por mes desde Room DAO + Sincronización background con Firestore
    override fun obtenerLecturasPorMesFlow(mes: String): Flow<List<Lectura>> {
        sincronizarLecturasRemotas()
        return lecturaDao.obtenerLecturasPorMes(mes.trim())
    }

    // SSOT: Todas las lecturas desde Room DAO + Sincronización background con Firestore
    override fun obtenerTodasLasLecturasFlow(): Flow<List<Lectura>> {
        sincronizarLecturasRemotas()
        return lecturaDao.obtenerTodasLasLecturas()
    }

    private fun sincronizarFacturasRemotas() {
        val fRef = facturasRef ?: return
        scope.launch {
            fRef.addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val lista = snapshot.toObjects<FacturaGeneral>()
                    scope.launch {
                        for (factura in lista) {
                            try {
                                facturaDao.insertarFactura(factura)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }

    private fun sincronizarLecturasRemotas() {
        val lRef = lecturasRef ?: return
        scope.launch {
            lRef.addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val lista = snapshot.toObjects<Lectura>()
                    scope.launch {
                        for (lectura in lista) {
                            try {
                                lecturaDao.insertarLectura(lectura)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }
}
