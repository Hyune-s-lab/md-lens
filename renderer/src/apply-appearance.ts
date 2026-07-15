import type { RenderRequest } from "./render-request";

const MIN_FONT_SIZE = 12;
const MAX_FONT_SIZE = 24;
const DEFAULT_CONTENT_WIDTH = 1152;
const MIN_CONTENT_WIDTH = 768;
const MAX_CONTENT_WIDTH = 1536;

export function applyAppearance(
  root: HTMLElement,
  viewer: HTMLElement,
  request: RenderRequest,
): void {
  const profile = request.profile ?? "compact";
  root.dataset.profile = profile;
  root.style.setProperty(
    "--md-lens-accent",
    request.theme === "dark" ? "#f0883e" : "#bc4c00",
  );
  root.style.setProperty(
    "--md-lens-bold-accent",
    request.theme === "dark" ? "#d2a8ff" : "#8250df",
  );
  root.style.setProperty(
    "--md-lens-code-accent",
    request.theme === "dark" ? "#7ee787" : "#116329",
  );
  applyAccent(root, "accentHeadings", request.accentHeadings ?? profile === "spacious");
  applyAccent(root, "accentBold", request.accentBold ?? false);
  applyAccent(root, "accentInlineCode", request.accentInlineCode ?? false);
  viewer.style.lineHeight = profile === "spacious" ? "1.75" : "1.5";
  viewer.style.fontSize = `${clampFontSize(request.fontSize)}px`;
  viewer.style.maxWidth = contentWidth(request.maxContentWidth);

  applyFont(viewer, request.fontFamily ?? "");
}

function applyAccent(root: HTMLElement, name: string, enabled: boolean): void {
  if (enabled) {
    root.dataset[name] = "true";
  } else {
    delete root.dataset[name];
  }
}

function applyFont(viewer: HTMLElement, family: string): void {
  if (family.length === 0) {
    viewer.style.removeProperty("font-family");
    delete viewer.dataset.customCodeFont;
    viewer.style.removeProperty("--md-lens-code-font");
    return;
  }
  viewer.style.fontFamily = fontStack(family, "system-ui, sans-serif");
  viewer.dataset.customCodeFont = "true";
  viewer.style.setProperty(
    "--md-lens-code-font",
    fontStack(family, "ui-monospace, monospace"),
  );
}

function clampFontSize(size: number): number {
  if (!Number.isFinite(size)) {
    return 14;
  }
  return Math.min(MAX_FONT_SIZE, Math.max(MIN_FONT_SIZE, Math.round(size)));
}

function contentWidth(width: number | null | undefined): string {
  if (width == null) {
    return "none";
  }
  const finiteWidth =
    typeof width === "number" && Number.isFinite(width)
      ? Math.round(width)
      : DEFAULT_CONTENT_WIDTH;
  return `${Math.min(MAX_CONTENT_WIDTH, Math.max(MIN_CONTENT_WIDTH, finiteWidth))}px`;
}

function fontStack(family: string, fallback: string): string {
  const escaped = family.replaceAll("\\", "\\\\").replaceAll('"', '\\"');
  return `"${escaped}", ${fallback}`;
}
