# Environment Preflight — Java + Spring

A tiny app on the **same stack** you'll use for the assessment — Java 21 + Spring Boot + Postgres
backend with Flyway migrations, React + TypeScript + Vite frontend, all orchestrated with Docker
Compose. It does nothing interesting on purpose: its only job is to confirm your machine can
**build and run the stack** *before* your timer starts, so you don't lose any of your window to
setup.

Run this ahead of time. If it comes up green, you're ready.

## Run it

```bash
docker compose up
```

First run pulls the base images, downloads the Maven dependencies and installs the frontend
packages — that's the slow part, and running it now means it's all cached when the real app
arrives. The backend's dependency set deliberately matches the assessment's, so your `~/.m2`
comes out of this warm rather than half-full. Subsequent runs are fast.

Then open:

- **Frontend:** <http://localhost:5173> — should show **✅ Stack is up** with `database: ok`.
- **API health:** <http://localhost:8080/api/v1/utils/health-check/> → `true`
- **API docs:** <http://localhost:8080/v3/api-docs>

Seeing the green check on the frontend means the whole chain works: the frontend built and
served, reached the backend through the same Vite proxy the real app uses, and the backend
migrated and queried Postgres.

## If it doesn't come up

- **Docker not running** — start Docker Desktop and retry.
- **Port already in use** (5173, 8080, 5432) — stop whatever's using it, or set `FRONTEND_PORT`,
  `API_PORT` or `DB_PORT`:

  ```bash
  API_PORT=8081 FRONTEND_PORT=5174 DB_PORT=5433 docker compose up
  ```

- **Slow first build** — expected. Maven resolving Spring Boot for the first time is the long
  pole; let it finish once.
- **The API restarts before the frontend is ready** — the frontend waits on the API's health
  check, which waits on Postgres, so bring the whole thing up together rather than starting
  services individually.

Tear down with `docker compose down` (add `-v` to also drop the database volume and the
dependency caches — which you probably don't want, since caching them is the point).

## What's inside

Almost nothing, deliberately.

- `backend/` — one Spring Boot app with a single `ping` table, one Flyway migration, and two
  endpoints: `health-check` (the API is answering) and `db-check` (it round-trips to Postgres).
  Hibernate is set to `validate`, so a schema that doesn't match the entity fails loudly here
  rather than in the assessment.
- `frontend/` — one page that calls `db-check` and renders the result.

The dependency lists are the interesting part, not the code: they mirror the assessment's so the
caches this primes are the ones you'll actually use.

## Exporting session logs

`scripts/` has three mechanical extractors that turn an AI coding session into a readable
markdown log (no LLM, no network — stdlib only). Run them from the repo root:

```bash
python3 scripts/extract_session_log.py          # Claude Code sessions (~/.claude/projects/)
python3 scripts/extract_codex_session_log.py    # Codex CLI sessions (~/.codex/sessions/)
python3 scripts/extract_copilot_session_log.py  # GitHub Copilot CLI (~/.copilot/session-store.db)
```

By default each writes the most recent session for the current directory to
`session_log.md` / `codex_session_log.md` / `copilot_session_log.md` in the repo root.

Useful flags (all three scripts):

- `--all` — one file per session (`session_log_<id>.md`, etc.).
- `--strict` — only sessions started in *this exact directory*; skips the parent-dir walk
  and the "newest session anywhere" fallback, so you never accidentally export an unrelated
  project's transcript.
- `--output DIR` — write to `DIR` instead of the repo root (or `--output -` for stdout).
- a session id (or unique prefix) — export just that one session.

Source-specific flags: the Codex extractor takes `--sessions-root DIR` and the Copilot
extractor takes `--db PATH` to point at a non-default session store.

Pasted images can't be read mechanically, so the extractors dump each one to a temp file and
leave a `[Image dumped to … — description pending]` marker in the log. The harness shortcuts
below do a follow-up pass that replaces those markers with a short image description; running
the scripts directly leaves the markers in place.

### From inside your AI harness

Each tool ships a shortcut so you can export without leaving the session:

- **Claude Code** — run the `/extract-claude-session-logs` slash command
  (`.claude/commands/extract-claude-session-logs.md`). Defaults to `--all`; append any of the
  flags above, e.g. `/extract-claude-session-logs --strict`.
- **Codex** — invoke the `extract-codex-session-logs` skill (as `$extract-codex-session-logs`
  or from the skills UI; see `.agents/skills/extract-codex-session-logs/`).
- **GitHub Copilot** — invoke the `extract-copilot-session-logs` skill (as
  `$extract-copilot-session-logs` or from the skills UI; see
  `.agents/skills/extract-copilot-session-logs/`).

Each shortcut runs its extractor with `--all` and passes through extra args like `--strict`
or `--output DIR`.
