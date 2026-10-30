# Resumen de Implementación: Prorrateo de Área Común por Ocupación / Personas (Iteración 5.1)

**Fecha:** Octubre 2026  
**Responsable:** Agente Desarrollador (`@SKILL_DESARROLLO.md`)  
**Estado:** Implementado y Validado (100% Tests en Verde)  
**Propuesta Técnica Base:** [`Propuesta_Prorrateo_Area_Comun_Personas.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/Propuestas/Propuesta_Prorrateo_Area_Comun_Personas.md)  

---

## 1. Objetivo y Alcance

Implementar los componentes fundamentales de la capa de dominio para soportar la distribución de costos de áreas comunes ponderada por cantidad de habitantes u ocupantes (`WEIGHTED_PERSONS`), preservando el cálculo equitativo previo (`EQUITY`) por defecto.

---

## 2. Modificaciones Realizadas

### 2.1. Modelos de Datos y Dominio
1. **[`TipoProrrateoComun.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/TipoProrrateoComun.kt) [CREADO]:**
   - Definición del enum de dominio con los valores `EQUITY` (división equitativa por departamento) y `WEIGHTED_PERSONS` (distribución ponderada por número de personas).
2. **[`Inquilino.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/Inquilino.kt) [MODIFICADO]:**
   - Incorporación de la propiedad `val numeroPersonas: Int = 1` en la entidad `@Entity(tableName = "inquilinos")`. El valor por defecto garantiza compatibilidad con datos existentes.

### 2.2. Motor de Cálculo
1. **[`CobroCalculator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt) [MODIFICADO]:**
   - Extensión de [`calcularResumenMensual`](method://com.example.kilowatt.util.CobroCalculator#calcularResumenMensual) con el parámetro `tipoProrrateoComun: TipoProrrateoComun = TipoProrrateoComun.EQUITY`.
   - Lógica de cómputo para `WEIGHTED_PERSONS`:
     - Cálculo de la suma total de ocupantes entre submedidores pagadores:
       $$P_{total} = \sum_{i \in P_{pagadores}} \max(1, \text{inquilino}_i.\text{numeroPersonas})$$
     - Determinación del costo unitario por persona:
       $$C_{persona} = \frac{TotalSoles_{comun}}{P_{total}}$$
     - Asignación proporcional a cada inquilino según su número de habitantes.
     - Fallback defensivo: Si el inquilino es `null` o `numeroPersonas <= 0`, se sanitiza a 1 habitante vía `.coerceAtLeast(1)`.
     - Si $P_{total} == 0$, la cuota unitaria devuelve `0.0`, previniendo divisiones por cero.

### 2.3. Pruebas Unitarias
1. **[`CobroCalculatorTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/CobroCalculatorTest.kt) [EXTENDIDO]:**
   - `calcularResumenMensual_weightedPersons_prorrateoProporcionalCorrecto`: Valida la asignación matemática exacta (4 personas vs 2 personas) y el principio de conservación de montos.
   - `calcularResumenMensual_weightedPersons_fallbackDefensivoConInquilinoNullOInvalido`: Comprueba sanitización defensiva de números negativos o inquilinos nulos a 1 persona.
   - `calcularResumenMensual_equityExplicit_mantienePartesIgualesIndependienteDePersonas`: Asegura que el modo `EQUITY` no se altere ante la presencia de conteos de personas dispares.
   - `calcularResumenMensual_weightedPersons_sinAreaComunOSinConsumo_noLanzaExcepcion`: Verifica estabilidad con ausencia de consumo comunitario.

---

## 3. Resultados de Compilación y Validación

- **Gradle Build (`:app:assembleDebug`):** `BUILD SUCCESSFUL` (0 errores).
- **Gradle Unit Tests (`:app:testDebugUnitTest`):** **41 tests pasados, 0 fallados, 0 omitidos** (100% en verde).

---

## 4. Archivos Involucrados

| Operación | Ruta del Archivo |
| :--- | :--- |
| **Creado** | `app/src/main/java/com/example/kilowatt/data/TipoProrrateoComun.kt` |
| **Modificado** | `app/src/main/java/com/example/kilowatt/data/Inquilino.kt` |
| **Modificado** | `app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt` |
| **Modificado** | `app/src/test/java/com/example/kilowatt/CobroCalculatorTest.kt` |
| **Creado** | `docs/Propuestas/Implementadas/Resumen_Impl_Prorrateo_Area_Comun_Personas_Iteracion_5_1.md` |
