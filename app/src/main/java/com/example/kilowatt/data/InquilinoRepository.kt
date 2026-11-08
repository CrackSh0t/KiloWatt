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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.room.withTransaction
import android.content.Context
import com.example.kilowatt.worker.OutboxScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume

class InquilinoRepository @Inject constructor(
    private val inquilinoDao: InquilinoDao,
    private val submedidorDao: SubmedidorDao,
    private val database: AppDatabase,
    @ApplicationContext private val context: Context,
) : IInquilinoRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    private var submedidoresListener: ListenerRegistration? = null
    private var inquilinosListener: ListenerRegistration? = null
    private var perfilListener: ListenerRegistration? = null

    private val uid: String?
        get() = auth.currentUser?.uid

    private val inquilinosRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("inquilinos") }

    private val submedidoresRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("submedidores") }

    private val perfilRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("perfil") }

    override fun cancelarListeners() {
        try {
            submedidoresListener?.remove()
            submedidoresListener = null
            inquilinosListener?.remove()
            inquilinosListener = null
            perfilListener?.remove()
            perfilListener = null
        } finally {
            submedidoresListener = null
            inquilinosListener = null
            perfilListener = null
        }
    }

    // Guardar Inquilino y Submedidor - SSOT: Local Room primero con IDs unificados de Firestore
    override suspend fun guardarInquilinoYSubmedidor(
        nombreCompleto: String,
        telefono: String,
        nombreEspacio: String,
        esAreaComun: Boolean,
        pagaAreaComun: Boolean,
        numeroPersonas: Int,
    ): OperationResult<Unit> {
        val currentUid = uid ?: return OperationResult.Failed(
            IllegalStateException("Usuario no autenticado"),
            "Usuario no autenticado",
        )

        val idSubmedidorReal = "sub_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val idInquilinoReal = if (esAreaComun) "" else "inq_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"

        val subTemp = Submedidor(
            idSubmedidor = idSubmedidorReal,
            nombreEspacio = nombreEspacio,
            esAreaComun = esAreaComun,
            pagaAreaComun = if (esAreaComun) false else pagaAreaComun,
            idInquilinoTitular = if (esAreaComun) null else idInquilinoReal,
        )
        val inqTemp = if (esAreaComun) null else Inquilino(
            idInquilino = idInquilinoReal,
            nombreCompleto = nombreCompleto,
            telefonoWhatsapp = telefono,
            numeroPersonas = numeroPersonas.coerceAtLeast(1),
        )

        try {
            database.withTransaction {
                submedidorDao.insertarSubmedidor(subTemp)
                database.pendingOperationDao().insertar(
                    PendingOperationFactory.crear(
                        currentUid, "SUBMEDIDOR", PendingOperationType.CREATE,
                        idSubmedidorReal, PendingOperationFactory.submedidor(subTemp),
                    ),
                )
                if (inqTemp != null) {
                    inquilinoDao.insertarInquilino(inqTemp)
                    database.pendingOperationDao().insertar(
                        PendingOperationFactory.crear(
                            currentUid, "INQUILINO", PendingOperationType.CREATE,
                            idInquilinoReal, PendingOperationFactory.inquilino(inqTemp),
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            return OperationResult.Failed(e, "No se pudo guardar localmente: ${e.localizedMessage}")
        }
        OutboxScheduler.programar(context, currentUid)
        return OperationResult.PendingSync(Unit, "Guardado localmente; pendiente de sincronización")
    }

    // SSOT: Submedidores desde Room DAO + Sincronización background con Firestore
    override fun obtenerSubmedidoresFlow(): Flow<List<Submedidor>> {
        sincronizarSubmedidoresRemotos()
        return submedidorDao.obtenerTodosLosSubmedidores()
    }

    // SSOT: Inquilinos desde Room DAO + Sincronización background con Firestore
    override fun obtenerInquilinosFlow(): Flow<List<Inquilino>> {
        sincronizarInquilinosRemotos()
        return inquilinoDao.obtenerTodosLosInquilinos()
    }

    // Actualizar cambios en Inquilino y Submedidor - SSOT: Local Room primero, luego Firestore
    override suspend fun actualizarInquilinoYSubmedidor(
        inquilino: Inquilino?,
        submedidor: Submedidor,
    ): OperationResult<Unit> {
        val inquilinoSanitizado = inquilino?.copy(
            numeroPersonas = inquilino.numeroPersonas.coerceAtLeast(1)
        )
        val currentUid = uid ?: return OperationResult.Failed(IllegalStateException("Usuario no autenticado"), "Usuario no autenticado")
        try {
            database.withTransaction {
                submedidorDao.insertarSubmedidor(submedidor)
                database.pendingOperationDao().insertar(PendingOperationFactory.crear(currentUid, "SUBMEDIDOR", PendingOperationType.UPDATE, submedidor.idSubmedidor, PendingOperationFactory.submedidor(submedidor)))
                if (inquilinoSanitizado != null) {
                    inquilinoDao.insertarInquilino(inquilinoSanitizado)
                    database.pendingOperationDao().insertar(PendingOperationFactory.crear(currentUid, "INQUILINO", PendingOperationType.UPDATE, inquilinoSanitizado.idInquilino, PendingOperationFactory.inquilino(inquilinoSanitizado)))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("InquilinoRepository", "entidad=Submedidor/Inquilino operación=actualizar id=${submedidor.idSubmedidor} uid=${uid ?: "anonimo"}", e)
            return OperationResult.Failed(e, "No se pudo actualizar localmente: ${e.localizedMessage}")
        }

        OutboxScheduler.programar(context, currentUid)
        return OperationResult.PendingSync(Unit, "Actualizado localmente; pendiente de sincronización")
    }

    // Eliminar registros - SSOT: Local Room primero, luego Firestore
    override suspend fun eliminarInquilinoYSubmedidor(
        inquilino: Inquilino?,
        submedidor: Submedidor,
    ): OperationResult<Unit> {
        val currentUid = uid ?: return OperationResult.Failed(IllegalStateException("Usuario no autenticado"), "Usuario no autenticado")
        try {
            database.withTransaction {
                submedidorDao.eliminarSubmedidor(submedidor)
                database.pendingOperationDao().insertar(PendingOperationFactory.crear(currentUid, "SUBMEDIDOR", PendingOperationType.DELETE, submedidor.idSubmedidor, PendingOperationFactory.submedidor(submedidor)))
                if (inquilino != null) {
                    inquilinoDao.eliminarInquilino(inquilino)
                    database.pendingOperationDao().insertar(PendingOperationFactory.crear(currentUid, "INQUILINO", PendingOperationType.DELETE, inquilino.idInquilino, PendingOperationFactory.inquilino(inquilino)))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("InquilinoRepository", "entidad=Submedidor/Inquilino operación=eliminar id=${submedidor.idSubmedidor} uid=${uid ?: "anonimo"}", e)
            return OperationResult.Failed(e, "No se pudo eliminar localmente: ${e.localizedMessage}")
        }

        OutboxScheduler.programar(context, currentUid)
        return OperationResult.PendingSync(Unit, "Eliminado localmente; pendiente de sincronización")
    }

    // Guardar perfil del propietario en Firestore (Suspend)
    override suspend fun guardarPerfilPropietario(
        nombre: String,
        numero: String,
        diaLimite: Int,
        tipoProrrateoComun: String,
    ): OperationResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            try {
                val pRef = perfilRef
                if (pRef == null) {
                    if (continuation.isActive) continuation.resume(OperationResult.Failed(IllegalStateException("Usuario no autenticado"), "Usuario no autenticado"))
                    return@suspendCancellableCoroutine
                }

                val perfil = mapOf(
                    "nombrePropietario" to nombre,
                    "numeroPago" to numero,
                    "diaLimitePago" to diaLimite,
                    "tipoProrrateoComun" to tipoProrrateoComun,
                )
                pRef.document("datos_pago")
                    .set(perfil)
                    .addOnSuccessListener {
                        if (continuation.isActive) continuation.resume(OperationResult.Synced(Unit))
                    }
                    .addOnFailureListener { e ->
                        if (continuation.isActive) continuation.resume(OperationResult.Failed(e, e.localizedMessage ?: "Error al guardar perfil"))
                    }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(OperationResult.Failed(e, e.localizedMessage ?: "Error al guardar perfil"))
            }
        }

    // Obtener perfil del propietario en tiempo real
    override fun obtenerPerfilPropietarioFlow(): Flow<Map<String, Any>?> = kotlinx.coroutines.flow.callbackFlow {
        val currentUid = auth.currentUser?.uid
        val pRef = perfilRef
        if (pRef == null || currentUid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        perfilListener = pRef.document("datos_pago").addSnapshotListener { snapshot, error ->
            if (auth.currentUser?.uid != currentUid) return@addSnapshotListener
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.data)
        }
        awaitClose {
            try {
                perfilListener?.remove()
                perfilListener = null
            } catch (e: Exception) {
                android.util.Log.e("InquilinoRepository", "Error al cerrar listener de perfil", e)
            }
        }
    }

    private fun sincronizarSubmedidoresRemotos() {
        val currentUid = auth.currentUser?.uid ?: return
        val subRef = submedidoresRef ?: return
        if (submedidoresListener != null) return

        submedidoresListener = subRef.addSnapshotListener { snapshot, error ->
            if (auth.currentUser?.uid != currentUid) return@addSnapshotListener
            if (error != null) {
                android.util.Log.e("InquilinoRepository", "entidad=Submedidor operación=sincronizar id=coleccion uid=$currentUid", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                snapshot.documentChanges.forEach { change ->
                    scope.launch {
                        if (auth.currentUser?.uid != currentUid) return@launch
                        val documentId = change.document.id
                        val tieneOperacionLocal = database.pendingOperationDao().existeOperacionPendiente(
                            currentUid,
                            "SUBMEDIDOR",
                            documentId,
                        )
                        if (!RemoteChangeReconciler.debeAplicarCambio(change.type, true, tieneOperacionLocal)) {
                            android.util.Log.w(
                                "InquilinoRepository",
                                "conflicto=REMOVED entidad=Submedidor id=$documentId uid=$currentUid operación_local=true",
                            )
                            return@launch
                        }
                        try {
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val submedidor = change.document.toObject<Submedidor>()
                                    if (submedidor != null) submedidorDao.insertarSubmedidor(submedidor)
                                }
                                DocumentChange.Type.REMOVED -> submedidorDao.eliminarPorId(documentId)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "InquilinoRepository",
                                "Entidad=Submedidor operación=${change.type} id=$documentId uid=$currentUid",
                                e,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sincronizarInquilinosRemotos() {
        val currentUid = auth.currentUser?.uid ?: return
        val inqRef = inquilinosRef ?: return
        if (inquilinosListener != null) return

        inquilinosListener = inqRef.addSnapshotListener { snapshot, error ->
            if (auth.currentUser?.uid != currentUid) return@addSnapshotListener
            if (error != null) {
                android.util.Log.e("InquilinoRepository", "entidad=Inquilino operación=sincronizar id=coleccion uid=$currentUid", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                snapshot.documentChanges.forEach { change ->
                    scope.launch {
                        if (auth.currentUser?.uid != currentUid) return@launch
                        val documentId = change.document.id
                        val tieneOperacionLocal = database.pendingOperationDao().existeOperacionPendiente(
                            currentUid,
                            "INQUILINO",
                            documentId,
                        )
                        if (!RemoteChangeReconciler.debeAplicarCambio(change.type, true, tieneOperacionLocal)) {
                            android.util.Log.w(
                                "InquilinoRepository",
                                "conflicto=REMOVED entidad=Inquilino id=$documentId uid=$currentUid operación_local=true",
                            )
                            return@launch
                        }
                        try {
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val inquilino = change.document.toObject<Inquilino>()
                                    if (inquilino != null) {
                                        inquilinoDao.insertarInquilino(
                                            inquilino.copy(numeroPersonas = inquilino.numeroPersonas.coerceAtLeast(1)),
                                        )
                                    }
                                }
                                DocumentChange.Type.REMOVED -> inquilinoDao.eliminarPorId(documentId)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "InquilinoRepository",
                                "Entidad=Inquilino operación=${change.type} id=$documentId uid=$currentUid",
                                e,
                            )
                        }
                    }
                }
            }
        }
    }
}
