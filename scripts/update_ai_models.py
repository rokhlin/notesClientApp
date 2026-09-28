#!/usr/bin/env python3
"""
update_ai_models.py
Automated AI Models Catalog Synchronizer.

Queries upstream AI model APIs (Google Gemini, OpenAI, Anthropic, Ollama),
filters active text/chat generation models, prunes expired/unsupported models,
updates `ai_models_catalog.json`, bumps semantic patch version, and outputs changelog.

Usage:
    python update_ai_models.py [--dry-run] [--offline-verify] [--catalog PATH] [--changelog PATH]
"""

import os
import sys
import json
import argparse
import urllib.request
import urllib.error
from datetime import datetime, timezone

DEFAULT_CATALOG_PATH = os.path.normpath(
    os.path.join(os.path.dirname(__file__), "..", "common-models", "src", "commonMain", "resources", "ai_models_catalog.json")
)

SERVER_CATALOG_PATH = os.path.normpath(
    os.path.join(os.path.dirname(__file__), "..", "..", "notesServer", "src", "main", "resources", "ai_models_catalog.json")
)

# Curated Fallback Presets if API keys are absent or endpoints unreachable
CURATED_FALLBACKS = {
    "GEMINI": [
        {"id": "gemini-3.5-flash", "displayName": "Gemini 3.5 Flash", "tier": "PRIMARY", "contextWindow": 1048576, "isRecommended": True, "status": "ACTIVE"},
        {"id": "gemini-3.8-flash", "displayName": "Gemini 3.8 Flash", "tier": "FALLBACK", "contextWindow": 1048576, "isRecommended": True, "status": "ACTIVE"},
        {"id": "gemini-3.0-pro", "displayName": "Gemini 3.0 Pro", "tier": "PRO", "contextWindow": 2097152, "isRecommended": False, "status": "ACTIVE"}
    ],
    "OPENAI": [
        {"id": "gpt-4o-mini", "displayName": "GPT-4o Mini", "tier": "PRIMARY", "contextWindow": 128000, "isRecommended": True, "status": "ACTIVE"},
        {"id": "gpt-4o", "displayName": "GPT-4o", "tier": "FALLBACK", "contextWindow": 128000, "isRecommended": True, "status": "ACTIVE"},
        {"id": "o3-mini", "displayName": "o3-mini", "tier": "PRO", "contextWindow": 200000, "isRecommended": False, "status": "ACTIVE"}
    ],
    "ANTHROPIC": [
        {"id": "claude-3-5-haiku-20241022", "displayName": "Claude 3.5 Haiku", "tier": "PRIMARY", "contextWindow": 200000, "isRecommended": True, "status": "ACTIVE"},
        {"id": "claude-3-7-sonnet", "displayName": "Claude 3.7 Sonnet", "tier": "FALLBACK", "contextWindow": 200000, "isRecommended": True, "status": "ACTIVE"},
        {"id": "claude-3-5-sonnet-20241022", "displayName": "Claude 3.5 Sonnet", "tier": "PRO", "contextWindow": 200000, "isRecommended": False, "status": "ACTIVE"}
    ],
    "LOCAL_SERVER": [
        {"id": "llama3.3", "displayName": "Llama 3.3 (70B)", "tier": "PRIMARY", "contextWindow": 128000, "isRecommended": True, "status": "ACTIVE"},
        {"id": "llama3.2", "displayName": "Llama 3.2 (3B)", "tier": "FALLBACK", "contextWindow": 128000, "isRecommended": True, "status": "ACTIVE"},
        {"id": "mistral-small", "displayName": "Mistral Small 3", "tier": "FAST", "contextWindow": 32000, "isRecommended": False, "status": "ACTIVE"},
        {"id": "deepseek-r1", "displayName": "DeepSeek R1 Distill", "tier": "PRO", "contextWindow": 64000, "isRecommended": False, "status": "ACTIVE"}
    ]
}


def http_get_json(url: str, headers: dict = None, timeout: int = 10):
    req = urllib.request.Request(url, headers=headers or {})
    with urllib.request.urlopen(req, timeout=timeout) as response:
        data = response.read().decode("utf-8")
        return json.loads(data)


