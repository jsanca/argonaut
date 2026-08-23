# argonaut-langgraph4j

LangGraph4j implementation of the Argonaut agentic mission.

## Purpose

Implements the Argonaut experiment using LangGraph4j's idiomatic graph-based abstractions. Exposes the common HTTP contract on port **8083**.

## What belongs here

- LangGraph4j graph definitions, nodes, and state management
- The agentic mission implemented using LangGraph4j's graph orchestration model
- HTTP controllers implementing `GET /api/health`, `GET /api/about`, `POST /api/experiment/run`
- LangGraph4j-specific orchestration logic

## What does not belong here

Framework-neutral contracts and structures — those live in `argonaut-core`.

## Current status

Skeleton. No LangGraph4j dependency or implementation yet. Source placeholder in `dev.jsanca.argonaut.langgraph4j`.
