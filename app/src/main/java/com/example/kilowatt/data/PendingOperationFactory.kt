package com.example.kilowatt.data

import org.json.JSONObject
import java.util.UUID

object PendingOperationFactory {
    fun crear(
        uid: String,
        entityType: String,
        operationType: PendingOperationType,
        documentId: String,
        payload: JSONObject,
    ) = PendingOperationEntity(
        id = UUID.randomUUID().toString(),
        uid = uid,
        entityType = entityType,
        operationType = operationType,
        documentId = documentId,
        payloadJson = payload.toString(),
        timestamp = System.currentTimeMillis(),
    )

    fun inquilino(inquilino: Inquilino) = JSONObject()
        .put("idInquilino", inquilino.idInquilino)
        .put("nombreCompleto", inquilino.nombreCompleto)
        .put("telefonoWhatsapp", inquilino.telefonoWhatsapp)
        .put("numeroPersonas", inquilino.numeroPersonas)

    fun submedidor(submedidor: Submedidor) = JSONObject()
        .put("idSubmedidor", submedidor.idSubmedidor)
        .put("nombreEspacio", submedidor.nombreEspacio)
        .put("esAreaComun", submedidor.esAreaComun)
        .put("pagaAreaComun", submedidor.pagaAreaComun)
        .put("idInquilinoTitular", submedidor.idInquilinoTitular)

    fun factura(factura: FacturaGeneral) = JSONObject()
        .put("idFactura", factura.idFactura)
        .put("mesPeriodo", factura.mesPeriodo)
        .put("kwhTotalesRecibo", factura.kwhTotalesRecibo)
        .put("montoTotalSoles", factura.montoTotalSoles)

    fun lectura(lectura: Lectura) = JSONObject()
        .put("idLectura", lectura.idLectura)
        .put("idSubmedidor", lectura.idSubmedidor)
        .put("mesPeriodo", lectura.mesPeriodo)
        .put("lecturaAnterior", lectura.lecturaAnterior)
        .put("lecturaActual", lectura.lecturaActual)
        .put("consumoKwh", lectura.consumoKwh)
        .put("montoPagarSoles", lectura.montoPagarSoles)
        .put("estadoPago", lectura.estadoPago)
        .put("fechaPago", lectura.fechaPago)
        .put("metodoPago", lectura.metodoPago)

    fun actualizacionPago(estadoPago: String, fechaPago: Long?, metodoPago: String?) = JSONObject()
        .put("estadoPago", estadoPago)
        .put("fechaPago", fechaPago)
        .put("metodoPago", metodoPago)
}
