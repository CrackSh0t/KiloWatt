# Informe de Problemas Encontrados: Diagnóstico del Cierre Inesperado (Crash) tras Cerrar Sesión

**Autor:** Arquitecto de Software Senior  
**Fecha:** Octubre 2026  
**Proyecto:** Kilowatt Android  
**Estado:** Informe Técnico de Incidencia y Análisis de Causa Raíz  

---

## 1. Resumen de la Incidencia

A raíz de las pruebas de validación en dispositivo real (capturadas en video), se detectó un cierre inesperado de la aplicación ("KiloWatt continúa fallando") inmediatamente después de presionar el botón "Cerrar Sesión" en el diálogo de *Perfil del Propietario*.

---

## 2. Análisis de Causa Raíz (Diagnóstico Arquitectónico)

Tras la revisión exhaustiva del código fuente (`MainActivity.kt`, `AuthRepository.kt`, `InquilinoRepository.kt`, `LecturaRepository.kt`), se identificaron tres factores concurrentes que provocan el fallo:

1. **Listener No Registrado en `cancelarListeners()` (`obtenerPerfilPropietarioFlow`):**
   - `InquilinoRepository` gestionaba el flujo de perfil del propietario mediante un `callbackFlow` con un `addSnapshotListener` independiente.
   - Dicho listener no estaba incluido en el método `cancelarListeners()`, por lo que al cerrar sesión continuaba activo e intentando emitir eventos de Firestore en segundo plano.

2. **Emisiones de Room Invalidation Tracker durante `clearAllTables()`:**
   - Al ejecutar `database.clearAllTables()`, SQLite vacía todas las tablas locales. Sin embargo, las consultas reactivas activas (`Flow` de Room) reaccionan inmediatamente intentando consultar tablas vacías o en proceso de bloqueo transaccional, emitiendo estados hacia las Vistas que ya se encuentran en proceso de destrucción (`finish()`).

3. **Concurrencia de Hilos y Destrucción de Contexto:**
   - La combinación de callbacks de Firestore activos, desautorización de Firebase Auth (`auth.signOut()`), limpieza de preferencias (`SettingsManager`) y destrucción de actividades sin cancelación explícita de ámbitos de corrutinas (`lifecycleScope` / `viewModelScope`) generaba una condición de carrera (*Race Condition*) en el runtime de Android.

---

## 3. Plan de Mitigación y Blindaje Técnico

Para solucionar definitivamente la incidencia visualizada en el video de prueba, se aplicarán las siguientes correcciones:

1. **Inclusión del Listener de Perfil en `cancelarListeners()`:**
   - Registrar y almacenar la `ListenerRegistration` del flujo de perfil del propietario en `InquilinoRepository` para garantizar su remoción inmediata al cerrar sesión.
2. **Protección de Emisiones en Room:**
   - Asegurar que los flujos de Room manejen excepciones de cierre de base de datos de manera defensiva (`catch { emit(emptyList()) }`).
3. **Estrategia de Cierre Atómico y Limpio:**
   - Cancelación de todos los flujos y escuchas remotas antes de purgar bases de datos locales y revocar tokens de autenticación.

---

## 4. Estado de Documentación y Siguientes Pasos

El presente informe ha sido registrado en la carpeta de propuestas de arquitectura para la supervisión y validación del Tech Lead. Una vez aprobado, se aplicarán los ajustes de código preventivos correspondientes.
