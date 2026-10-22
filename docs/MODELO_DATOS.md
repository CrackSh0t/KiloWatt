# Modelo de Datos - Kilowatt

Este documento describe la estructura y modelado de datos de la aplicación Kilowatt, incluyendo las entidades persistidas en la base de datos local (Room), las colecciones sincronizadas en la nube (Cloud Firestore), las clases de datos auxiliares para reglas de negocio y cálculo, y las relaciones y restricciones existentes.

---

## 1. Entidades Room (Base de Datos Local)

La base de datos local está gestionada por `AppDatabase` (versión 3), utilizando Room sobre SQLite.

### 1.1. `Inquilino`
Representa al arrendatario o titular asignado a uno o más espacios físicos / submedidores.
- **Tabla:** `inquilinos`
- **Campos:**
  - `idInquilino`: `Long` (o `String`) — **Clave Primaria** (autogenerada en Room).
  - `nombreCompleto`: `String` — Nombre y apellidos del inquilino.
  - `telefonoWhatsapp`: `String` — Teléfono de contacto para el envío de notificaciones y recibos por WhatsApp.

### 1.2. `Submedidor`
Representa un medidor de luz secundario asignado a una habitación, departamento o zona específica (incluyendo áreas comunes).
- **Tabla:** `submedidores`
- **Campos:**
  - `idSubmedidor`: `String` (o `Long`) — **Clave Primaria**. Identificador único del submedidor.
  - `idInquilino`: `Long?` (o `String?`) — **Clave Foránea Lógica** hacia `Inquilino(idInquilino)`. Opcional (`null` si es área común o está desocupado).
  - `nombreEspacio`: `String` — Etiqueta legible (ej. "Habitación 101", "Piso 2", "Bomba de Agua").
  - `esAreaComun`: `Boolean` — Flag que determina si este submedidor mide un consumo comunitario.
  - `pagaAreaComun`: `Boolean` — Flag que determina si al inquilino de este espacio se le debe prorratear el consumo de áreas comunes.

### 1.3. `FacturaGeneral`
Almacena la información del recibo principal emitido por la empresa distribuidora de energía eléctrica para el inmueble en un mes específico.
- **Tabla:** `factura_general`
- **Campos:**
  - `idFactura`: `Long` (o `String`) — **Clave Primaria** (autogenerada o basada en el periodo).
  - `mesPeriodo`: `String` — Periodo de facturación en formato estándar `YYYY-MM` (ej. "2026-10").
  - `montoTotalSoles`: `Double` — Importe total a pagar del recibo general en moneda nacional (PEN).
  - `kwhTotales`: `Double` — Consumo total facturado por la distribuidora en kilovatios hora (kWh).

### 1.4. `Lectura`
Registro periódico de la medición de energía registrada por un submedidor individual para un mes específico.
- **Tabla:** `lecturas`
- **Índice único:** `(idSubmedidor, mesPeriodo)` (restricción de unicidad para evitar duplicados en el mismo mes).
- **Campos:**
  - `idLectura`: `Long` (o `String`) — **Clave Primaria**.
  - `idSubmedidor`: `String` (o `Long`) — **Clave Foránea Lógica** hacia `Submedidor(idSubmedidor)`.
  - `mesPeriodo`: `String` — Periodo facturado asociado a la lectura (`YYYY-MM`).
  - `lecturaAnterior`: `Double` — Valor del contador en kWh al inicio del periodo.
  - `lecturaActual`: `Double` — Valor del contador en kWh al final del periodo.
  - `consumoKwh`: `Double` — Consumo neto del periodo (`lecturaActual - lecturaAnterior`).
  - `estadoPago`: `String` — Estado de cobranza (`"PENDIENTE"`, `"PAGADO"` o `"VENCIDO"` evaluado dinámicamente).
  - `fechaPago`: `Long?` — Fecha/timestamp del registro de pago.
  - `metodoPago`: `String?` — Medio de recepción del pago (`"Yape"`, `"Plin"`, `"Efectivo"`, `"Transferencia Bancaria"`).

---

## 2. Modelos Firestore (Base de Datos en la Nube)

En Cloud Firestore, los datos se estructuran bajo un esquema multitenant jerárquico por usuario autenticado (`usuarios/{uid}/...`):

### 2.1. Colección `usuarios/{uid}/inquilinos`
Documentos que representan inquilinos registrados por el arrendador.
- **Document ID:** Identificador generado por Firestore (`idInquilino`).
- **Campos:**
  - `idInquilino`: `String`
  - `nombreCompleto`: `String`
  - `telefonoWhatsapp`: `String`

### 2.2. Colección `usuarios/{uid}/submedidores`
Documentos que representan los submedidores registrados.
- **Document ID:** Identificador asignado o generado (`idSubmedidor`).
- **Campos:**
  - `idSubmedidor`: `String`
  - `idInquilino`: `String` (vacío si no tiene asignación)
  - `nombreEspacio`: `String`
  - `esAreaComun`: `Boolean`
  - `pagaAreaComun`: `Boolean`

