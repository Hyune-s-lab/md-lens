import { describe, expect, it } from "vitest";

import { renderMarkdown } from "./render-markdown";

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
});
