import katex from "katex";

declare global {
  interface Window {
    mdLensRuntimes?: Record<string, unknown>;
  }
}

window.mdLensRuntimes = window.mdLensRuntimes ?? {};
window.mdLensRuntimes.katex = katex;
