# PRD: KiloWatt - Gestión de Consumo Eléctrico Compartido

**Versión:** 1.0
**Estado:** En Desarrollo
**Propósito:** Automatizar el cálculo y la distribución de costos eléctricos en propiedades con múltiples submedidores.

---

## 1. Visión General
KiloWatt es una herramienta diseñada para propietarios de inmuebles que comparten un medidor principal de energía eléctrica con varios inquilinos. La aplicación resuelve la complejidad de calcular cuánto debe pagar cada persona basándose en su consumo real (kWh) y el costo variable del recibo de la empresa eléctrica.

## 2. Objetivos del Producto
- **Transparencia:** Mostrar a cada inquilino el cálculo exacto de su consumo y el precio por kWh aplicado.
- **Eficiencia:** Eliminar los cálculos manuales propensos a errores.
- **Persistencia:** Mantener un historial de lecturas y pagos por cada periodo/mes.

## 3. Público Objetivo
- Propietarios de departamentos alquilados, cuartos o locales comerciales que utilizan submedidores internos.

---

## 4. Funcionalidades Principales

### 4.1. Configuración de la Propiedad
- **Registro de Inquilinos:** Almacenar nombre completo y teléfono (WhatsApp) para contacto directo.
- **Gestión de Submedidores:** Identificar cada espacio (ej: "Depa 101", "Pasillo 2do piso") y asociarlo a un inquilino o marcarlo como "Área Común".
- **Visualización de Lista:** Ver de forma rápida todos los inquilinos configurados en el sistema.

### 4.2. Registro de Factura Base (Recibo de Luz)
- **Ingreso de Datos Mensuales:** El usuario ingresa el monto total a pagar en Soles (S/) y el total de kWh consumidos según el recibo principal del proveedor de energía (Enel, Luz del Sur, etc.).
- **Cálculo de Precio Unitario:** El sistema calcula automáticamente el costo por kWh del mes en curso (`Monto Total / Total kWh`).

### 4.3. Control de Lecturas y Cobro
- **Registro de Lecturas:** Captura de lectura anterior y actual de cada submedidor.
- **Cálculo Automático:**
  - `Consumo = Lectura Actual - Lectura Anterior`
  - `Monto a Pagar = Consumo * Precio por kWh calculado`
- **Validación:** El sistema impide guardar lecturas actuales menores a las anteriores.

---

## 5. Arquitectura Técnica

### 5.1. Stack Tecnológico
- **Lenguaje:** Kotlin
- **Base de Datos:** SQLite via **Room Persistence Library** (Arquitectura Offline-First).
- **UI:** XML Layouts con Material Design 3.
- **Patrón:** MVVM / Repository (simplificado en actividades para la etapa inicial).

### 5.2. Entidades de Datos (Esquema Room)
- **Inquilino:** `idInquilino`, `nombreCompleto`, `telefono`.
- **Submedidor:** `idSubmedidor`, `nombreEspacio`, `esAreaComun`, `idInquilinoTitular`.
- **FacturaGeneral:** `mesPeriodo`, `kwhTotalesRecibo`, `montoTotalSoles`.
- **Lectura:** `mesPeriodo`, `lecturaAnterior`, `lecturaActual`, `consumoKwh`, `montoPagarSoles`.

---

## 6. Roadmap (Próximas Mejoras)
- [ ] **Exportación a WhatsApp:** Generar un mensaje pre-formateado con el detalle del cobro para enviarlo al inquilino con un clic.
- [ ] **Historial por Inquilino:** Ver gráficas de consumo mensual de un departamento específico.
- [ ] **Gestión de Áreas Comunes:** Prorratear automáticamente el consumo de pasadizos o baños comunes entre todos los inquilinos.
- [ ] **Soporte Multi-moneda:** Permitir el uso de otras monedas además del Sol (S/).
