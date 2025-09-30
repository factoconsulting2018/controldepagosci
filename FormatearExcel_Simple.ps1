# Script simple para formatear archivos Excel para importación en la app Checklist
# Versión simplificada que no requiere Microsoft Office

param(
    [Parameter(Mandatory=$true)]
    [string]$ArchivoExcel,
    
    [Parameter(Mandatory=$false)]
    [string]$Contrasena = "celeste"
)

# Verificar si el archivo existe
if (-not (Test-Path $ArchivoExcel)) {
    Write-Error "El archivo '$ArchivoExcel' no existe."
    exit 1
}

# Verificar si es un archivo Excel
$extension = [System.IO.Path]::GetExtension($ArchivoExcel).ToLower()
if ($extension -notin @('.xlsx', '.xls')) {
    Write-Error "El archivo debe ser un archivo Excel (.xlsx o .xls)."
    exit 1
}

Write-Host "=== FORMATEADOR SIMPLE DE EXCEL PARA CHECKLIST APP ===" -ForegroundColor Green
Write-Host "Archivo: $ArchivoExcel" -ForegroundColor Yellow
Write-Host "Contraseña: $Contrasena" -ForegroundColor Yellow
Write-Host ""

try {
    # Crear archivo de respaldo
    $backupFile = $ArchivoExcel -replace '\.xlsx?$', '_backup_original.xlsx'
    Write-Host "Creando respaldo: $backupFile" -ForegroundColor Cyan
    Copy-Item $ArchivoExcel $backupFile
    
    # Crear archivo de instrucciones
    $instruccionesFile = $ArchivoExcel -replace '\.xlsx?$', '_INSTRUCCIONES_FORMATO.txt'
    
    $instrucciones = @"
=== INSTRUCCIONES PARA FORMATEAR EL ARCHIVO EXCEL ===

Archivo: $ArchivoExcel
Contraseña: $Contrasena

FORMATO REQUERIDO PARA LA IMPORTACIÓN:

Columna A - NOMBRE (OBLIGATORIO)
- Tipo: Texto
- Formato: Cualquier texto
- Ejemplo: "Juan Pérez"

Columna B - CÉDULA (OPCIONAL)
- Tipo: Texto o Número
- Formato: Texto o número
- Ejemplo: "12345678" o 12345678

Columna C - TIPO PERSONA (OPCIONAL)
- Tipo: Texto
- Formato: "Físico" o "Jurídico"
- Valor por defecto: "Físico"
- Ejemplo: "Físico"

Columna D - REPRESENTANTE (OPCIONAL)
- Tipo: Texto
- Formato: Cualquier texto
- Ejemplo: "María González"

Columna E - TELÉFONO (OPCIONAL)
- Tipo: Texto o Número
- Formato: Texto o número
- Ejemplo: "5551234567" o 5551234567

Columna F - CI-FC (OPCIONAL)
- Tipo: Texto
- Formato: Cualquier texto
- Ejemplo: "CI-123456"

Columna G - EJECUTIVO (OPCIONAL)
- Tipo: Texto
- Formato: Cualquier texto
- Ejemplo: "Carlos López"

Columna H - PATENTADO (OPCIONAL)
- Tipo: Booleano
- Formato: "Sí"/"No" o TRUE/FALSE
- Valor por defecto: "No"
- Ejemplo: "Sí" o TRUE

Columna I - PENDIENTE PAGO (OPCIONAL)
- Tipo: Booleano
- Formato: "Sí"/"No" o TRUE/FALSE
- Valor por defecto: "No"
- Ejemplo: "No" o FALSE

Columna J - TIPO RÉGIMEN (OPCIONAL)
- Tipo: Texto
- Formato: Cualquier texto
- Ejemplo: "Simplificado"

INSTRUCCIONES DE FORMATEO:

1. ABRIR EL ARCHIVO EXCEL
   - Abrir: $ArchivoExcel
   - Contraseña: $Contrasena

2. CONFIGURAR ENCABEZADOS (Fila 1)
   - A1: "Nombre"
   - B1: "Cédula"
   - C1: "Tipo Persona"
   - D1: "Representante"
   - E1: "Teléfono"
   - F1: "CI-FC"
   - G1: "Ejecutivo"
   - H1: "Patentado"
   - I1: "Pendiente Pago"
   - J1: "Tipo Régimen"

3. FORMATEAR COLUMNAS
   - Seleccionar todas las columnas A-J
   - Formato de celda → Texto
   - Esto evita que Excel convierta números automáticamente

4. VALIDAR DATOS
   - Verificar que la columna A (Nombre) no esté vacía
   - Verificar que los valores booleanos sean "Sí"/"No" o TRUE/FALSE
   - Verificar que no haya filas vacías entre los datos

5. GUARDAR
   - Guardar como .xlsx
   - Mantener la contraseña: $Contrasena

EJEMPLO DE DATOS CORRECTOS:

A           B          C        D              E           F         G           H    I    J
Nombre      Cédula     Tipo     Representante  Teléfono    CI-FC     Ejecutivo   Pat  Pag  Régimen
Juan Pérez  12345678   Físico   -              5551234567  CI-123    Carlos L.   Sí   No   Simplificado
María G.    87654321   Jurídico Ana Torres     5559876543  CI-456    Luis M.     No   Sí   Común

NOTAS IMPORTANTES:
- La primera fila debe contener los encabezados
- Los datos deben empezar desde la fila 2
- No dejar filas vacías entre los datos
- El archivo debe estar en formato .xlsx
- La contraseña debe ser: $Contrasena

Una vez formateado, el archivo estará listo para importar en la app Checklist.
"@

    Write-Host "Creando archivo de instrucciones: $instruccionesFile" -ForegroundColor Cyan
    $instrucciones | Out-File -FilePath $instruccionesFile -Encoding UTF8
    
    Write-Host ""
    Write-Host "=== PROCESO COMPLETADO ===" -ForegroundColor Green
    Write-Host "✅ Respaldo creado: $backupFile" -ForegroundColor Green
    Write-Host "✅ Instrucciones creadas: $instruccionesFile" -ForegroundColor Green
    Write-Host ""
    Write-Host "PRÓXIMOS PASOS:" -ForegroundColor Yellow
    Write-Host "1. Abrir el archivo: $ArchivoExcel" -ForegroundColor White
    Write-Host "2. Leer las instrucciones: $instruccionesFile" -ForegroundColor White
    Write-Host "3. Formatear manualmente según las instrucciones" -ForegroundColor White
    Write-Host "4. Guardar el archivo" -ForegroundColor White
    Write-Host "5. Importar en la app Checklist" -ForegroundColor White
    Write-Host ""
    Write-Host "Contraseña del archivo: $Contrasena" -ForegroundColor Yellow
    
} catch {
    Write-Error "Error durante el proceso: $($_.Exception.Message)"
    exit 1
}

