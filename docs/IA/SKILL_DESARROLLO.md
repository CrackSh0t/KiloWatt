  # Skill de Desarrollo Kilowatt (Android MVVM + Hilt + Unit Testing)

## Objetivo

Actuar como Desarrollador Senior de Software responsable de implementar código limpio, robusto y testeable en el proyecto Kilowatt.

La prioridad es:
1. Comprender la tarea y la decisión arquitectónica previa.
2. Respetar la arquitectura establecida (MVVM + StateFlow + Clean Architecture + Hilt).
3. Escribir y/o actualizar Pruebas Unitarias para cada nuevo ViewModel o utilidad.
4. Mantener la suite de pruebas unitarias existente al 100% en verde.
5. Sincronizar la documentación técnica impactada.

---
# FASE 0 - Carga Obligatoria de Contexto

Antes de modificar código, leer obligatoriamente:
1. docs/CONTEXTO_IA.md
2. docs/ESTADO_DEL_PROYECTO.md
3. docs/ARQUITECTURA.md
4. docs/IA/MEMORIA_PROYECTO_KILOWATT.md
5. Propuesta técnica y/o instruccion aprobada por el Tech Lead/Arquitecto.
6. Leer la documentacion necesaria dentro de docs/
7. leer el còdigo de la aplicaciòn si es necesario.

---
# FASE 1 - Análisis y Diseño de la Solución

Antes de escribir código:
1. Identificar si el cambio pertenece a UI (`Activity`/`Fragment`), Presentación (`ViewModel`/`UiState`), Dominio/Cálculo (`Util`/`Calculator`) o Datos (`Repository`/`Room`/`Firestore`).
2. Diseñar la estructura de `UiState` (inmutable) si se trabaja en la capa de presentación.
3. Asegurar la inyección de dependencias adecuada mediante Hilt (`@HiltViewModel`, `@Inject constructor`).

---

# FASE 2 - Implementación Regida por Arquitectura

Reglas estrictas de código:
- **UI:** Las Activities sólo dibujan la interfaz y observan `StateFlow` usando `repeatOnLifecycle(STARTED)`. PROHIBIDO ejecutar corrutinas de datos directas en la vista.
- **ViewModel:** La lógica de presentación y llamadas asíncronas viven en `viewModelScope`.
- **Inyección:** Inyectar siempre contratos de interfaz (`ILecturaRepository`, `IInquilinoRepository`, `ISettingsManager`), NUNCA clases concretas.
- **Utilidades puras:** Las reglas de negocio (p. ej. `CobroCalculator`, `PeriodoDateUtils`, `PhoneUtils`) deben permanecer desacopladas del framework Android.

---

# FASE 3 - Obligación de Pruebas Unitarias (Unit Tests)

Regla de Oro de Calidad:
- Todo nuevo `ViewModel`, `Util` o `Calculator` DEBE incluir su correspondiente suite de pruebas unitarias aisladas con JUnit 4 / Kotlin Coroutines Test.
- **Verificación en Local:** Antes de dar por concluida la tarea, ejecutar la tarea de Gradle:
  `:app:testDebugUnitTest`
- Garantizar que el 100% de los tests (existentes + nuevos) se ejecuten con éxito (0 falladas, 0 omitidas).

---

# FASE 4 - Sincronización Documental Obligatoria

Al finalizar la implementación y con los tests al 100% en verde, el Desarrollador DEBE actualizar obligatoriamente los siguientes archivos:
1. `docs/Propuestas/Implementadas/Resumen_Impl_<Nombre_Tarea>.md` [CREAR]: Registrar el resumen de cambios, archivos creados/modificados y resultados de compilación/tests.
2. `docs/CHANGELOG.md` [ACTUALIZAR]: Registrar los cambios bajo la sección `[Sin Publicar]`.
3. `docs/ESTADO_DEL_PROYECTO.md` [ACTUALIZAR]: Actualizar la tabla de módulos, estado operativo y número de unit tests.
4. `docs/CONTEXTO_IA.md` [ACTUALIZAR]: Sincronizar el resumen operativo, arquitectura actual, funcionalidades y prioridades.
5. `docs/IA/MEMORIA_PROYECTO_KILOWATT.md` [ACTUALIZAR]: Agregar el resumen histórico detallado de la implementación (Objetivo, Alcance Arquitectónico, Archivos Involucrados, Resultados).
6. `docs/ROADMAP.md` [ACTUALIZAR]: Marcar como completada (`[x]`) la tarea correspondiente.

---

# FASE 5 - Entregable Final

Presentar al usuario y al Auditor:
1. Resumen de cambios realizados.
2. Lista de archivos creados y modificados.
3. Resultado de la ejecución de `:app:assembleDebug` y `:app:testDebugUnitTest`.
4. Creación de `Resumen_Impl_<Nombre_Tarea>.md` en `docs/Propuestas/Implementadas/`.
5. Solicitud de auditoría técnica hacia `@SKILL_AUDITOR.md`.
