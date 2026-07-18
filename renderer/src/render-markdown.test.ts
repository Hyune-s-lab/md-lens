import { describe, expect, it } from "vitest";

import { renderMarkdown } from "./render-markdown";
import type { RenderRequest } from "./render-request";

function render(source: string): string {
  const request: RenderRequest = {
    version: 5,
    source,
    baseUrl: "file:///project/docs/guide.md",
    documentType: "markdown",
    theme: "light",
    profile: "compact",
    fontFamily: "",
    fontSize: 14,
    maxContentWidth: 1152,
  };
  return renderMarkdown(request).html;
}

describe("renderMarkdown", () => {
  it("renders GitHub-flavored task lists", () => {
    const result = renderMarkdown({
      version: 5,
      source: "# Tasks\n\n- [x] Render Markdown",
      baseUrl: "file:///project/docs/guide.md",
      documentType: "markdown",
      theme: "light",
      profile: "compact",
      fontFamily: "",
      fontSize: 14,
      maxContentWidth: 1152,
    });

    expect(result.html).toContain('<h1 id="tasks">Tasks</h1>');
    expect(result.html).toContain('type="checkbox"');
    expect(result.html).toContain("checked");
  });

  it("removes executable and interactive content", () => {
    const result = renderMarkdown({
      version: 5,
      source: [
        '<script>alert("no")</script>',
        '<div style="position: fixed" onclick="alert(1)">content</div>',
        '<input type="text" value="editable">',
        '<a href="javascript:alert(1)">unsafe</a>',
      ].join("\n"),
      baseUrl: "file:///project/unsafe.md",
      documentType: "markdown",
      theme: "dark",
      profile: "compact",
      fontFamily: "",
      fontSize: 14,
      maxContentWidth: 1152,
    });

    expect(result.html).not.toMatch(/script|onclick|style=|javascript:/i);
    expect(result.html).not.toContain('type="text"');
    expect(result.html).toContain("content");
    expect(result.html).toContain("unsafe");
  });

  it("creates stable GitHub-style anchors for headings", () => {
    const result = renderMarkdown({
      version: 5,
      source: "## Hello, MdLens!\n\n## Hello, MdLens!",
      baseUrl: "file:///project/readme.md",
      documentType: "markdown",
      theme: "light",
      profile: "compact",
      fontFamily: "",
      fontSize: 14,
      maxContentWidth: 1152,
    });

    expect(result.html).toContain('<h2 id="hello-mdlens">');
    expect(result.html).toContain('<h2 id="hello-mdlens-1">');
  });

  it("renders every GitHub alert kind with its title and icon", () => {
    const kinds: Array<[string, string, string]> = [
      ["NOTE", "Note", "octicon-info"],
      ["TIP", "Tip", "octicon-light-bulb"],
      ["IMPORTANT", "Important", "octicon-report"],
      ["WARNING", "Warning", "octicon-alert"],
      ["CAUTION", "Caution", "octicon-stop"],
    ];
    for (const [marker, title, icon] of kinds) {
      const html = render(`> [!${marker}]\n> Useful text.`);
      expect(html).toContain(`markdown-alert-${marker.toLowerCase()}`);
      expect(html).toContain(`>${title}</p>`);
      expect(html).toContain(icon);
      expect(html).toContain("Useful text.");
      expect(html).not.toContain(`[!${marker}]`);
      expect(html).not.toContain("<blockquote>");
    }
  });

  it("keeps alert content that spans multiple paragraphs", () => {
    const html = render("> [!NOTE]\n>\n> First paragraph.\n>\n> Second paragraph.");
    expect(html).toContain("markdown-alert-note");
    expect(html).toContain("First paragraph.");
    expect(html).toContain("Second paragraph.");
  });

  it("leaves regular and near-miss blockquotes untouched", () => {
    const plain = render("> Just a quote.");
    expect(plain).toContain("<blockquote>");
    expect(plain).not.toContain("markdown-alert");

    const trailingText = render("> [!NOTE] inline text on the marker line");
    expect(trailingText).toContain("<blockquote>");
    expect(trailingText).not.toContain("markdown-alert");

    const unknownKind = render("> [!DANGER]\n> Not a GitHub kind.");
    expect(unknownKind).toContain("<blockquote>");
    expect(unknownKind).not.toContain("markdown-alert");

    const nested = render("> outer quote\n> > [!NOTE]\n> > nested marker");
    expect(nested).toContain("<blockquote>");
    expect(nested).not.toContain("markdown-alert");
  });

  it("renders footnotes with back-references", () => {
    const html = render("Body text[^1] here.\n\n[^1]: The footnote body.");
    expect(html).toContain("data-footnote-ref");
    expect(html).toContain('<section class="footnotes" data-footnotes');
    expect(html).toContain("The footnote body.");
    expect(html).toContain("data-footnote-backref");
    expect(html).toContain('id="footnote-label"');
  });

  it("keeps documents without footnotes byte-identical in structure", () => {
    const html = render("Plain text with [brackets] and a caret^ but no footnotes.");
    expect(html).not.toContain("data-footnotes");
    expect(html).toContain("Plain text with [brackets] and a caret^ but no footnotes.");
  });
});
