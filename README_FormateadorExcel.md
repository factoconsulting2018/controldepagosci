# Formateador de Excel para Checklist App

Este conjunto de scripts de PowerShell te ayuda a formatear archivos Excel para que sean compatibles con la importación en la aplicación Checklist.

## 📁 Archivos Incluidos

- `FormatearExcel.ps1` - Script completo que formatea automáticamente (requiere Microsoft Office)
- `FormatearExcel_Simple.ps1` - Script simple que genera instrucciones (no requiere Office)
- `README_FormateadorExcel.md` - Este archivo de documentación

## 🚀 Uso Rápido

### Opción 1: Formateo Automático (Recomendado)
```powershell
.\FormatearExcel.ps1 -ArchivoExcel "C:\Ruta\TuArchivo.xlsx"
```

### Opción 2: Formateo Manual con Instrucciones
```powershell
.\FormatearExcel_Simple.ps1 -ArchivoExcel "C:\Ruta\TuArchivo.xlsx"
```

## 📋 Formato Requerido

El archivo Excel debe tener exactamente **10 columnas** en este orden:

| Columna | Nombre | Tipo | Requerido | Ejemplo |
|---------|--------|------|-----------|---------|
| A | **Nombre** | Texto | ✅ Sí | "Juan Pérez" |
| B | **Cédula** | Texto/Número | ❌ No | "12345678" |
| C | **Tipo Persona** | Texto | ❌ No | "Físico" |
| D | **Representante** | Texto | ❌ No | "María González" |
| E | **Teléfono** | Texto/Número | ❌ No | "5551234567" |
| F | **CI-FC** | Texto | ❌ No | "CI-123456" |
| G | **Ejecutivo** | Texto | ❌ No | "Carlos López" |
| H | **Patentado** | Booleano | ❌ No | "Sí" |
| I | **Pendiente Pago** | Booleano | ❌ No | "No" |
| J | **Tipo Régimen** | Texto | ❌ No | "Simplificado" |

## 🔧 Requisitos

### Para FormatearExcel.ps1 (Automático):
- Windows con Microsoft Office Excel instalado
- PowerShell 5.0 o superior
- Permisos para ejecutar scripts de PowerShell

### Para FormatearExcel_Simple.ps1 (Manual):
- Windows con PowerShell 5.0 o superior
- No requiere Microsoft Office

## 📝 Ejemplos de Uso

### Ejemplo 1: Archivo con contraseña por defecto
```powershell
.\FormatearExcel.ps1 -ArchivoExcel "C:\Datos\Clientes.xlsx"
```

### Ejemplo 2: Archivo con contraseña personalizada
```powershell
.\FormatearExcel.ps1 -ArchivoExcel "C:\Datos\Clientes.xlsx" -Contrasena "miPassword123"
```

### Ejemplo 3: Formateo manual
```powershell
.\FormatearExcel_Simple.ps1 -ArchivoExcel "C:\Datos\Clientes.xlsx"
```

## ⚙️ Configuración de PowerShell

Si es la primera vez que ejecutas scripts de PowerShell, necesitas habilitar la ejecución:

```powershell
# Ejecutar como Administrador
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

## 🎯 Qué Hace Cada Script

### FormatearExcel.ps1 (Automático)
- ✅ Abre el archivo Excel automáticamente
- ✅ Configura los encabezados correctos
- ✅ Formatea todas las columnas como texto
- ✅ Establece valores por defecto
- ✅ Ajusta el ancho de columnas
- ✅ Aplica formato de tabla
- ✅ Crea un respaldo del archivo original
- ✅ Guarda el archivo formateado

### FormatearExcel_Simple.ps1 (Manual)
- ✅ Crea un respaldo del archivo original
- ✅ Genera un archivo de instrucciones detalladas
- ✅ No requiere Microsoft Office
- ✅ Funciona en cualquier sistema Windows

## 📊 Ejemplo de Archivo Formateado

```
A           B          C        D              E           F         G           H    I    J
Nombre      Cédula     Tipo     Representante  Teléfono    CI-FC     Ejecutivo   Pat  Pag  Régimen
Juan Pérez  12345678   Físico   -              5551234567  CI-123    Carlos L.   Sí   No   Simplificado
María G.    87654321   Jurídico Ana Torres     5559876543  CI-456    Luis M.     No   Sí   Común
```

## 🔒 Seguridad

- **Contraseña por defecto**: `celeste`
- **Formato de archivo**: `.xlsx` (Excel 2007+)
- **Respaldo automático**: Se crea antes de cualquier modificación

## ❗ Validaciones Importantes

1. **Nombre**: Campo obligatorio, no puede estar vacío
2. **Cédula**: Si hay duplicados, se mantiene el existente
3. **Teléfono**: Acepta texto o número
4. **Booleanos**: "Sí"/"No", TRUE/FALSE, 1/0
5. **Importación**: Mantiene existentes y agrega nuevos

## 🐛 Solución de Problemas

### Error: "El archivo no existe"
- Verifica que la ruta del archivo sea correcta
- Usa comillas si la ruta contiene espacios

### Error: "No se puede ejecutar scripts"
- Ejecuta PowerShell como Administrador
- Configura la política de ejecución

### Error: "Microsoft Office no encontrado"
- Usa `FormatearExcel_Simple.ps1` en su lugar
- O instala Microsoft Office

### Error: "Contraseña incorrecta"
- Verifica la contraseña del archivo
- La contraseña por defecto es `celeste`

## 📞 Soporte

Si encuentras problemas:

1. Verifica que el archivo sea `.xlsx` o `.xls`
2. Confirma que la contraseña sea correcta
3. Asegúrate de que PowerShell tenga permisos
4. Revisa que Microsoft Office esté instalado (para el script automático)

## 🔄 Actualizaciones

- **v1.0** - Versión inicial con formateo automático y manual
- Soporte para archivos protegidos con contraseña
- Validación de formato y estructura
- Creación automática de respaldos

---

**¡Tu archivo Excel estará listo para importar en la app Checklist!** 🎉
