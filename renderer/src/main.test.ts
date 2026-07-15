import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("github-markdown-css/github-markdown-dark.css?inline", () => ({
  default: ".markdown-body { background-color: #0d1117; }",
}));
vi.mock("github-markdown-css/github-markdown-light.css?inline", () => ({
  default: ".markdown-body { background-color: #ffffff; }",
}));

describe("viewer theme", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    document.documentElement.removeAttribute("data-theme");
    document.documentElement.removeAttribute("data-profile");
    delete window.mdLensRuntimes;
    document.head.innerHTML = "";
    document.body.innerHTML = '<main id="viewer" class="markdown-body"></main>';
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("applies a dark render request to the document", async () => {
    vi.resetModules();
    await import("./main");

    window.mdLens.connect({
      error: vi.fn(),
      loadRuntime: vi.fn(),
      openLink: vi.fn(),
      ready: vi.fn(),
      rendered: vi.fn(),
    });
    window.mdLens.render({
      version: 4,
      source: "# Dark",
      baseUrl: "file:///README.md",
      documentType: "markdown",
      theme: "dark",
      profile: "compact",
      bodyFontFamily: "",
      codeFontFamily: "",
      fontScale: 100,
    });

    await vi.advanceTimersByTimeAsync(75);

    expect(document.documentElement.dataset.theme).toBe("dark");
    expect(document.getElementById("viewer")?.style.maxWidth).toBe("none");
    expect(document.head.querySelector("style[data-md-lens-theme]")?.textContent).toContain(
      "background-color: #0d1117",
    );
    expect(document.getElementById("viewer")?.innerHTML).toContain("<h1");
  });

  it("applies spacious typography without resizing document images", async () => {
    vi.resetModules();
    await import("./main");

    window.mdLens.connect({
      error: vi.fn(),
      loadRuntime: vi.fn(),
      openLink: vi.fn(),
      ready: vi.fn(),
      rendered: vi.fn(),
    });
    window.mdLens.render({
      version: 4,
      source: "# Readable\n\n![Diagram](diagram.png)",
      baseUrl: "file:///README.md",
      documentType: "markdown",
      theme: "light",
      profile: "spacious",
      bodyFontFamily: "Atkinson Hyperlegible",
      codeFontFamily: "JetBrains Mono",
      fontScale: 130,
      maxContentWidth: 1280,
    });

    await vi.advanceTimersByTimeAsync(75);

    const viewer = document.getElementById("viewer");
    const image = viewer?.querySelector("img");
    expect(document.documentElement.dataset.profile).toBe("spacious");
    expect(document.documentElement.style.getPropertyValue("--md-lens-accent")).toBe(
      "#bc4c00",
    );
    expect(viewer?.style.fontSize).toBe("20.8px");
    expect(viewer?.style.lineHeight).toBe("1.75");
    expect(viewer?.style.maxWidth).toBe("1280px");
    expect(viewer?.style.fontFamily).toContain("Atkinson Hyperlegible");
    expect(viewer?.style.getPropertyValue("--md-lens-code-font")).toContain(
      "JetBrains Mono",
    );
    expect(image?.style.width).toBe("");
    expect(image?.style.transform).toBe("");
  });

  it("clamps text scaling and clears custom fonts when defaults are restored", async () => {
    vi.resetModules();
    await import("./main");

    window.mdLens.connect({
      error: vi.fn(),
      loadRuntime: vi.fn(),
      openLink: vi.fn(),
      ready: vi.fn(),
      rendered: vi.fn(),
    });
    window.mdLens.render({
      version: 4,
      source: "Text",
      baseUrl: "file:///README.md",
      documentType: "markdown",
      theme: "dark",
      profile: "spacious",
      bodyFontFamily: "Inter",
      codeFontFamily: "JetBrains Mono",
      fontScale: 500,
      maxContentWidth: 9999,
    });
    await vi.advanceTimersByTimeAsync(75);

    const viewer = document.getElementById("viewer");
    expect(viewer?.style.fontSize).toBe("28.8px");
    expect(viewer?.style.maxWidth).toBe("1536px");
    expect(document.documentElement.style.getPropertyValue("--md-lens-accent")).toBe(
      "#f0883e",
    );

    window.mdLens.render({
      version: 4,
      source: "Text",
      baseUrl: "file:///README.md",
      documentType: "markdown",
      theme: "light",
      profile: "compact",
      bodyFontFamily: "",
      codeFontFamily: "",
      fontScale: 50,
      maxContentWidth: null,
    });
    await vi.advanceTimersByTimeAsync(75);

    expect(document.documentElement.dataset.profile).toBe("compact");
    expect(viewer?.style.fontSize).toBe("14.4px");
    expect(viewer?.style.maxWidth).toBe("none");
    expect(viewer?.style.lineHeight).toBe("1.5");
    expect(viewer?.style.fontFamily).toBe("");
    expect(viewer?.dataset.customCodeFont).toBeUndefined();
    expect(viewer?.style.getPropertyValue("--md-lens-code-font")).toBe("");
  });

  it("loads Mermaid only when a diagram block is rendered", async () => {
    vi.resetModules();
    await import("./main");
    const loadRuntime = vi.fn();
    const rendered = vi.fn();

    window.mdLens.connect({
      error: vi.fn(),
      loadRuntime,
      openLink: vi.fn(),
      ready: vi.fn(),
      rendered,
    });
    window.mdLens.render({
      version: 4,
      source: "```mermaid\nflowchart LR\nA --> B\n```",
      baseUrl: "file:///README.md",
      documentType: "markdown",
      theme: "light",
      profile: "compact",
      bodyFontFamily: "",
      codeFontFamily: "",
      fontScale: 150,
      maxContentWidth: 1152,
    });

    await vi.advanceTimersByTimeAsync(75);
    expect(loadRuntime).toHaveBeenCalledOnce();
    expect(loadRuntime).toHaveBeenCalledWith("mermaid");

    window.mdLensRuntimes = {
      mermaid: {
        initialize: vi.fn(),
        render: vi.fn().mockResolvedValue({ svg: "<svg><text>Diagram</text></svg>" }),
      },
    };
    window.mdLens.runtimeReady("mermaid");
    await vi.runAllTimersAsync();

    const diagram = document.querySelector<HTMLElement>(".md-lens-diagram");
    expect(document.getElementById("viewer")?.style.fontSize).toBe("24px");
    expect(diagram?.textContent).toContain("Diagram");
    expect(getComputedStyle(diagram!).fontSize).toBe("16px");
    expect(getComputedStyle(diagram!).lineHeight).toBe("1.5");
    expect(rendered).toHaveBeenCalledOnce();
  });

});
