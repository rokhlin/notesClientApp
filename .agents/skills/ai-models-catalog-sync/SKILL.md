---
name: ai-models-catalog-sync
description: >-
  Autonomous skill to synchronize, validate, and maintain the active AI models catalog (ai_models_catalog.json).
  Queries upstream APIs (Google Gemini, OpenAI, Anthropic, Ollama), removes expired or deactivated models,
  updates model tiers, bumps catalog semantic version, and prepares GitHub release notes.
---

# AI Models Catalog Synchronizer (`ai-models-catalog-sync`)

This skill defines the autonomous protocol and operational instructions for curating, refreshing, and verifying active foundation models across supported AI providers (Google Gemini, OpenAI, Anthropic, and Local Server LLMs).

## Architectural Purpose
- **Eliminate Hardcoded Model Liabilities**: Ensure client applications never crash due to silent upstream deprecations (e.g. `gemini-2.5-flash` deactivation).
- **Autonomous Curation**: Routinely query cloud provider model endpoints, detect newly deployed generation models, flag sunset or deprecated models, and record metadata into `ai_models_catalog.json`.
- **Scheduled CI/CD Integration**: Triggered weekly via GitHub Actions cron or on-demand via agent slash commands / workflows.

---

## 1. Skill Execution Protocol

### Step 1: Endpoint & Credential Verification
Verify available environment variables:
- `GEMINI_API_KEY`: For Google Gemini API (`https://generativelanguage.googleapis.com/v1beta/models`)
- `OPENAI_API_KEY`: For OpenAI API (`https://api.openai.com/v1/models`)
- `ANTHROPIC_API_KEY`: For Anthropic API (`https://api.anthropic.com/v1/models`)

If credentials are unavailable, the skill falls back to curated verified active defaults (`CURATED_FALLBACKS`) and tests schema integrity.

### Step 2: Running the Synchronization Engine
Execute the standalone updater script from the repository root:
```bash
# Standard sync with changelog output
python scripts/update_ai_models.py --changelog build/ai_models_changelog.md

# Dry-run inspection (no files modified)
python scripts/update_ai_models.py --dry-run

# Air-gapped / offline CI schema verification
python scripts/update_ai_models.py --offline-verify
```

### Step 3: Lifecycle Filtering & Tier Assignment
The engine enforces the following filtering rules:
1. **Google Gemini**:
   - Filter `supportedGenerationMethods` containing `"generateContent"`.
   - Exclude `embedding`, `imagen`, `aqa`, and deprecated `gemini-2.5` series.
   - Primary: `gemini-3.5-flash`
   - Fallback: `gemini-3.8-flash`
   - Pro: `gemini-3.0-pro`
2. **OpenAI**:
   - Filter chat models prefixed with `gpt-4o`, `o1`, `o3`.
   - Exclude legacy `babbage`, `davinci`, `whisper`, `tts`.
   - Primary: `gpt-4o-mini`
   - Fallback: `gpt-4o`
   - Pro: `o3-mini`
3. **Anthropic**:
   - Filter active Claude models.
   - Primary: `claude-3-5-haiku-20241022`
   - Fallback: `claude-3-7-sonnet`
   - Pro: `claude-3-5-sonnet-20241022`
4. **Local Server (Ollama / OpenAI-compatible)**:
   - Primary: `llama3.3` (70B)
   - Fallback: `llama3.2` (3B)
   - Fast: `mistral-small`
   - Reasoning: `deepseek-r1`

### Step 4: Catalog Versioning & Release Tagging
If any models are added, deprecated, or updated:
1. Increments `catalogVersion` patch (e.g. `1.0.0` $\to$ `1.0.1`).
2. Updates `updatedAt` with the current UTC ISO-8601 timestamp.
3. Synchronizes `common-models/src/commonMain/resources/ai_models_catalog.json` and mirrors to `notesServer/src/main/resources/ai_models_catalog.json`.
4. Outputs release markdown changelog for automated GitHub Release creation (`models-vX.Y.Z`).

---

## 2. Directory Layout & References
- Script: [update_ai_models.py](../../../scripts/update_ai_models.py)
- Catalog JSON: [ai_models_catalog.json](../../../common-models/src/commonMain/resources/ai_models_catalog.json)
- GitHub Actions: [.github/workflows/update_ai_models.yml](../../../.github/workflows/update_ai_models.yml)
- References:
  - [Provider Endpoints & Schemas](./references/provider_endpoints.md)
  - [Catalog Diff Examples](./examples/catalog_sync_example.md)
