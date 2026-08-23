# Eval: Java AI Backend Engineering

## Prompt

Add a chat endpoint to a Spring Boot 4 catalog service. Requirements: RAG over products with tenant filter, LangChain4j OpenAI-compatible client, audit log with source IDs, no PII in logs. Propose package layout, ports, and one integration test approach.

## Expected Agent Behavior

- Proposes chat.* module with pipeline stages and SPI ports to catalog retrieval
- Keeps catalog domain free of chat imports
- Shows config via env vars, timeouts, top-k limits
- Includes audit fields and guardrails (scope, size, rate)
- Integration test mocks LLM and retrieval ports

## Failure Signals

- Single ChatService calling repository and LLM directly with no ports
- Tenant ID taken from request body without auth binding
- Logs full user message and retrieved document bodies by default
- CI test requires live API key
