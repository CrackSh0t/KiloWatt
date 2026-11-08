# Auditoría técnica — DT-07 Iteración 2

**Fecha de re-auditoría:** 2026-11-08  
**Auditor:** Agente Auditor  
**Alcance:** recuperación de la outbox, actualización parcial de pagos, composición Hilt del Worker, migraciones Room, pruebas y Hook de Gradle.  
**Referencia:** subsanación documentada en `Resumen_Impl_DT_Iteracion_2.md`.

## 1. Recuperación de operaciones `PROCESSING`

**Resultado: Cumple.**

`PendingOperationDao.obtenerPendientes` incluye las operaciones `PENDING` y
las `PROCESSING` cuyo `timestamp` es menor o igual al umbral recibido
(`app/src/main/java/com/example/kilowatt/data/PendingOperationDao.kt:13-29`).
`SyncWorker` calcula el umbral como el instante actual menos
`PROCESSING_TIMEOUT_MS`, definido en cinco minutos
(`app/src/main/java/com/example/kilowatt/worker/SyncWorker.kt:33-35,75-77`).
Al iniciar un intento, `marcarIntento` actualiza estado, contador, motivo y
timestamp, evitando que una operación recién tomada sea recuperada
prematuramente (`PendingOperationDao.kt:34-42`).

La prueba instrumentada `PendingOperationDaoTest` verifica inserción,
transición a `PROCESSING`, exclusión antes del timeout y recuperación después
del timeout (`app/src/androidTest/java/com/example/kilowatt/PendingOperationDaoTest.kt:39-58`).

## 2. Actualización parcial del estado de pago

**Resultado: Cumple.**

`LecturaRepository.actualizarEstadoPago` registra en la outbox únicamente el
payload de pago generado por `PendingOperationFactory.actualizacionPago`
(`app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt:92-108`).
Para una actualización de `LECTURA`, `SyncWorker` usa
`reference.update(payload.toPaymentUpdateMap())`; el mapa explícito contiene
solo `estadoPago`, `fechaPago` y `metodoPago`
(`app/src/main/java/com/example/kilowatt/worker/SyncWorker.kt:48-57,69-73`).
No se serializan `idSubmedidor`, periodo, lecturas ni montos parciales, por lo
que no pueden sobrescribirse con valores por defecto.

## 3. Inyección Hilt y única composición del Worker

**Resultado: Cumple.**

`SyncWorker` está anotado con `@HiltWorker` y usa `@AssistedInject`; recibe
`PendingOperationDao`, `FirebaseAuth` y `FirebaseFirestore` por constructor
(`app/src/main/java/com/example/kilowatt/worker/SyncWorker.kt:19-26`).
No contiene llamadas a `AppDatabase.getDatabase()` ni crea directamente
instancias de Room, Auth o Firestore. La base provista por
`DatabaseModule` es singleton y sus DAO se inyectan desde esa misma instancia.

## 4. Migraciones Room sin fallback destructivo

**Resultado: Cumple.**

`AppDatabase.getDatabase` registra `MIGRATION_5_6` y `MIGRATION_6_7` y termina
la construcción sin `fallbackToDestructiveMigration`
(`app/src/main/java/com/example/kilowatt/data/AppDatabase.kt:55-63`).
`DatabaseModule.provideAppDatabase` aplica la misma política explícita
(`app/src/main/java/com/example/kilowatt/di/DatabaseModule.kt:24-35`).
Por tanto, ninguna de las dos rutas de construcción borra automáticamente la
outbox o los datos locales ante una versión no registrada.

## 5. Pruebas y ejecución del Hook

**Resultado: Cumple.**

La prueba instrumentada de Room cubre inserción, actualización de estado,
recuperación por timeout y eliminación. `SyncWorkerTest` cubre resolución de
UID, timeout de cinco minutos, eliminación después del éxito, reintento ante
fallo transitorio y marcado `FAILED` ante fallo permanente
(`app/src/test/java/com/example/kilowatt/SyncWorkerTest.kt:8-112`).

El Hook configurado en `.github/hooks/run-unit-tests.json` ejecuta:

```text
./gradlew :app:testDebugUnitTest
```

La ejecución de re-auditoría finalizó correctamente:

```text
BUILD SUCCESSFUL
40 actionable tasks: 40 up-to-date
```

## 6. Matriz de verificación

| # | Criterio | Evidencia principal | Resultado |
|---|---|---|---|
| 1 | Recuperación de `PROCESSING` tras cinco minutos | `PendingOperationDao.kt:13-29`, `SyncWorker.kt:33-35,75-77` | ✅ Cumple |
| 2 | UPDATE parcial de pago mediante mapa explícito | `LecturaRepository.kt:92-108`, `SyncWorker.kt:48-57,69-73` | ✅ Cumple |
| 3 | Worker gestionado por Hilt | `SyncWorker.kt:19-26` | ✅ Cumple |
| 4 | Sin migración destructiva | `AppDatabase.kt:55-63`, `DatabaseModule.kt:24-35` | ✅ Cumple |
| 5 | Hook unitario en verde y cobertura asociada | `.github/hooks/run-unit-tests.json`, pruebas DAO/Worker | ✅ Cumple |

## Dictamen formal

**APROBADA.**

Los cinco criterios de re-auditoría de DT-07 Iteración 2 están implementados
con evidencia verificable en código y pruebas. La suite requerida por el Hook
finaliza en verde y no se mantienen los bloqueos que motivaron el dictamen
original de **NO APROBADA**.
