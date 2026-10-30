# Propuesta Técnica: Prorrateo de Área Común por Factor de Ocupación / Personas

**Fecha:** Octubre 2026  
**Autor:** Agente Arquitecto de Software (`@SKILL_ARQUITECTO.md`)  
**Estado:** Propuesta Técnica / En Evaluación  
**Ubicación:** `Propuesta_Prorrateo_Area_Comun_Personas.md`  

---

## 1. Resumen Ejecutivo

Actualmente, el sistema de facturación y prorrateo de energía eléctrica de **Kilowatt** distribuye el costo monetario del consumo de las áreas comunes (submedidores con `esAreaComun = true`) en partes estrictamente iguales entre los submedidores privados configurados con `pagaAreaComun = true`.

Sin embargo, en la gestión inmobiliaria real (edificios multifamiliares, quintas, departamentos compartidos), el uso de los servicios comunitarios (bomba de agua, iluminación de pasillos, lavandería común, portón eléctrico) está directamente correlacionado con el **número de ocupantes que habitan cada unidad**. La división equitativa genera fricción y percepción de injusticia cuando un inquilino solitario debe pagar la misma cuota de área común que una unidad habitada por 4 o 5 personas.

Esta propuesta técnica formaliza el diseño arquitectónico para incorporar una **modalidad de prorrateo ponderado por número de personas**, permitiendo al arrendador elegir libremente entre dos modalidades globales:
1. **`EQUITY` (Equitativa / Por Departamento):** Mantiene el comportamiento actual (división en partes iguales por submedidor).
2. **`WEIGHTED_PERSONS` (Ponderada por Personas / Ocupantes):** Distribuye el costo total del área común en función de la proporción de habitantes de cada departamento.

---

## 2. FASE 1: Comprensión del Problema y Necesidad del Negocio

### 2.1. Objetivo
Permitir que el arrendador configure la cantidad de ocupantes de cada inquilino/departamento y elija el método de distribución de gastos comunes (`EQUITY` vs. `WEIGHTED_PERSONS`), recalculando de manera transparente y exacta la cuota correspondiente a cada arrendatario.

### 2.2. Necesidad del Negocio
* **Justicia y equidad en el cobro:** Evitar que inquilinos con baja ocupación subsidien el consumo de unidades familiares numerosas en servicios de alto impacto continuo como la electrobomba de agua.
* **Flexibilidad operativa para el arrendador:** Adaptabilidad a diferentes acuerdos de arrendamiento y tipos de inmuebles (alquiler por habitaciones vs. departamentos enteros).
* **Transparencia en el recibo:** Claridad documental en los comprobantes PDF emitidos y en los desgloses de WhatsApp sobre por qué se cobra determinado monto de área común.

### 2.3. Problema Actual
* El modelo de datos `Inquilino` solo almacena `idInquilino`, `nombreCompleto` y `telefonoWhatsapp`. No existe registro de ocupación.
* `CobroCalculator.calcularResumenMensual(...)` divide `totalSolesAreaComun / cantidadPagadores` de forma invariable.
* No existe ningún parámetro ni configuración global para alternar la lógica de prorrateo común.

### 2.4. Valor Esperado
* Mayor satisfacción de los inquilinos y reducción de reclamos por cuotas de luz.
* Incremento de valor funcional del producto Kilowatt como herramienta profesional de administración inmobiliaria.

---

## 3. FASE 2: Análisis de Impacto

### 3.1. Componentes Afectados

