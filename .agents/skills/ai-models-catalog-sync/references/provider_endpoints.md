# Provider Model Endpoints & Query Specifications

This document outlines the REST APIs and query behaviors used by `ai-models-catalog-sync` to inspect and monitor model lifecycles.

---

## 1. Google Gemini API

- **Endpoint**: `GET https://generativelanguage.googleapis.com/v1beta/models`
- **Authentication**: `?key=$GEMINI_API_KEY`
- **Response Format**:
  ```json
  {
    "models": [
      {
        "name": "models/gemini-3.5-flash",
        "version": "001",
        "displayName": "Gemini 3.5 Flash",
        "description": "Fast and versatile multimodal model for high-frequency tasks",
        "inputTokenLimit": 1048576,
        "outputTokenLimit": 8192,
        "supportedGenerationMethods": ["generateContent", "countTokens"]
      }
    ]
  }
  ```
- **Filter Heuristics**:
  - Keep models where `"generateContent"` in `supportedGenerationMethods`.
  - Exclude models with `aqa`, `embedding`, `imagen`.
  - Flag models no longer returned as `DEPRECATED`.

---

## 2. OpenAI API

- **Endpoint**: `GET https://api.openai.com/v1/models`
- **Authentication**: Header `Authorization: Bearer $OPENAI_API_KEY`
- **Response Format**:
  ```json
  {
    "data": [
      {
        "id": "gpt-4o-mini",
        "object": "model",
        "created": 1721267866,
        "owned_by": "system"
      }
    ]
  }
  ```
- **Filter Heuristics**:
  - Filter for IDs starting with `gpt-4o`, `o1`, `o3`.
  - Exclude legacy completion models, embeddings, audio, and moderation endpoints.

---

## 3. Anthropic API

- **Endpoint**: `GET https://api.anthropic.com/v1/models`
- **Authentication**: Headers:
  - `x-api-key: $ANTHROPIC_API_KEY`
  - `anthropic-version: 2023-06-01`
- **Response Format**:
  ```json
  {
    "data": [
      {
        "id": "claude-3-5-haiku-20241022",
        "type": "model",
        "display_name": "Claude 3.5 Haiku",
        "created_at": "2024-10-22T00:00:00Z"
      }
    ]
  }
  ```
- **Filter Heuristics**:
  - Extract active generation models in the `claude-3` and `claude-3.5` / `claude-3.7` family.

---

## 4. Local Server (Ollama / LocalAI)

- **Endpoints**:
  - Ollama: `GET http://localhost:11434/api/tags`
  - OpenAI-compatible: `GET http://localhost:11434/v1/models`
- **Filter Heuristics**:
  - Pull installed models dynamically if reachable.
  - Maintain curated local recommendations (`llama3.3`, `llama3.2`, `mistral-small`, `deepseek-r1`).
