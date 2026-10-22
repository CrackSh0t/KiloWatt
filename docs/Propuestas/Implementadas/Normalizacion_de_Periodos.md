# Entregable de Arquitectura: Diseño de `PeriodoDateUtils` para Normalización y Ordenamiento de Periodos

Este documento contiene la propuesta de diseño técnico y análisis arquitectónico para el componente utilitario `PeriodoDateUtils` en Kilowatt.

---

## 1. Resumen Ejecutivo

En Kilowatt, la propiedad `mesPeriodo` es la clave de agrupación temporal de facturas generales y lecturas individuales. Actualmente se captura como texto libre en la interfaz de usuario (`LecturasActivity.kt`), generando variaciones de formato (`"Agosto 2026"`, `"2026-08"`, `"08/2026"`, etc.).

Para habilitar de manera robusta la **evaluación de vencimientos** (Fase 2B) y los **futuros gráficos históricos** (Fase 3), se requiere consolidar `PeriodoDateUtils` como un componente reutilizable de dominio capaz de **normalizar, parsear y convertir cualquier variación de `mesPeriodo` en una fecha cronológicamente ordenable**.

---

## 2. Análisis del Manejo Actual de `mesPeriodo`

### A. Dónde y Cómo se Utiliza en la Aplicación
1. **Captura en UI (`LecturasActivity.kt`):**
   - Se ingresa mediante un `TextInputEditText` libre (`etMesPeriodo`).
2. **Identificador en Firestore (`LecturaRepository.kt`):**
   - Se aplica `sanitizeDocId(mesPeriodo)` sustituyendo caracteres especiales como `/` por `_` para el ID de documento (`usuarios/{uid}/facturas/{mesPeriodo}`).
3. **Búsqueda y Agrupación:**
   - Se realizan comparaciones con `equals(..., ignoreCase = true)` y filtros de Firestore con `whereEqualTo("mesPeriodo", mes)`.
4. **Indización en Room (`Lectura.kt`):**
   - Forma parte del índice único compuesto `@Index(value = ["idSubmedidor", "mesPeriodo"], unique = true)`.

### B. Limitaciones del Enfoque Actual
* **Riesgo de Ordenamiento Alfabético Incorrecto:**
  - Al ordenar cadenas de texto directamente, *"Agosto 2026"* antecede a *"Diciembre 2026"*, pero *"Enero 2026"* antecede a *"Febrero 2026"*. En una lista o gráfico histórico, esto rompería el eje cronológico de tiempo.
* **Sensibilidad a Variaciones Tipográficas:**
  - Pequeñas variaciones como `"Agosto 2026"`, `"Agosto  2026"` (doble espacio) o `"agosto/2026"` se tratarían como periodos distintos si no se normalizan.

---

## 3. Análisis de Impacto

* **Componentes Afectados:** Motor de cálculo (`CobroCalculator.kt`), Renderizado de lista (`CobroAdapter.kt`), Resumen mensual (`ResumenCobrosActivity.kt`) y la futura pantalla de gráficos (`HistorialConsumoActivity`).
* **Impacto en Datos:** Nulo sobre la base de datos (Firestore y Room mantendrán sus registros sin cambios).
* **Complejidad ArquITECTÓNICA:** **Baja**.

---

## 4. Evaluación Arquitectónica

| Criterio | Respuesta | Justificación |
| :--- | :---: | :--- |
| **¿La solución encaja con la arquitectura actual?** | **SI** | Se ubica como un utilitario de dominio puro en el paquete `util/`, sin dependencias del framework de Android. |
| **¿Debe modificarse la arquitectura principal?** | **NO** | No altera el flujo de repositorios ni la estructura de datos. |
| **¿Existe una alternativa más simple?** | **NO** | Centralizar la lógica de fechas en un único utilitario evita duplicar parsers en múltiples actividades o adaptadores. |
| **¿Se está introduciendo deuda técnica?** | **NO** | Al contrario, elimina la deuda técnica causada por parseos dispersos. |

---

## 5. Diseño Técnico Propuesto para `PeriodoDateUtils`

### A. Estructura y Métodos Principales

```
┌─────────────────────────────────────────────────────────────┐
│                     PeriodoDateUtils                        │
├─────────────────────────────────────────────────────────────┤
│ + convertirAFechaOrdenable(mesPeriodo: String): Date?       │
│ + obtenerFechaLimitePago(mesPeriodo: String, dia: Int): Date│
│ + esFechaVencida(mesPeriodo: String, dia: Int): Boolean     │
│ + formatearParaGrafico(mesPeriodo: String): String          │
│ + formatearFechaLimite(mesPeriodo: String, dia: Int): String│
└─────────────────────────────────────────────────────────────┘
```

### B. Especificación de Contrato del Componente

