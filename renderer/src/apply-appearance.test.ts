import { afterEach, describe, expect, it } from "vitest";

import { applyAppearance } from "./apply-appearance";
import type { RenderRequest } from "./render-request";

const baseRequest: RenderRequest = {
  version: 5,
  source: "",
  baseUrl: "file:///doc.md",
  documentType: "markdown",
  theme: "dark",
  profile: "compact",
  fontFamily: "",
  fontSize: 14,
  maxContentWidth: null,
};

describe("applyAppearance highlight groups", () => {
  afterEach(() => {
    for (const name of ["accentHeadings", "accentBold", "accentInlineCode", "profile"]) {
      delete document.documentElement.dataset[name];
    }
  });

  it("marks each enabled highlight group on the root element", () => {
    const viewer = document.createElement("main");
    applyAppearance(document.documentElement, viewer, {
      ...baseRequest,
      accentHeadings: true,
      accentBold: true,
      accentInlineCode: false,
    });

    expect(document.documentElement.dataset.accentHeadings).toBe("true");
    expect(document.documentElement.dataset.accentBold).toBe("true");
    expect(document.documentElement.dataset.accentInlineCode).toBeUndefined();
    expect(
      document.documentElement.style.getPropertyValue("--md-lens-code-accent"),
    ).toBe("#7ee787");

    applyAppearance(document.documentElement, viewer, { ...baseRequest, accentBold: false });
    expect(document.documentElement.dataset.accentBold).toBeUndefined();
  });

  it("applies the default accent presets per theme", () => {
    const viewer = document.createElement("main");
    applyAppearance(document.documentElement, viewer, baseRequest);

    expect(document.documentElement.style.getPropertyValue("--md-lens-accent")).toBe("#f0883e");
    expect(document.documentElement.style.getPropertyValue("--md-lens-bold-accent")).toBe("#e3b341");
    expect(document.documentElement.style.getPropertyValue("--md-lens-code-accent")).toBe("#7ee787");

    applyAppearance(document.documentElement, viewer, { ...baseRequest, theme: "light" });
    expect(document.documentElement.style.getPropertyValue("--md-lens-accent")).toBe("#bc4c00");
    expect(document.documentElement.style.getPropertyValue("--md-lens-bold-accent")).toBe("#9a6700");
    expect(document.documentElement.style.getPropertyValue("--md-lens-code-accent")).toBe("#116329");
  });

  it("applies selected accent presets and falls back on unknown keys", () => {
    const viewer = document.createElement("main");
    applyAppearance(document.documentElement, viewer, {
      ...baseRequest,
      accentHeadingsColor: "blue",
      accentBoldColor: "pink",
      accentInlineCodeColor: "not-a-color",
    });

    expect(document.documentElement.style.getPropertyValue("--md-lens-accent")).toBe("#79c0ff");
    expect(document.documentElement.style.getPropertyValue("--md-lens-bold-accent")).toBe("#f778ba");
    expect(document.documentElement.style.getPropertyValue("--md-lens-code-accent")).toBe("#7ee787");
  });

  it("keeps the pre-0.4 behavior for requests without accent fields", () => {
    const viewer = document.createElement("main");
    applyAppearance(document.documentElement, viewer, { ...baseRequest, profile: "spacious" });

    expect(document.documentElement.dataset.accentHeadings).toBe("true");
    expect(document.documentElement.dataset.accentBold).toBeUndefined();
    expect(document.documentElement.dataset.accentInlineCode).toBeUndefined();
  });
});
