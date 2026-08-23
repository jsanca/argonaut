# argonaut-langchain4j

LangChain4j implementation of the Argonaut agentic mission.

## Purpose

Implements the Argonaut experiment using LangChain4j's idiomatic abstractions. Exposes the common HTTP contract on port **8082**.

## What belongs here

- LangChain4j configuration and AI service definitions
- The agentic mission implemented using LangChain4j tools, AI services, and memory
- HTTP controllers implementing `GET /api/health`, `GET /api/about`, `POST /api/experiment/run`
- LangChain4j-specific orchestration logic

## What does not belong here

Framework-neutral contracts and structures — those live in `argonaut-core`.

## Current status

Skeleton. No LangChain4j dependency or implementation yet. Source placeholder in `dev.jsanca.argonaut.langchain4j`.