### 2.3. Colección `usuarios/{uid}/facturas`
Documentos con la factura general del inmueble para un periodo.
- **Document ID:** Mes saneado (`sanitizeDocId(mesPeriodo)`, ej. `"2026_10"`).
- **Campos:**
  - `mesPeriodo`: `String`
  - `montoTotalSoles`: `Double`
  - `kwhTotales`: `Double`

### 2.4. Colección `usuarios/{uid}/lecturas`
Documentos con lecturas mensuales por submedidor.
- **Document ID:** Generado o clave compuesta `${idSubmedidor}_${mesPeriodo}`.
- **Campos:**
  - `idLectura`: `String`
  - `idSubmedidor`: `String`
  - `mesPeriodo`: `String`
  - `lecturaAnterior`: `Double`
  - `lecturaActual`: `Double`
  - `consumoKwh`: `Double`
  - `estadoPago`: `String` ("PENDIENTE" | "PAGADO")
  - `fechaPago`: `Long?` (milisegundos)
  - `metodoPago`: `String?` ("Yape", "Plin", "Efectivo", "Transferencia Bancaria")

### 2.5. Documento `usuarios/{uid}/perfil/datos_pago`
Información de cobranza y perfil del arrendador.
- **Campos:**
  - `nombrePropietario`: `String` — Nombre del dueño o administrador para emisión del comprobante.
  - `numeroPago`: `String` — Cuenta o número telefónico de pago (ej. Yape / Plin / BCP).
  - `diaLimitePago`: `Int` — Día del mes fijado como fecha límite de pago.

---

## 3. Data Classes y Modelos de Negocio

Estas clases modelan transferencias internas, cálculos y presentaciones en UI:

### 3.1. `InquilinoConMedidor`
Agrupación en UI para administración y listados de configuración.
- `inquilino`: `Inquilino?`
- `submedidor`: `Submedidor`

### 3.2. `DetalleCobro`
Representa el desglose financiero individual a cobrar a cada inquilino o espacio.
- `nombreInquilino`: `String`
- `telefonoWhatsapp`: `String`
- `nombreEspacio`: `String`
- `mesPeriodo`: `String`
- `lecturaAnterior`: `Double`
- `lecturaActual`: `Double`
- `consumoKwh`: `Double`
- `precioKwh`: `Double` (Costo por kWh calculado a partir de la factura general)
- `montoAreaComunSoles`: `Double` (Cuota prorrateada asignada de áreas comunes)
- `montoConsumoPropioSoles`: `Double` (Gasto individual por consumo propio)
- `montoPagarSoles`: `Double` (`montoConsumoPropioSoles + montoAreaComunSoles`)
- `estadoPago`: `String` ("PENDIENTE" | "PAGADO")
- `fechaPago`: `Long?` (milisegundos)
- `metodoPago`: `String?` ("Yape", "Plin", "Efectivo", "Transferencia Bancaria")

### 3.3. `ResumenCobroResult`
Resultado del cálculo integral de prorrateo mensual procesado por `CobroCalculator`.
- `montoTotalRecibo`: `Double`
- `kwhReciboPrincipal`: `Double`
- `sumaKwhSubmedidores`: `Double`
- `precioKwhPromedio`: `Double` (`montoTotalRecibo / kwhReciboPrincipal`)
- `totalSolesAreaComun`: `Double`
- `cuotaAreaComunPorInquilino`: `Double`
- `detalles`: `List<DetalleCobro>`

### 3.4. DTOs de Historial y Gráficos (`ConsumoHistorico.kt`)
Estructuras para series de tiempo y ordenamiento cronológico por submedidor e inmueble:
- `PuntoHistorico`: `mesPeriodo: String`, `fechaSort: Date?`, `consumoKwh: Double`, `montoSoles: Double`.
- `HistoricoSubmedidor`: `idSubmedidor: String`, `nombreEspacio: String`, `nombreInquilino: String`, `puntos: List<PuntoHistorico>`.
- `ConsumoHistoricoResult`: `puntosInmuebleMatriz: List<PuntoHistorico>`, `historicoPorSubmedidor: List<HistoricoSubmedidor>`, `promedioKwhMensual: Double`, `mesMayorConsumo: String?`.

### 3.5. `ValidacionLecturaResult`
Tipo algebraico sellado para validación de consistencia de lecturas.
- `Valido`: Estado exitoso.
- `Invalido(mensaje: String)`: Estado de error con descripción del problema.

---

## 4. Relaciones y Cardinalidades

```
[ Inquilino ] 1 <--- 0..1 [ Submedidor ]
                                | 1
                                |
                                | 0..*
                                v
                        [ Lectura ]
                                ^
                                | 0..*
                                |
[ FacturaGeneral ] 1 -----------+
```