| Capa / Componente | Archivo(s) | Naturaleza del Impacto |
| :--- | :--- | :--- |
| **Modelos de Dominio / Room** | `Inquilino.kt` | Adición del campo `numeroPersonas: Int = 1`. |
| **Enums de Negocio** | `TipoProrrateoComun.kt` *(Nuevo)* | Definición del enum `EQUITY`, `WEIGHTED_PERSONS`. |
| **Motor de Cálculo (Dominio)** | `CobroCalculator.kt` | Parámetro `tipoProrrateoComun`, cálculo de suma ponderada y cuotas individuales. |
| **Base de Datos Local (Room)** | `AppDatabase.kt` | Incremento de versión (v5 ➔ v6) y script de migración `MIGRATION_5_6`. |
| **Repositorios y Sincronización** | `IInquilinoRepository.kt`, `InquilinoRepository.kt` | Inclusión de `numeroPersonas` en operaciones CRUD locales y remotas. |
| **Preferencias y Perfil** | `ISettingsManager.kt`, `SettingsManager.kt` | Gestión de la preferencia `tipoProrrateoComun` con persistencia en SharedPreferences y Firestore. |
| **Presentación - Resumen** | `ResumenCobrosViewModel.kt`, `ResumenCobrosActivity.kt`, `item_cobro.xml` | Inyección de la preferencia, visualización del factor de ocupación en tarjetas de cobro. |
| **Presentación - Configuración** | `ConfiguracionViewModel.kt`, `ConfiguracionActivity.kt`, `activity_configuracion.xml`, `dialog_editar_inquilino.xml`, `item_inquilino.xml` | Inputs numéricos para `numeroPersonas` (registro y edición) y selector de modalidad en perfil o ajustes. |
| **Exportación / Comprobantes** | `PdfGenerator.kt` | Detalle en el desglose del recibo del inquilino (ej. "Cuota común: 3 personas × S/ 7.50"). |
| **Pruebas Unitarias** | `CobroCalculatorTest.kt`, `ResumenCobrosViewModelTest.kt`, `ConfiguracionViewModelTest.kt` | Casos de prueba exhaustivos para ambas modalidades y condiciones de borde. |

### 3.2. Clasificación de Complejidad
* **Complejidad Global:** **Media**
* **Justificación:** La lógica matemática es sencilla y encapsulada en `CobroCalculator`. El mayor cuidado radica en la migración de esquemas de datos (Room y Firestore) para garantizar que los registros existentes no sufran regresiones ni valores nulos.

---

## 4. FASE 3: Evaluación Arquitectónica

* **¿La solución encaja con la arquitectura actual?** **SÍ.** Se mantiene el patrón MVVM + Clean Architecture ligera, con `CobroCalculator` como objeto puro de dominio desacoplado de Android SDK, y persistencia a través del patrón Repositorio (SSOT Room + Firestore).
* **¿Debe modificarse la arquitectura?** **NO.** No se alteran capas ni patrones existentes; se extiende el modelo y la parametrización de cálculo.
* **¿Existe una alternativa más simple?** Se evaluó fijar un monto por persona manualmente en cada recibo, pero se descartó porque rompe el principio de conservación de energía (la suma de cobros debe igualar el monto total de la factura general). El cálculo automatizado garantiza consistencia contable.
* **¿Se está introduciendo deuda técnica?** **NO.** Se incluye la migración explícita en Room (`Migration(5, 6)`) para evitar depender de `fallbackToDestructiveMigration()` y proteger los datos en entornos de producción.

---

## 5. FASE 4: Diseño Técnico Detallado

### 5.1. Modelos de Datos

#### 5.1.1. Enum `TipoProrrateoComun`
```kotlin
package com.example.kilowatt.data

enum class TipoProrrateoComun {
    EQUITY,             // Por departamento / espacio (partes iguales)
    WEIGHTED_PERSONS    // Ponderado por cantidad de personas / ocupantes
}
```

#### 5.1.2. Entidad `Inquilino`
```kotlin
package com.example.kilowatt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inquilinos")
data class Inquilino(
    @PrimaryKey val idInquilino: String = "",
    val nombreCompleto: String = "",
    val telefonoWhatsapp: String = "",
    val numeroPersonas: Int = 1
)
```

#### 5.1.3. Preferencia en `ISettingsManager` / `SettingsManager`
* Clave SharedPreferences: `"tipo_prorrateo_comun"`
* Valor por defecto: `TipoProrrateoComun.EQUITY.name`
* Documento Firestore: `usuarios/{uid}/perfil/datos_pago` con campo `tipoProrrateoComun: "EQUITY" | "WEIGHTED_PERSONS"`.

---

### 5.2. Fórmulas Matemáticas de Prorrateo

Sean:
* $M_{total}$: Monto total en soles de la factura general eléctrica.
* $K_{total\_subs}$: Suma total de consumo en kWh de todos los submedidores registrados con lectura en el periodo.
* $S_{comun}$: Submedidores configurados como áreas comunes ($esAreaComun = true$).
* $S_{part}$: Submedidores particulares correspondientes a inquilinos ($esAreaComun = false$).
* $P_{pagadores}$: Subconjunto de $S_{part}$ con $pagaAreaComun = true$.

