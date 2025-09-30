# Instrucciones para el Sistema de Clientes en Checklist App

## Nuevas Funcionalidades Implementadas

### 1. Estructura de Datos de Cliente
Se ha agregado una nueva estructura de datos `Cliente` que incluye los siguientes campos:
- **Nombre**: Nombre del cliente
- **Cédula**: Número de cédula del cliente
- **Tipo de Persona**: Físico o Jurídico
- **Representante**: Nombre del representante (para personas jurídicas)
- **Teléfono**: Número de teléfono de contacto
- **CI-FC**: Código CI-FC
- **Ejecutivo**: Nombre del ejecutivo asignado
- **Patentado**: Estado de patente (checkbox)
- **Pendiente de Pago**: Estado de pago (checkbox)
- **Tipo de Régimen**: Tipo de régimen fiscal

### 2. Precarga Automática de Clientes
- **Precarga Única**: Al iniciar la aplicación, se cargan automáticamente los clientes desde los archivos disponibles
- **Archivos Soportados**: 
  - `C:\Users\ronal\OneDrive\Escritorio\controldepagos\Clientes_de_Contabilidad_Totales.json` (prioridad)
  - `C:\Users\ronal\OneDrive\Escritorio\controldepagos\Clientes de Contabilidad Totales.xlsx` (con contraseña "celeste")
- **Una Sola Vez**: Los datos se cargan solo una vez y se almacenan localmente en la aplicación

### 3. Formulario de Preguntas Actualizado
El formulario para agregar preguntas ahora incluye:
- **Sección de Información de la Pregunta**: Título, subtítulo, categoría y posición
- **Sección de Información del Cliente**: Todos los campos mencionados anteriormente
- **Botón "Seleccionar Cliente Existente"**: Permite elegir de los clientes ya precargados

### 4. Visualización de Clientes
- Las preguntas ahora muestran información del cliente asociado
- Formato: "Cliente: [Nombre] - [Cédula]"
- Se muestra en el subtítulo de cada pregunta

## Cómo Usar

### Precarga Automática:
1. Al abrir la aplicación por primera vez, se cargan automáticamente los clientes desde los archivos
2. Se muestra un mensaje con la cantidad de clientes precargados
3. Los datos se almacenan localmente y no se vuelven a cargar

### Para Agregar una Nueva Pregunta con Cliente:
1. Ir a la sección de Preguntas
2. Hacer clic en "Agregar Pregunta"
3. Completar la información de la pregunta (título, subtítulo, categoría, posición)
4. Completar la información del cliente (nombre y cédula son obligatorios)
5. Opcionalmente, hacer clic en "Seleccionar Cliente Existente" para elegir de los clientes precargados
6. Hacer clic en "Agregar"

### Para Seleccionar Cliente Existente:
1. En el formulario de agregar pregunta, hacer clic en "Seleccionar Cliente Existente"
2. Elegir el cliente deseado de la lista de clientes precargados
3. Los campos se llenarán automáticamente con los datos del cliente seleccionado

## Estructura del Archivo Excel
El archivo Excel debe tener las siguientes columnas (en orden):
1. Nombre
2. Cédula
3. Tipo de Persona (Físico/Jurídico)
4. Representante
5. Teléfono
6. CI-FC
7. Ejecutivo
8. Patentado (Si/No)
9. Pendiente de Pago (Si/No)
10. Tipo de Régimen

## Dependencias Agregadas
Se han agregado las siguientes dependencias para la lectura de archivos Excel:
- `org.apache.poi:poi:5.2.4`
- `org.apache.poi:poi-ooxml:5.2.4`
- `org.apache.poi:poi-scratchpad:5.2.4`

## Notas Importantes
- El archivo Excel debe estar protegido con contraseña "celeste"
- La primera fila del Excel debe contener los encabezados
- Los datos se cargan desde la segunda fila en adelante
- Si hay errores en la carga del Excel, se mostrará un mensaje de error
- Los clientes se almacenan localmente en la aplicación después de la carga