### Detalle de Cardinalidades
1. **Inquilino a Submedidor (1 : 0..1 o 1 : N lógico):**
   - Un submedidor está asociado como máximo a un único inquilino en un momento dado (`idInquilino`), o a ninguno si es área común o está libre.
   - Un inquilino puede estar asignado a un submedidor (o varios si alquila más de un espacio).
2. **Submedidor a Lectura (1 : N):**
   - Un submedidor genera una lectura por cada periodo facturado (`mesPeriodo`).
3. **FacturaGeneral a Lectura (1 : N por periodo):**
   - Cada factura general define un `mesPeriodo`. Para ese mismo periodo se recopilan las lecturas de los distintos submedidores para realizar el prorrateo.

---

## 5. Restricciones de Negocio

1. **Lectura no decreciente:**
   - Para un mismo submedidor y periodo, `lecturaActual >= lecturaAnterior`. La lectura actual no puede ser menor a la lectura previa.
2. **Valores no negativos:**
   - Las lecturas, montos en soles y consumos en kWh no pueden ser negativos (`>= 0.0`).
3. **Unicidad de lectura por periodo:**
   - Solo puede existir una lectura por cada par `(idSubmedidor, mesPeriodo)`.
4. **Protección contra división por cero:**
   - Si los kWh de la factura general son cero o no existen, el costo por kWh resultante es `0.0` para salvaguardar la estabilidad del cálculo.
5. **Prorrateo de Áreas Comunes:**
   - El consumo de los submedidores marcados con `esAreaComun = true` se valoriza al costo unitario del kWh y su importe resultante se distribuye equitativamente solo entre aquellos submedidores que tengan `pagaAreaComun = true`.

---

## 6. Diagrama Textual de Relaciones

```
+-----------------------------------------------------------+
|                        USUARIO (UID)                      |
+-----------------------------------------------------------+
       |                        |                     |
       v (1:N)                  v (1:N)               v (1:1)
+-------------------+    +--------------------+  +--------------------+
|     INQUILINO     |    |   FACTURA_GENERAL  |  |  DATOS_PAGO_PERFIL |
+-------------------+    +--------------------+  +--------------------+
| PK idInquilino    |    | PK mesPeriodo      |  | nombrePropietario  |
| nombreCompleto    |    | montoTotalSoles    |  | numeroPago         |
| telefonoWhatsapp  |    | kwhTotales         |  | diaLimitePago      |
+-------------------+    +--------------------+  +--------------------+
       ^                        |
       | 0..1                   | (mesPeriodo)
       |                        |
       | (idInquilino)          |
+-------------------+           |
|    SUBMEDIDOR     |           |
+-------------------+           |
| PK idSubmedidor   |           |
| FK idInquilino    |           |
| nombreEspacio     |           |
| esAreaComun       |           |
| pagaAreaComun     |           |
+-------------------+           |
       |                        |
       | 1:N                    v 1:N
       |                 +--------------------+
       +---------------->|      LECTURA       |
          (idSubmedidor) +--------------------+
                         | PK idLectura       |
                         | FK idSubmedidor    |
                         | FK mesPeriodo      |
                         | lecturaAnterior    |
                         | lecturaActual      |
                         | consumoKwh         |
                         +--------------------+
```

---

## 7. Observaciones Técnicas

### 7.1. Coexistencia de Room y Cloud Firestore
- **Room (`AppDatabase`):** Fue diseñado inicialmente como almacén de persistencia relacional local (entidades SQLite con `@Entity`, DAOs y transacciones locales).
- **Cloud Firestore:** Opera como la fuente remota principal y repositorio en tiempo real. Todas las operaciones en la capa de datos (`InquilinoRepository`, `LecturaRepository`) sincronizan directamente las colecciones bajo la ruta de usuario autenticado (`usuarios/{uid}/...`) mediante `callbackFlow` y `addSnapshotListener`.

### 7.2. Identificadores y Claves
- En **Room**, las claves primarias numéricas utilizan autoincremento (`Long`), mientras que en **Firestore** se utilizan IDs de documento de tipo `String`.
- En Firestore, para las facturas se usa el periodo saneado (`sanitizeDocId(factura.mesPeriodo)`) sustituyendo caracteres especiales como `/` por `_` para evitar anomalías en las rutas de documentos.
- Para las lecturas en Firestore, se busca y filtra mediante queries estructuradas por periodo (`whereEqualTo("mesPeriodo", mes)`).

### 7.3. Manejo de Preferencias y Perfil del Arrendador
- Los datos de pago del propietario (`nombrePropietario`, `numeroPago`, `diaLimitePago`) no están modelados en una tabla de Room; se persisten localmente en `SharedPreferences` mediante `SettingsManager` para acceso síncrono rápido y se respaldan en la nube en el documento Firestore `usuarios/{uid}/perfil/datos_pago`.

### 7.4. Migraciones
- `AppDatabase` cuenta con configuración `fallbackToDestructiveMigration()`. En caso de cambios estructurales en esquemas locales de SQLite, las tablas locales se regeneran limpiamente sin bloquear el flujo proveniente de Firestore.
