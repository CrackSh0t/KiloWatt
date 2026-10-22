# Entregable de Arquitectura: Historial y Gráficos de Consumo

Este documento contiene la propuesta de diseño técnico y análisis arquitectónico para la implementación de la funcionalidad de **Historial y Gráficos de Consumo** en Kilowatt.

---

## 1. Resumen Ejecutivo

La funcionalidad de **Historial y Gráficos de Consumo** busca proporcionar al arrendador una herramienta de inteligencia visual para analizar la evolución temporal del consumo eléctrico en kilovatios hora (kWh) y Soles (S/), identificando tendencias, picos estacionales (fugas o sobreconsumo) y comparativas relativas entre inquilinos.

La solución se apoyará en la librería madura **MPAndroidChart** integrada mediante `ViewBinding` sobre una nueva pantalla dedicada (`HistorialConsumoActivity.kt`), reutilizando las colecciones existentes en Cloud Firestore sin alterar esquemas ni introducir deuda técnica.

---

## 2. Análisis del Problema y Necesidad de Negocio

* **Objetivo:** Ofrecer gráficos interactivos y métricas consolidadas del consumo energético.
* **Necesidad del Negocio:**
  - El arrendador carece de visibilidad sobre la tendencia de consumo de sus inmuebles a lo largo del tiempo.
  - Facilita la resolución de discrepancias con inquilinos al contar con un registro histórico visual.
  - Permite detectar oportunamente anomalías (fugas de energía o sobreconsumo por artefactos defectuosos).
* **Problema Actual:** Las pantallas actuales (`ResumenCobrosActivity`) solo permiten consultar un mes a la vez mediante un selector desplegable. No existe una vista que compare periodos o muestre la evolución temporal.
* **Valor Esperado:** Transparencia total, control financiero del inmueble y toma de decisiones basada en datos históricos.

---

## 3. Análisis de Datos Disponibles y Librerías Recomendadas

### A. Datos Disponibles en el Sistema
1. **Histórico de la Factura General:** Colección `usuarios/{uid}/facturas` (`mesPeriodo`, `kwhTotalesRecibo`, `montoTotalSoles`).
2. **Histórico por Submedidor:** Colección `usuarios/{uid}/lecturas` (`idSubmedidor`, `mesPeriodo`, `consumoKwh`, `montoPagarSoles`).
3. **Mapeo de Inquilinos:** Colección `usuarios/{uid}/inquilinos` y `submedidores` (`idInquilinoTitular`).

### B. Evaluación de Librerías de Gráficos (Charting Libraries)
* **Recomendación Arquitectónica: MPAndroidChart (`com.github.PhilJay:MPAndroidChart`)**
  - **Motivo:** Es la biblioteca estándar más madura para Android con Vistas clásicas XML / `ViewBinding`. Ofrece gráficos de líneas (tendencias mensuales), barras agrupadas (comparativas entre inquilinos) y gráficos circulares, con soporte para zoom, tooltips interactivos y personalización de ejes sin sobrecarga de configuración.

---

## 4. Evaluación Arquitectónica

| Criterio | Evaluación | Justificación |
| :--- | :---: | :--- |
| **¿Encaja con la arquitectura actual?** | **SÍ** | Sigue la estructura por capas: Presentación (`Activity` + XML), Dominio (`ConsumoHistoricoCalculator`) y Datos (`LecturaRepository`). |
| **¿Debe modificarse la arquitectura base?** | **NO** | Reutiliza los conectores de Firestore e inmutabilidad de datos de la capa actual. |
| **¿Existe una alternativa más simple?** | **NO** | Usar una librería probada evita implementar renderizado Canvas manual en la interfaz de usuario. |
| **¿Se está introduciendo deuda técnica?** | **NO** | La lógica de agrupamiento y ordenamiento de fechas se aísla en un objeto de dominio puro. |

---

## 5. Diseño Técnico Propuesto

