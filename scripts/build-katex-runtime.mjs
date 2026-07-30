import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

import { rolldown } from "rolldown";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const entryPath = resolve(root, "renderer/runtime/katex.ts");
const cssPath = resolve(root, "node_modules/katex/dist/katex.min.css");
const fontDir = resolve(root, "node_modules/katex/dist/fonts");
const outputPath = resolve(root, "build/generated/katex/runtime-katex.js");

const katexVersion = JSON.parse(
  await readFile(resolve(root, "node_modules/katex/package.json"), "utf8"),
).version;

const bundle = await rolldown({ input: entryPath });
const { output } = await bundle.generate({ format: "iife", minify: true });
const [chunk] = output;
if (chunk?.type !== "chunk") {
  throw new Error("Rolldown did not produce a katex runtime chunk");
}

// Inline KaTeX CSS with fonts as data URIs so it works under CSP font-src 'none'.
const cssRaw = await readFile(cssPath, "utf8");
const fontRe = /url\(["']?(?:\.\/)?fonts\/([^"')]+)["']?\)/g;
const fontFilenames = [...new Set([...cssRaw.matchAll(fontRe)].map((m) => m[1]))];
const fontDataUris = new Map();
for (const filename of fontFilenames) {
  const fontPath = resolve(fontDir, filename);
  const fontData = await readFile(fontPath);
  const ext = filename.split(".").pop();
  const mime = ext === "woff2" ? "font/woff2" : ext === "woff" ? "font/woff" : "application/octet-stream";
  fontDataUris.set(filename, `data:${mime};base64,${fontData.toString("base64")}`);
}
const inlinedCss = cssRaw.replace(fontRe, (_, filename) => {
  const dataUri = fontDataUris.get(filename);
  if (!dataUri) {
    throw new Error(`Missing KaTeX font: ${filename}`);
  }
  return `url("${dataUri}")`;
});

const runtime = [
  `/*! KaTeX ${katexVersion} (MIT) with inlined fonts. */`,
  chunk.code,
  `var __mdLensKatexStyle = document.createElement("style");`,
  `__mdLensKatexStyle.textContent = ${JSON.stringify(inlinedCss)};`,
  `document.head.appendChild(__mdLensKatexStyle);`,
  "",
].join("\n");

await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, runtime);
