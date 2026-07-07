# Claude Project Guide

This file is the primary instruction document for AI agents working in this repository.

## Project Summary
- App: Spotify Wrapper
- Backend: Java 17, Spring Boot 3.2.x, Maven, MySQL
- Frontend: jQuery + Bootstrap + Handlebars (SPA)
- Ports: backend `9090`, frontend `3000`

## Start Here
1. Read `README.md` for setup and feature behavior.
2. Read `QUICK_START.md` for local bootstrap.
3. Read `CONTEXT.md` for architecture and recent changes.
4. Read `AGENTS.md` for conventions and pitfalls.

## Core Conventions
- Keep patches minimal and scoped to the requested behavior.
- Preserve frontend template-first rendering.
- Avoid constructing UI HTML directly inside files under `frontend/js`.
- Reuse/extend Handlebars templates in `frontend/index.html` and shared helpers in `Utils`.
- Preserve player polling behavior:
  - No background polling while tab is inactive.
  - Post-completion refresh around 2 seconds after expected track end.
- Treat no-active-device responses as valid empty state, not fatal errors.

## Run Commands
- Start all: `./start.sh`
- Stop/status: `./stop.sh stop` / `./stop.sh status`
- Backend tests: `mvn -f backend/pom.xml test`
- Backend compile: `mvn -f backend/pom.xml clean compile`
- Frontend run: `cd frontend && npm install && npm start`

## Skill Index
- `/.claude/skills/repo-architecture.md`
- `/.claude/skills/backend-spotify-api.md`
- `/.claude/skills/frontend-template-rendering.md`
- `/.claude/skills/player-polling-behavior.md`
- `/.claude/skills/devices-empty-state.md`
- `/.claude/skills/testing-and-validation.md`
- `/.claude/skills/debugging-workflow.md`

Use these skill files as procedural playbooks for common tasks.
