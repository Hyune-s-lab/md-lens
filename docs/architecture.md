# Architecture

This document MUST describe the current MdLens architecture. Provisional choices are marked as candidates.

```text
Markdown or Mermaid file
    -> thin Kotlin host
    -> JCEF
    -> deep TypeScript renderer
        -> Markdown
        -> Mermaid 11.16.0 (loaded only when used)
        -> GitHub themes
        -> reading profiles and typography
```

## Ownership

Kotlin MUST own only JetBrains extension registration, file events, JCEF lifecycle, bridge transport, IDE navigation, Settings UI and persistence, and platform fallback behavior.

TypeScript MUST own Markdown parsing, sanitization, rendering profiles, themes, typography, diagram engines, DOM updates, render scheduling, error isolation, and renderer diagnostics.

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
| Compatibility baseline | IntelliJ Platform 2026.1 (`since-build` 261) |
| Renderer | TypeScript 7.0.2 bundled by Vite 8.1.4 |
| Markdown | Marked 18.0.6 |
| Architecture diagrams | Mermaid 11.16.0 with a curated offline Material Design Icons subset |
| Sanitization | DOMPurify 3.4.11 plus a restrictive Content Security Policy |
| Styling | github-markdown-css 5.9.0 with GitHub Light and GitHub Dark output plus Compact and Spacious profiles |
| Appearance settings | Application-level theme, profile, curated installed body and code fonts, 90%–180% text scaling, 768–1536 px or full available content width, and live preview under Settings > Tools > MdLens |
| Renderer delivery | One self-contained core HTML resource plus a separate Mermaid runtime injected only when a document uses Mermaid |
| Plugin ID | `dev.hyunelab.mdlens` |

## Lightweight Baseline

Measurements MUST be repeated for releases that materially change the renderer or host lifecycle.

Measured on an Apple Silicon Mac with Java 21 and Node.js 22.21.1:

- Plugin distribution: 1.08 MB
- Self-contained core renderer: 115.4 KB raw, 30.8 KB gzip
- Lazy Mermaid runtime with curated icons: 3.57 MB raw, 983.6 KB gzip
- Core renderer module load: 47.8 ms and 4.1 MiB heap
- 100 KiB Markdown fixture: 175.1 ms median, 186.4 ms p95, and 41.5 MiB retained heap for one rendered result

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

Themes own surface, text, code, and link colors. Profiles own reading rhythm and heading hierarchy; Spacious uses a theme-aware orange heading accent so headings remain distinct from GitHub's blue links. Both profiles retain GitHub's H1 and H2 bottom dividers.

Font selection offers separate curated sets of installed body and code fonts. If a selected font becomes unavailable, MdLens uses the default system font. Text scaling changes document typography only; images retain their intrinsic size and Mermaid retains its diagram geometry.

Content width is independent of the reading profile. Both profiles use the full available width by default. Users can instead apply the same configurable 768–1536 px limit to either profile, starting at the recommended 1152 px value. Content remains left-aligned.

The Settings preview reuses the bundled TypeScript renderer with a fixed Markdown sample containing heading levels, inline styles, and TypeScript and YAML code blocks. A vertical splitter places the scrollable controls in the top 38% and the full-width preview in the bottom 62%. It exists only while Settings is open, does not load optional runtimes, and is disposed with the Settings UI. The renderer reserves a stable vertical-scrollbar gutter to avoid horizontal text reflow when controls change the preview height.

## Open Decisions

- Syntax highlighter and language scope
- Custom CSS isolation and resource policy
- D2 runtime and syntax convention
- Excalidraw read-only workflow
- Render diagnostic interface and AI usage guide format
