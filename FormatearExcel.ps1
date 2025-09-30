# Script para formatear archivos Excel para importación en la app Checklist
# Autor: Asistente IA
# Fecha: $(Get-Date -Format "yyyy-MM-dd")

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

Write-Host "=== FORMATEADOR DE EXCEL PARA CHECKLIST APP ===" -ForegroundColor Green
Write-Host "Archivo: $ArchivoExcel" -ForegroundColor Yellow
Write-Host "Contraseña: $Contrasena" -ForegroundColor Yellow
Write-Host ""

try {
    # Cargar el ensamblado de Excel
    Add-Type -AssemblyName Microsoft.Office.Interop.Excel
    
    # Crear aplicación Excel
    $excel = New-Object -ComObject Excel.Application
    $excel.Visible = $false
    $excel.DisplayAlerts = $false
    
    Write-Host "Abriendo archivo Excel..." -ForegroundColor Cyan
    
    # Abrir el archivo
    $workbook = $excel.Workbooks.Open($ArchivoExcel, $false, $true, $null, $Contrasena)
    $worksheet = $workbook.Worksheets.Item(1)
    
    Write-Host "Archivo abierto exitosamente." -ForegroundColor Green
    
    # Obtener el rango usado
    $usedRange = $worksheet.UsedRange
    $lastRow = $usedRange.Rows.Count
    $lastCol = $usedRange.Columns.Count
    
    Write-Host "Filas encontradas: $lastRow" -ForegroundColor Cyan
    Write-Host "Columnas encontradas: $lastCol" -ForegroundColor Cyan
    
    # Verificar que tenga al menos 10 columnas
    if ($lastCol -lt 10) {
        Write-Warning "El archivo tiene menos de 10 columnas. Se agregarán columnas faltantes."
        
        # Agregar columnas faltantes
        for ($i = $lastCol + 1; $i -le 10; $i++) {
            $worksheet.Cells.Item(1, $i).Value2 = ""
        }
        $lastCol = 10
    }
    
    # Definir encabezados estándar
    $encabezados = @(
        "Nombre",
        "Cédula", 
        "Tipo Persona",
        "Representante",
        "Teléfono",
        "CI-FC",
        "Ejecutivo",
        "Patentado",
        "Pendiente Pago",
        "Tipo Régimen"
    )
    
    Write-Host "Configurando encabezados..." -ForegroundColor Cyan
    
    # Establecer encabezados en la primera fila
    for ($col = 1; $col -le 10; $col++) {
        $worksheet.Cells.Item(1, $col).Value2 = $encabezados[$col - 1]
        $worksheet.Cells.Item(1, $col).Font.Bold = $true
        $worksheet.Cells.Item(1, $col).Interior.Color = 0xCCCCCC  # Gris claro
    }
    
    Write-Host "Formateando datos..." -ForegroundColor Cyan
    
    # Formatear datos desde la fila 2
    for ($row = 2; $row -le $lastRow; $row++) {
        # Columna A (Nombre) - Texto
        $worksheet.Cells.Item($row, 1).NumberFormat = "@"
        
        # Columna B (Cédula) - Texto
        $worksheet.Cells.Item($row, 2).NumberFormat = "@"
        
        # Columna C (Tipo Persona) - Texto con validación
        $tipoPersona = $worksheet.Cells.Item($row, 3).Value2
        if ([string]::IsNullOrWhiteSpace($tipoPersona)) {
            $worksheet.Cells.Item($row, 3).Value2 = "Físico"
        }
        $worksheet.Cells.Item($row, 3).NumberFormat = "@"
        
        # Columna D (Representante) - Texto
        $worksheet.Cells.Item($row, 4).NumberFormat = "@"
        
        # Columna E (Teléfono) - Texto
        $worksheet.Cells.Item($row, 5).NumberFormat = "@"
        
        # Columna F (CI-FC) - Texto
        $worksheet.Cells.Item($row, 6).NumberFormat = "@"
        
        # Columna G (Ejecutivo) - Texto
        $worksheet.Cells.Item($row, 7).NumberFormat = "@"
        
        # Columna H (Patentado) - Booleano
        $patentado = $worksheet.Cells.Item($row, 8).Value2
        if ($patentado -eq $null -or [string]::IsNullOrWhiteSpace($patentado)) {
            $worksheet.Cells.Item($row, 8).Value2 = "No"
        }
        $worksheet.Cells.Item($row, 8).NumberFormat = "@"
        
        # Columna I (Pendiente Pago) - Booleano
        $pendientePago = $worksheet.Cells.Item($row, 9).Value2
        if ($pendientePago -eq $null -or [string]::IsNullOrWhiteSpace($pendientePago)) {
            $worksheet.Cells.Item($row, 9).Value2 = "No"
        }
        $worksheet.Cells.Item($row, 9).NumberFormat = "@"
        
        # Columna J (Tipo Régimen) - Texto
        $worksheet.Cells.Item($row, 10).NumberFormat = "@"
        
        # Mostrar progreso cada 100 filas
        if ($row % 100 -eq 0) {
            Write-Host "Procesando fila $row de $lastRow..." -ForegroundColor Yellow
        }
    }
    
    # Ajustar ancho de columnas
    Write-Host "Ajustando ancho de columnas..." -ForegroundColor Cyan
    $worksheet.Columns.Item(1).ColumnWidth = 25  # Nombre
    $worksheet.Columns.Item(2).ColumnWidth = 15  # Cédula
    $worksheet.Columns.Item(3).ColumnWidth = 12  # Tipo Persona
    $worksheet.Columns.Item(4).ColumnWidth = 20  # Representante
    $worksheet.Columns.Item(5).ColumnWidth = 15  # Teléfono
    $worksheet.Columns.Item(6).ColumnWidth = 12  # CI-FC
    $worksheet.Columns.Item(7).ColumnWidth = 15  # Ejecutivo
    $worksheet.Columns.Item(8).ColumnWidth = 10  # Patentado
    $worksheet.Columns.Item(9).ColumnWidth = 12  # Pendiente Pago
    $worksheet.Columns.Item(10).ColumnWidth = 15 # Tipo Régimen
    
    # Aplicar formato de tabla
    Write-Host "Aplicando formato de tabla..." -ForegroundColor Cyan
    $range = $worksheet.Range("A1:J$lastRow")
    $range.Borders.LineStyle = 1
    $range.Borders.Weight = 2
    
    # Congelar paneles
    $worksheet.Range("A2").Select()
    $excel.ActiveWindow.FreezePanes = $true
    
    # Crear archivo de respaldo
    $backupFile = $ArchivoExcel -replace '\.xlsx?$', '_backup_original.xlsx'
    Write-Host "Creando respaldo: $backupFile" -ForegroundColor Cyan
    Copy-Item $ArchivoExcel $backupFile
    
    # Guardar archivo formateado
    Write-Host "Guardando archivo formateado..." -ForegroundColor Cyan
    $workbook.Save()
    
    # Cerrar archivo
    $workbook.Close($false)
    $excel.Quit()
    
    # Liberar objetos COM
    [System.Runtime.Interopservices.Marshal]::ReleaseComObject($worksheet) | Out-Null
    [System.Runtime.Interopservices.Marshal]::ReleaseComObject($workbook) | Out-Null
    [System.Runtime.Interopservices.Marshal]::ReleaseComObject($excel) | Out-Null
    
    Write-Host ""
    Write-Host "=== FORMATEO COMPLETADO EXITOSAMENTE ===" -ForegroundColor Green
    Write-Host "✅ Archivo formateado: $ArchivoExcel" -ForegroundColor Green
    Write-Host "✅ Respaldo creado: $backupFile" -ForegroundColor Green
    Write-Host "✅ Filas procesadas: $($lastRow - 1)" -ForegroundColor Green
    Write-Host "✅ Columnas configuradas: 10" -ForegroundColor Green
    Write-Host ""
    Write-Host "El archivo está listo para importar en la app Checklist." -ForegroundColor Yellow
    Write-Host "Contraseña del archivo: $Contrasena" -ForegroundColor Yellow
    
} catch {
    Write-Error "Error durante el formateo: $($_.Exception.Message)"
    
    # Intentar cerrar Excel si está abierto
    try {
        if ($workbook) { $workbook.Close($false) }
        if ($excel) { $excel.Quit() }
    } catch {
        # Ignorar errores al cerrar
    }
    
    exit 1
}

