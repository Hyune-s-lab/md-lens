# MdLens Agent Guidelines

## Product

- MdLens MUST remain a lightweight, read-only Markdown viewer for JetBrains IDEs.
- Direct human use SHOULD remain intuitive.
- MdLens MUST NOT add editing, autocomplete, inspections, refactoring, WYSIWYG, or background indexing.
- Core rendering MUST work offline and MUST NOT depend on a runtime CDN.

## Architecture

- Kotlin MUST contain only stable JetBrains Platform and JCEF integration.
- Product behavior and volatile rendering logic MUST live in TypeScript.
- The Kotlin/TypeScript bridge MUST remain small, serializable, and versioned.
- Renderer and diagram resources MUST load only when needed and MUST be released with the viewer.
- Viewing a document MUST NOT write to the project.
- Markdown and diagram input MUST be treated as untrusted content and sanitized.

## Language

- Public content MUST be written in English, including documentation, UI text, Git history, issues, PRs, and release notes, except accepted decision records.
- Private discussion with the project owner MAY be written in Korean.
- Code identifiers and established technical terms SHOULD keep their original names.

## Documentation

- `README.md` MUST remain a concise product entry point and describe only key product capabilities.
- `docs/roadmap.md` MUST own versioned release scope and future work.
- `docs/architecture.md` MUST describe only the current architecture; accepted decisions belong under `docs/decisions/`.
- Accepted decision records under `docs/decisions/` MUST be written in Korean.
- Marketplace Overview MUST describe durable product capabilities, not version-specific implementation details.
- Marketplace Overview MUST link only to the GitHub repository root, not to individual documents such as the roadmap.
- Marketplace What's New MUST summarize only the current release, group changes by change type like the GitHub Release notes, and MUST NOT contain links.
- Detailed status or decisions MUST NOT be duplicated across documents.

## Release Preparation

- GitHub Releases MUST own published release notes and version history.
- GitHub Release titles MUST use the `v<version>` form, such as `v0.1.0`.
- Release notes MUST group changes by change type, using Spring Boot-style emoji headings such as `:star: New Features`, `:lady_beetle: Bug Fixes`, `:notebook_with_decorative_cover: Documentation`, and `:hammer: Dependency Upgrades`.
- All feature changes in one release MUST share a single `:star: New Features` section; split sections only when the change type differs.
- Every GitHub Release MUST begin as a draft.
- The release workflow MUST build the draft's `v<version>` tag, run fast package validation, attach the resulting plugin ZIP, and upload the same ZIP to JetBrains Marketplace before the draft is published.
- When the project owner asks to create a release draft, the agent MUST create the draft, immediately dispatch its release workflow, and report only after that workflow completes.

## Publishing

- A GitHub Release draft MUST be published only after its release workflow succeeds.
- The first JetBrains Marketplace listing MUST be created through the Marketplace UI.
- Marketplace uploads MUST use a repository secret only after the first manual Marketplace listing; JetBrains approval remains manual.

## Verification

- Changes SHOULD be small, runnable, and tested at the closest useful level.
- Pull request CI MUST run fast tests and package validation; JetBrains Marketplace MUST own compatibility verification for submitted plugin versions.
- Renderer tests SHOULD cover valid input, invalid diagrams, and unsafe content.
- Changes affecting lightweight behavior MUST measure the relevant size, startup, or memory impact.
- Manual smoke testing MUST cover only behavior that automation cannot verify.
- Reports MUST list only checks that were actually run and MUST disclose remaining risk.

## Git

- Work MUST happen on a focused branch and reach `main` through a pull request.
- Pull requests MUST be opened ready for review, MUST NOT be drafts, and MUST assign `Hyune-c`.
- AI agents MAY merge pull requests, create GitHub Releases, and attach build artifacts autonomously.
- JetBrains Marketplace uploads MUST NOT happen without the project owner's explicit request.
- Unrelated changes MUST NOT be mixed in one commit.
- Generated output, IDE caches, and local environment files MUST NOT be committed.
