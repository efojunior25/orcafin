# OrçaFin — MVP

Organizador financeiro pessoal. Backend em Java 21 + Spring Boot 4 + PostgreSQL, frontend em React + Vite (dark mode por padrão).

## Rodando tudo com Docker (recomendado)

Pré-requisito: Docker Desktop instalado e rodando.

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Postgres: localhost:5432 (user/senha/db: `orcafin`)

Para parar: `docker compose down` (os dados do Postgres ficam salvos no volume `orcafin-postgres-data`).

## Rodando em modo desenvolvimento (sem Docker para back/front)

1. Suba só o banco: `docker compose up postgres -d`
2. Backend: `cd backend && ./mvnw.cmd spring-boot:run`
3. Frontend: `cd frontend && npm install && npm run dev` (abre em http://localhost:5173)

## Lançamento por texto com IA local (Ollama)

Na página de Transações, digite algo como "Pizza 59,90" no campo de lançamento rápido — a IA interpreta o valor, o tipo (receita/despesa) e sugere uma categoria, e abre o formulário já preenchido para você revisar antes de salvar (nunca salva sozinho).

Isso é opcional e roda separado do resto (`profile ai` do Docker Compose), porque consome bastante recursos:

```bash
docker compose --profile ai up -d ollama
docker exec orcafin-ollama ollama pull llama3.1:8b
```

- **Com GPU NVIDIA** (testado com RTX 3050 6GB): usa CUDA automaticamente se o Docker tiver o NVIDIA Container Toolkit configurado, resposta em ~3-9s.
- **Sem GPU**: roda na CPU, mas com modelos de 8B fica bem lento numa máquina com 8GB de RAM; nesse caso troque para um modelo bem menor (`OLLAMA_MODEL=llama3.2:1b` no `.env`) — a qualidade da extração piora bastante, mas funciona.

Para parar (libera VRAM/RAM quando não estiver usando): `docker compose stop ollama`.

## Backup e restauração do banco

Backup manual (gera um `.sql.zip` em `backups/`, mantém os últimos 30 dias):

```powershell
.\scripts\backup-db.ps1
```

Restaurar um backup (**apaga os dados atuais** antes de restaurar):

```powershell
.\scripts\restore-db.ps1 -BackupZip .\backups\orcafin_2026-08-06_202453.sql.zip
```

### Automatizar (rodar todo dia às 3h da manhã)

Isso cria uma tarefa agendada do Windows. Rode no PowerShell **como Administrador**:

```powershell
$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument '-NoProfile -ExecutionPolicy Bypass -File "C:\Users\edson\OneDrive\Documentos\SaaS\app-financeiro\scripts\backup-db.ps1"'
$trigger = New-ScheduledTaskTrigger -Daily -At 3am
Register-ScheduledTask -TaskName "OrcaFin Backup" -Action $action -Trigger $trigger -Description "Backup diário do banco do OrçaFin"
```

Os arquivos de backup **não são versionados no Git** (contêm dados financeiros reais). Guarde-os também fora do PC de tempos em tempos (pendrive, nuvem) — o Docker rodar localmente não te protege contra falha de disco.

## Variáveis de ambiente importantes

- `JWT_SECRET`: troque por uma string longa e aleatória antes de expor publicamente. Pode ser definida num arquivo `.env` na raiz (lido pelo `docker-compose.yml`).

## Próximos passos (fora do MVP atual)

- Integração Open Finance (fase 2)
- App mobile (React Native, reaproveitando lógica do frontend web)
- Notificações push