1. **`convertirAFechaOrdenable(mesPeriodo: String): Date?`**
   - **Propósito:** Parsea la cadena ingresada y retorna un objeto `Date` correspondiente al primer día de ese mes a las `00:00:00`.
   - **Utilidad:** Permite ordenar cualquier lista de periodos mediante `list.sortedBy { PeriodoDateUtils.convertirAFechaOrdenable(it.mesPeriodo) }` para alimentar gráficos de líneas o barras.

2. **`obtenerFechaLimitePago(mesPeriodo: String, diaLimite: Int): Date?`**
   - **Propósito:** Calcula la fecha y hora límite de vencimiento (día `diaLimite` del mes siguiente a la facturación, a las `23:59:59`).
   - **Utilidad:** Soporte directo para la evaluación de cobranza en Fase 2B.

3. **`esFechaVencida(mesPeriodo: String, diaLimite: Int, fechaActual: Date = Date()): Boolean`**
   - **Propósito:** Retorna `true` si la fecha actual ha superado la fecha límite de pago.

4. **`formatearParaGrafico(mesPeriodo: String): String`**
   - **Propósito:** Genera una etiqueta corta para el eje X de gráficos (ejemplo: `"Agosto 2026"` -> `"Ago 26"`).

5. **`formatearFechaLimite(mesPeriodo: String, diaLimite: Int): String`**
   - **Propósito:** Retorna un texto formateado en español (ejemplo: `"05/09/2026"`).

### C. Estrategia de Parseo Multi-Formato

El parser ejecutará una secuencia jerárquica de resolución:

1. **Paso 1 (Saneamiento):** Convertir a minúsculas, eliminar acentos y remover prefijos comunes (`"mes de "`, `"recibo "`).
2. **Paso 2 (Formato Textual en Español):** Mapeo de diccionarios para nombres de mes (`"enero"`, `"febrero"`, ..., `"septiembre"`, `"setiembre"`, `"diciembre"`) combinados con expresiones regulares para el año de 4 dígitos (`20\d{2}`).
3. **Paso 3 (Formatos Numéricos Estándar):** Intento secuencial con `SimpleDateFormat` para los patrones: `yyyy-MM`, `MM/yyyy`, `MM-yyyy`, `yyyy/MM`.
4. **Paso 4 (Fallback Seguro):** Si la cadena no puede ser interpretada (ej. `"Periodo Especial"`), retorna `null` para evitar cierres inesperados.

---

## 6. Estrategia de Implementación

1. **Fase 1 (Pruebas Unitarias de Especificación):**
   - Diseñar y ampliar la suite de pruebas unitarias en `PeriodoDateUtilsTest.kt` validando todos los formatos aceptados y casos de borde.
2. **Fase 2 (Refactorización del Componente):**
   - Consolidar el componente `PeriodoDateUtils.kt` en el paquete `com.example.kilowatt.util`.
3. **Fase 3 (Integración en Módulos de Cobro y Vencimiento):**
   - Conectar `PeriodoDateUtils` en `CobroCalculator.kt` y `CobroAdapter.kt`.
4. **Fase 4 (Integración Futura en Gráficos):**
   - Utilizar `convertirAFechaOrdenable` y `formatearParaGrafico` en la construcción de gráficos con MPAndroidChart.

---

## 7. Análisis de Riesgos

1. **Riesgo de Inconsistencia de Mes Siguiente:**
   - *Análisis:* Si el periodo es Diciembre (mes 11 en `Calendar`), incrementar un mes debe avanzar correctamente al año siguiente (Enero del año + 1).
   - *Mitigación:* Usar los métodos aritméticos de `Calendar.add(Calendar.MONTH, 1)`, los cuales gestionan de manera nativa el cambio de año.
2. **Riesgo de Desempeño por Creación de Instancias:**
   - *Análisis:* Crear múltiples instancias de `SimpleDateFormat` en bucles de listas largas puede causar recolección de basura innecesaria.
   - *Mitigación:* Reutilizar o aislar los formateadores dentro del objeto singleton `PeriodoDateUtils`.
3. **Riesgo de Cadenas Arbitrarias:**
   - *Análisis:* Que un usuario ingrese un texto no relacionado con fechas (ej. `"Recibo Casa"`).
   - *Mitigación:* Retornar `null` y aplicar un fallback a `PENDIENTE` sin romper el flujo de UI ni el cálculo de saldos.

---

## 8. Recomendación Final del Arquitecto

### **APROBAR**

El diseño propuesto para `PeriodoDateUtils` resuelve de manera simple, desacoplada y robusta la necesidad de ordenamiento cronológico para gráficos futuros y la evaluación de vencimientos actuales, manteniendo la compatibilidad total con los datos ingresados previamente.
