# Corrección de errores de eliminación en Configuración

Se han identificado dos errores de compilación en `ConfiguracionActivity.kt` debido a que las funciones para eliminar registros no existen en los DAOs de la base de datos Room.

## Cambios Propuestos

### Componente de Datos (Room DAOs)

#### [MODIFY] [InquilinoDao.kt](file:///D:/Usuarios/Fernando/AndroidStudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/InquilinoDao.kt)
- Agregar la anotación `@Delete` y la función `eliminarInquilino(inquilino: Inquilino)`.

#### [MODIFY] [SubmedidorDao.kt](file:///D:/Usuarios/Fernando/AndroidStudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/SubmedidorDao.kt)
- Agregar la anotación `@Delete` y la función `eliminarSubmedidor(submedidor: Submedidor)`.

## Plan de Verificación

### Pruebas de Compilación
- Ejecutar un build del proyecto para asegurar que las referencias en `ConfiguracionActivity` ahora se resuelven correctamente.

### Pruebas Manuales
- Abrir la pantalla de configuración, editar un inquilino y probar el botón "Eliminar" para confirmar que el registro desaparece de la lista.
