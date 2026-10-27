# Propuesta de Gobernanza, Estructura Documental y Asignación de Responsabilidades por Agente IA

**Fecha:** Octubre 2026  
**Autor:** Agente Arquitecto de Software  
**Estado:** Propuesta Técnica / En Revisión  
**Ubicación:** `docs/Propuestas/Propuesta_Gobernanza_y_Estructura_Documental_Agentes.md`

---

## 1. Resumen Ejecutivo

El objetivo de esta propuesta es analizar la estructura actual de la documentación contenida en la carpeta `docs/` y sus subcarpetas (`docs/IA/`, `docs/Propuestas/`, `docs/Propuestas/Implementadas/`), evaluar la pertinencia de los archivos existentes y definir formalmente **qué información debe actualizar o crear cada uno de los tres agentes IA (`skill_arquitecto`, `skill_auditor`, `skill_desarrollo`)** durante sus interacciones continuas.

Adicionalmente, se identifica una inconsistencia estructural en `ROADMAP.md` y la falta de un registro centralizado de hallazgos de deuda técnica no bloqueante, proponiendo los ajustes necesarios para garantizar la sostenibilidad, trazabilidad y sincronización del conocimiento del proyecto **Kilowatt**.

---

## 2. Evaluación de la Estructura Documental Actual

### 2.1. Fortalezas Encontradas
1. **Especialización de Roles (`docs/IA/`):** Los archivos [`SKILL_ARQUITECTO.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/IA/SKILL_ARQUITECTO.md), [`SKILL_AUDITOR.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/IA/SKILL_AUDITOR.md) y [`SKILL_DESARROLLO.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/IA/SKILL_DESARROLLO.md) definen claramente los límites operativos, fases y restricciones de cada rol.
2. **Registro Histórico Consolidado ([`MEMORIA_PROYECTO_KILOWATT.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/IA/MEMORIA_PROYECTO_KILOWATT.md)):** Ofrece una bitácora detallada de los hitos técnicos, contexto, archivos modificados y resultados de validación de cada refactorización o funcionalidad completada.
3. **Documentación de Dominio y Decisiones:** [`ARQUITECTURA.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/ARQUITECTURA.md), [`DECISIONES_TECNICAS.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/DECISIONES_TECNICAS.md), [`MODELO_DATOS.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/MODELO_DATOS.md) y [`REGLAS_DE_DESARROLLO.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/REGLAS_DE_DESARROLLO.md) cubren exhaustivamente las directrices técnicas del proyecto.
4. **Histórico de Propuestas (`docs/Propuestas/Implementadas/`):** Excelente práctica de archivo de propuestas e informes de auditoría ejecutados.

### 2.2. Oportunidades de Mejora e Inconsistencias Detectadas
1. **Inconsistencia de Contenido en [`ROADMAP.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/ROADMAP.md):**
   - **Diagnóstico:** El archivo `ROADMAP.md` actualmente contiene una copia del archivo promocional/README del proyecto (con capturas de pantalla, insignias, guías de instalación y fórmulas de cálculo), en lugar de la hoja de ruta cronológica por fases y tareas pendientes/completadas.
   - **Efecto:** Genera confusión para la IA y para desarrolladores sobre dónde consultar el estado de avance de las fases del proyecto.
2. **Ausencia de un Registro de Deuda Técnica Centralizado (`docs/DEUDA_TECNICA.md`):**
   - **Diagnóstico:** Actualmente los hallazgos de severidad media, baja u observaciones detectados durante las auditorías quedan archivados en los informes individuales de `docs/Propuestas/Implementadas/`.
   - **Efecto:** La deuda técnica acumulada se vuelve invisible a menos que se revisen decenas de informes antiguos uno por uno.
3. **Falta de Delimitación Determinista de Actualización entre Agentes:**
   - **Diagnóstico:** Aunque `SKILL_DESARROLLO.md` menciona actualizar ciertos documentos, no existe una matriz explícita que evite que los agentes pisen responsabilidades o dejen documentos desfasados (ej. `CONTEXTO_IA.md` vs `ESTADO_DEL_PROYECTO.md`).

---

## 3. Propuesta de Ajustes al Esquema de Archivos

Para optimizar el esquema de archivos sin sobrecargar a los agentes, se proponen las siguientes modificaciones estructurales:

### 3.1. Reestructuración y Saneamiento de Archivos Existentes
* **Corregir [`ROADMAP.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/ROADMAP.md):**
  - **Sustentación:** Reemplazar el contenido duplicado tipo README por una estructura clara de fases del proyecto (Fase 1: MVVM, Fase 2: SSOT, Fase 3: Sellos/PDF, Fase 4: Rediseño UI, Fase 5: Próximas características) con casillas de verificación (`[x]` / `[ ]`).
  - **Ubicación del contenido promocional:** El contenido comercial/introductorio pertenece al `README.md` principal en la raíz del proyecto.

