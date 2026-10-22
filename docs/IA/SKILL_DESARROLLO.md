# Skill de Desarrollo Kilowatt

## Objetivo

Este documento define el flujo obligatorio que debe seguir cualquier IA antes de realizar cambios en el proyecto Kilowatt.

La prioridad es:

1. Comprender el proyecto.
2. Mantener la coherencia arquitectónica.
3. Evitar cambios innecesarios.
4. Mantener actualizada la documentación.
5. Garantizar trazabilidad de las decisiones.

---

# FASE 0 - Carga Obligatoria de Contexto

Antes de analizar cualquier solicitud, la IA debe leer:

1. docs/CONTEXTO_IA.md
2. docs/VISION_PRODUCTO.md
3. docs/ESTADO_DEL_PROYECTO.md
4. docs/ARQUITECTURA.md
5. docs/REGLAS_DE_DESARROLLO.md
6. docs/ROADMAP.md
7. docs/DECISIONES_TECNICAS.md

Si existe documentación adicional relacionada con la tarea solicitada, también deberá ser revisada.

---

# FASE 1 - Comprensión de la Solicitud

Antes de modificar código, la IA debe identificar:

## Objetivo

¿Qué problema se intenta resolver?

## Alcance

¿Qué parte del sistema será afectada?

## Impacto

¿Qué funcionalidades podrían verse afectadas?

## Riesgos

¿Qué podría romperse?

---

# FASE 2 - Análisis del Proyecto

Antes de implementar:

- Identificar archivos involucrados.
- Revisar dependencias.
- Revisar flujo actual.
- Revisar arquitectura existente.
- Evitar duplicar funcionalidades.

La IA debe priorizar reutilizar código existente.

---

# FASE 3 - Diseño de la Solución

La IA debe presentar:

## Resumen de implementación

Qué pretende hacer.

## Archivos afectados

Lista de archivos que serán modificados.

## Archivos nuevos

Lista de archivos a crear.

## Posibles riesgos

Impactos detectados.

No implementar todavía.

---

# FASE 4 - Implementación

Reglas:

- Realizar los cambios mínimos necesarios.
- Respetar las convenciones del proyecto.
- Mantener compatibilidad con el código existente.
- Evitar refactorizaciones no solicitadas.
- Evitar cambios masivos fuera del alcance.

---

# FASE 5 - Validación

Revisar:

- Imports
- Referencias
- Dependencias
- Posibles errores de compilación
- Código muerto
- Recursos faltantes

Si detecta problemas potenciales debe documentarlos.

---

# FASE 6 - Actualización Documental

La documentación es parte obligatoria del desarrollo.

Actualizar únicamente los documentos impactados.

Posibles documentos:

- docs/ESTADO_DEL_PROYECTO.md
- docs/ROADMAP.md
- docs/ARQUITECTURA.md
- docs/MODELO_DATOS.md
- docs/DECISIONES_TECNICAS.md
- docs/CONTEXTO_IA.md
- docs/CHANGELOG.md
- docs/MEMORIA_PROYECTO_KILOWATT.md

---

# FASE 7 - Resumen Final

La IA debe indicar:

## Cambios realizados

Lista resumida.

## Archivos modificados

Lista completa.

## Documentación actualizada

Lista completa.

## Riesgos pendientes

Si existen.

## Próximos pasos recomendados

Opcional.

---

# Reglas Especiales

## Regla 1

Nunca modificar arquitectura principal sin justificación.

## Regla 2

No eliminar documentación existente.

## Regla 3

Toda nueva funcionalidad debe reflejarse en la documentación correspondiente.

## Regla 4

Toda decisión arquitectónica relevante debe registrarse en DECISIONES_TECNICAS.md.

## Regla 5

Todo cambio funcional debe registrarse en CHANGELOG.md.

## Regla 6

Todo cambio importante debe quedar reflejado en MEMORIA_PROYECTO_KILOWATT.md.

---

# Filosofía del Proyecto

La calidad del proyecto es más importante que la velocidad de implementación.

La documentación forma parte del producto.

La IA debe actuar como arquitecto, desarrollador y mantenedor responsable del sistema, no únicamente como generador de código.
