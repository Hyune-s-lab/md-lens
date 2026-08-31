import { describe, expect, it, vi } from "vitest";

import { renderCodeHighlights, type HighlightApi } from "./render-highlight";

const sqlLanguages = [
  "sql",
  "pgsql",
  "postgres",
  "postgresql",
  "psql",
  "mysql",
  "mariadb",
  "plsql",
  "sqlite",
] as const;

async function renderSql(language: string, highlighted: string): Promise<HTMLElement> {
  const root = document.createElement("main");
  root.innerHTML = `<pre><code class="language-${language}">source</code></pre>`;
  const api: HighlightApi = {
    getLanguage: () => ({}),
    highlight: () => ({ value: highlighted }),
  };

  await renderCodeHighlights(root, async () => api, vi.fn());

  return root.querySelector("code")!;
}

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

  it("colors plain function calls across language grammars without touching strings or comments", async () => {
    const root = document.createElement("main");
    root.innerHTML = '<pre><code class="language-kotlin">source</code></pre>';
    const api: HighlightApi = {
      getLanguage: () => ({}),
      highlight: () => ({
        value: [
          '<span class="hljs-keyword">val</span> result = calculate(value)',
          '<span class="hljs-comment">// ignored(comment)</span>',
          '<span class="hljs-string">"ignored(string)"</span>',
          'if (result) repository.save(result)',
          'café(result)',
        ].join("\n"),
      }),
    };

    await renderCodeHighlights(root, async () => api, vi.fn());

    const code = root.querySelector("code")!;
    const calls = Array.from(code.querySelectorAll(".hljs-title.function_.invoke__")).map(
      (element) => element.textContent,
    );
    expect(calls).toEqual(["calculate", "save", "café"]);
    expect(code.querySelector(".hljs-comment")!.innerHTML).toBe("// ignored(comment)");
    expect(code.querySelector(".hljs-string")!.innerHTML).toBe('"ignored(string)"');
  });

  it("leaves plaintext function-like text undecorated", async () => {
    const root = document.createElement("main");
    root.innerHTML = '<pre><code class="language-plaintext">if (value) example()</code></pre>';
    const api: HighlightApi = {
      getLanguage: () => ({}),
      highlight: () => ({ value: "if (value) example()" }),
    };

    await renderCodeHighlights(root, async () => api, vi.fn());

    expect(root.querySelector(".invoke__")).toBeNull();
  });

  it.each(sqlLanguages)("preserves native %s DDL highlighting without decorating table names", async (language) => {
    const highlighted = '<span class="hljs-keyword">CREATE TABLE</span> users (id INT);';

    const code = await renderSql(language, highlighted);

    expect(code.innerHTML).toBe(highlighted);
    expect(code.querySelector(".invoke__")).toBeNull();
  });

  it.each(sqlLanguages)(
    "preserves native %s INSERT highlighting without decorating column-list table names",
    async (language) => {
      const highlighted =
        '<span class="hljs-keyword">INSERT INTO</span> users (id, name) <span class="hljs-keyword">VALUES</span> (1, \'Ada\');';

      const code = await renderSql(language, highlighted);

      expect(code.innerHTML).toBe(highlighted);
      expect(code.querySelector(".invoke__")).toBeNull();
    },
  );

  it.each(sqlLanguages)(
    "preserves native %s CTE highlighting without decorating CTE column lists",
    async (language) => {
      const highlighted =
        '<span class="hljs-keyword">WITH</span> totals (amount) <span class="hljs-keyword">AS</span> (<span class="hljs-keyword">SELECT</span> 1) <span class="hljs-keyword">SELECT</span> amount <span class="hljs-keyword">FROM</span> totals;';

      const code = await renderSql(language, highlighted);

      expect(code.innerHTML).toBe(highlighted);
      expect(code.querySelector(".invoke__")).toBeNull();
    },
  );

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