### 3.2. Creación de un Nuevo Archivo de Control
* **Crear [`docs/DEUDA_TECNICA.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/DEUDA_TECNICA.md):**
  - **Sustentación:** Servirá como la bitácora viva donde el **Auditor** registra y categoriza la deuda técnica, refactorizaciones pendientes y hallazgos no bloqueantes. El **Arquitecto** utilizará este archivo como insumo para planificar y priorizar las siguientes fases en el `ROADMAP.md`.

---

## 4. Matriz de Responsabilidades y Asignación de Archivos por Agente

A continuación se define con precisión la interacción que cada uno de los tres agentes IA mantendrá con los archivos del sistema documental:

```
┌───────────────────────────────────────────────────────────────────────────────────┐
│                                 AGENTE ARQUITECTO                                 │
│  Responsable de: Diseño, Evaluación, Modelo de Datos, Roadmap y Reglas Técnicas  │
└────────────────────────────────────────┬──────────────────────────────────────────┘
                                         │
┌────────────────────────────────────────▼──────────────────────────────────────────┐
│                                  AGENTE AUDITOR                                   │
│  Responsable de: Control de Calidad, Hallazgos, Seguridad y Deuda Técnica         │
└────────────────────────────────────────┬──────────────────────────────────────────┘
                                         │
┌────────────────────────────────────────▼──────────────────────────────────────────┐
│                                 AGENTE DESARROLLO                                 │
│  Responsable de: Código, Unit Tests, Changelog, Estado, Memoria y Contexto IA     │
└───────────────────────────────────────────────────────────────────────────────────┘
```

---

### 4.1. Agente Arquitecto (`@SKILL_ARQUITECTO.md`)

**Misión:** Definir el rumbo técnico, evaluar el impacto de los cambios, diseñar las soluciones antes de su implementación y velar por la sostenibilidad del software.

| Archivo | Acción Principal | Frecuencia / Momento | Descripción de la Información a Actualizar |
| :--- | :---: | :--- | :--- |
| `docs/Propuestas/Propuesta_<Nombre>.md` | **CREAR** | Previo a cualquier refactor o nueva feature compleja | Redactar el diseño técnico, análisis de impacto, componentes afectados, riesgos y estrategia de implementación en fases. |
| [`docs/ARQUITECTURA.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/ARQUITECTURA.md) | **ACTUALIZAR** | Al modificar patrones, capas o flujo de datos | Actualizar diagramas, componentes clave de las capas (UI, Dominio, Datos) e integración de librerías base. |
| [`docs/DECISIONES_TECNICAS.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/DECISIONES_TECNICAS.md) | **ACTUALIZAR** | Al adoptar o cambiar tecnologías/patrones | Documentar nuevas ADRs (Architecture Decision Records) especificando decisión y motivos de elección. |
| [`docs/MODELO_DATOS.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/MODELO_DATOS.md) | **ACTUALIZAR** | Al agregar/modificar entidades o DTOs | Actualizar tablas Room, colecciones Firestore, Data Classes de negocio, relaciones y reglas de validación. |
| [`docs/ROADMAP.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/ROADMAP.md) | **ACTUALIZAR** | Al planificar nuevas fases o priorizar tareas | Definir y ordenar las fases futuras del proyecto, incorporar ítems provenientes de `DEUDA_TECNICA.md`. |
| [`docs/VISION_PRODUCTO.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/VISION_PRODUCTO.md) | **ACTUALIZAR** | Si cambian los casos de uso o alcance | Ajustar los casos de uso principales (CU-01 a CU-06) o el flujo de trabajo mensual esperado. |
| [`docs/REGLAS_DE_DESARROLLO.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/REGLAS_DE_DESARROLLO.md) | **ACTUALIZAR** | Al establecer nuevas convenciones de código | Incorporar nuevas normas de nomenclatura, patrones de UI, concurrencia o convenciones de commits. |

* **Restricción del Arquitecto:** ❌ No modifica código fuente (`app/src/...`), no escribe clases concretas ni genera unit tests.

---

### 4.2. Agente Auditor (`@SKILL_AUDITOR.md`)

**Misión:** Proteger la calidad e integridad del sistema mediante revisiones independientes, detección de vulnerabilidades, verificación de pruebas y fiscalización de reglas de arquitectura.

| Archivo | Acción Principal | Frecuencia / Momento | Descripción de la Información a Actualizar |
| :--- | :---: | :--- | :--- |
| `docs/Propuestas/Informe_Auditoria_<Nombre>.md` | **CREAR** | Durante la fase de auditoría de una solución o código | Redactar el análisis funcional, técnico, casos borde, seguridad, rendimiento y clasificación de hallazgos (Crítico, Alto, Medio, Bajo). |
| `docs/DEUDA_TECNICA.md` *(Nuevo)* | **ACTUALIZAR / CREAR** | Posterior a cada auditoría concluida | Registrar y mantener actualizado el inventario de deuda técnica acumulada, vulnerabilidades menores u observaciones pendientes. |
| `docs/Propuestas/Implementadas/` | **MOVER** | Al emitir la recomendación final `APROBADO ✅` | Trasladar la propuesta técnica original y el informe de auditoría correspondiente desde `docs/Propuestas/` hacia `docs/Propuestas/Implementadas/`. |

* **Restricción del Auditor:** ❌ No modifica código fuente, no corrige bugs directamente ni altera la hoja de ruta (`ROADMAP.md`).

---

### 4.3. Agente Desarrollo (`@SKILL_DESARROLLO.md`)

**Misión:** Construir código limpio, robusto y testeable siguiendo las directrices del Arquitecto, garantizando el 100% de pruebas unitarias en verde y manteniendo sincronizada la documentación operativa y la memoria del proyecto.

| Archivo | Acción Principal | Frecuencia / Momento | Descripción de la Información a Actualizar |
| :--- | :---: | :--- | :--- |
| `app/src/main/...` y `app/src/test/...` | **CREAR / MODIFICAR** | Durante la fase de implementación | Código fuente en Kotlin, layouts XML, recursos y suites de unit tests con JUnit 4. |
| `docs/Propuestas/Implementadas/Resumen_Impl_<Nombre>.md` | **CREAR** | Al finalizar el desarrollo de una tarea | Resumen de la implementación realizada, lista de archivos creados/modificados y resultado de ejecuciones Gradle (`assembleDebug` y `testDebugUnitTest`). |
| [`docs/CHANGELOG.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/CHANGELOG.md) | **ACTUALIZAR** | Obligatorio al finalizar la tarea | Registrar los cambios bajo la sección `[Sin Publicar]`, categorizados en `Añadido`, `Modificado`, `Arreglado` o `Eliminado`. |
| [`docs/ESTADO_DEL_PROYECTO.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/ESTADO_DEL_PROYECTO.md) | **ACTUALIZAR** | Obligatorio al finalizar la tarea | Actualizar la tabla de módulos/funcionalidades, su estado (ej. `✅ Operativo / MVVM`), el recuento de unit tests en verde y la lista de limitaciones. |
| [`docs/CONTEXTO_IA.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/CONTEXTO_IA.md) | **ACTUALIZAR** | Obligatorio al finalizar la tarea | Sincronizar las secciones de arquitectura actual, funcionalidades implementadas, deuda técnica y prioridades para la carga rápida de contexto de IA. |
| [`docs/IA/MEMORIA_PROYECTO_KILOWATT.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/IA/MEMORIA_PROYECTO_KILOWATT.md) | **ACTUALIZAR** | Obligatorio al finalizar la tarea | Agregar una nueva sección completa de hito histórico con: 1. Objetivo y Contexto, 2. Alcance Arquitectónico, 3. Detalle de Archivos Involucrados, 4. Resultados de Validación. |
| [`docs/ROADMAP.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/ROADMAP.md) | **ACTUALIZAR** | Al completar un ítem planificado | Marcar la tarea completada con la casilla `[x]` dentro de la fase correspondiente. |

