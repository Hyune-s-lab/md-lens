export interface KatexApi {
  renderToString(tex: string, options: { displayMode?: boolean; throwOnError?: boolean; output?: string }): string;
}

export type KatexLoader = () => Promise<KatexApi>;
export type KatexErrorReporter = (message: string) => void;

// Characters that indicate the content between $...$ is likely a math expression
// rather than a currency value or plain text.
const MATH_INDICATOR_RE = /[\\^_{}\[\]|=+\-*/()<>]|\\[a-zA-Z]/;

// Matches display math $$...$$ where content is non-empty and contains math indicators.
function isLikelyDisplayMath(content: string): boolean {
  return content.trim().length > 0;
}

// Matches inline math $...$ where content looks like a math expression.
// Currency like $49 or $5.00 should NOT be treated as math.
function isLikelyInlineMath(content: string): boolean {
  const trimmed = content.trim();
  if (trimmed.length === 0) {
    return false;
  }
  // Pure numbers (e.g. "49", "5.00") are currency, not math
  if (/^\d+([.,]\d+)*$/.test(trimmed)) {
    return false;
  }
  // Contains a math indicator character
  if (MATH_INDICATOR_RE.test(trimmed)) {
      return true;
  }
  // Single letters like "a", "x", "n" are math variables
  return /^[a-zA-Z]$/.test(trimmed);
}

export async function renderMath(
  root: HTMLElement,
  loadKatex: KatexLoader,
  reportError: KatexErrorReporter,
): Promise<void> {
  const hasMath = hasMathExpression(root);
  if (!hasMath) {
    return;
  }

  let katex: KatexApi;
  try {
    katex = await loadKatex();
  } catch (error) {
    reportError(`Unable to load KaTeX: ${errorMessage(error)}`);
    return;
  }

  // Process text nodes that contain math, skipping <code> and <pre> elements.
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
    acceptNode(node) {
      const parent = node.parentElement;
      if (parent === null) {
        return NodeFilter.FILTER_REJECT;
      }
      if (parent.tagName === "CODE" || parent.tagName === "PRE") {
        return NodeFilter.FILTER_REJECT;
      }
      if (parent.closest("code, pre")) {
        return NodeFilter.FILTER_REJECT;
      }
      const text = node.nodeValue ?? "";
      if (!text.includes("$")) {
        return NodeFilter.FILTER_REJECT;
      }
      return NodeFilter.FILTER_ACCEPT;
    },
  });

  const targets: Text[] = [];
  let current = walker.nextNode();
  while (current !== null) {
    targets.push(current as Text);
    current = walker.nextNode();
  }

  for (const textNode of targets) {
    try {
      replaceMathInTextNode(textNode, katex);
    } catch (error) {
      reportError(`KaTeX rendering failed: ${errorMessage(error)}`);
    }
  }
}

function hasMathExpression(root: HTMLElement): boolean {
  return root.textContent?.includes("$") ?? false;
}

function replaceMathInTextNode(textNode: Text, katex: KatexApi): void {
  const text = textNode.nodeValue ?? "";
  if (!text.includes("$")) {
    return;
  }

  const fragments: (Node | { type: "display"; content: string } | { type: "inline"; content: string })[] = [];
  let pos = 0;

  while (pos < text.length) {
    const remaining = text.slice(pos);

    // Check for display math $$...$$
    const displayStart = remaining.indexOf("$$");
    if (displayStart !== -1) {
      const displayEnd = remaining.indexOf("$$", displayStart + 2);
      if (displayEnd !== -1) {
        const content = remaining.slice(displayStart + 2, displayEnd);
        if (isLikelyDisplayMath(content)) {
          if (displayStart > 0) {
            fragments.push(document.createTextNode(remaining.slice(0, displayStart)));
          }
          fragments.push({ type: "display", content: content.trim() });
          pos += displayEnd + 2;
          continue;
        }
      }
    }

    // Check for inline math $...$
    const inlineStart = remaining.indexOf("$");
    if (inlineStart !== -1) {
      const searchFrom = inlineStart + 1;
      const inlineEnd = remaining.indexOf("$", searchFrom);
      if (inlineEnd !== -1 && inlineEnd > inlineStart + 0) {
        const inlineContent = remaining.slice(inlineStart + 1, inlineEnd);
        if (!inlineContent.startsWith("$") && isLikelyInlineMath(inlineContent)) {
          if (inlineStart > 0) {
            fragments.push(document.createTextNode(remaining.slice(0, inlineStart)));
          }
          fragments.push({ type: "inline", content: inlineContent.trim() });
          pos += inlineEnd + 1;
          continue;
        }
      }
    }

    // No more matches — push the rest as text
    fragments.push(document.createTextNode(remaining));
    break;
  }

  if (fragments.length === 0) {
    return;
  }

  const parent = textNode.parentNode;
  if (parent === null) {
    return;
  }

  for (const frag of fragments) {
    if (frag instanceof Node) {
      parent.insertBefore(frag, textNode);
    } else {
      const html = katex.renderToString(frag.content, {
        displayMode: frag.type === "display",
        throwOnError: false,
        output: "html",
      });
      const wrapper = document.createElement("span");
      wrapper.className = frag.type === "display"
        ? "md-lens-math md-lens-math-display"
        : "md-lens-math md-lens-math-inline";
      wrapper.innerHTML = html;
      parent.insertBefore(wrapper, textNode);
    }
  }
  parent.removeChild(textNode);
}

function errorMessage(error: unknown): string {
  const message = error instanceof Error ? error.message : String(error);
  return message.split("\n", 1)[0] || "Unknown KaTeX error";
}
