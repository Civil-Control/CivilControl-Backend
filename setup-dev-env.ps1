# Script para configurar el entorno de desarrollo local

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CONFIGURACION DE ENTORNO DE DESARROLLO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar si ya existe .env
if (Test-Path ".env") {
    Write-Host "El archivo .env ya existe." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Opciones:" -ForegroundColor White
    Write-Host "1. Sobrescribir con valores por defecto" -ForegroundColor Gray
    Write-Host "2. Mantener el archivo actual" -ForegroundColor Gray
    Write-Host "3. Crear .env.local (recomendado para personalizacion)" -ForegroundColor Gray
    Write-Host ""
    $option = Read-Host "Selecciona una opcion (1/2/3)"

    if ($option -eq "2") {
        Write-Host "Se mantiene el archivo .env actual." -ForegroundColor Green
        exit 0
    } elseif ($option -eq "3") {
        $targetFile = ".env.local"
    } else {
        $targetFile = ".env"
    }
} else {
    $targetFile = ".env"
}

Write-Host ""
Write-Host "Copiando .env.dev.template a $targetFile..." -ForegroundColor Yellow

# Copiar el template
if (Test-Path ".env.dev.template") {
    Copy-Item ".env.dev.template" -Destination $targetFile
    Write-Host "Archivo creado: $targetFile" -ForegroundColor Green
} else {
    Write-Host "ERROR: No se encuentra .env.dev.template" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  CONFIGURACION COMPLETADA" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Siguiente paso:" -ForegroundColor Yellow
Write-Host ""

if ($targetFile -eq ".env.local") {
    Write-Host "1. Edita $targetFile con tus valores personalizados" -ForegroundColor White
} else {
    Write-Host "1. (Opcional) Edita $targetFile si necesitas valores personalizados" -ForegroundColor White
}

Write-Host "2. Ejecuta la aplicacion:" -ForegroundColor White
Write-Host "   .\mvnw.cmd spring-boot:run" -ForegroundColor Gray
Write-Host ""
Write-Host "O si usas IDE:" -ForegroundColor White
Write-Host "   - IntelliJ: Instala 'EnvFile' plugin y configura para leer $targetFile" -ForegroundColor Gray
Write-Host "   - VS Code: Las variables se cargaran automaticamente" -ForegroundColor Gray
Write-Host ""
Write-Host "NOTA IMPORTANTE:" -ForegroundColor Yellow
Write-Host "Spring Boot no carga archivos .env automaticamente." -ForegroundColor Yellow
Write-Host "Necesitas usar una de estas opciones:" -ForegroundColor Yellow
Write-Host ""
Write-Host "Opcion 1: Usar docker-compose (recomendado)" -ForegroundColor White
Write-Host "  docker-compose up" -ForegroundColor Gray
Write-Host ""
Write-Host "Opcion 2: Cargar manualmente las variables antes de ejecutar:" -ForegroundColor White
Write-Host "  Get-Content $targetFile | ForEach-Object { if (`$_ -match '^([^#].+?)=(.+)$') { [Environment]::SetEnvironmentVariable(`$matches[1], `$matches[2]) } }" -ForegroundColor Gray
Write-Host "  .\mvnw.cmd spring-boot:run" -ForegroundColor Gray
Write-Host ""
Write-Host "Opcion 3: Configurar en tu IDE (ver documentacion del IDE)" -ForegroundColor White
Write-Host ""

