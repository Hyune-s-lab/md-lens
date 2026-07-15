import darkTheme from "github-markdown-css/github-markdown-dark.css?inline";
import lightTheme from "github-markdown-css/github-markdown-light.css?inline";
import hljsDarkTheme from "highlight.js/styles/github-dark.css?inline";
import hljsLightTheme from "highlight.js/styles/github.css?inline";

import "./viewer.css";
import { applyAppearance } from "./apply-appearance";
import { loadRuntime, runtimeFailed, runtimeReady } from "./load-runtime";
import { renderDocument } from "./render-document";
import { renderCodeHighlights } from "./render-highlight";
import { renderMermaidDiagrams } from "./render-mermaid";
import type { RenderRequest } from "./render-request";

interface MdLensHost {
  error(message: string): void;
  loadRuntime(name: string): void;
  openLink(href: string): void;
  ready(): void;
  rendered(): void;
}

interface MdLensBridge {
  connect(host: MdLensHost): void;
  render(request: RenderRequest): void;
  runtimeFailed(name: string, message: string): void;
  runtimeReady(name: string): void;
}

declare global {
  interface Window {
    mdLens: MdLensBridge;
  }
}

const viewer = requiredElement("viewer");
const themeStyle = document.createElement("style");
themeStyle.dataset.mdLensTheme = "true";
document.head.append(themeStyle);

let host: MdLensHost | undefined;
let pendingRequest: RenderRequest | undefined;
let renderTimer: number | undefined;
let renderGeneration = 0;

window.mdLens = {
  connect(nextHost) {
    host = nextHost;
    host.ready();
  },
  runtimeFailed(name, message) {
    runtimeFailed(name, message);
  },
  runtimeReady(name) {
    runtimeReady(name);
  },
  render(request) {
    pendingRequest = request;
    if (renderTimer !== undefined) {
      window.clearTimeout(renderTimer);
    }
    renderTimer = window.setTimeout(flushRender, 75);
  },
};

function flushRender(): void {
  const request = pendingRequest;
  pendingRequest = undefined;
  renderTimer = undefined;
  if (request === undefined) {
    return;
  }

  const generation = ++renderGeneration;
  void renderRequest(request, generation);
}

async function renderRequest(request: RenderRequest, generation: number): Promise<void> {
  try {
    const scrollTop = document.documentElement.scrollTop;
    themeStyle.textContent =
      request.theme === "dark" ? darkTheme + hljsDarkTheme : lightTheme + hljsLightTheme;
    document.documentElement.dataset.theme = request.theme;
    applyAppearance(document.documentElement, viewer, request);
    viewer.classList.remove("md-lens-error");
    viewer.innerHTML = renderDocument(request).html;
    await renderMermaidDiagrams(
      viewer,
      request.theme,
      () => loadRuntime("mermaid", requestRuntimeFromHost),
      (message) => host?.error(message),
    );
    await renderCodeHighlights(
      viewer,
      () => loadRuntime("highlight", requestRuntimeFromHost),
      (message) => host?.error(message),
    );
    if (generation !== renderGeneration) {
      return;
    }
    document.documentElement.scrollTop = scrollTop;
    host?.rendered();
  } catch (error) {
    if (generation !== renderGeneration) {
      return;
    }
    const message = error instanceof Error ? error.message : String(error);
    viewer.textContent = `Unable to render this document: ${message}`;
    viewer.classList.add("md-lens-error");
    host?.error(message);
  }
}

function requestRuntimeFromHost(name: string): void {
  if (host === undefined) {
    throw new Error("MdLens host is not connected");
  }
  host.loadRuntime(name);
}

document.addEventListener("click", (event) => {
  const target = event.target;
  const anchor = target instanceof Element ? target.closest("a") : null;
  if (!(anchor instanceof HTMLAnchorElement)) {
    return;
  }

  event.preventDefault();
  const url = new URL(anchor.href);
  if (url.pathname === window.location.pathname && url.hash.length > 1) {
    const id = decodeURIComponent(url.hash.slice(1));
    document.getElementById(id)?.scrollIntoView();
    return;
  }
  host?.openLink(anchor.href);
});

function requiredElement(id: string): HTMLElement {
  const element = document.getElementById(id);
  if (element === null) {
    throw new Error(`Missing #${id}`);
  }
  return element;
}
