package com.example.kilowatt

import com.example.kilowatt.data.RemoteChangeReconciler
import com.google.firebase.firestore.DocumentChange
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteChangeReconcilerTest {
    @Test
    fun addedSeAplicaCuandoUidCoincideYSinOperacionLocal() {
        assertTrue(RemoteChangeReconciler.debeAplicarCambio(DocumentChange.Type.ADDED, uidCoincide = true, tieneOperacionLocal = false))
    }

    @Test
    fun modifiedSeAplicaCuandoUidCoincideYSinOperacionLocal() {
        assertTrue(RemoteChangeReconciler.debeAplicarCambio(DocumentChange.Type.MODIFIED, uidCoincide = true, tieneOperacionLocal = false))
    }

    @Test
    fun addedSeBloqueaSiExisteOperacionLocal() {
        assertFalse(
            RemoteChangeReconciler.debeAplicarCambio(
                DocumentChange.Type.ADDED,
                uidCoincide = true,
                tieneOperacionLocal = true,
            ),
        )
    }

    @Test
    fun modifiedSeBloqueaSiExisteOperacionLocal() {
        assertFalse(
            RemoteChangeReconciler.debeAplicarCambio(
                DocumentChange.Type.MODIFIED,
                uidCoincide = true,
                tieneOperacionLocal = true,
            ),
        )
    }

    @Test
    fun removedSeBloqueaSiExisteOperacionLocal() {
        assertFalse(
            RemoteChangeReconciler.debeAplicarCambio(
                DocumentChange.Type.REMOVED,
                uidCoincide = true,
                tieneOperacionLocal = true,
            ),
        )
    }

    @Test
    fun cualquierCambioSeIgnoraSiCambiaLaSesion() {
        assertFalse(RemoteChangeReconciler.debeAplicarCambio(DocumentChange.Type.REMOVED, uidCoincide = false))
        assertFalse(RemoteChangeReconciler.debeAplicarCambio(DocumentChange.Type.ADDED, uidCoincide = false))
        assertFalse(RemoteChangeReconciler.debeAplicarCambio(DocumentChange.Type.MODIFIED, uidCoincide = false))
    }
}
