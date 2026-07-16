package dev.hyunelab.mdlens.settings

import java.util.Locale

enum class MdLensPreviewSample(
    val displayName: String,
    val languageCode: String,
    val markdown: String,
) {
    ENGLISH(
        "English",
        "en",
        """
            # H1 — Reading Preview

            The preview uses the same renderer as a document, including **bold text**, `inline code`, and [links](#h2--table).

            ## H2 — Typography hierarchy

            ### H3 — Text styles

            Compare headings, paragraphs, and emphasis before applying appearance changes.

            ## H2 — Table

            | Density  | Line height | Best for               |
            | -------- | ----------- | ---------------------- |
            | Compact  | 1.5         | Skimming and diffs     |
            | Spacious | 1.75        | Long reading sessions  |

            > Blockquotes follow the density, and highlight groups accent headings, bold, and inline code.

            - Compact keeps familiar GitHub spacing.
            - Spacious is tuned for longer reading sessions.
        """.trimIndent(),
    ),
    KOREAN(
        "한국어",
        "ko",
        """
            # H1 — 읽기 미리보기

            미리보기는 문서와 같은 렌더러를 사용하며 **굵은 텍스트**, `인라인 코드`, [링크](#h2--표)를 포함합니다.

            ## H2 — 타이포그래피 계층

            ### H3 — 텍스트 스타일

            모양 설정을 적용하기 전에 제목, 문단, 강조가 어떻게 보이는지 비교해 보세요.

            ## H2 — 표

            | 밀도 | 줄 간격 | 어울리는 용도      |
            | ---- | ------- | ------------------ |
            | 간결 | 1.5     | 훑어보기와 diff    |
            | 여유 | 1.75    | 긴 글 읽기         |

            > 인용구는 밀도를 따르고, 하이라이트 그룹은 제목·굵은 글씨·인라인 코드를 강조합니다.

            - 간결은 익숙한 GitHub 간격을 유지합니다.
            - 여유는 긴 글 읽기에 맞춰 조정되어 있습니다.
        """.trimIndent(),
    ),
    JAPANESE(
        "日本語",
        "ja",
        """
            # H1 — 読書プレビュー

            プレビューはドキュメントと同じレンダラーを使い、**太字テキスト**、`インラインコード`、[リンク](#h2--テーブル)を含みます。

            ## H2 — タイポグラフィ階層

            ### H3 — テキストスタイル

            外観設定を適用する前に、見出し・段落・強調の見え方を比較してください。

            ## H2 — テーブル

            | 密度       | 行間 | 向いている用途       |
            | ---------- | ---- | -------------------- |
            | コンパクト | 1.5  | 流し読みと diff      |
            | ゆったり   | 1.75 | 長文の読書           |

            > 引用は密度に従い、ハイライトグループは見出し・太字・インラインコードを強調します。

            - コンパクトは馴染みのある GitHub の間隔を保ちます。
            - ゆったりは長文の読書向けに調整されています。
        """.trimIndent(),
    ),
    CHINESE(
        "中文",
        "zh",
        """
            # H1 — 阅读预览

            预览使用与文档相同的渲染器，包括**粗体文本**、`行内代码`和[链接](#h2--表格)。

            ## H2 — 排版层级

            ### H3 — 文本样式

            在应用外观设置之前，比较标题、段落和强调的显示效果。

            ## H2 — 表格

            | 密度 | 行距 | 适合的场景       |
            | ---- | ---- | ---------------- |
            | 紧凑 | 1.5  | 快速浏览与 diff  |
            | 宽松 | 1.75 | 长时间阅读       |

            > 引用块遵循密度设置，高亮组会强调标题、粗体和行内代码。

            - 紧凑保持熟悉的 GitHub 间距。
            - 宽松为长时间阅读而调校。
        """.trimIndent(),
    ),
    CODE(
        "Code",
        "",
        """
            # H1 — Code Preview

            Syntax highlighting uses the bundled highlight.js runtime with the GitHub theme.

            ## H2 — Kotlin

            ```kotlin
            data class RenderRequest(
              val source: String,
              val density: Density = Density.SPACIOUS,
            )

            fun render(request: RenderRequest): String {
              val label = if (request.density == Density.SPACIOUS) "Reading" else "Compact"
              return "${'$'}label: ${'$'}{request.source.length} characters"
            }
            ```

            ## H2 — Python

            ```python
            from dataclasses import dataclass

            @dataclass
            class RenderRequest:
                source: str
                density: str = "spacious"

            def render(request: RenderRequest) -> str:
                label = "Reading" if request.density == "spacious" else "Compact"
                return f"{label}: {len(request.source)} characters"
            ```

            ## H2 — SQL

            ```sql
            SELECT title, reading_time_min
            FROM documents
            WHERE format = 'markdown'
            ORDER BY reading_time_min DESC
            LIMIT 5;
            ```

            ## H2 — JSON

            ```json
            {
              "theme": "sync",
              "density": "spacious",
              "highlight": {
                "headings": "orange",
                "bold": "gold",
                "inlineCode": "green"
              },
              "contentWidth": "full"
            }
            ```

            ## H2 — YAML

            ```yaml
            viewer:
              mode: read-only
              theme: github-dark
              density: spacious
              typography:
                scale: 110
                content-width: full
              offline: true
            ```
        """.trimIndent(),
    ),
    MERMAID(
        "Mermaid",
        "",
        """
            # H1 — Mermaid Preview

            Diagrams render with the bundled Mermaid runtime and follow the selected theme.

            ## H2 — Flowchart with styled nodes

            ```mermaid
            flowchart LR
              subgraph IDE[JetBrains IDE]
                A[/"README.md"/] --> B{MdLens viewer}
              end
              subgraph Runtimes[Bundled runtimes]
                C[Marked + DOMPurify]
                D[Mermaid 11]
                E[highlight.js]
              end
              B --> C --> F([Reading view])
              B -. lazy .-> D --> F
              B -. lazy .-> E --> F
              classDef source fill:#dbeafe,stroke:#2563eb,color:#1e3a8a
              classDef viewer fill:#ffedd5,stroke:#ea580c,color:#7c2d12
              classDef runtime fill:#dcfce7,stroke:#16a34a,color:#14532d
              class A source
              class B,F viewer
              class C,D,E runtime
            ```

            ## H2 — Sequence diagram

            ```mermaid
            sequenceDiagram
              autonumber
              participant IDE
              participant Viewer
              participant Runtime as Mermaid runtime
              IDE->>Viewer: render(request)
              activate Viewer
              alt diagram block found
                Viewer->>Runtime: loadRuntime("mermaid")
                Runtime-->>Viewer: ready
              else markdown only
                Viewer->>Viewer: skip runtime
              end
              Viewer-->>IDE: rendered()
              deactivate Viewer
              Note over IDE,Viewer: Anchors and links stay clickable
            ```

            ## H2 — Pie chart

            ```mermaid
            pie showData title Where reading time goes
              "Prose" : 55
              "Code blocks" : 25
              "Diagrams" : 15
              "Tables" : 5
            ```
        """.trimIndent(),
    ),
    ;

    companion object {
        fun forLocale(locale: Locale): MdLensPreviewSample =
            entries.firstOrNull { it.languageCode.isNotEmpty() && it.languageCode == locale.language }
                ?: ENGLISH
    }
}
