import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

import { JSDOM } from "jsdom";
import createDOMPurify from "dompurify";

const dom = new JSDOM('<!doctype html><body><div id="target"></div></body>', {
  pretendToBeVisual: true,
  runScripts: "dangerously",
});
dom.window.SVGElement.prototype.getBBox = () => ({ height: 20, width: 100, x: 0, y: 0 });
dom.window.SVGElement.prototype.getComputedTextLength = () => 100;
dom.window.HTMLCanvasElement.prototype.getContext = () => ({
  font: "",
  measureText: (text) => ({ width: String(text).length * 8 }),
});

const runtimeSource = await readFile("build/generated/mermaid/runtime-mermaid.js", "utf8");
const runtimeLoadStarted = performance.now();
dom.window.eval(runtimeSource);
const runtimeLoadMs = performance.now() - runtimeLoadStarted;
const mermaid = dom.window.mdLensRuntimes?.mermaid;
assert(mermaid, "Bundled Mermaid runtime did not register its API");
mermaid.initialize({
  htmlLabels: false,
  securityLevel: "strict",
  startOnLoad: false,
  suppressErrorRendering: true,
  theme: "default",
});

const flowchartRenderStarted = performance.now();
const result = await mermaid.render(
  "md-lens-smoke",
  "flowchart LR\n  Source --> Viewer",
  dom.window.document.getElementById("target"),
);
const flowchartRenderMs = performance.now() - flowchartRenderStarted;
assert(result.svg.startsWith("<svg"), "Bundled Mermaid runtime did not return SVG");
assert(result.svg.includes("Source"), "Bundled Mermaid SVG omitted the diagram label");
const sanitizer = createDOMPurify(dom.window);
const sanitizedSvg = sanitizer.sanitize(result.svg, {
  FORBID_TAGS: ["foreignObject", "script"],
  USE_PROFILES: { svg: true, svgFilters: true },
});
assert(sanitizedSvg.includes("Source"), "SVG sanitization removed the diagram label");

const architectureRenderStarted = performance.now();
const architecture = await mermaid.render(
  "md-lens-architecture-smoke",
  [
    "architecture-beta",
    "  group api(mdi:cloud)[API]",
    "  service db(mdi:database)[Database] in api",
    "  service server(mdi:server)[Server] in api",
    "  db:R -- L:server",
  ].join("\n"),
  dom.window.document.getElementById("target"),
);
const architectureRenderMs = performance.now() - architectureRenderStarted;
assert(architecture.svg.includes("architecture-service"), "Architecture diagram was not rendered");
assert(architecture.svg.includes("Database"), "Architecture diagram omitted its service label");
assert(architecture.svg.includes('viewBox="0 0 24 24"'), "Bundled Material Design icons were not rendered");

console.log(
  JSON.stringify({
    architectureSvgBytes: architecture.svg.length,
    architectureRenderMs: Number(architectureRenderMs.toFixed(2)),
    flowchartSvgBytes: sanitizedSvg.length,
    flowchartRenderMs: Number(flowchartRenderMs.toFixed(2)),
    runtimeLoadMs: Number(runtimeLoadMs.toFixed(2)),
    runtimeBytes: runtimeSource.length,
  }),
);
