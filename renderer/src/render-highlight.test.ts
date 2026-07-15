import { describe, expect, it, vi } from "vitest";

import { renderCodeHighlights, type HighlightApi } from "./render-highlight";

describe("renderCodeHighlights", () => {
  it("does not load the runtime when the document has no fenced code", async () => {
    const root = document.createElement("main");
    root.innerHTML = "<p>Plain Markdown</p><pre><code>no language</code></pre>";
    const load = vi.fn<() => Promise<HighlightApi>>();

    await renderCodeHighlights(root, load, vi.fn());

    expect(load).not.toHaveBeenCalled();
  });

  it("treats mermaid fences as diagrams, not code", async () => {
    const root = document.createElement("main");
    root.innerHTML = '<pre><code class="language-mermaid">flowchart LR</code></pre>';
    const load = vi.fn<() => Promise<HighlightApi>>();

    await renderCodeHighlights(root, load, vi.fn());

    expect(load).not.toHaveBeenCalled();
  });

  it("highlights known languages, skips unknown ones, and sanitizes output", async () => {
    const root = document.createElement("main");
    root.innerHTML = [
      '<pre><code class="language-kotlin">val x = 1</code></pre>',
      '<pre><code class="language-rust">let x = 1;</code></pre>',
    ].join("");
    const highlight = vi.fn<HighlightApi["highlight"]>().mockReturnValue({
      value: '<span class="hljs-keyword">val</span> x = 1<img src="x" onerror="alert(1)">',
    });
    const api: HighlightApi = {
      getLanguage: (name) => (name === "kotlin" ? {} : undefined),
      highlight,
    };
    const reportError = vi.fn();

    await renderCodeHighlights(root, async () => api, reportError);

    const kotlinBlock = root.querySelector("code.language-kotlin")!;
    expect(kotlinBlock.classList.contains("hljs")).toBe(true);
    expect(kotlinBlock.innerHTML).toContain('<span class="hljs-keyword">val</span>');
    expect(kotlinBlock.innerHTML).not.toContain("onerror");
    expect(root.querySelector("code.language-rust")!.textContent).toBe("let x = 1;");
    expect(highlight).toHaveBeenCalledTimes(1);
    expect(highlight).toHaveBeenCalledWith("val x = 1", { ignoreIllegals: true, language: "kotlin" });
    expect(reportError).not.toHaveBeenCalled();
  });

  it("isolates highlighting failures per block", async () => {
    const root = document.createElement("main");
    root.innerHTML = [
      '<pre><code class="language-kotlin">broken</code></pre>',
      '<pre><code class="language-yaml">key: value</code></pre>',
    ].join("");
    const api: HighlightApi = {
      getLanguage: () => ({}),
      highlight: vi
        .fn<HighlightApi["highlight"]>()
        .mockImplementationOnce(() => {
          throw new Error("boom");
        })
        .mockReturnValueOnce({ value: '<span class="hljs-attr">key</span>: value' }),
    };
    const reportError = vi.fn();

    await renderCodeHighlights(root, async () => api, reportError);

    expect(root.querySelector("code.language-kotlin")!.textContent).toBe("broken");
    expect(root.querySelector("code.language-yaml")!.innerHTML).toContain("hljs-attr");
    expect(reportError).toHaveBeenCalledOnce();
  });

  it("reports a runtime load failure and leaves code unstyled", async () => {
    const root = document.createElement("main");
    root.innerHTML = '<pre><code class="language-kotlin">val x = 1</code></pre>';
    const reportError = vi.fn();

    await renderCodeHighlights(
      root,
      async () => {
        throw new Error("offline");
      },
      reportError,
    );

    expect(root.querySelector("code")!.textContent).toBe("val x = 1");
    expect(reportError).toHaveBeenCalledWith("Unable to load syntax highlighting: offline");
  });
});
