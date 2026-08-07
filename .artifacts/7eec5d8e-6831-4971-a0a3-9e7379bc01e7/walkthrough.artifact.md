# Corrección de Funciones de Eliminación

He corregido los errores de compilación habilitando la capacidad de borrar registros de la base de datos.

## Cambios Realizados

### 🗄️ Base de Datos (DAOs)
- **InquilinoDao:** Se añadió la función `eliminarInquilino` con la anotación `@Delete` para permitir borrar inquilinos de la tabla.
- **SubmedidorDao:** Se añadió la función `eliminarSubmedidor` con la anotación `@Delete` para permitir borrar submedidores de la tabla.

## Verificación
- **Compilación:** El proyecto ahora compila correctamente (`assembleDebug` exitoso).
- **Funcionalidad:** La pantalla de Configuración ahora puede ejecutar las acciones de "Eliminar" desde el diálogo de edición sin errores de referencia.

> [!TIP]
> Ahora puedes gestionar tu lista de inquilinos y medidores de forma completa (Crear, Editar y Eliminar).
