# argonaut-spring-ai

Spring AI implementation of the Argonaut agentic mission.

## Purpose

Implements the Argonaut experiment using Spring AI's idiomatic abstractions. Exposes the common HTTP contract on port **8081**.

## What belongs here

- Spring AI configuration and bean definitions
- The agentic mission implemented using Spring AI tools, advisors, and chat models
- HTTP controllers implementing `GET /api/health`, `GET /api/about`, `POST /api/experiment/run`
- Spring AI-specific orchestration logic

## What does not belong here

Framework-neutral contracts and structures — those live in `argonaut-core`.

## Current status

Skeleton. No Spring AI dependency or implementation yet. Source placeholder in `dev.jsanca.argonaut.springai`.
