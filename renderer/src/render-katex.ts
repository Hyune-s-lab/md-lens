export interface KatexApi {
  renderToString(tex: string, options: { displayMode?: boolean; throwOnError?: boolean; output?: string }): string;
}

export type KatexLoader = () => Promise<KatexApi>;
export type KatexErrorReporter = (message: string) => void;

// Matches $$...$$ (display) and $...$ (inline), skipping code spans and code blocks.
const DISPLAY_MATH_RE = /\$\$([\s\S]+?)\$\$/g;
const INLINE_MATH_RE = /\$([^\n$]+?)\$/g;

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

  // Tokenize: find $$...$$ and $...$ in order, replacing with rendered HTML.
  const fragments: Node[] = [];
  let pos = 0;

  while (pos < text.length) {
    const remaining = text.slice(pos);

    // Check for display math $$...$$
    const displayStart = remaining.indexOf("$$");
    if (displayStart !== -1) {
      const displayEnd = remaining.indexOf("$$", displayStart + 2);
      if (displayEnd !== -1) {
        // Text before the match
        if (displayStart > 0) {
          fragments.push(document.createTextNode(remaining.slice(0, displayStart)));
        }
        const content = remaining.slice(displayStart + 2, displayEnd).trim();
        const html = katex.renderToString(content, {
          displayMode: true,
          throwOnError: false,
          output: "html",
        });
        const wrapper = document.createElement("span");
        wrapper.className = "md-lens-math md-lens-math-display";
        wrapper.innerHTML = html;
        fragments.push(wrapper);
        pos += displayEnd + 2;
        continue;
      }
    }

    // Check for inline math $...$
    const inlineStart = remaining.indexOf("$");
    if (inlineStart !== -1) {
      // Find closing $ (not $$)
      let searchFrom = inlineStart + 1;
      const inlineEnd = remaining.indexOf("$", searchFrom);
      if (inlineEnd !== -1 && inlineEnd > inlineStart + 0) {
        const inlineContent = remaining.slice(inlineStart + 1, inlineEnd);
        // Make sure it's not $$ (already handled above)
        if (inlineContent.length > 0 && !inlineContent.startsWith("$")) {
          // Text before the match
          if (inlineStart > 0) {
            fragments.push(document.createTextNode(remaining.slice(0, inlineStart)));
          }
          const html = katex.renderToString(inlineContent.trim(), {
            displayMode: false,
            throwOnError: false,
            output: "html",
          });
          const wrapper = document.createElement("span");
          wrapper.className = "md-lens-math md-lens-math-inline";
          wrapper.innerHTML = html;
          fragments.push(wrapper);
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
    parent.insertBefore(frag, textNode);
  }
  parent.removeChild(textNode);
}

function errorMessage(error: unknown): string {
  const message = error instanceof Error ? error.message : String(error);
  return message.split("\n", 1)[0] || "Unknown KaTeX error";
}
