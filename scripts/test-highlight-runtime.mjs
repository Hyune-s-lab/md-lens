import assert from "node:assert/strict";
import { readFile, stat } from "node:fs/promises";

import { JSDOM } from "jsdom";

const dom = new JSDOM("<!doctype html><body></body>", { runScripts: "dangerously" });

const runtimePath = "build/generated/highlight/runtime-highlight.js";
const runtimeSource = await readFile(runtimePath, "utf8");
const runtimeLoadStarted = performance.now();
dom.window.eval(runtimeSource);
const runtimeLoadMs = performance.now() - runtimeLoadStarted;

const hljs = dom.window.mdLensRuntimes?.highlight;
assert(hljs, "Bundled highlight runtime did not register its API");

const expectedLanguages = [
  "bash",
  "dockerfile",
  "java",
  "javascript",
  "json",
  "kotlin",
  "python",
  "shell",
  "sql",
  "typescript",
  "xml",
  "yaml",
];
for (const language of expectedLanguages) {
  assert(hljs.getLanguage(language), `Bundled highlight runtime is missing ${language}`);
}
for (const alias of ["kt", "ts", "js", "py", "sh", "yml", "html"]) {
  assert(hljs.getLanguage(alias), `Bundled highlight runtime is missing the ${alias} alias`);
}
assert.equal(hljs.getLanguage("rust"), undefined, "Unexpected language bundled");

const kotlinResult = hljs.highlight("data class Order(val id: Long)", {
  ignoreIllegals: true,
  language: "kotlin",
});
assert(kotlinResult.value.includes("hljs-keyword"), "Kotlin highlighting produced no tokens");
assert(!kotlinResult.value.includes("<script"), "Highlight output must not contain scripts");

const { size } = await stat(runtimePath);
console.log(
  `highlight runtime OK: ${(size / 1024).toFixed(1)} KiB, ` +
    `${expectedLanguages.length} languages, loaded in ${runtimeLoadMs.toFixed(1)} ms`,
);