```
┌─────────────────────────────────────────────────────────────┐
│                 HistorialConsumoActivity                    │
│   (Tabs: Tendencia Inmueble / Evolución Espacio / Comparativa)│
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                ConsumoHistoricoCalculator                   │
│   (Filtra, ordena por fecha con PeriodoDateUtils y agrupa)   │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                    LecturaRepository                        │
│   (Nuevo método: obtenerTodasLasLecturasFlow())             │
└──────────────┬───────────────────────────────┬──────────────┘
               ▼                               ▼
        Cloud Firestore                 Room AppDatabase
   (usuarios/{uid}/lecturas)           (Tabla: lecturas v5)
```

### A. Nuevos Componentes
1. **`HistorialConsumoActivity.kt` / `activity_historial_consumo.xml`**:
   - Pantalla dedicada accesible desde el menú principal (`MainActivity`).
2. **DTOs de Dominio (`ConsumoHistorico.kt`):**
   ```kotlin
   data class PuntoHistorico(
       val mesPeriodo: String,
       val fechaSort: Date,
       val consumoKwh: Double,
       val montoSoles: Double
   )

   data class HistoricoSubmedidor(
       val idSubmedidor: String,
       val nombreEspacio: String,
       val nombreInquilino: String,
       val puntos: List<PuntoHistorico>
   )

   data class ConsumoHistoricoResult(
       val puntosInmuebleMatriz: List<PuntoHistorico>,
       val historicoPorSubmedidor: List<HistoricoSubmedidor>,
       val promedioKwhMensual: Double,
       val mesMayorConsumo: String?
   )
   ```
3. **Calculador de Dominio (`ConsumoHistoricoCalculator.kt`):**
   - Objeto puro en Kotlin para agrupar lecturas, ordenar cronológicamente mediante `PeriodoDateUtils` y calcular picos y promedios.

---

## 6. Estrategia de Implementación en Fases

- **Fase 1 (Capa de Datos y Dominio):**
  - Implementación del flujo global `obtenerTodasLasLecturasFlow()` en `LecturaRepository`.
  - Creación de DTOs y lógica pura de cálculo en `ConsumoHistoricoCalculator` respaldada por pruebas unitarias (`ConsumoHistoricoCalculatorTest`).
- **Fase 2 (Configuración e Integración UI):**
  - Inclusión de la dependencia `MPAndroidChart` en `gradle/libs.versions.toml`.
  - Construcción de `HistorialConsumoActivity` con layout de gráficos de líneas y barras.
- **Fase 3 (Renderizado e Interacción):**
  - Vinculación de tendencias globales del inmueble y selector por inquilino/submedidor.
- **Fase 4 (Documentación y Sincronización):**
  - Sincronización completa de la documentación del proyecto (`ROADMAP.md`, `ESTADO_DEL_PROYECTO.md`, `MODELO_DATOS.md`, `CHANGELOG.md`, `MEMORIA_PROYECTO_KILOWATT.md`).

---

## 7. Análisis de Riesgos

1. **Ordenamiento Cronológico Incorrecto:**
   - *Riesgo:* Ordenar las cadenas de periodo alfabéticamente ("Agosto", "Diciembre", "Enero") generaría ejes X erróneos.
   - *Mitigación:* Ordenar strictly por el objeto `Date` resuelto por `PeriodoDateUtils`.
2. **Rendimiento de Carga:**
   - *Riesgo:* Agrupar cientos de lecturas históricas en el hilo principal de UI.
   - *Mitigación:* Procesar el agrupamiento dentro de `Dispatchers.Default`.

---

## 8. Recomendación Final del Arquitecto

### **APROBAR CON CAMBIOS**

La propuesta responde directamente al Roadmap del proyecto, aporta valor significativo al negocio y respeta la arquitectura en capas. Se recomienda aprobar el diseño para proceder en fases secuenciales una vez otorgada la conformidad.
