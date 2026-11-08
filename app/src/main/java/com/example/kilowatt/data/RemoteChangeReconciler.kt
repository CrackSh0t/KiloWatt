package com.example.kilowatt.data

import com.google.firebase.firestore.DocumentChange

internal object RemoteChangeReconciler {
    fun debeAplicarCambio(
        tipo: DocumentChange.Type,
        uidCoincide: Boolean,
        tieneOperacionLocal: Boolean = false,
    ): Boolean = uidCoincide && (tipo != DocumentChange.Type.REMOVED || !tieneOperacionLocal)
}