# Función para mostrar ayuda
function Show-Help {
    Write-Host "=== AYUDA - FORMATEADOR SIMPLE DE EXCEL ===" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Uso:" -ForegroundColor Yellow
    Write-Host "  .\FormatearExcel_Simple.ps1 -ArchivoExcel 'ruta\archivo.xlsx' [-Contrasena 'contraseña']"
    Write-Host ""
    Write-Host "Parámetros:" -ForegroundColor Yellow
    Write-Host "  -ArchivoExcel    Ruta completa al archivo Excel a formatear (OBLIGATORIO)"
    Write-Host "  -Contrasena      Contraseña del archivo Excel (OPCIONAL, default: 'celeste')"
    Write-Host ""
    Write-Host "Ejemplos:" -ForegroundColor Yellow
    Write-Host "  .\FormatearExcel_Simple.ps1 -ArchivoExcel 'C:\Datos\Clientes.xlsx'"
    Write-Host "  .\FormatearExcel_Simple.ps1 -ArchivoExcel 'C:\Datos\Clientes.xlsx' -Contrasena 'miPassword'"
    Write-Host ""
    Write-Host "El script:" -ForegroundColor Green
    Write-Host "  ✅ Crea un respaldo del archivo original"
    Write-Host "  ✅ Genera instrucciones detalladas de formateo"
    Write-Host "  ✅ No requiere Microsoft Office instalado"
    Write-Host "  ✅ Funciona con cualquier versión de Windows"
    Write-Host ""
    Write-Host "NOTA: Este script crea las instrucciones para formatear manualmente." -ForegroundColor Yellow
    Write-Host "Para formateo automático, use FormatearExcel.ps1 (requiere Microsoft Office)." -ForegroundColor Yellow
    Write-Host ""
}

# Mostrar ayuda si no se proporcionan parámetros
if ($args.Count -eq 0) {
    Show-Help
    exit 0
}
