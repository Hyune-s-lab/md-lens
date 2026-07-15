import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

import { rolldown } from "rolldown";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const entryPath = resolve(root, "renderer/runtime/highlight.ts");
const outputPath = resolve(root, "build/generated/highlight/runtime-highlight.js");

const hljsVersion = JSON.parse(
  await readFile(resolve(root, "node_modules/highlight.js/package.json"), "utf8"),
).version;

const bundle = await rolldown({ input: entryPath });
const { output } = await bundle.generate({ format: "iife", minify: true });
const [chunk] = output;
if (chunk?.type !== "chunk") {
  throw new Error("Rolldown did not produce a highlight runtime chunk");
}

const runtime = [
  `/*! highlight.js ${hljsVersion} (BSD-3-Clause) with a curated language subset. */`,
  chunk.code,
  "",
].join("\n");

await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, runtime);
