package com.example.kilowatt.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class InquilinoRepository @Inject constructor(
    private val inquilinoDao: InquilinoDao,
    private val submedidorDao: SubmedidorDao,
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
        } catch (_: Exception) {}
    }

    // Guardar Inquilino y Submedidor - SSOT: Local Room primero con IDs unificados de Firestore
    override suspend fun guardarInquilinoYSubmedidor(
        nombreCompleto: String,
        telefono: String,
        nombreEspacio: String,
        esAreaComun: Boolean,
        pagaAreaComun: Boolean,
        numeroPersonas: Int,
    ): Pair<Boolean, String?> {
        val inqRef = inquilinosRef
        val subRef = submedidoresRef

        val idSubmedidorReal = subRef?.document()?.id ?: "sub_${System.currentTimeMillis()}"
        val idInquilinoReal = if (esAreaComun) "" else (inqRef?.document()?.id ?: "inq_${System.currentTimeMillis()}")

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
            submedidorDao.insertarSubmedidor(subTemp)
            if (inqTemp != null) {
                inquilinoDao.insertarInquilino(inqTemp)
            }
        } catch (_: Exception) {}

        return suspendCancellableCoroutine { continuation ->
            try {
                if (inqRef == null || subRef == null) {
                    if (continuation.isActive) continuation.resume(Pair(true, null))
                    return@suspendCancellableCoroutine
                }

                val batch = db.batch()

                if (esAreaComun) {
                    batch.set(subRef.document(idSubmedidorReal), subTemp)
                } else {
                    if (inqTemp != null) {
                        batch.set(inqRef.document(idInquilinoReal), inqTemp)
                    }
                    batch.set(subRef.document(idSubmedidorReal), subTemp)
                }

                batch.commit()
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

    // SSOT: Submedidores desde Room DAO + Sincronización background con Firestore
    override fun obtenerSubmedidoresFlow(): Flow<List<Submedidor>> {
        sincronizarSubmedidoresRemotos()
        return submedidorDao.obtenerTodosLosSubmedidores()
            .catch { emit(emptyList()) }
    }

    // SSOT: Inquilinos desde Room DAO + Sincronización background con Firestore
    override fun obtenerInquilinosFlow(): Flow<List<Inquilino>> {
        sincronizarInquilinosRemotos()
        return inquilinoDao.obtenerTodosLosInquilinos()
            .catch { emit(emptyList()) }
    }

    // Actualizar cambios en Inquilino y Submedidor - SSOT: Local Room primero, luego Firestore
    override fun actualizarInquilinoYSubmedidor(
        inquilino: Inquilino?,
        submedidor: Submedidor,
        onComplete: (Boolean, String?) -> Unit,
    ) {
        val inquilinoSanitizado = inquilino?.copy(
            numeroPersonas = inquilino.numeroPersonas.coerceAtLeast(1)
        )
        scope.launch {
            try {
                submedidorDao.insertarSubmedidor(submedidor)
                if (inquilinoSanitizado != null) {
                    inquilinoDao.insertarInquilino(inquilinoSanitizado)
                }
            } catch (_: Exception) {}
        }

        try {
            val inqRef = inquilinosRef
            val subRef = submedidoresRef

            if (subRef == null) {
                onComplete(true, null)
                return
            }

            val batch = db.batch()
            batch.set(subRef.document(submedidor.idSubmedidor), submedidor)
            if (inquilinoSanitizado != null && inqRef != null && inquilinoSanitizado.idInquilino.isNotEmpty()) {
                batch.set(inqRef.document(inquilinoSanitizado.idInquilino), inquilinoSanitizado)
            }

            batch.commit()
                .addOnSuccessListener { onComplete(true, null) }
                .addOnFailureListener { e -> onComplete(true, "Guardado localmente. Error en nube: ${e.localizedMessage}") }
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage)
        }
    }

    // Eliminar registros - SSOT: Local Room primero, luego Firestore
    override fun eliminarInquilinoYSubmedidor(
        inquilino: Inquilino?,
        submedidor: Submedidor,
        onComplete: (Boolean, String?) -> Unit,
    ) {
        scope.launch {
            try {
                submedidorDao.eliminarSubmedidor(submedidor)
                if (inquilino != null) {
                    inquilinoDao.eliminarInquilino(inquilino)
                }
            } catch (_: Exception) {}
        }

        try {
            val inqRef = inquilinosRef
            val subRef = submedidoresRef

            if (subRef == null) {
                onComplete(true, null)
                return
            }

            val batch = db.batch()
            batch.delete(subRef.document(submedidor.idSubmedidor))
            if (inquilino != null && inqRef != null && inquilino.idInquilino.isNotEmpty()) {
                batch.delete(inqRef.document(inquilino.idInquilino))
            }

            batch.commit()
                .addOnSuccessListener { onComplete(true, null) }
                .addOnFailureListener { e -> onComplete(true, "Eliminado localmente. Error en nube: ${e.localizedMessage}") }
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage)
        }
    }

    // Guardar perfil del propietario en Firestore (Suspend)
    override suspend fun guardarPerfilPropietario(
        nombre: String,
        numero: String,
        diaLimite: Int,
        tipoProrrateoComun: String,
    ): Pair<Boolean, String?> =
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
                    "tipoProrrateoComun" to tipoProrrateoComun,
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
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snapshot?.data)
        }
        awaitClose {
            try {
                perfilListener?.remove()
                perfilListener = null
            } catch (_: Exception) {}
        }
    }.catch { emit(null) }

    private fun sincronizarSubmedidoresRemotos() {
        val currentUid = auth.currentUser?.uid ?: return
        val subRef = submedidoresRef ?: return
        if (submedidoresListener != null) return

        submedidoresListener = subRef.addSnapshotListener { snapshot, error ->
            if (auth.currentUser?.uid != currentUid) return@addSnapshotListener
            if (error == null && snapshot != null) {
                val lista = snapshot.toObjects<Submedidor>()
                scope.launch {
                    for (sub in lista) {
                        try {
                            submedidorDao.insertarSubmedidor(sub)
                        } catch (_: Exception) {}
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
            if (error == null && snapshot != null) {
                val lista = snapshot.toObjects<Inquilino>().map { inq ->
                    inq.copy(numeroPersonas = inq.numeroPersonas.coerceAtLeast(1))
                }
                scope.launch {
                    for (inq in lista) {
                        try {
                            inquilinoDao.insertarInquilino(inq)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }
}
