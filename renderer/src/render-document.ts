import { renderMarkdown, type RenderResult } from "./render-markdown";
import type { RenderRequest } from "./render-request";

export function renderDocument(request: RenderRequest): RenderResult {
  if (request.documentType === "markdown") {
    return renderMarkdown(request);
  }

  const code = document.createElement("code");
  code.className = "language-mermaid";
  code.textContent = request.source;
  const pre = document.createElement("pre");
  pre.append(code);

  return { html: pre.outerHTML };
}
