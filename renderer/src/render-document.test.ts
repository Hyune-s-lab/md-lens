import { describe, expect, it } from "vitest";

import { renderDocument } from "./render-document";

describe("renderDocument", () => {
  it("turns a standalone Mermaid file into a safe diagram block", () => {
    const result = renderDocument({
      version: 4,
      source: 'flowchart LR\n  A["<script>alert(1)</script>"] --> B',
      baseUrl: "file:///project/system.mmd",
      documentType: "mermaid",
      theme: "light",
      profile: "compact",
      bodyFontFamily: "",
      codeFontFamily: "",
      fontScale: 100,
      maxContentWidth: 1152,
    });

    expect(result.html).toContain('class="language-mermaid"');
    expect(result.html).toContain("&lt;script&gt;alert(1)&lt;/script&gt;");
    expect(result.html).not.toContain("<script>");
  });
});
