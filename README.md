<!-----



Conversion time: 4.783 seconds.


Using this Markdown file:

1. Paste this output into your source file.
2. See the notes and action items below regarding this conversion run.
3. Check the rendered output (headings, lists, code blocks, tables) for proper
   formatting and use a linkchecker before you publish this page.

Conversion notes:

* Docs™ to Markdown version 2.0β2
* Sun Aug 02 2026 10:12:55 GMT-0700 (hora de verano del Pacífico)
* Source doc: Documento de Especificaciones Técnicas y Funcionales - App Submedidores de Luz
* This is a partial selection. Check to make sure intra-doc links work.
* Tables are currently converted to HTML tables.
----->



# Documento de Especificaciones Técnicas y Funcionales (PRD)


## 

---
Aplicación de Gestión y Cobro de Submedidores de Luz

**Versión:** 1.0

**Estado:** Aprobado para desarrollo

**Fecha de Emisión:** Agosto 2026


## 1. Resumen Ejecutivo y Objetivos

El presente documento especifica los requisitos técnicos y funcionales para el desarrollo de una aplicación móvil/web orientada a la administración de submedidores eléctricos en propiedades multifamiliares o locales comerciales. El sistema automatizará el cálculo de distribución proporcional de costos del recibo principal emitido por la empresa proveedora de energía, gestionará consumos de áreas comunes y facilitará la emisión de recibos individuales y el envío de notificaciones de cobro a través de canales digitales como WhatsApp.


### Objetivos Principales



* **Automatización:** Eliminar errores manuales de cálculo y el uso recurrente de hojas de cálculo complejas.
* **Transparencia:** Garantizar que cada inquilino entienda el desglose de su consumo y su contribución a áreas compartidas.
* **Agilidad en Cobros:** Reducir el tiempo necesario para emitir recibos y realizar notificaciones a menos de 5 minutos mensuales.


## 2. Lógica de Negocio y Algoritmo de Cálculo

La aplicación utiliza un algoritmo de cálculo basado en la participación relativa respecto al total de lecturas registradas en los submedidores internos. Este método absorbe dinámicamente variaciones tarifarias, cargos fijos, alumbrado público e impuestos (IGV) incluidos en la factura principal.


### 2.1. Fórmulas de Cálculo



1. **Consumo por Submedidor ($C_i$):** \
$$C_i = L_{actual, i} - L_{anterior, i}$$
2. **Consumo Total de Submedidores ($C_{total}$):** \
$$C_{total} = \sum_{i=1}^{n} C_i$$
3. **Porcentaje de Participación ($P_i$):** \
$$P_i = \frac{C_i}{C_{total}} \times 100$$
4. **Costo Directo por Submedidor ($M_i$):** \
$$M_i = P_i \times \text{Monto Total Recibo Principal (S/.)}$$
5. **Prorrateo de Áreas Comunes ($AC_{asignado}$):** \
Si un submedidor $k$ está catalogado como área común y es compartido entre un grupo $G$ de inquilinos: \
$$\text{Cuota Compartida} = \frac{M_k}{\text{Cantidad de inquilinos en } G}$$
6. **Monto Total Individual a Pagar ($T_i$):** \
$$T_i = M_i + \sum \text{Cuotas Compartidas Asignadas}$$


## 3. Arquitectura y Flujo de Pantallas


### 3.1. Pantalla 1: Dashboard (Inicio)



* **Resumen del Mes:** Visualización del periodo actual, consumo global registrado en submedidores vs. recibo de la compañía, e importe total consolidado.
* **Listado de Estado de Submedidores:** Muestra cada espacio/inquilino con su consumo actual y una etiqueta visual de estado (*Lectura Pendiente*, *Por Cobrar*, *Pagado*).


### 3.2. Pantalla 2: Registro de Lecturas



* **Formulario de Lectura:** Selección del submedidor, visualización de lectura anterior (bloqueada para evitar alteraciones), e ingreso de lectura actual.
* **Validación en Tiempo Real:** Alerta inmediata si la lectura ingresada es menor a la anterior.
* **Módulo Multimedidor:** Soporte para inquilinos que cuentan con más de un submedidor asignado (ej. Vivienda + Tienda + Cocina).


### 3.3. Pantalla 3: Reportes y Comprobantes



* **Vista Consolidada (Tabla Control estilo Excel):** Muestra lecturas iniciales, finales, consumo en kWh, porcentaje de participación, costo directo, costo de áreas comunes y total a pagar.
* **Generación de Recibos Individuales:** Vista detallada por inquilino exportable en PDF.
* **Integración WhatsApp:** Botón directo para disparar un mensaje con la plantilla de cobro formateada.


### 3.4. Pantalla 4: Configuración y Tarifas