---

## 5. Protocolo y Flujo de Trabajo Continuado

Para asegurar que las interacciones no dejen archivos desfasados, se establece el siguiente protocolo secuencial de 4 pasos por cada iteración o tarea de desarrollo:

```
  [PASO 1: ARQUITECTURA]
  Arquitecto elabora `Propuesta_X.md` y actualiza `ARQUITECTURA.md`, `MODELO_DATOS.md` o `ROADMAP.md` si aplica.
           │
           ▼
  [PASO 2: DESARROLLO]
  Desarrollo implementa código + unit tests (100% verde) + genera `Resumen_Impl_X.md` y actualiza `CHANGELOG.md`,
  `ESTADO_DEL_PROYECTO.md`, `CONTEXTO_IA.md`, `MEMORIA_PROYECTO_KILOWATT.md` y `ROADMAP.md`.
           │
           ▼
  [PASO 3: AUDITORÍA]
  Auditor revisa código y tests, redacta `Informe_Auditoria_X.md`. Registrará hallazgos no bloqueantes en `DEUDA_TECNICA.md`.
           │
           ▼
  [PASO 4: CIERRE Y ARCHIVO]
  Al aprobar (`APROBADO ✅`), el Auditor mueve los documentos de la propuesta e informe a `docs/Propuestas/Implementadas/`.
```

