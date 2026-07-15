import { afterEach, describe, expect, it } from "vitest";

import { applyAppearance } from "./apply-appearance";
import type { RenderRequest } from "./render-request";

const baseRequest: RenderRequest = {
  version: 4,
  source: "",
  baseUrl: "file:///doc.md",
  documentType: "markdown",
  theme: "dark",
  profile: "compact",
  bodyFontFamily: "",
  codeFontFamily: "",
  fontScale: 100,
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

  it("keeps the pre-0.4 behavior for requests without accent fields", () => {
    const viewer = document.createElement("main");
    applyAppearance(document.documentElement, viewer, { ...baseRequest, profile: "spacious" });

    expect(document.documentElement.dataset.accentHeadings).toBe("true");
    expect(document.documentElement.dataset.accentBold).toBeUndefined();
    expect(document.documentElement.dataset.accentInlineCode).toBeUndefined();
  });
});
