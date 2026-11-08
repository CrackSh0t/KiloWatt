# Auditoría técnica — DT-08 Iteración 3

**Fecha:** 2026-11-08  
**Auditor:** Agente Auditor  
**Alcance:** reconciliación Firestore–Room, bajas remotas, precedencia de outbox, aislamiento multi-tenant, pruebas y Hook de Gradle.  
**Referencia:** `docs/Propuestas/Implementadas/Resumen_Impl_DT_Iteracion_3.md`.

## 1. Procesamiento individual de cambios remotos

**Resultado: Cumple.**

Los listeners recorren `snapshot.documentChanges` y procesan cada cambio de
forma individual. `InquilinoRepository` maneja `ADDED`/`MODIFIED` mediante
inserción y `REMOVED` mediante `submedidorDao.eliminarPorId` e
`inquilinoDao.eliminarPorId` (`InquilinoRepository.kt:260-294,309-347`).
`LecturaRepository` aplica el mismo contrato a facturas y lecturas, incluyendo
los borrados por `documentId` (`LecturaRepository.kt:145-180,194-229`).

## 2. Precedencia de la outbox ante bajas remotas

**Resultado: Cumple.**

`PendingOperationDao.existeOperacionPendiente` consulta por `uid`,
`entityType`, `documentId` y estados `PENDING`, `PROCESSING` o `FAILED`
(`PendingOperationDao.kt:47-62`). Antes de cualquier inserción o borrado, cada
listener consulta esta operación y pasa el resultado a
`RemoteChangeReconciler.debeAplicarCambio`. Una notificación `REMOVED` con
operación local pendiente se bloquea, se conserva el registro local y se
registra el conflicto con entidad, documento y UID
(`InquilinoRepository.kt:264-275,313-324`;
`LecturaRepository.kt:149-160,198-209`).

La regla central queda expresada en
`RemoteChangeReconciler.kt:5-10`: el cambio solo se aplica con UID válido y un
`REMOVED` se rechaza si existe una operación local.

## 3. Aislamiento estricto por UID

**Resultado: Cumple.**

Cada listener captura el UID al suscribirse y descarta inmediatamente cualquier
callback cuya sesión activa ya no coincida
(`InquilinoRepository.kt:255,304`; `LecturaRepository.kt:140,189`).
Además, cada `scope.launch` vuelve a validar `auth.currentUser?.uid` antes de
consultar la outbox o escribir en Room
(`InquilinoRepository.kt:263,312`;
`LecturaRepository.kt:148,197`). Esto cubre la ventana de carrera entre el
callback de Firestore y la ejecución asíncrona.

## 4. Pruebas y ejecución del Hook

**Resultado: Cumple.**

`RemoteChangeReconcilerTest` verifica:

- aplicación de `ADDED` con UID coincidente;
- aplicación de `MODIFIED` con UID coincidente;
- bloqueo de `REMOVED` cuando existe una operación local;
- rechazo de cambios cuando cambia la sesión
  (`app/src/test/java/com/example/kilowatt/RemoteChangeReconcilerTest.kt:9-35`).

El Hook configurado en `.github/hooks/run-unit-tests.json` ejecuta:

```text
./gradlew :app:testDebugUnitTest
```

La ejecución formal de auditoría finalizó en verde:

```text
BUILD SUCCESSFUL in 11s
40 actionable tasks: 40 up-to-date
```

## 5. Matriz de verificación

| # | Criterio | Evidencia principal | Resultado |
|---|---|---|---|
| 1 | Procesamiento individual de `REMOVED` | `InquilinoRepository.kt:260-294,309-347`; `LecturaRepository.kt:145-180,194-229` | ✅ Cumple |
| 2 | Precedencia de outbox | `PendingOperationDao.kt:47-62`; `RemoteChangeReconciler.kt:5-10` | ✅ Cumple |
| 3 | UID y aislamiento multi-tenant | Validaciones en callbacks y corrutinas de ambos repositorios | ✅ Cumple |
| 4 | Tests y Hook en verde | `RemoteChangeReconcilerTest.kt`; `.github/hooks/run-unit-tests.json` | ✅ Cumple |

## Dictamen formal

**APROBADA.**

Los cuatro criterios de la auditoría DT-08 están implementados y respaldados
por código verificable y pruebas unitarias. Las bajas remotas se reconcilian
por identificador, no destruyen cambios locales pendientes, los callbacks
quedan aislados por UID y la suite requerida finaliza correctamente.
