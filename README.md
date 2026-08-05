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

## Variáveis de ambiente importantes

- `JWT_SECRET`: troque por uma string longa e aleatória antes de expor publicamente. Pode ser definida num arquivo `.env` na raiz (lido pelo `docker-compose.yml`).

## Próximos passos (fora do MVP atual)

- Entrada de lançamentos via texto livre com LLM local (Ollama)
- Integração Open Finance (fase 2)
- App mobile (React Native, reaproveitando lógica do frontend web)
- Notificações push
