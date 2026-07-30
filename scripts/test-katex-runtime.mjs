import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(fileURLToPath(import.meta.url), "../..");
const runtimePath = resolve(root, "build/generated/katex/runtime-katex.js");

let failures = 0;

function assert(condition, message) {
  if (!condition) {
    console.error(`FAIL: ${message}`);
    failures++;
  } else {
    console.log(`PASS: ${message}`);
  }
}

const runtime = await readFile(runtimePath, "utf8");

// The runtime must register itself on window.mdLensRuntimes
assert(
  runtime.includes('window.mdLensRuntimes.katex'),
  "runtime registers katex on window.mdLensRuntimes",
);

// The runtime must inline KaTeX CSS with data URI fonts
assert(
  runtime.includes("data:font/woff2;base64,"),
  "runtime contains inlined woff2 fonts as data URIs",
);

// The runtime must inject a <style> element
assert(
  runtime.includes("__mdLensKatexStyle"),
  "runtime injects a style element for KaTeX CSS",
);

// Must not reference external font URLs
assert(
  !runtime.includes("url(./fonts/") && !runtime.includes('url("./fonts/'),
  "runtime does not reference external font files",
);

if (failures > 0) {
  console.error(`\n${failures} test(s) failed.`);
  process.exit(1);
} else {
  console.log("\nAll katex runtime tests passed.");
}
