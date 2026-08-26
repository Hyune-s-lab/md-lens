import { describe, expect, it } from "vitest";
import { renderMath } from "./render-katex";
import type { KatexApi } from "./render-katex";

function createMockKatex(): KatexApi {
  return {
    renderToString(tex: string, options: { displayMode?: boolean }): string {
      const mode = options.displayMode ? "display" : "inline";
      return `<span class="katex-mock" data-mode="${mode}" data-tex="${tex}">mock:${tex}</span>`;
    },
  };
}

describe("renderMath", () => {
  it("renders inline math", async () => {
    const root = document.createElement("div");
    root.textContent = "The equation $E=mc^2$ is famous.";
    const errors: string[] = [];
    await renderMath(root, async () => createMockKatex(), (m) => errors.push(m));
    expect(errors).toEqual([]);
    const mock = root.querySelector(".katex-mock");
    expect(mock).not.toBeNull();
    expect(mock?.getAttribute("data-mode")).toBe("inline");
    expect(mock?.getAttribute("data-tex")).toBe("E=mc^2");
  });

  it("renders display math", async () => {
    const root = document.createElement("div");
    root.innerHTML = "<p>$$\\sum_{i=1}^n x_i$$</p>";
    const errors: string[] = [];
    await renderMath(root, async () => createMockKatex(), (m) => errors.push(m));
    expect(errors).toEqual([]);
    const mock = root.querySelector(".katex-mock");
    expect(mock).not.toBeNull();
    expect(mock?.getAttribute("data-mode")).toBe("display");
  });

  it("skips math inside code blocks", async () => {
    const root = document.createElement("div");
    root.innerHTML = "<pre><code>$E=mc^2$</code></pre>";
    const errors: string[] = [];
    await renderMath(root, async () => createMockKatex(), (m) => errors.push(m));
    expect(errors).toEqual([]);
    expect(root.querySelector(".katex-mock")).toBeNull();
    expect(root.querySelector("code")?.textContent).toBe("$E=mc^2$");
  });

  it("does nothing when no math is present", async () => {
    const root = document.createElement("div");
    root.textContent = "No math here at all.";
    const errors: string[] = [];
    await renderMath(root, async () => createMockKatex(), (m) => errors.push(m));
    expect(errors).toEqual([]);
    expect(root.querySelector(".katex-mock")).toBeNull();
  });

  it("reports error when KaTeX fails to load", async () => {
    const root = document.createElement("div");
    root.textContent = "$E=mc^2$";
    const errors: string[] = [];
    await renderMath(
      root,
      async () => {
        throw new Error("Network error");
      },
      (m) => errors.push(m),
    );
    expect(errors.length).toBe(1);
    expect(errors[0]).toContain("Unable to load KaTeX");
  });

  it("renders multiple inline math expressions", async () => {
    const root = document.createElement("div");
    root.textContent = "$a$ and $b$";
    const errors: string[] = [];
    await renderMath(root, async () => createMockKatex(), (m) => errors.push(m));
    expect(errors).toEqual([]);
    const mocks = root.querySelectorAll(".katex-mock");
    expect(mocks.length).toBe(2);
    expect(mocks[0]?.getAttribute("data-tex")).toBe("a");
    expect(mocks[1]?.getAttribute("data-tex")).toBe("b");
  });

  it("does not treat currency $49 as math", async () => {
    const root = document.createElement("div");
    root.textContent = "Costs about $49 per month and $147 per quarter.";
    const errors: string[] = [];
    await renderMath(root, async () => createMockKatex(), (m) => errors.push(m));
    expect(errors).toEqual([]);
    expect(root.querySelector(".katex-mock")).toBeNull();
    expect(root.textContent).toContain("$49");
    expect(root.textContent).toContain("$147");
  });

  it("does not treat $5.00 as math", async () => {
    const root = document.createElement("div");
    root.textContent = "Price: $5.00 and $10.99";
    const errors: string[] = [];
    await renderMath(root, async () => createMockKatex(), (m) => errors.push(m));
    expect(errors).toEqual([]);
    expect(root.querySelector(".katex-mock")).toBeNull();
    expect(root.textContent).toContain("$5.00");
    expect(root.textContent).toContain("$10.99");
  });

  it("renders math with LaTeX commands but not pure numbers", async () => {
    const root = document.createElement("div");
    root.textContent = "Value is $\\frac{a}{b}$ and cost is $50";
    const errors: string[] = [];
    await renderMath(root, async () => createMockKatex(), (m) => errors.push(m));
    expect(errors).toEqual([]);
    const mocks = root.querySelectorAll(".katex-mock");
    expect(mocks.length).toBe(1);
    expect(mocks[0]?.getAttribute("data-tex")).toBe("\\frac{a}{b}");
    expect(root.textContent).toContain("$50");
  });

  it("preserves unmatched $ as literal text", async () => {
    const root = document.createElement("div");
    root.textContent = "Price is $49 only (no closing dollar)";
    const errors: string[] = [];
    await renderMath(root, async () => createMockKatex(), (m) => errors.push(m));
    expect(errors).toEqual([]);
    expect(root.querySelector(".katex-mock")).toBeNull();
    expect(root.textContent).toContain("$49");
  });

  it("does not treat currency with surrounding text as math", async () => {
    const root = document.createElement("div");
    root.textContent = "단일 c6g.large 약 $49/월, 클러스터3노드약$147/월";
    const errors: string[] = [];
    await renderMath(root, async () => createMockKatex(), (m) => errors.push(m));
    expect(errors).toEqual([]);
    expect(root.querySelector(".katex-mock")).toBeNull();
    expect(root.textContent).toContain("$49");
    expect(root.textContent).toContain("$147");
  });
});
