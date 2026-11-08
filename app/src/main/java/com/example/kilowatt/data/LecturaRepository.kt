package com.example.kilowatt.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import com.example.kilowatt.domain.OperationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.room.withTransaction
import android.content.Context
import com.example.kilowatt.worker.OutboxScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume

class LecturaRepository @Inject constructor(
    private val lecturaDao: LecturaDao,
    private val facturaDao: FacturaGeneralDao,
    private val database: AppDatabase,
    @ApplicationContext private val context: Context,
) : ILecturaRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    private var facturasListener: ListenerRegistration? = null
    private var lecturasListener: ListenerRegistration? = null

    private val uid: String?
        get() = auth.currentUser?.uid

    private val facturasRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("facturas") }

    private val lecturasRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("lecturas") }

    private fun sanitizeDocId(docId: String): String = docId.trim().replace("/", "_").replace("\\", "_")

    override fun cancelarListeners() {
        try {
            facturasListener?.remove()
            facturasListener = null
            lecturasListener?.remove()
            lecturasListener = null
        } finally {
            facturasListener = null
            lecturasListener = null
        }
    }

    // Guardar / Actualizar Recibo Base (Factura General) - SSOT: Local Room primero, luego Firestore
    override suspend fun guardarFactura(factura: FacturaGeneral): OperationResult<Unit> {
        val currentUid = uid ?: return OperationResult.Failed(IllegalStateException("Usuario no autenticado"), "Usuario no autenticado")
        try {
            database.withTransaction {
                facturaDao.insertarFactura(factura)
                database.pendingOperationDao().insertar(
                    PendingOperationFactory.crear(currentUid, "FACTURA", PendingOperationType.CREATE, sanitizeDocId(factura.mesPeriodo), PendingOperationFactory.factura(factura)),
                )
            }
        } catch (e: Exception) {
            return OperationResult.Failed(e, "No se pudo guardar la factura localmente: ${e.localizedMessage}")
        }
        OutboxScheduler.programar(context, currentUid)
        return OperationResult.PendingSync(Unit, "Factura guardada localmente; pendiente de sincronización")
    }

    // Guardar / Actualizar Lectura de un submedidor - SSOT: Local Room primero, luego Firestore
    override suspend fun guardarLectura(lectura: Lectura): OperationResult<Unit> {
        val currentUid = uid ?: return OperationResult.Failed(IllegalStateException("Usuario no autenticado"), "Usuario no autenticado")
        try {
            database.withTransaction {
                lecturaDao.insertarLectura(lectura)
                database.pendingOperationDao().insertar(
                    PendingOperationFactory.crear(currentUid, "LECTURA", PendingOperationType.CREATE, sanitizeDocId(lectura.idLectura), PendingOperationFactory.lectura(lectura)),
                )
            }
        } catch (e: Exception) {
            return OperationResult.Failed(e, "No se pudo guardar la lectura localmente: ${e.localizedMessage}")
        }
        OutboxScheduler.programar(context, currentUid)
        return OperationResult.PendingSync(Unit, "Lectura guardada localmente; pendiente de sincronización")
    }

    // Actualizar el estado y auditoría de pago - SSOT: Local Room primero, luego Firestore
    override suspend fun actualizarEstadoPago(
        idLectura: String,
        nuevoEstado: String,
        fechaPago: Long?,
        metodoPago: String?,
    ): OperationResult<Unit> {
        val currentUid = uid ?: return OperationResult.Failed(IllegalStateException("Usuario no autenticado"), "Usuario no autenticado")
        try {
            database.withTransaction {
                lecturaDao.actualizarEstadoPago(idLectura, nuevoEstado, fechaPago, metodoPago)
                val payload = PendingOperationFactory.actualizacionPago(nuevoEstado, fechaPago, metodoPago)
                database.pendingOperationDao().insertar(
                    PendingOperationFactory.crear(currentUid, "LECTURA", PendingOperationType.UPDATE, sanitizeDocId(idLectura), payload),
                )
            }
        } catch (e: Exception) {
            return OperationResult.Failed(e, "No se pudo actualizar el pago localmente: ${e.localizedMessage}")
        }
        OutboxScheduler.programar(context, currentUid)
        return OperationResult.PendingSync(Unit, "Pago actualizado localmente; pendiente de sincronización")
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
        val currentUid = auth.currentUser?.uid ?: return
        val fRef = facturasRef ?: return
        if (facturasListener != null) return

        facturasListener = fRef.addSnapshotListener { snapshot, error ->
            if (auth.currentUser?.uid != currentUid) return@addSnapshotListener
            if (error != null) {
                android.util.Log.e("LecturaRepository", "entidad=FacturaGeneral operación=sincronizar id=coleccion uid=$currentUid", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                snapshot.documentChanges.forEach { change ->
                    scope.launch {
                        if (auth.currentUser?.uid != currentUid) return@launch
                        val documentId = change.document.id
                        val tieneOperacionLocal = database.pendingOperationDao().existeOperacionPendiente(
                            currentUid,
                            "FACTURA",
                            documentId,
                        )
                        if (!RemoteChangeReconciler.debeAplicarCambio(change.type, true, tieneOperacionLocal)) {
                            android.util.Log.w(
                                "LecturaRepository",
                                "conflicto=REMOVED entidad=FacturaGeneral id=$documentId uid=$currentUid operación_local=true",
                            )
                            return@launch
                        }
                        try {
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val factura = change.document.toObject<FacturaGeneral>()
                                    if (factura != null) facturaDao.insertarFactura(factura)
                                }
                                DocumentChange.Type.REMOVED -> facturaDao.eliminarPorId(documentId)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "LecturaRepository",
                                "Entidad=FacturaGeneral operación=${change.type} id=$documentId uid=$currentUid",
                                e,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sincronizarLecturasRemotas() {
        val currentUid = auth.currentUser?.uid ?: return
        val lRef = lecturasRef ?: return
        if (lecturasListener != null) return

        lecturasListener = lRef.addSnapshotListener { snapshot, error ->
            if (auth.currentUser?.uid != currentUid) return@addSnapshotListener
            if (error != null) {
                android.util.Log.e("LecturaRepository", "entidad=Lectura operación=sincronizar id=coleccion uid=$currentUid", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                snapshot.documentChanges.forEach { change ->
                    scope.launch {
                        if (auth.currentUser?.uid != currentUid) return@launch
                        val documentId = change.document.id
                        val tieneOperacionLocal = database.pendingOperationDao().existeOperacionPendiente(
                            currentUid,
                            "LECTURA",
                            documentId,
                        )
                        if (!RemoteChangeReconciler.debeAplicarCambio(change.type, true, tieneOperacionLocal)) {
                            android.util.Log.w(
                                "LecturaRepository",
                                "conflicto=REMOVED entidad=Lectura id=$documentId uid=$currentUid operación_local=true",
                            )
                            return@launch
                        }
                        try {
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val lectura = change.document.toObject<Lectura>()
                                    if (lectura != null) lecturaDao.insertarLectura(lectura)
                                }
                                DocumentChange.Type.REMOVED -> lecturaDao.eliminarPorId(documentId)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "LecturaRepository",
                                "Entidad=Lectura operación=${change.type} id=$documentId uid=$currentUid",
                                e,
                            )
                        }
                    }
                }
            }
        }
    }
}