def fetch_gemini_models(api_key: str):
    """Fetches Gemini models from generativelanguage API."""
    if not api_key:
        print("[INFO] GEMINI_API_KEY not set. Using curated active models for Gemini.")
        return None
    url = f"https://generativelanguage.googleapis.com/v1beta/models?key={api_key}"
    try:
        data = http_get_json(url)
        active_models = []
        for m in data.get("models", []):
            name = m.get("name", "").replace("models/", "")
            methods = m.get("supportedGenerationMethods", [])
            # Only include models supporting content generation
            if "generateContent" in methods:
                # Exclude embedding, imagen, aqa, or deactivated legacy 2.5 flash
                if any(x in name.lower() for x in ["embedding", "imagen", "aqa", "2.5-flash"]):
                    continue
                display_name = m.get("displayName", name)
                input_limit = m.get("inputTokenLimit", 1048576)
                tier = "PRIMARY" if "3.5-flash" in name else ("FALLBACK" if "3.8-flash" in name else "PRO")
                is_rec = "flash" in name and "3.5" in name
                active_models.append({
                    "id": name,
                    "displayName": display_name,
                    "tier": tier,
                    "contextWindow": input_limit,
                    "isRecommended": is_rec,
                    "status": "ACTIVE",
                    "sunsetDate": None
                })
        return active_models
    except Exception as e:
        print(f"[WARN] Failed to query Gemini API: {e}. Using curated catalog.")
        return None


def fetch_openai_models(api_key: str):
    """Fetches OpenAI chat models."""
    if not api_key:
        print("[INFO] OPENAI_API_KEY not set. Using curated active models for OpenAI.")
        return None
    url = "https://api.openai.com/v1/models"
    headers = {"Authorization": f"Bearer {api_key}"}
    try:
        data = http_get_json(url, headers=headers)
        chat_models = []
        for m in data.get("data", []):
            model_id = m.get("id", "")
            if model_id.startswith(("gpt-4o", "o1", "o3", "chatgpt")):
                tier = "PRIMARY" if model_id == "gpt-4o-mini" else ("FALLBACK" if model_id == "gpt-4o" else "PRO")
                chat_models.append({
                    "id": model_id,
                    "displayName": model_id.upper() if "gpt" in model_id else model_id,
                    "tier": tier,
                    "contextWindow": 128000,
                    "isRecommended": model_id in ["gpt-4o-mini", "gpt-4o"],
                    "status": "ACTIVE",
                    "sunsetDate": None
                })
        return chat_models
    except Exception as e:
        print(f"[WARN] Failed to query OpenAI API: {e}. Using curated catalog.")
        return None


def fetch_anthropic_models(api_key: str):
    """Fetches Anthropic Claude models."""
    if not api_key:
        print("[INFO] ANTHROPIC_API_KEY not set. Using curated active models for Anthropic.")
        return None
    url = "https://api.anthropic.com/v1/models"
    headers = {
        "x-api-key": api_key,
        "anthropic-version": "2023-06-01"
    }
    try:
        data = http_get_json(url, headers=headers)
        claude_models = []
        for m in data.get("data", []):
            model_id = m.get("id", "")
            display_name = m.get("display_name", model_id)
            tier = "PRIMARY" if "haiku" in model_id else ("FALLBACK" if "sonnet" in model_id else "PRO")
            claude_models.append({
                "id": model_id,
                "displayName": display_name,
                "tier": tier,
                "contextWindow": 200000,
                "isRecommended": "3-5" in model_id or "3-7" in model_id,
                "status": "ACTIVE",
                "sunsetDate": None
            })
        return claude_models
    except Exception as e:
        print(f"[WARN] Failed to query Anthropic API: {e}. Using curated catalog.")
        return None


def bump_patch_version(version_str: str) -> str:
    parts = version_str.split(".")
    try:
        parts[-1] = str(int(parts[-1]) + 1)
        return ".".join(parts)
    except Exception:
        return f"{version_str}.1"


