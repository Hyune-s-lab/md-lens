# Roadmap

Released versions keep only the essentials here; full detail lives in the GitHub Release notes.

## 0.4.x

- Syntax highlighting, highlight groups with color presets, and an IDE-synced theme with better defaults
- Settings redesign with Restore defaults and preview tabs (multilingual, Code, Mermaid)
- Editing handoff layouts and anchor navigation across Markdown files
- IDE compatibility baseline lowered to 2025.1

## Earlier Releases

MdLens was originally developed as MarkdownNeat. Releases before 0.4.0 shipped under the previous plugin at <https://github.com/Hyune-s-lab/markdown-neat> (archived).

- **0.3.x** — Reading profiles, font selection, text scaling, content width, and live preview
- **0.2.x** — Mermaid diagrams, bundled runtime with offline icons, and modular IDE compatibility
- **0.1.x** — Markdown viewer with GitHub Flavored Markdown, themes, offline rendering, and JCEF fallback

## Later

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
