package com.example.kilowatt.data

import com.google.firebase.firestore.DocumentChange

internal object RemoteChangeReconciler {
    fun debeAplicarCambio(
        @Suppress("UNUSED_PARAMETER") tipo: DocumentChange.Type,
        uidCoincide: Boolean,
        tieneOperacionLocal: Boolean = false,
    ): Boolean = uidCoincide && !tieneOperacionLocal
}
