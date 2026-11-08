# Resumen de implementación — DT-08 Iteración 3

**Fecha:** Noviembre 2026  
**Estado:** Implementada; pendiente de validación contra Firestore real con dos clientes.

## Alcance

Se completó la reconciliación de cambios remotos Firestore–Room para evitar que
las bajas remotas dejen datos obsoletos y para proteger cambios locales todavía
pendientes de publicación.

## Cambios realizados

- `InquilinoRepository` procesa individualmente cambios `ADDED`, `MODIFIED` y
  `REMOVED` para inquilinos y submedidores.
- `LecturaRepository` aplica el mismo tratamiento para facturas y lecturas.
- Los DAOs de Room incorporan borrado por identificador de entidad.
- `PendingOperationDao` expone `existeOperacionPendiente` por UID, tipo de
  entidad y document ID.
- Una notificación `REMOVED` no elimina Room si existe una operación local
  `PENDING`, `PROCESSING` o `FAILED`; el conflicto se registra con contexto de
  entidad, documento y UID.
- Cada callback valida el UID capturado antes de programar trabajo y vuelve a
  validarlo dentro de la corrutina antes de leer la outbox o modificar Room.
- `RemoteChangeReconcilerTest` cubre `ADDED`, `MODIFIED`, `REMOVED` con conflicto
  local y cambio de sesión.

## Validación

La suite requerida se ejecuta mediante:

```text
./gradlew :app:testDebugUnitTest
```

La prueba instrumentada contra Room/Firestore real con dos usuarios queda como
validación de integración posterior; la lógica de decisión y protección de
conflictos está cubierta unitariamente.
