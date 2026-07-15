# Roadmap

## 0.4.2 — Settings Redesign and Verifier Cleanup

- Merge body and code font into a single font field (bridge version 5)
- Replace text scale percentage with font size in pixels (default 14)
- Replace sliders with dropdowns for content width (128 px steps)
- Align all form controls and tighten settings panel spacing
- Remove optional JCEF module dependency that failed to resolve on 2025.1
- Replace deprecated SimpleListCellRenderer with stable textListCellRenderer

## 0.4.1 — Compatibility Backport

- Lower the IDE compatibility baseline from 2026.1 to 2025.1 so MdLens runs on IDEs from the past year
- Verify JCEF loading on 2025.1 where JCEF is platform-internal, not a bundled plugin
- Run Plugin Verifier against 2025.1, 2025.2, 2025.3, and 2026.1

## Earlier Releases

MdLens was originally developed as MarkdownNeat. Releases 0.1.x through 0.4.x shipped under the previous plugin at <https://github.com/Hyune-s-lab/markdown-neat> (archived).

- **0.4.x** — Syntax highlighting, highlight groups, editing handoff, and anchor navigation
- **0.3.x** — Reading profiles, font selection, text scaling, content width, and live preview
- **0.2.x** — Mermaid diagrams, bundled runtime with offline icons, and modular IDE compatibility
- **0.1.x** — Markdown viewer with GitHub Flavored Markdown, themes, offline rendering, and JCEF fallback

## Later

- Let each highlight group's color be customized individually.
- Detect viewer bootstrap failures, log them, and fall back to the plain-text viewer.
- Add D2 and Excalidraw through the shared optional-runtime boundary.
- Add bundled themes beyond GitHub Light and GitHub Dark.
- Add Markdown extensions such as footnotes, alerts, and math rendering where they remain lightweight and safe.
- Add optional custom CSS overrides and a copyable AI usage guide.
- Bundle a curated subset of frequently used diagram icons for offline and deterministic rendering.
- Build a Chrome extension that replaces GitHub's built-in Mermaid rendering with the bundled Mermaid 11 runtime
  - Edge runs Chrome extensions as-is
  - GitHub cannot draw `@{ img }` icon nodes and only partially honors frontmatter themes
  - Reuse the icon and theme conventions and force a white canvas in dark mode