* **Gestión de Submedidores e Inquilinos:** Altas, bajas y modificaciones de medidores, asociando inquilino titular y tipo de medidor (Individual o Área Común).
* **Asignación de Áreas Comunes:** Definición de qué inquilinos comparten los costos de submedidores comunes (ej. Baño 2do Piso).
* **Parámetros de Factura Principal:** Captura mensual del consumo global (kWh) y monto total (S/.) del recibo emitido por la empresa eléctrica.


## 4. Estructura de la Base de Datos


<table>
  <tr>
   <td><strong>Tabla</strong>
   </td>
   <td><strong>Campo</strong>
   </td>
   <td><strong>Tipo de Dato</strong>
   </td>
   <td><strong>Descripción</strong>
<p>
 
   </td>
  </tr>
  <tr>
   <td><strong>Inquilinos</strong>
   </td>
   <td>id_inquilino
   </td>
   <td>UUID / Integer
   </td>
   <td>Identificador único del inquilino
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>nombre_completo
   </td>
   <td>String
   </td>
   <td>Nombre del inquilino
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>telefono_whatsapp
   </td>
   <td>String
   </td>
   <td>Número de contacto para envío de recibo
   </td>
  </tr>
  <tr>
   <td><strong>Submedidores</strong>
   </td>
   <td>id_medidor
   </td>
   <td>UUID / Integer
   </td>
   <td>Identificador del submedidor
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>nombre_espacio
   </td>
   <td>String
   </td>
   <td>Ej. "Depa 101", "Baño 2do piso", "Tienda"
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>es_area_comun
   </td>
   <td>Boolean
   </td>
   <td>TRUE si el costo se prorratea entre varios
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>id_inquilino_titular
   </td>
   <td>UUID / Integer
   </td>
   <td>Inquilino asociado (null si es área común)
   </td>
  </tr>
  <tr>
   <td><strong>Lecturas</strong>
   </td>
   <td>id_lectura
   </td>
   <td>UUID / Integer
   </td>
   <td>Identificador del registro de lectura
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>id_medidor
   </td>
   <td>UUID / Integer
   </td>
   <td>FK a Submedidores
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>mes_periodo
   </td>
   <td>Date / String
   </td>
   <td>Periodo de lectura (ej. "2026-06")
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>lectura_anterior
   </td>
   <td>Decimal
   </td>
   <td>Valor del contador en el mes anterior
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>lectura_actual
   </td>
   <td>Decimal
   </td>
   <td>Valor del contador en el mes actual
   </td>
  </tr>
  <tr>
   <td><strong>Factura_General</strong>
   </td>
   <td>id_factura
   </td>
   <td>UUID / Integer
   </td>
   <td>Registro de la factura de la empresa de luz
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>mes_periodo
   </td>
   <td>Date / String
   </td>
   <td>Periodo del recibo oficial
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>kwh_totales
   </td>
   <td>Decimal
   </td>
   <td>Total kWh consumidos según recibo oficial
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>monto_total_soles
   </td>
   <td>Decimal
   </td>
   <td>Monto total a pagar a la empresa de luz
   </td>
  </tr>
</table>



## 5. Especificación del Mensaje de WhatsApp

La aplicación generará dinámicamente un texto estructurado para ser enviado vía API de WhatsApp Web/Business:


    📄 *RECIBO DE CONSUMO DE LUZ - [MES/AÑO]*


    👤 *Inquilino:* [Nombre del Inquilino]


    ____________________________________


    🔹 *1. Consumo Propio:*


    • Submedidor: [Nombre Espacio]


    • Lectura Anterior: [Lectura_Ant] kWh


    • Lectura Actual: [Lectura_Act] kWh


    • Consumo: [kWh_Consumidos] kWh ([Porcentaje]% del total)


    • Subtotal: S/ [Monto_Directo]


    🔹 *2. Áreas Comunes Compartidas:*


    • [Nombre Área Común] ([kWh_Comun] kWh ÷ [Num_Inquilinos]): S/ [Monto_Compartido]


    ____________________________________


    💰 *TOTAL A PAGAR:* *S/ [Monto_Total]*


    📌 *Por favor realizar el abono mediante Yape / Transferencia antes del [Fecha Límite].*


## 6. Recomendaciones Tecnológicas para la Implementación



* **Plataformas No-Code / Low-Code recomendadas:** FlutterFlow, Glide, AppSheet o Retool (para un despliegue súper rápido utilizando Google Sheets o PostgreSQL como backend).
* **Base de Datos:** PostgreSQL / Supabase o Firebase para persistencia en tiempo real.
* **Generación de Documentos:** Integración con librerías HTML to PDF para la exportación de comprobantes oficiales.
