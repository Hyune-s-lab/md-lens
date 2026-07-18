# Architecture

This document MUST describe the current MdLens architecture. Provisional choices are marked as candidates.

```text
Markdown or Mermaid file
    -> thin Kotlin host
    -> JCEF
    -> deep TypeScript renderer
        -> Markdown
        -> Mermaid 11.16.0 (loaded only when used)
        -> highlight.js 11.11.1 (loaded only when used)
        -> GitHub themes (synced with the IDE or fixed)
        -> reading profiles, typography, and highlight colors
```

## Ownership

Kotlin MUST own only JetBrains extension registration, file events, JCEF lifecycle, bridge transport, IDE navigation, Settings UI and persistence, and platform fallback behavior.

TypeScript MUST own Markdown parsing, sanitization, rendering profiles, themes, typography, highlight color presets, diagram engines, DOM updates, render scheduling, error isolation, and renderer diagnostics.

The bridge MUST remain small:

```text
Kotlin -> TypeScript: render(request) | runtimeReady | runtimeFailed
TypeScript -> Kotlin: ready | rendered | openLink | error | loadRuntime
```

## Current Stack

| Area | Choice |
| --- | --- |
| Host | Kotlin 2.3.21 + IntelliJ Platform Gradle Plugin 2.18.1 |
| IDE integration | Exclusive, dumb-aware `FileEditorProvider` with a read-only JCEF editor and plain-text fallback |
| JCEF module boundary | Optional `com.intellij.modules.jcef` plugin dependency with a separate descriptor |
| Compatibility baseline | IntelliJ Platform 2025.1 (`since-build` 251) |
| Renderer | TypeScript 7.0.2 bundled by Vite 8.1.4 |
| Markdown | Marked 18.0.6 with marked-footnote 1.4.0, plus GitHub-style alerts in the renderer |
| Syntax highlighting | highlight.js 11.11.1 with a curated 12-language subset, loaded lazily |
| Architecture diagrams | Mermaid 11.16.0 with a curated offline Material Design Icons subset, loaded lazily |
| Sanitization | DOMPurify 3.4.11 plus a restrictive Content Security Policy |
| Styling | github-markdown-css 5.9.0 with GitHub Light and GitHub Dark output plus Compact and Spacious profiles |
| Appearance settings | Application-level theme (Sync with IDE, Light, Dark), density, highlight groups with eight color presets, single installed font with 12–24 px size, 768–1536 px or full available content width, restore defaults, and a tabbed live preview under Settings > Tools > MdLens |
| Renderer delivery | One self-contained core HTML resource plus separate Mermaid and highlight.js runtimes injected only when a document uses them |
| Plugin ID | `dev.hyunelab.mdlens` |

## Lightweight Baseline

Measurements MUST be repeated for releases that materially change the renderer or host lifecycle.

Measured on an Apple Silicon Mac with Java 21 and Node.js 22, at 0.5.0:

- Plugin distribution: 1.14 MB
- Self-contained core renderer: 126.2 KB raw, 34.9 KB gzip
- Lazy highlight.js runtime: 57.5 KB raw, 18.5 KB gzip
- Lazy Mermaid runtime with curated icons: 3.57 MB raw, 976.3 KB gzip
- Core renderer module load: 48.4 ms and 4.2 MiB heap
- 100 KiB Markdown fixture: 150.1 ms median, 174.8 ms p95, and 44.7 MiB retained heap for one rendered result

Run `npm run measure:renderer` to reproduce renderer module load, render latency, and retained heap measurements.  
Times and memory are a local baseline, not release budgets.  
They isolate MdLens's TypeScript renderer in Node.js with JSDOM and do not claim to measure the IDE-owned JCEF runtime.

## Constraints

- Core rendering MUST work offline.
- MdLens MUST own the normal editor for supported Markdown files while enabled.
- Content MUST be sanitized and MUST NOT write to the project.
- Renderer resources MUST be lazy-loaded and disposed with the viewer.
- Diagram failures MUST remain local to their block.
- Size, startup, render time, and memory MUST be measured.

## Appearance Contract

Themes own surface, text, code, and link colors. The default Sync with IDE theme resolves to GitHub Light or GitHub Dark from the IDE appearance at render time; open viewers re-render when the IDE theme changes. Profiles own reading rhythm and heading hierarchy, and both retain GitHub's H1 and H2 bottom dividers.

Highlight groups accent headings, bold, and inline code independently. Each group picks from eight color presets with built-in light/dark pairs; the renderer owns the hex values and falls back to the defaults (orange, gold, green) for unknown keys. The blockquote border keeps GitHub's neutral color and does not follow the heading accent.

A single installed font applies to body and code, falling back to the system font when unavailable. Font size is stored in pixels (12–24). Images retain their intrinsic size and Mermaid retains its diagram geometry. Content width is independent of the profile: full available width by default, or a 768–1536 px limit. Content remains left-aligned.

The Settings preview reuses the bundled TypeScript renderer and offers sample tabs — English, 한국어, 日本語, and 中文 for typography and tables, plus Code (Kotlin, Python, SQL, JSON, YAML) and Mermaid — with the initial tab following the IDE display language. It exists only while Settings is open, loads optional runtimes only for the tabs that need them, and is disposed with the Settings UI. The renderer reserves a stable vertical-scrollbar gutter to avoid horizontal text reflow when controls change the preview height.

## Open Decisions

- Custom CSS isolation and resource policy
- D2 runtime and syntax convention
- Excalidraw read-only workflow
- Render diagnostic interface and AI usage guide format