#### 1. Importe Monetario de las Áreas Comunes ($TotalSoles_{comun}$)
Para cada submedidor comunitario $c \in S_{comun}$ con consumo $kwh_c$:
$$\text{PorcentajeConsumo}_c = \begin{cases} \frac{kwh_c}{K_{total\_subs}} & \text{si } K_{total\_subs} > 0 \\ 0.0 & \text{si } K_{total\_subs} = 0 \end{cases}$$

$$\text{PagoSoles}_{c} = \text{PorcentajeConsumo}_c \times M_{total}$$
$$TotalSoles_{comun} = \sum_{c \in S_{comun}} \text{PagoSoles}_{c}$$

#### 2. Distribución Modalidad A: `EQUITY` (Equitativa)
$$\text{CuotaComun}_i = \begin{cases} \frac{TotalSoles_{comun}}{|P_{pagadores}|} & \text{si } |P_{pagadores}| > 0 \\ 0.0 & \text{en otro caso} \end{cases}$$

#### 3. Distribución Modalidad B: `WEIGHTED_PERSONS` (Ponderada por Personas)
Para cada submedidor pagador $i \in P_{pagadores}$, sea $Inq_i$ su inquilino titular asociado.
$$\text{Personas}_i = \begin{cases} Inq_i.numeroPersonas & \text{si } Inq_i \neq null \text{ y } Inq_i.numeroPersonas \ge 1 \\ 1 & \text{en cualquier otro caso (fallback seguro)} \end{cases}$$

Suma total de personas de las unidades pagadoras:
$$\text{Personas}_{total} = \sum_{i \in P_{pagadores}} \text{Personas}_i$$

Costo unitario por persona:
$$\text{CostoPorPersona} = \begin{cases} \frac{TotalSoles_{comun}}{\text{Personas}_{total}} & \text{si } \text{Personas}_{total} > 0 \\ 0.0 & \text{si } \text{Personas}_{total} = 0 \end{cases}$$

Cuota de área común para la unidad $i$:
$$\text{CuotaComun}_i = \text{Personas}_i \times \text{CostoPorPersona}$$

#### 4. Importe Final a Pagar por la Unidad $i$
$$\text{PagoBaseSoles}_i = \left( \frac{kwh_i}{K_{total\_subs}} \right) \times M_{total}$$
$$\text{TotalFinalPagar}_i = \text{PagoBaseSoles}_i + \text{CuotaComun}_i$$

#### 5. Propiedad de Conservación Financiera
$$\sum_{i \in S_{part}} \text{TotalFinalPagar}_i = M_{total} \quad (\text{sujeto únicamente a redondeos de céntimos en centavos de Sol})$$

---

### 5.3. Interfaz de `CobroCalculator`

```kotlin
fun calcularResumenMensual(
    mes: String,
    factura: FacturaGeneral,
    submedidores: List<Submedidor>,
    lecturas: List<Lectura>,
    inquilinos: List<Inquilino>,
    diaLimitePago: Int = 5,
    tipoProrrateoComun: TipoProrrateoComun = TipoProrrateoComun.EQUITY
): ResumenCobroResult
```

El valor por defecto `= TipoProrrateoComun.EQUITY` preserva total retrocompatibilidad con invocaciones existentes y tests unitarios previos.

---

## 6. FASE 5: Estrategia de Implementación Incremental

La implementación se estructura en **cuatro fases secuenciales y desacopladas**, donde cada fase debe compilar y ejecutarse sin romper el flujo de trabajo:

```
[Fase 1: Dominio & Motor de Cálculo] 
         │
         ▼
[Fase 2: Persistencia, Room v6 & Firestore] 
         │
         ▼
[Fase 3: UI de Configuración e Inquilinos] 
         │
         ▼
[Fase 4: UI de Resumen, Tarjetas & Recibo PDF]
```

### Iteración 5.1: Dominio y Cálculo Puro (`CobroCalculator`)
1. Crear el enum `TipoProrrateoComun`.
2. Actualizar el modelo `Inquilino` con el campo `numeroPersonas: Int = 1`.
3. Extender `CobroCalculator.calcularResumenMensual` incorporando el parámetro y la lógica de ponderación.
4. Crear suite completa de pruebas en `CobroCalculatorTest.kt`:
   - Prorrateo equitativo estándar (`EQUITY`).
   - Prorrateo ponderado con diferentes cantidades de ocupantes (ej. 1 persona, 3 personas, 2 personas).
   - Inquilinos con submedidor sin titular asignado (fallback a 1 persona).
   - Inquilinos con 0 o valores negativos (sanitización defensiva a 1 persona).
   - Comprobación de la suma total frente al recibo principal.

