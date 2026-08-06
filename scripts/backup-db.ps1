# Faz um dump do Postgres do OrçaFin (rodando via Docker Compose) e guarda em ./backups
# na raiz do projeto, com timestamp no nome. Mantém só os últimos $RetentionDays dias.

param(
    [string]$ContainerName = "orcafin-postgres",
    [string]$DbUser = "orcafin",
    [string]$DbName = "orcafin",
    [int]$RetentionDays = 30
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$BackupDir = Join-Path $ProjectRoot "backups"

if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir | Out-Null
}

$Timestamp = Get-Date -Format "yyyy-MM-dd_HHmmss"
$DumpFile = Join-Path $BackupDir "orcafin_$Timestamp.sql"
$ZipFile = "$DumpFile.zip"

Write-Host "Gerando dump de '$DbName' do container '$ContainerName'..."
docker exec $ContainerName pg_dump -U $DbUser $DbName | Out-File -FilePath $DumpFile -Encoding utf8

if (-not $?) {
    throw "Falha ao gerar o dump. Verifique se o container '$ContainerName' está rodando."
}

Compress-Archive -Path $DumpFile -DestinationPath $ZipFile -Force
Remove-Item $DumpFile

$SizeKb = [math]::Round((Get-Item $ZipFile).Length / 1KB, 1)
Write-Host "Backup criado: $ZipFile ($SizeKb KB)"

$Cutoff = (Get-Date).AddDays(-$RetentionDays)
Get-ChildItem -Path $BackupDir -Filter "orcafin_*.sql.zip" |
    Where-Object { $_.LastWriteTime -lt $Cutoff } |
    ForEach-Object {
        Write-Host "Removendo backup antigo: $($_.Name)"
        Remove-Item $_.FullName
    }

Write-Host "Backup concluído."
