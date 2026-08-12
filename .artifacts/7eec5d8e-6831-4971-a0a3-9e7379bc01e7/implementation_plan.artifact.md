# Plan de Implementación: Distribución Matemática Exacta y Cobertura Total de Inquilinos

El objetivo es asegurar que la suma de los cobros individuales sea exactamente igual al monto del recibo general, independientemente de si hay lecturas faltantes, estimados de la compañía eléctrica o desfases en la toma de lecturas.

## Análisis del Problema Actual
1. **Inquilinos Ausentes:** Si un inquilino no tiene una lectura registrada para el mes seleccionado, no aparece en la lista y no se le asigna ninguna cuota (ni siquiera la de área común). Esto hace que el monto total "recaudado" sea menor al del recibo.
2. **Desajuste de Lecturas:** La suma de los submedidores rara vez coincide con el medidor principal. La lógica debe absorber esta diferencia de forma transparente.

## Lógica Propuesta: "Cierre de Caja" Mensual
Para garantizar la coincidencia del 100%:
1. **Pivote en Submedidores:** La lista del resumen se generará a partir de la lista de **todos** los submedidores registrados (que no sean áreas comunes), no solo de las lecturas encontradas.
2. **Cálculo de Consumos Propios:**
   - Se busca la lectura correspondiente al mes para cada submedidor.
   - Si no existe, su consumo propio es `0 kWh`.
3. **Monto Remanente (A repartir):**
   - `Monto Total Recibo` - `Suma de (Consumos Propios * Precio Unitario)`.
   - Este remanente incluye: Áreas comunes medidas, fugas de energía, consumos no medidos del principal y ajustes por "estimados" de la compañía.
4. **Distribución Equitativa:** El remanente se divide entre **todos** los inquilinos que tienen activado `pagaAreaComun`, incluso si no tienen lectura propia este mes.

## Cambios Propuestos

### Actividades

#### [MODIFY] [ResumenCobrosActivity.kt](file:///D:/Usuarios/Fernando/AndroidStudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt)
- Cambiar la fuente de datos principal de `lecturas` a `submedidores`.
- Implementar la lógica de búsqueda de lecturas opcionales.
- Recalcular la distribución del monto común basándose en la lista completa de pagadores.

## Plan de Verificación

### Pruebas Manuales
1. Tener 3 inquilinos registrados.
2. Ingresar un recibo de S/ 120.00.
3. Ingresar lecturas para solo 2 de los 3 inquilinos.
4. Verificar que en el Resumen aparezcan los 3 inquilinos.
5. Verificar que la suma de los 3 cobros sea exactamente S/ 120.00.
