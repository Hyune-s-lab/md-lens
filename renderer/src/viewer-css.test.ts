import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { afterEach, describe, expect, it } from "vitest";

const viewerStyles = readFileSync(resolve(process.cwd(), "renderer/src/viewer.css"), "utf8");
const lightTheme = readFileSync(
  resolve(process.cwd(), "node_modules/github-markdown-css/github-markdown-light.css"),
  "utf8",
);
const githubBlockquoteMargins = `
  .markdown-body blockquote > :first-child { margin-top: 0; }
  .markdown-body blockquote > :last-child { margin-bottom: 0; }
`;

describe("Spacious viewer styles", () => {
  afterEach(() => {
    document.documentElement.removeAttribute("data-profile");
    document.documentElement.removeAttribute("data-theme");
    document.head.innerHTML = "";
    document.body.innerHTML = "";
  });

  it("keeps outer paragraph rhythm without adding empty space inside blockquotes", () => {
    expect(viewerStyles).toContain("scrollbar-gutter: stable");
    expect(viewerStyles).toContain(':root[data-profile="spacious"] .markdown-body p');
    document.documentElement.dataset.profile = "spacious";
    document.head.append(styleElement(githubBlockquoteMargins), styleElement(viewerStyles));
    document.body.innerHTML = `
      <main id="viewer" class="markdown-body" style="font-size: 28.8px; line-height: 1.75">
        <p>Outer paragraph</p>
        <blockquote><p>Quoted paragraph</p></blockquote>
      </main>
    `;

    const outerParagraph = document.querySelector<HTMLElement>("#viewer > p")!;
    const quotedParagraph = document.querySelector<HTMLElement>("blockquote > p")!;
    const viewer = document.querySelector<HTMLElement>("#viewer")!;
    expect(getComputedStyle(viewer).maxWidth).toBe("none");
    expect(getComputedStyle(viewer).marginLeft).toBe("0px");
    expect(getComputedStyle(viewer).marginRight).toBe("0px");
    expect(getComputedStyle(outerParagraph).marginTop).toBe("1em");
    expect(getComputedStyle(outerParagraph).marginBottom).toBe("1em");
    expect(getComputedStyle(quotedParagraph).marginTop).toBe("0px");
    expect(getComputedStyle(quotedParagraph).marginBottom).toBe("0px");
  });

  it("keeps fenced code blocks visually distinct on GitHub Light", () => {
    document.documentElement.dataset.theme = "light";
    document.head.append(styleElement(lightTheme), styleElement(viewerStyles));
    document.body.innerHTML = `
      <main class="markdown-body">
        <pre><code>const viewer = "read-only";</code></pre>
      </main>
    `;

    const codeBlock = document.querySelector<HTMLElement>("pre")!;
    expect(getComputedStyle(codeBlock).backgroundColor).toBe("rgb(246, 248, 250)");
    expect(getComputedStyle(codeBlock).borderStyle).toBe("solid");
    expect(getComputedStyle(codeBlock).borderColor).toBe("rgb(209, 217, 224)");
  });

  it("keeps GitHub underlines on spacious H1 and H2 headings", () => {
    document.documentElement.dataset.profile = "spacious";
    document.head.append(styleElement(lightTheme), styleElement(viewerStyles));
    document.body.innerHTML = `
      <main class="markdown-body">
        <h1>H1</h1>
        <h2>H2</h2>
        <h3>H3</h3>
      </main>
    `;

    expect(getComputedStyle(document.querySelector("h1")!).borderBottomStyle).toBe("solid");
    expect(getComputedStyle(document.querySelector("h2")!).borderBottomStyle).toBe("solid");
    expect(getComputedStyle(document.querySelector("h3")!).borderBottomStyle).toBe("none");
  });
});

function styleElement(css: string): HTMLStyleElement {
  const style = document.createElement("style");
  style.textContent = css;
  return style;
}
