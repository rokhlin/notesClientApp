# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Implemented `AuthManager` reactive session management and state holder for user credentials, tokens, and per-user API key/signing secret.
- Added `LoginRequiredDialog` modal gating barrier preventing unauthorized access to system settings, storage paths, and cloud synchronization profiles.
- Implemented `SettingsDialog` two-tier configuration management separating Tier 1 User Cloud Profile & Storage Paths from Tier 2 Device Hardware Settings.
- Implemented `DeviceSettingsDriver` managing local device module settings (Skia GPU hardware acceleration, stylus pressure curve, local disk cache directory) stored strictly on the physical hardware.
- Implemented `ProtectedNoteCodec` container encoding/decoding for self-contained `.nap` protected notes with magic header `NA_PROTECTED_V1`, client application signature, PBKDF2 check-tag verification, and native unencrypted payload preservation.
- Added `ProtectedNoteBarrier` in-editor password challenge barrier protecting individual notes with quick `[🔒 Re-Lock]` action in top bar.
- Implemented `HmacSignatureEngine` and `PureCrypto` providing zero-dependency pure Kotlin SHA-256 and HMAC-SHA256 canonical request signing and verification for cross-platform KMP targets.
- Added comprehensive unit test suite `AuthSettingsAndProtectedNoteTest` validating auth lifecycle, two-tier separation, `.nap` container codecs, and HMAC canonical signing.
- Implemented `MarkdownFormatter` pure functional text transformation engine supporting selection wrapping (Bold, Italic, Strikethrough, Inline Code, Wikilinks, Links, Math), line-prefix toggling (H1-H3, Blockquotes, Bullet Lists, Task Checklists), code blocks, and markdown tables.
- Implemented Compose Multiplatform `EditorToolbar` docked formatting action bar grounded in Obsidian and Samsung Notes UX benchmarks, supporting horizontal scrolling, accessible high-contrast Material 3 tokens, and selection-aware text manipulation.
- Added comprehensive unit test suite `MarkdownFormatterTest` covering empty selections, selection wrapping, heading toggles, and complex markdown block formats.
- Added comprehensive living system architecture document (`docs/system_architecture.md`) detailing multiplatform runtime topology, Skia inking and Bézier smoothing pipelines, E2EE vault crypto protocols, API integration contracts, and zero-SQL storage architecture.
- Initial project structure for Compose Multiplatform client.
- Implemented shared Kotlin Multiplatform `:common-models` module defining `Note`, `InkPoint`, `InkStroke`, `CanvasLayer`, `CmnManifest`, `NoteMetadata`, `NotesIndexCatalog`, and `SyncDTOs`.
- Implemented Jetpack Navigation Compose Multiplatform routing with type-safe destinations (`NoteListRoute`, `NoteDetailRoute`, `CanvasRoute`, `SettingsRoute`).
- Configured Material 3 high-contrast brand tokens (Brand Indigo, Cyan, Amber) with formal Light and Dark color schemes.
- Added responsive `AppScaffold` adapting between compact phone top/bottom bar and expanded navigation rail.
- Added accessible `PrimaryButton` with loading state and 48dp minimum touch target.
- Implemented `ObsidianScaffold` multi-pane workspace layout (ADR Q6: strictly no tabs) with persistent 52dp `ObsidianRibbon`, animated `ObsidianSidebar` (Files, Tags, Bookmarks), and collapsible Outline & Metadata inspector.
- Implemented floating `QuickSwitcherDialog` (`Ctrl/Cmd + O`) with real-time fuzzy search, keyboard navigation (`ArrowUp`/`ArrowDown`/`Enter`/`Esc`), and empty-query note creation action.
- Implemented `MarkdownEngine` pluggable strategy architecture (ADR Q5) with `MarkdownEngineRegistry`, `AstMarkdownEngine` (fast CommonMark AST parsing), and `RichTextMarkdownEngine` (WYSIWYG live rendering with callouts and interactive checklists).
- Added workspace engine selector control with live source edit / rendered view toggle.
- Implemented `WikilinkParser` supporting standard `[[TargetNote]]` and aliased `[[TargetNote|Display Text]]` wikilinks with bi-directional backlink discovery across notes.
- Implemented interactive `WikilinkAutocompletePopup` triggerable on typing `[[` in markdown source mode with fuzzy filtering and keyboard navigation.
- Integrated clickable wikilink chip rendering in both AST and Rich-Text markdown engines with navigation dispatch and unresolved note prompt.
- Added live incoming backlinks inspector pane in `ObsidianScaffold` right inspector with count badges and direct note navigation.
- Implemented `CatmullRomConverter` supporting cubic Bézier spline interpolation and boundary ghost point synthesis for fluid handwritten inking.
- Added `BrushConfig` with dynamic pressure modulation formulas across 6 writing instruments (`PEN`, `FOUNTAIN_PEN`, `PENCIL`, `CALLIGRAPHY_BRUSH`, `HIGHLIGHTER`, `VECTOR_ERASER`).
- Built continuous vertical page roll `SkiaHandwrittenCanvas` with visual page break dividers, multi-layer cached path rendering, two-finger pan/zoom, and vector eraser hit-testing.
- Built floating Samsung Notes inking `CanvasToolbar` with instrument switcher, color swatches, dynamic stroke width slider, and undo/redo/clear controls.
- Embedded handwritten vector canvas directly into `App.kt` when opening `NoteType.CANVAS` notes.
- Implemented `ShapeRecognizer` classifying geometric primitives (Straight Line, Rectangle, Circle, Ellipse, Triangle) with mathematical boundary, perimeter, and radial error variance algorithms.
- Added 0.5s draw-and-hold auto-snapping to `SkiaHandwrittenCanvas`, automatically replacing rough contours with crisp canonical geometries.
- Added Shapes Tool popup in `CanvasToolbar` enabling direct insertion of centered vector primitives into the active canvas layer.
- Implemented `CmnPackageSerializer` for native `.cmn` compound package format with dedicated `CMN\x01` (`0x43 0x4D 0x4E 0x01`) magic header enforcement and JSON manifest payload encoding/decoding.
- Implemented `SvgExporter` converting multi-layer Catmull-Rom smoothed Bézier curves into standard infinite-resolution W3C SVG XML documents.
- Added `ExportCanvasDialog` with live SVG markup preview, binary header status verification, code copy, and download actions.
- Implemented `NoteStorageRepository` and `JsonIndexNoteRepository` providing zero-SQL sandboxed file persistence (ADR Q19) with decoupled note payload files.
- Built lightweight title-only indexing catalog `notes_index.json` (ADR Q17) guaranteeing zero leakage of note contents during high-speed in-memory queries.
- Added `StorageVaultDialog` storage inspector in settings with live catalog inspection, vault storage metrics, and orphan file re-indexing.
- Implemented `E2eeCryptoEngine` featuring authenticated AES-GCM-256 symmetric encryption, 12-byte IV, 16-byte MAC authentication tags, constant-time verification, and multi-round passphrase key derivation.
- Implemented `Bip39RecoveryKit` generating 12-word mnemonic recovery phrases and deterministically deriving 256-bit root encryption keys (ADR Q15).
- Built `VaultUnlockDialog` and protected note UI cards in `App.kt` enforcing client-side decryption barriers on encrypted notes.
- Implemented `BiometricAuthManager` and `SimulatedBiometricAuthManager` multiplatform biometric adapter interface with hardware keystore/enclave simulation and secure key release.
- Integrated quick biometric unlock action into `VaultUnlockDialog` with instantaneous biometric challenge authentication.
- Note data models and basic UI screens.
- GitHub Actions CI/CD workflows for PR validation, AI code review, and release automation.
- Specialized subagent `canvas-specialist` for handwritten notes and Skia canvas engine.
- Custom skills `skia-canvas-drawing` (Catmull-Rom splines, brushes, shapes) and `cmn-file-format` (.cmn container format and SVG export).

## [1.0.0] - 2026-09-26
### Added
- Initial release of Notes Multiplatform Client application.
