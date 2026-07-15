import { JSDOM } from "jsdom";

const dom = new JSDOM();
globalThis.window = dom.window;
globalThis.document = dom.window.document;

globalThis.gc?.();
const heapBeforeLoad = process.memoryUsage().heapUsed;
const loadStartedAt = performance.now();
const { renderMarkdown } = await import("../renderer/src/render-markdown.ts");
const moduleLoadMs = performance.now() - loadStartedAt;
globalThis.gc?.();
const heapAfterLoad = process.memoryUsage().heapUsed;

const source = createFixture(100 * 1024);
const request = {
  version: 3,
  source,
  baseUrl: "file:///benchmark/README.md",
  documentType: "markdown",
  theme: "light",
  profile: "compact",
  bodyFontFamily: "",
  codeFontFamily: "",
  fontScale: 100,
  maxContentWidth: null,
};

globalThis.gc?.();
const heapBeforeRender = process.memoryUsage().heapUsed;
const retainedResult = renderMarkdown(request);
globalThis.gc?.();
const heapAfterRender = process.memoryUsage().heapUsed;

for (let index = 0; index < 3; index += 1) {
  renderMarkdown(request);
}

const renderTimes = [];
for (let index = 0; index < 15; index += 1) {
  const startedAt = performance.now();
  renderMarkdown(request);
  renderTimes.push(performance.now() - startedAt);
}

renderTimes.sort((left, right) => left - right);
const report = {
  fixtureBytes: Buffer.byteLength(source),
  outputBytes: Buffer.byteLength(retainedResult.html),
  rendererModuleLoadMs: round(moduleLoadMs),
  medianRenderMs: round(percentile(renderTimes, 0.5)),
  p95RenderMs: round(percentile(renderTimes, 0.95)),
  rendererModuleHeapKiB: round((heapAfterLoad - heapBeforeLoad) / 1024),
  retainedSingleRenderHeapKiB: round((heapAfterRender - heapBeforeRender) / 1024),
};

console.log(JSON.stringify(report, null, 2));

function createFixture(targetBytes) {
  const section = `
## Lightweight Markdown

- [x] GitHub-style task lists
- [ ] Safe offline rendering

| Feature | Status |
| --- | --- |
| Tables | Ready |
| Links | [Architecture](docs/architecture.md) |

> MdLens keeps the host thin and the renderer focused.

\`\`\`typescript
const viewer = "read-only";
\`\`\`
`;
  let fixture = "# MdLens Benchmark\n";
  while (Buffer.byteLength(fixture) < targetBytes) {
    fixture += section;
  }
  return fixture;
}

function percentile(sortedValues, ratio) {
  const index = Math.min(sortedValues.length - 1, Math.floor(sortedValues.length * ratio));
  return sortedValues[index];
}

function round(value) {
  return Math.round(value * 100) / 100;
}
