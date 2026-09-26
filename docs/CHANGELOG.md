# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Initial project structure for Compose Multiplatform client.
- Implemented shared Kotlin Multiplatform `:common-models` module defining `Note`, `InkPoint`, `InkStroke`, `CanvasLayer`, `CmnManifest`, `NoteMetadata`, `NotesIndexCatalog`, and `SyncDTOs`.
- Implemented Jetpack Navigation Compose Multiplatform routing with type-safe destinations (`NoteListRoute`, `NoteDetailRoute`, `CanvasRoute`, `SettingsRoute`).
- Configured Material 3 high-contrast brand tokens (Brand Indigo, Cyan, Amber) with formal Light and Dark color schemes.
- Added responsive `AppScaffold` adapting between compact phone top/bottom bar and expanded navigation rail.
- Added accessible `PrimaryButton` with loading state and 48dp minimum touch target.
- Note data models and basic UI screens.
- GitHub Actions CI/CD workflows for PR validation, AI code review, and release automation.
- Specialized subagent `canvas-specialist` for handwritten notes and Skia canvas engine.
- Custom skills `skia-canvas-drawing` (Catmull-Rom splines, brushes, shapes) and `cmn-file-format` (.cmn container format and SVG export).

## [1.0.0] - 2026-09-26
### Added
- Initial release of Notes Multiplatform Client application.
