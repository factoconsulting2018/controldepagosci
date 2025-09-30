# Script específico para formatear "Clientes de Contabilidad Totales.xlsx"
# Soluciona el error "Cannot get a NUMERIC value from a STRING cell"

param(
    [Parameter(Mandatory=$false)]
    [string]$ArchivoExcel = "C:\Users\ronal\OneDrive\Escritorio\CONTROLDEPAGOS\Clientes de Contabilidad Totales.xlsx",
    
    [Parameter(Mandatory=$false)]
    [string]$Contrasena = "celeste"
)

# Verificar si el archivo existe
if (-not (Test-Path $ArchivoExcel)) {
    Write-Error "El archivo '$ArchivoExcel' no existe."
    Write-Host "Verifica que el archivo esté en la ruta correcta." -ForegroundColor Red
    exit 1
}

Write-Host "=== FORMATEADOR ESPECÍFICO PARA CLIENTES DE CONTABILIDAD ===" -ForegroundColor Green
Write-Host "Archivo: $ArchivoExcel" -ForegroundColor Yellow
Write-Host "Contraseña: $Contrasena" -ForegroundColor Yellow
Write-Host ""

try {
    # Crear archivo de respaldo
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $backupFile = $ArchivoExcel -replace '\.xlsx$', "_backup_$timestamp.xlsx"
    Write-Host "Creando respaldo: $backupFile" -ForegroundColor Cyan
    Copy-Item $ArchivoExcel $backupFile
    
    # Crear archivo de instrucciones específicas
    $instruccionesFile = $ArchivoExcel -replace '\.xlsx$', '_INSTRUCCIONES_ESPECIFICAS.txt'
    
    $instrucciones = @"
=== INSTRUCCIONES ESPECÍFICAS PARA CLIENTES DE CONTABILIDAD ===

Archivo: $ArchivoExcel
Contraseña: $Contrasena
Fecha: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

PROBLEMA DETECTADO:
- Error: "Cannot get a NUMERIC value from a STRING cell"
- Filas afectadas: 2 a 997 (996 filas)
- Causa: Inconsistencias en tipos de datos en las celdas

SOLUCIÓN PASO A PASO:

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

3. FORMATEAR TODAS LAS COLUMNAS COMO TEXTO
   - Seleccionar todas las columnas A-J
   - Clic derecho → Formato de celdas
   - Pestaña "Número" → Categoría "Texto"
   - Clic "Aceptar"

4. VALIDAR DATOS ESPECÍFICOS
   - Columna A (Nombre): NO puede estar vacía
   - Columna B (Cédula): Convertir números a texto
   - Columna E (Teléfono): Convertir números a texto
   - Columna H (Patentado): "Sí" o "No"
   - Columna I (Pendiente Pago): "Sí" o "No"

5. ELIMINAR FÓRMULAS PROBLEMÁTICAS
   - Buscar celdas con fórmulas (Ctrl+G → Especial → Fórmulas)
   - Copiar y pegar como valores (Ctrl+C → Pegado especial → Valores)

6. VERIFICAR FILAS VACÍAS
   - Eliminar filas completamente vacías
   - No dejar filas vacías entre los datos

7. GUARDAR
   - Guardar como .xlsx
   - Mantener la contraseña: $Contrasena

FORMATO ESPERADO:

A           B          C        D              E           F         G           H    I    J
Nombre      Cédula     Tipo     Representante  Teléfono    CI-FC     Ejecutivo   Pat  Pag  Régimen
Juan Pérez  12345678   Físico   -              5551234567  CI-123    Carlos L.   Sí   No   Simplificado
María G.    87654321   Jurídico Ana Torres     5559876543  CI-456    Luis M.     No   Sí   Común

VALIDACIONES CRÍTICAS:
- Todas las celdas deben ser de tipo TEXTO
- No debe haber fórmulas en las celdas de datos
- La columna Nombre NO puede estar vacía
- Los valores booleanos deben ser "Sí" o "No"

SOLUCIÓN AUTOMÁTICA:
Si el problema persiste, usa el script automático:
.\FormatearExcel.ps1 -ArchivoExcel "$ArchivoExcel" -Contrasena "$Contrasena"

ARCHIVO DE RESPALDO:
$backupFile

Una vez formateado correctamente, el archivo estará listo para importar sin errores.
"@

    Write-Host "Creando archivo de instrucciones específicas: $instruccionesFile" -ForegroundColor Cyan
    $instrucciones | Out-File -FilePath $instruccionesFile -Encoding UTF8
    
    Write-Host ""
    Write-Host "=== PROCESO COMPLETADO ===" -ForegroundColor Green
    Write-Host "✅ Respaldo creado: $backupFile" -ForegroundColor Green
    Write-Host "✅ Instrucciones específicas creadas: $instruccionesFile" -ForegroundColor Green
    Write-Host ""
    Write-Host "PRÓXIMOS PASOS:" -ForegroundColor Yellow
    Write-Host "1. Abrir el archivo: $ArchivoExcel" -ForegroundColor White
    Write-Host "2. Leer las instrucciones: $instruccionesFile" -ForegroundColor White
    Write-Host "3. Formatear manualmente según las instrucciones específicas" -ForegroundColor White
    Write-Host "4. Guardar el archivo" -ForegroundColor White
    Write-Host "5. Importar en la app Checklist" -ForegroundColor White
    Write-Host ""
    Write-Host "Contraseña del archivo: $Contrasena" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "⚠️  IMPORTANTE: Este archivo tiene problemas específicos de formato." -ForegroundColor Red
    Write-Host "   Sigue las instrucciones específicas para solucionarlos." -ForegroundColor Red
    
} catch {
    Write-Error "Error durante el proceso: $($_.Exception.Message)"
    exit 1
}

# Función para mostrar ayuda
function Show-Help {
    Write-Host "=== AYUDA - FORMATEADOR ESPECÍFICO ===" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Uso:" -ForegroundColor Yellow
    Write-Host "  .\FormatearExcel_Especifico.ps1 [-ArchivoExcel 'ruta\archivo.xlsx'] [-Contrasena 'contraseña']"
    Write-Host ""
    Write-Host "Parámetros:" -ForegroundColor Yellow
    Write-Host "  -ArchivoExcel    Ruta al archivo Excel (default: Clientes de Contabilidad Totales.xlsx)"
    Write-Host "  -Contrasena      Contraseña del archivo Excel (default: 'celeste')"
    Write-Host ""
    Write-Host "Ejemplos:" -ForegroundColor Yellow
    Write-Host "  .\FormatearExcel_Especifico.ps1"
    Write-Host "  .\FormatearExcel_Especifico.ps1 -ArchivoExcel 'C:\MiArchivo.xlsx'"
    Write-Host "  .\FormatearExcel_Especifico.ps1 -Contrasena 'miPassword'"
    Write-Host ""
    Write-Host "Este script:" -ForegroundColor Green
    Write-Host "  ✅ Crea un respaldo con timestamp"
    Write-Host "  ✅ Genera instrucciones específicas para el problema detectado"
    Write-Host "  ✅ Incluye validaciones críticas"
    Write-Host "  ✅ Proporciona solución paso a paso"
    Write-Host ""
}

# Mostrar ayuda si se solicita
if ($args -contains "-h" -or $args -contains "--help") {
    Show-Help
    exit 0
}