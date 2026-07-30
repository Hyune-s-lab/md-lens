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
});
