# Restaura um backup gerado pelo backup-db.ps1 para o Postgres do OrçaFin.
# ATENÇÃO: isso APAGA os dados atuais do banco antes de restaurar.
#
# Uso: .\restore-db.ps1 -BackupZip .\backups\orcafin_2026-08-06_120000.sql.zip

param(
    [Parameter(Mandatory = $true)]
    [string]$BackupZip,
    [string]$ContainerName = "orcafin-postgres",
    [string]$DbUser = "orcafin",
    [string]$DbName = "orcafin"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BackupZip)) {
    throw "Arquivo não encontrado: $BackupZip"
}

$TempDir = Join-Path $env:TEMP "orcafin-restore-$(Get-Random)"
New-Item -ItemType Directory -Path $TempDir | Out-Null
Expand-Archive -Path $BackupZip -DestinationPath $TempDir -Force
$SqlFile = Get-ChildItem -Path $TempDir -Filter "*.sql" | Select-Object -First 1

if (-not $SqlFile) {
    throw "Nenhum arquivo .sql encontrado dentro do zip."
}

Write-Host "Isso vai APAGAR os dados atuais de '$DbName' e restaurar a partir de $($SqlFile.Name)."
$confirm = Read-Host "Digite SIM para continuar"
if ($confirm -ne "SIM") {
    Write-Host "Cancelado."
    Remove-Item $TempDir -Recurse -Force
    exit 0
}

Write-Host "Recriando o banco..."
docker exec $ContainerName psql -U $DbUser -d postgres -c "DROP DATABASE IF EXISTS $DbName;"
docker exec $ContainerName psql -U $DbUser -d postgres -c "CREATE DATABASE $DbName OWNER $DbUser;"

Write-Host "Restaurando dados..."
Get-Content $SqlFile.FullName | docker exec -i $ContainerName psql -U $DbUser -d $DbName

Remove-Item $TempDir -Recurse -Force
Write-Host "Restauração concluída."