# Función para mostrar ayuda
function Show-Help {
    Write-Host "=== AYUDA - FORMATEADOR DE EXCEL ===" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Uso:" -ForegroundColor Yellow
    Write-Host "  .\FormatearExcel.ps1 -ArchivoExcel 'ruta\archivo.xlsx' [-Contrasena 'contraseña']"
    Write-Host ""
    Write-Host "Parámetros:" -ForegroundColor Yellow
    Write-Host "  -ArchivoExcel    Ruta completa al archivo Excel a formatear (OBLIGATORIO)"
    Write-Host "  -Contrasena      Contraseña del archivo Excel (OPCIONAL, default: 'celeste')"
    Write-Host ""
    Write-Host "Ejemplos:" -ForegroundColor Yellow
    Write-Host "  .\FormatearExcel.ps1 -ArchivoExcel 'C:\Datos\Clientes.xlsx'"
    Write-Host "  .\FormatearExcel.ps1 -ArchivoExcel 'C:\Datos\Clientes.xlsx' -Contrasena 'miPassword'"
    Write-Host ""
    Write-Host "El script:" -ForegroundColor Green
    Write-Host "  ✅ Configura los encabezados correctos"
    Write-Host "  ✅ Formatea todas las columnas como texto"
    Write-Host "  ✅ Establece valores por defecto"
    Write-Host "  ✅ Ajusta el ancho de columnas"
    Write-Host "  ✅ Aplica formato de tabla"
    Write-Host "  ✅ Crea un respaldo del archivo original"
    Write-Host ""
}

# Mostrar ayuda si no se proporcionan parámetros
if ($args.Count -eq 0) {
    Show-Help
    exit 0
}
