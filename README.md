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

First run pulls the base images, downloads the Maven dependencies, installs the frontend
packages, and builds the session-log extractor's image — that's the slow part, and running it
now means it's all cached when the real app (and the real export) arrives. The backend's
dependency set deliberately matches the assessment's, so your `~/.m2` comes out of this warm
rather than half-full. Subsequent runs are fast.

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

- **Windows: a script fails with `bad interpreter`, `$'\r': command not found`, or `no such file
  or directory`.** Your copy was cloned before we fixed line endings, and `git pull` doesn't
  rewrite files you already have. Delete the folder and clone again, or run this from the repo
  root (it discards any local changes):

  ```bash
  git rm -r --cached -q .
  git reset --hard
  ```

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
markdown log plus a raw JSON envelope (no LLM, no network — stdlib only). They run in Docker
(`scripts/Dockerfile`), so no local Python install is required — only Docker itself, and
`docker compose up` above already warms the image build for you.

```bash
image="rfp-monster-session-log-extractor:local"
docker build -q -t "$image" scripts >/dev/null
mounts=(-v "$PWD:$PWD" -v /tmp:/tmp)
[ -d "$HOME/.claude/projects" ] && mounts+=(-v "$HOME/.claude/projects:$HOME/.claude/projects:ro")
docker run --rm -e HOME="$HOME" -u "$(id -u):$(id -g)" -w "$PWD" "${mounts[@]}" "$image" \
  extract_claude_session_log.py --all   # or extract_codex_session_log.py / extract_copilot_session_log.py
```

Swap the mounted session-storage directory (and script) for Codex (`~/.codex/sessions`) or
Copilot (`~/.copilot`) — the container only gets read-only access to that one directory, plus
read-write access to this checkout and `/tmp` (where pasted images get dumped), nothing else
on the machine.

Defaults to `--all`, writing two files per session: a readable `*_session_log_<id>.md` and a
raw `*_session_log_raw_<id>.json` envelope (the markdown is unprefixed for Claude:
`session_log_<id>.md`, since a downstream consumer matches only the `codex_`/`copilot_`
prefixes). Useful flags (all three scripts): `--strict` (only sessions started in *this exact
directory* — no parent-dir walk, no newest-anywhere fallback), `--output DIR` (or `--output -`
for stdout), a session id to export just one, `--no-raw` to skip the envelope, and
`--raw-tool-result-bytes N` to change the tool-result cap. Source-specific: Codex takes
`--sessions-root DIR`, Copilot takes `--db PATH`.

Pasted images can't be read mechanically, so the extractors dump each one to a temp file and
leave a `[Image dumped to … — description pending]` marker in the markdown log. The harness
shortcuts below do a follow-up pass that replaces those markers with a short image
description; running the container directly leaves the markers in place.

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
or `--output DIR`, then does the image-description pass on the markdown output.

Then drag both files per session — the `.md` and the `.json` — into Qualified. The markdown
is what gets scored; the raw envelope is what lets a human grader see images and full output.