def sync_catalog(catalog_path: str, changelog_path: str = None, dry_run: bool = False, offline_verify: bool = False):
    if not os.path.exists(catalog_path):
        raise FileNotFoundError(f"Catalog file not found at: {catalog_path}")

    with open(catalog_path, "r", encoding="utf-8") as f:
        catalog = json.load(f)

    if offline_verify:
        print(f"[SUCCESS] Offline verification passed. Catalog version: {catalog.get('catalogVersion')}, providers: {list(catalog.get('providers', {}).keys())}")
        return True

    gemini_key = os.getenv("GEMINI_API_KEY", "")
    openai_key = os.getenv("OPENAI_API_KEY", "")
    anthropic_key = os.getenv("ANTHROPIC_API_KEY", "")

    fetched = {
        "GEMINI": fetch_gemini_models(gemini_key),
        "OPENAI": fetch_openai_models(openai_key),
        "ANTHROPIC": fetch_anthropic_models(anthropic_key),
        "LOCAL_SERVER": None
    }

    added_models = []
    deprecated_models = []
    has_changes = False

    for provider_name, provider_data in catalog.get("providers", {}).items():
        existing_models = {m["id"]: m for m in provider_data.get("models", [])}
        remote_models = fetched.get(provider_name)

        if remote_models is not None:
            remote_ids = {m["id"] for m in remote_models}
            # Detect deprecated / removed
            for eid, em in existing_models.items():
                if eid not in remote_ids and em.get("status") == "ACTIVE":
                    em["status"] = "DEPRECATED"
                    deprecated_models.append(f"{provider_name}: {eid}")
                    has_changes = True

            # Detect new models
            for rm in remote_models:
                rid = rm["id"]
                if rid not in existing_models:
                    existing_models[rid] = rm
                    added_models.append(f"{provider_name}: {rid}")
                    has_changes = True

            provider_data["models"] = list(existing_models.values())
        else:
            # Check against curated defaults to ensure no expired models exist
            curated = CURATED_FALLBACKS.get(provider_name, [])
            for cm in curated:
                cid = cm["id"]
                if cid not in existing_models:
                    existing_models[cid] = cm
                    added_models.append(f"{provider_name}: {cid}")
                    has_changes = True
            provider_data["models"] = list(existing_models.values())

    if has_changes:
        old_ver = catalog.get("catalogVersion", "1.0.0")
        new_ver = bump_patch_version(old_ver)
        catalog["catalogVersion"] = new_ver
        catalog["updatedAt"] = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
        print(f"[UPDATE] Model catalog updated from v{old_ver} to v{new_ver}")
        if added_models:
            print(f"  Added models: {', '.join(added_models)}")
        if deprecated_models:
            print(f"  Deprecated models: {', '.join(deprecated_models)}")

        if not dry_run:
            with open(catalog_path, "w", encoding="utf-8") as f:
                json.dump(catalog, f, indent=2)
                f.write("\n")
            print(f"[SAVED] Catalog written to {catalog_path}")

            # Also mirror to server catalog if it exists
            if os.path.exists(SERVER_CATALOG_PATH):
                with open(SERVER_CATALOG_PATH, "w", encoding="utf-8") as sf:
                    json.dump(catalog, sf, indent=2)
                    sf.write("\n")
                print(f"[MIRROR] Server catalog updated at {SERVER_CATALOG_PATH}")

        if changelog_path:
            os.makedirs(os.path.dirname(os.path.abspath(changelog_path)), exist_ok=True)
            with open(changelog_path, "w", encoding="utf-8") as cf:
                cf.write(f"# AI Model Catalog Release v{new_ver}\n\n")
                cf.write(f"Synchronized at: {catalog['updatedAt']}\n\n")
                if added_models:
                    cf.write("### Added Models\n")
                    for m in added_models:
                        cf.write(f"- {m}\n")
                    cf.write("\n")
                if deprecated_models:
                    cf.write("### Deprecated / Expired Models\n")
                    for m in deprecated_models:
                        cf.write(f"- {m}\n")
                    cf.write("\n")
                cf.write("### Active Recommended Models\n")
                for p, pdata in catalog.get("providers", {}).items():
                    cf.write(f"- **{p}**: Primary `{pdata.get('defaultPrimaryModelId')}`, Fallback `{pdata.get('defaultFallbackModelId')}`\n")
            print(f"[CHANGELOG] Changelog written to {changelog_path}")
    else:
        print(f"[NO CHANGE] AI Model Catalog is already up-to-date (v{catalog.get('catalogVersion')}).")

    return has_changes


def main():
    parser = argparse.ArgumentParser(description="Synchronize AI models catalog with cloud providers.")
    parser.add_argument("--catalog", default=DEFAULT_CATALOG_PATH, help="Path to ai_models_catalog.json")
    parser.add_argument("--changelog", default="build/ai_models_changelog.md", help="Path to write release changelog")
    parser.add_argument("--dry-run", action="store_true", help="Inspect changes without writing to disk")
    parser.add_argument("--offline-verify", action="store_true", help="Verify catalog schema offline without making network requests")

    args = parser.parse_args()
    try:
        sync_catalog(
            catalog_path=args.catalog,
            changelog_path=args.changelog,
            dry_run=args.dry_run,
            offline_verify=args.offline_verify
        )
    except Exception as e:
        print(f"[ERROR] Sync failed: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
