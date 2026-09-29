# Catalog Synchronization Example & Output

This document illustrates the execution lifecycle of `update_ai_models.py` when syncing new and deprecated models.

---

## Example CLI Output

```text
$ python scripts/update_ai_models.py --changelog build/ai_models_changelog.md
[INFO] Querying Gemini models API...
[INFO] Querying OpenAI models API...
[INFO] Querying Anthropic models API...
[UPDATE] Model catalog updated from v1.0.0 to v1.0.1
  Added models: OPENAI: o3-mini-high
  Deprecated models: GEMINI: gemini-2.5-flash
[SAVED] Catalog written to common-models/src/commonMain/resources/ai_models_catalog.json
[MIRROR] Server catalog updated at ../notesServer/src/main/resources/ai_models_catalog.json
[CHANGELOG] Changelog written to build/ai_models_changelog.md
```

---

## Example Generated Changelog (`build/ai_models_changelog.md`)

```markdown
# AI Model Catalog Release v1.0.1

Synchronized at: 2026-10-05T00:00:15Z

### Added Models
- OPENAI: o3-mini-high

### Deprecated / Expired Models
- GEMINI: gemini-2.5-flash

### Active Recommended Models
- **GEMINI**: Primary `gemini-3.5-flash`, Fallback `gemini-3.8-flash`
- **OPENAI**: Primary `gpt-4o-mini`, Fallback `gpt-4o`
- **ANTHROPIC**: Primary `claude-3-5-haiku-20241022`, Fallback `claude-3-7-sonnet`
- **LOCAL_SERVER**: Primary `llama3.3`, Fallback `llama3.2`
```
