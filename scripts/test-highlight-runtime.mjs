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
  "c",
  "cpp",
  "csharp",
  "css",
  "dart",
  "diff",
  "dockerfile",
  "go",
  "graphql",
  "groovy",
  "ini",
  "java",
  "javascript",
  "json",
  "kotlin",
  "less",
  "lua",
  "makefile",
  "markdown",
  "nginx",
  "nix",
  "objectivec",
  "perl",
  "pgsql",
  "php",
  "php-template",
  "plaintext",
  "powershell",
  "protobuf",
  "python",
  "python-repl",
  "r",
  "ruby",
  "rust",
  "scala",
  "scss",
  "shell",
  "sql",
  "swift",
  "typescript",
  "vbnet",
  "wasm",
  "xml",
  "yaml",
];
for (const language of expectedLanguages) {
  assert(hljs.getLanguage(language), `Bundled highlight runtime is missing ${language}`);
}
for (const alias of [
  "kt",
  "kts",
  "ts",
  "js",
  "py",
  "sh",
  "zsh",
  "yml",
  "html",
  "cs",
  "toml",
  "postgres",
  "postgresql",
  "psql",
  "mysql",
  "mariadb",
  "plsql",
  "sqlite",
  "proto",
  "ps1",
]) {
  assert(hljs.getLanguage(alias), `Bundled highlight runtime is missing the ${alias} alias`);
}
assert.equal(hljs.getLanguage("brainfuck"), undefined, "Unexpected language bundled");

const kotlinResult = hljs.highlight("data class Order(val id: Long)", {
  ignoreIllegals: true,
  language: "kotlin",
});
assert(kotlinResult.value.includes("hljs-keyword"), "Kotlin highlighting produced no tokens");
assert(!kotlinResult.value.includes("<script"), "Highlight output must not contain scripts");

const sqlResult = hljs.highlight(
  "SELECT COUNT(order_id) FROM orders WHERE status = 'PAID' GROUP BY customer_id",
  { ignoreIllegals: true, language: "sql" },
);
assert(sqlResult.value.includes("hljs-keyword"), "SQL highlighting produced no keywords");
assert(sqlResult.value.includes("hljs-built_in"), "SQL highlighting produced no built-ins");
assert(sqlResult.value.includes("hljs-string"), "SQL highlighting produced no strings");

const rustResult = hljs.highlight("fn main() { let value = Some(42); }", {
  ignoreIllegals: true,
  language: "rust",
});
assert(rustResult.value.includes("hljs-keyword"), "Rust highlighting produced no tokens");

const { size } = await stat(runtimePath);
assert(size <= 256 * 1024, `Highlight runtime is too large: ${(size / 1024).toFixed(1)} KiB`);
console.log(
  `highlight runtime OK: ${(size / 1024).toFixed(1)} KiB, ` +
    `${expectedLanguages.length} languages, loaded in ${runtimeLoadMs.toFixed(1)} ms`,
);