---

## 6. Plan de Modificación de las Skills de IA

Para formalizar estas reglas dentro de las instrucciones de las IAs, se propone actualizar los siguientes tres archivos de skills en la carpeta `docs/IA/`:

1. **En [`docs/IA/SKILL_ARQUITECTO.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/IA/SKILL_ARQUITECTO.md):**
   - Incorporar la sección **"Sincronización Documental Obligatoria del Arquitecto"** especificando la actualización de `ARQUITECTURA.md`, `DECISIONES_TECNICAS.md`, `MODELO_DATOS.md`, `ROADMAP.md` y `VISION_PRODUCTO.md`.
2. **En [`docs/IA/SKILL_AUDITOR.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/IA/SKILL_AUDITOR.md):**
   - Incorporar la sección **"Sincronización Documental y Control de Deuda Técnica"** especificando el mantenimiento de `DEUDA_TECNICA.md` y el traslado de documentos aprobados a `docs/Propuestas/Implementadas/`.
3. **En [`docs/IA/SKILL_DESARROLLO.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/IA/SKILL_DESARROLLO.md):**
   - Reforzar la **"FASE 4 - Sincronización Documental"** exigiendo la actualización síncrona en cada entrega de `CHANGELOG.md`, `ESTADO_DEL_PROYECTO.md`, `CONTEXTO_IA.md`, `MEMORIA_PROYECTO_KILOWATT.md` y `ROADMAP.md`.

---

## 7. Conclusión y Siguientes Pasos

Esta propuesta establece un marco claro, ordenado y sin solapamientos para el mantenimiento de la documentación en **Kilowatt**.

**Acción recomendada:**
1. Aprobar la presente propuesta.
2. Crear el archivo `docs/DEUDA_TECNICA.md`.
3. Saneamiento de `ROADMAP.md`.
4. Sincronizar las directrices en `SKILL_ARQUITECTO.md`, `SKILL_AUDITOR.md` y `SKILL_DESARROLLO.md`.
