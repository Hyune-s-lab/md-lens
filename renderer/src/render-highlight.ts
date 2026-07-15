import DOMPurify from "dompurify";

export interface HighlightApi {
  getLanguage(name: string): unknown;
  highlight(code: string, options: { ignoreIllegals?: boolean; language: string }): { value: string };
}

export type HighlightLoader = () => Promise<HighlightApi>;
export type HighlightErrorReporter = (message: string) => void;

export async function renderCodeHighlights(
  root: HTMLElement,
  loadHighlight: HighlightLoader,
  reportError: HighlightErrorReporter,
): Promise<void> {
  const blocks = Array.from(root.querySelectorAll<HTMLElement>('pre > code[class*="language-"]'))
    .map((code) => ({ code, language: languageOf(code) }))
    .filter((block): block is { code: HTMLElement; language: string } => block.language !== null);

  if (blocks.length === 0) {
    return;
  }

  let highlighter: HighlightApi;
  try {
    highlighter = await loadHighlight();
  } catch (error) {
    reportError(`Unable to load syntax highlighting: ${errorMessage(error)}`);
    return;
  }

  for (const { code, language } of blocks) {
    if (highlighter.getLanguage(language) === undefined) {
      continue;
    }
    try {
      const result = highlighter.highlight(code.textContent ?? "", {
        ignoreIllegals: true,
        language,
      });
      code.innerHTML = String(DOMPurify.sanitize(result.value, { USE_PROFILES: { html: true } }));
      code.classList.add("hljs");
    } catch (error) {
      reportError(`Syntax highlighting failed: ${errorMessage(error)}`);
    }
  }
}

function languageOf(code: Element): string | null {
  const match = /(?:^|\s)language-([\w+-]+)/.exec(code.className);
  const language = match?.[1]?.toLowerCase() ?? null;
  return language === "mermaid" ? null : language;
}

function errorMessage(error: unknown): string {
  const message = error instanceof Error ? error.message : String(error);
  return message.split("\n", 1)[0] || "Unknown highlighting error";
}
