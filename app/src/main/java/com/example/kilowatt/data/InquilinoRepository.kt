package com.example.kilowatt.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import javax.inject.Inject

class InquilinoRepository @Inject constructor(
    private val inquilinoDao: InquilinoDao,
    private val submedidorDao: SubmedidorDao,
) : IInquilinoRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val uid: String?
        get() = auth.currentUser?.uid

    private val inquilinosRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("inquilinos") }

    private val submedidoresRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("submedidores") }

    private val perfilRef
        get() = uid?.let { db.collection("usuarios").document(it).collection("perfil") }

    // Guardar Inquilino y Submedidor - SSOT: Local Room primero, luego Firestore
    override suspend fun guardarInquilinoYSubmedidor(
        nombreCompleto: String,
        telefono: String,
        nombreEspacio: String,
        esAreaComun: Boolean,
        pagaAreaComun: Boolean,
    ): Pair<Boolean, String?> {
        val idSubmedidorTemp = "sub_${System.currentTimeMillis()}"
        val idInquilinoTemp = if (esAreaComun) "" else "inq_${System.currentTimeMillis()}"

        val subTemp = Submedidor(
            idSubmedidor = idSubmedidorTemp,
            nombreEspacio = nombreEspacio,
            esAreaComun = esAreaComun,
            pagaAreaComun = if (esAreaComun) false else pagaAreaComun,
            idInquilinoTitular = if (esAreaComun) null else idInquilinoTemp,
        )
        val inqTemp = if (esAreaComun) null else Inquilino(
            idInquilino = idInquilinoTemp,
            nombreCompleto = nombreCompleto,
            telefonoWhatsapp = telefono,
        )

        try {
            submedidorDao.insertarSubmedidor(subTemp)
            if (inqTemp != null) {
                inquilinoDao.insertarInquilino(inqTemp)
            }
        } catch (_: Exception) {}

        return suspendCancellableCoroutine { continuation ->
            try {
                val inqRef = inquilinosRef
                val subRef = submedidoresRef

                if (inqRef == null || subRef == null) {
                    if (continuation.isActive) continuation.resume(Pair(true, null))
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
    }

    // SSOT: Inquilinos desde Room DAO + Sincronización background con Firestore
    override fun obtenerInquilinosFlow(): Flow<List<Inquilino>> {
        sincronizarInquilinosRemotos()
        return inquilinoDao.obtenerTodosLosInquilinos()
    }

    // Actualizar cambios en Inquilino y Submedidor - SSOT: Local Room primero, luego Firestore
    override fun actualizarInquilinoYSubmedidor(
        inquilino: Inquilino?,
        submedidor: Submedidor,
        onComplete: (Boolean, String?) -> Unit,
    ) {
        scope.launch {
            try {
                submedidorDao.insertarSubmedidor(submedidor)
                if (inquilino != null) {
                    inquilinoDao.insertarInquilino(inquilino)
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
            if (inquilino != null && inqRef != null && inquilino.idInquilino.isNotEmpty()) {
                batch.set(inqRef.document(inquilino.idInquilino), inquilino)
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
    override suspend fun guardarPerfilPropietario(nombre: String, numero: String, diaLimite: Int): Pair<Boolean, String?> =
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
    override fun obtenerPerfilPropietarioFlow(): Flow<Map<String, Any>?> = kotlinx.coroutines.flow.callbackFlow {
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

    private fun sincronizarSubmedidoresRemotos() {
        val currentUid = auth.currentUser?.uid ?: return
        val subRef = submedidoresRef ?: return
        scope.launch {
            subRef.addSnapshotListener { snapshot, error ->
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
    }

    private fun sincronizarInquilinosRemotos() {
        val currentUid = auth.currentUser?.uid ?: return
        val inqRef = inquilinosRef ?: return
        scope.launch {
            inqRef.addSnapshotListener { snapshot, error ->
                if (auth.currentUser?.uid != currentUid) return@addSnapshotListener
                if (error == null && snapshot != null) {
                    val lista = snapshot.toObjects<Inquilino>()
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
}