### Iteración 5.2: Capa de Persistencia, Migración Room y Firestore
1. Definir migración en `AppDatabase`:
   ```kotlin
   val MIGRATION_5_6 = object : Migration(5, 6) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE inquilinos ADD COLUMN numeroPersonas INTEGER NOT NULL DEFAULT 1")
       }
   }
   ```
   Incrementar versión de `@Database` a `6` y registrar `addMigrations(MIGRATION_5_6)`.
2. Actualizar `IInquilinoRepository` e `InquilinoRepository` para soportar `numeroPersonas` en creación, edición y deserialización de snapshots Firestore.
3. Actualizar `ISettingsManager` y `SettingsManager` con la propiedad `tipoProrrateoComun`.
4. Sincronizar la preferencia en Firestore bajo `usuarios/{uid}/perfil/datos_pago`.

### Iteración 5.3: Interfaz de Usuario de Configuración y Gestión de Inquilinos
1. Actualizar `dialog_editar_inquilino.xml` y `activity_configuracion.xml` añadiendo el campo numérico `etNumeroPersonas` (hint: "Cantidad de Ocupantes / Personas").
2. Actualizar `item_inquilino.xml` para mostrar visualmente el número de personas (ej. badge `👥 2`).
3. Actualizar `ConfiguracionViewModel` y `ConfiguracionActivity` para capturar y validar `numeroPersonas >= 1`.
4. Añadir en el diálogo o sección de perfil (`dialog_perfil_propietario.xml`) el selector de modalidad de prorrateo (`EQUITY` vs `WEIGHTED_PERSONS`).

### Iteración 5.4: Interfaz de Cobros y Comprobantes
1. Conectar `ResumenCobrosViewModel` leyendo `settingsManager.tipoProrrateoComun` y enviándolo a `CobroCalculator`.
2. Reflejar en `item_cobro.xml` la información del factor común asignado.
3. Ajustar `PdfGenerator.kt` para desglosar la cuota de área común en el comprobante cuando se use prorrateo ponderado.

---

## 7. FASE 6: Evaluación de Riesgos y Mitigaciones

| Riesgo Identificado | Severidad | Probabilidad | Mitigación Arquitectónica |
| :--- | :---: | :---: | :--- |
| **Inquilinos existentes en Firestore sin campo `numeroPersonas`** | Media | Alta | El modelo `Inquilino` declara `val numeroPersonas: Int = 1`. Al deserializar un documento antiguo donde el campo no existe, Firestore y Kotlin asignan automáticamente el valor por defecto `1`. |
| **Pérdida de datos en SQLite local por actualización de esquema** | Alta | Baja | Se implementa la migración explícita `MIGRATION_5_6` con `ALTER TABLE inquilinos ADD COLUMN numeroPersonas INTEGER NOT NULL DEFAULT 1`. Si la app se actualiza en dispositivos de usuarios reales, la base de datos no se destruye. |
| **División por cero si total de personas suma 0** | Alta | Muy Baja | Sanitización obligatoria en `CobroCalculator`: `val personasEfectivas = inquilino?.numeroPersonas?.coerceAtLeast(1) ?: 1`. Si por alguna anomalía extrema la suma total de personas fuera 0, el costo por persona devuelve `0.0`. |
| **Desbalance de centavos por redondeo flotante** | Baja | Media | El cálculo utiliza `Double` en precisión completa para montos y se redondea a 2 decimales solo al momento de renderizar en UI / PDF. La suma de partes se valida en tests unitarios. |
| **Inquilino no asignado a submedidor que paga área común** | Media | Baja | Si un submedidor tiene `pagaAreaComun = true` pero `idInquilinoTitular == null`, se computa un factor de 1 ocupante por defecto para ese departamento. |

---

## 8. FASE 7: Recomendación Final del Arquitecto

### Dictamen: **APROBAR**

La propuesta técnica es sólida, modular y preserva al 100% la compatibilidad hacia atrás del sistema mediante el uso de valores por defecto defensivos (`1` persona, modalidad `EQUITY`). La arquitectura actual absorbe el cambio de manera natural y sin fricciones.

Se autoriza el paso a la fase de implementación por parte del Agente de Desarrollo siguiendo rigurosamente las iteraciones 5.1 a 5.4 planificadas.
