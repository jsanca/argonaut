# argonaut-embabel

Embabel implementation of the Argonaut agentic mission.

## Purpose

Implements the Argonaut experiment using Embabel's idiomatic abstractions. Exposes the common HTTP contract on port **8084**.

## What belongs here

- Embabel configuration and agent definitions
- The agentic mission implemented using Embabel's actions and orchestration model
- HTTP controllers implementing `GET /api/health`, `GET /api/about`, `POST /api/experiment/run`
- Embabel-specific orchestration logic

## What does not belong here

Framework-neutral contracts and structures — those live in `argonaut-core`.

## Current status

Skeleton. No Embabel dependency or implementation yet. Source placeholder in `dev.jsanca.argonaut.embabel`.
