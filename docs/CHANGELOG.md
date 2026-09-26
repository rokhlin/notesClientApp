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
- Note data models and basic UI screens.
- GitHub Actions CI/CD workflows for PR validation, AI code review, and release automation.
- Specialized subagent `canvas-specialist` for handwritten notes and Skia canvas engine.
- Custom skills `skia-canvas-drawing` (Catmull-Rom splines, brushes, shapes) and `cmn-file-format` (.cmn container format and SVG export).

## [1.0.0] - 2026-09-26
### Added
- Initial release of Notes Multiplatform Client application.
