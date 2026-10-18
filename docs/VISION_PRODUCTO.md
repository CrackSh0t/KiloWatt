# Visión del Producto - Kilowatt

Este documento describe la propuesta de valor, los usuarios objetivo, los casos de uso esenciales y el ciclo de vida operativo de **Kilowatt**.

---

## 1. Objetivo de la Aplicación

**Kilowatt** tiene como objetivo simplificar, automatizar y brindar total transparencia al proceso de prorrateo y cobro del consumo eléctrico en inmuebles compartidos (casas multifamiliares, quintas, departamentos divididos o locales comerciales) que cuentan con un único suministro eléctrico matriz y múltiples submedidores internos.

### Principales problemas que resuelve:
* **Complejidad y errores de cálculo manual:** Elimina la necesidad de hojas de cálculo o fórmulas complejas para obtener el costo real por kWh y distribuir el consumo de áreas comunes.
* **Falta de transparencia hacia el inquilino:** Provee un desglose formal y verificable (lectura anterior, lectura actual, consumo individual, costo unitario y cuota de áreas comunes).
* **Fricción en la cobranza:** Automatiza la generación de comprobantes digitales (PDF) y mensajes listos para ser enviados por WhatsApp con la información bancaria y fecha de vencimiento.

---

## 2. Usuario Objetivo

* **Perfil Principal (Administrador / Propietario):**
  * Propietarios de viviendas, arrendadores o administradores de condominios pequeños que gestionan el cobro de servicios a múltiples inquilinos.
  * Buscan una herramienta móvil ágil, rápida de consultar al momento de inspeccionar los medidores físicos y que no requiera configuraciones complejas.
* **Perfil Secundario (Inquilino / Consumidor Final):**
  * Destinatario final de la información generada. Requiere claridad sobre cómo se calculó el monto cobrado para evitar discrepancias con el arrendador.

---

## 3. Casos de Uso Principales

### CU-01: Configuración de Datos de Pago y Perfil
* El administrador configura su nombre, métodos de pago preferidos (cuentas bancarias, números de billeteras digitales como Yape/Plin) y el día de vencimiento mensual para que aparezcan en los comprobantes y recordatorios.

### CU-02: Gestión de Inquilinos y Submedidores
* Alta, modificación y baja de espacios alquilados y sus submedidores correspondientes.
* Definición de propiedades del submedidor:
  * Si el espacio corresponde a un área común o a un inquilino particular.
  * Si el inquilino asume el pago proporcional de las áreas comunes.
  * Asociación de datos de contacto (teléfono para WhatsApp).

### CU-03: Registro del Recibo Matriz (Factura General)
* Al recibir el recibo oficial de la empresa suministradora de energía, el administrador ingresa:
  * Periodo de facturación (mes/año).
  * Consumo total facturado en kWh.
  * Importe total a pagar en Soles (S/).

### CU-04: Toma y Registro de Lecturas de Submedidores
* Registro de la lectura actual del medidor de cada espacio para el periodo correspondiente.
* Validación automática de consistencia (evitar lecturas menores a la anterior o números negativos).

### CU-05: Cálculo y Prorrateo Automático de Cobros
* Deducción del costo promedio por kWh a partir del recibo matriz.
* Determinación del remanente de energía no registrado en submedidores individuales (atribuible a áreas comunes o pérdidas).
* Distribución equitativa del costo de áreas comunes entre los inquilinos configurados para asumirla.
* Cálculo del total neto a pagar por cada inquilino.

### CU-06: Notificación y Emisión de Comprobantes
* Envío de mensaje preformateado y detallado vía WhatsApp directamente al contacto del inquilino.
* Generación de comprobante individual en formato PDF listo para imprimir o compartir.
* Generación de reporte consolidado mensual para archivo y control interno.

---

## 4. Flujo de Trabajo Esperado (Ciclo Mensual)

El flujo operativo estándar sigue los siguientes pasos secuenciales:

```
┌───────────────────────────────────────────────┐
│ 1. Llegada del Recibo Principal               │
│    (Se ingresa monto total S/ y kWh del mes)  │
└──────────────────────┬────────────────────────┘
                       │
┌──────────────────────▼────────────────────────┐
│ 2. Ronda de Inspección de Submedidores        │
│    (Se capturan las lecturas actuales en la   │
│     pantalla de Lecturas)                     │
└──────────────────────┬────────────────────────┘
                       │
┌──────────────────────▼────────────────────────┐
│ 3. Revisión de Balances y Cobros              │
│    (La app calcula automáticamente consumo    │
│     individual, áreas comunes y totales)      │
└──────────────────────┬────────────────────────┘
                       │
┌──────────────────────▼────────────────────────┐
│ 4. Cobranza y Comunicación                    │
│    (Se envían comprobantes por WhatsApp o PDF │
│     a cada inquilino con la fecha límite)     │
└───────────────────────────────────────────────┘
```
