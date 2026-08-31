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
      decorateFunctionCalls(code, language);
      code.classList.add("hljs");
    } catch (error) {
      reportError(`Syntax highlighting failed: ${errorMessage(error)}`);
    }
  }
}

const functionCallPattern = /([$_\p{ID_Start}][$_\u200C\u200D\p{ID_Continue}]*)(?=\s*\()/gu;
const nonCallLanguages = new Set([
  "css",
  "diff",
  "dockerfile",
  "html",
  "ini",
  "json",
  "less",
  "makefile",
  "mariadb",
  "markdown",
  "md",
  "mysql",
  "nginx",
  "nix",
  "pgsql",
  "plaintext",
  "plsql",
  "postgres",
  "postgresql",
  "properties",
  "protobuf",
  "psql",
  "scss",
  "sql",
  "sqlite",
  "text",
  "toml",
  "txt",
  "wasm",
  "xml",
  "yaml",
  "yml",
]);
const nonCallIdentifiers = new Set([
  "catch",
  "delete",
  "do",
  "else",
  "for",
  "foreach",
  "if",
  "match",
  "new",
  "return",
  "sizeof",
  "switch",
  "synchronized",
  "throw",
  "try",
  "typeof",
  "when",
  "while",
  "with",
]);

function decorateFunctionCalls(code: HTMLElement, language: string): void {
  if (nonCallLanguages.has(language)) {
    return;
  }
  const walker = code.ownerDocument.createTreeWalker(code, 4);
  const textNodes: Text[] = [];
  let current = walker.nextNode();
  while (current !== null) {
    if (current.nodeType === 3) {
      const textNode = current as Text;
      if (textNode.parentElement?.closest('[class*="hljs-"]') === null) {
        textNodes.push(textNode);
      }
    }
    current = walker.nextNode();
  }

  for (const textNode of textNodes) {
    const source = textNode.data;
    const matches = Array.from(source.matchAll(functionCallPattern));
    if (matches.length === 0) {
      continue;
    }

    const fragment = code.ownerDocument.createDocumentFragment();
    let offset = 0;
    for (const match of matches) {
      const index = match.index;
      const name = match[1];
      if (index === undefined || name === undefined) {
        continue;
      }
      if (nonCallIdentifiers.has(name.toLowerCase())) {
        continue;
      }
      fragment.append(source.slice(offset, index));
      const span = code.ownerDocument.createElement("span");
      span.classList.add("hljs-title", "function_", "invoke__");
      span.textContent = name;
      fragment.append(span);
      offset = index + name.length;
    }
    fragment.append(source.slice(offset));
    textNode.replaceWith(fragment);
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
