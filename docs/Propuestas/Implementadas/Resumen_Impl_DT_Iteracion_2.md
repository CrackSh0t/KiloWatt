# Resumen de implementación — DT-07 Iteración 2

**Fecha:** Noviembre 2026  
**Estado:** Subsanada; pendiente de validación de integración contra Firestore real.

## Alcance

Se implementó una outbox persistente para que las escrituras locales puedan sobrevivir a la pérdida de conectividad y al cierre de la aplicación.

## Cambios realizados

- `PendingOperationEntity` almacena `id`, `uid`, `entityType`, `operationType`, `documentId`, `payloadJson`, `timestamp`, `attempts`, `status` y `errorReason`.
- `PendingOperationDao` permite insertar, consultar por UID, marcar intentos, eliminar operaciones confirmadas y purgar operaciones de una cuenta.
- Room se actualizó a la versión 7 mediante `MIGRATION_6_7`, que crea la tabla e índice de la outbox sin destruir datos existentes.
- `InquilinoRepository` y `LecturaRepository` usan `database.withTransaction` para agrupar la escritura local y el alta de la operación pendiente.
- Las operaciones locales devuelven `PendingSync` y programan trabajo único por UID con restricción `NetworkType.CONNECTED`.
- `SyncWorker` procesa CREATE/UPDATE/DELETE con IDs deterministas, elimina operaciones confirmadas, usa `Result.retry()` para fallos recuperables y `Result.failure()` para payloads o entidades inválidos.
- `PendingOperationDao` recupera operaciones `PROCESSING` cuyo timestamp supera cinco minutos y el worker actualiza el timestamp al iniciar cada intento.
- Las actualizaciones de pago serializan exclusivamente `estadoPago`, `fechaPago` y `metodoPago`; el worker envía un mapa parcial explícito sin sobrescribir los demás campos de `Lectura`.
- `SyncWorker` usa `@HiltWorker` y recibe por constructor el DAO, `FirebaseAuth` y `FirebaseFirestore`, compartiendo la instancia de Room provista por Hilt.
- Se eliminó `fallbackToDestructiveMigration()` de las dos rutas de construcción de Room.
- El logout ya purga la base Room completa, incluyendo la outbox.

## Pruebas

- Se añadieron pruebas instrumentadas con Room en memoria para inserción, transición a `PROCESSING`, recuperación por timeout y eliminación, además de pruebas unitarias de las decisiones de recuperación del worker.
- La suite requerida se valida con `./gradlew :app:testDebugUnitTest`; una ejecución con `--rerun-tasks` también se utilizó para descartar el EOF transitorio de caché.

## Limitaciones conocidas

- La integración contra Firestore real y la ejecución del DAO requieren una prueba instrumentada/emulador con autenticación, Room y red controladas; la suite unitaria cubre contratos, migración y decisiones de entrada del Worker.
- Las pruebas instrumentadas requieren un dispositivo o emulador conectado; la sincronización contra Firestore real requiere autenticación y red controladas.
