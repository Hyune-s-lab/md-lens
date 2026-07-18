import DOMPurify from "dompurify";
import { marked } from "marked";
import markedFootnote from "marked-footnote";

import type { RenderRequest } from "./render-request";

export interface RenderResult {
  html: string;
}

marked.use(markedFootnote());

export function renderMarkdown(request: RenderRequest): RenderResult {
  const unsafeHtml = marked.parse(request.source, {
    async: false,
    gfm: true,
  });

  const content = DOMPurify.sanitize(unsafeHtml, {
    FORBID_ATTR: ["style"],
    FORBID_TAGS: ["button", "form", "iframe", "object", "select", "style", "textarea"],
    RETURN_DOM_FRAGMENT: true,
  });

  for (const input of content.querySelectorAll("input")) {
    if (input.type !== "checkbox") {
      input.remove();
      continue;
    }
    input.disabled = true;
    input.removeAttribute("name");
    input.removeAttribute("value");
  }

  transformAlerts(content);

  const headingCounts = new Map<string, number>();
  for (const heading of content.querySelectorAll("h1, h2, h3, h4, h5, h6")) {
    if (heading.closest("section[data-footnotes]")) {
      continue;
    }
    const baseSlug = githubSlug(heading.textContent ?? "");
    const duplicateIndex = headingCounts.get(baseSlug) ?? 0;
    headingCounts.set(baseSlug, duplicateIndex + 1);
    heading.id = duplicateIndex === 0 ? baseSlug : `${baseSlug}-${duplicateIndex}`;
  }

  const template = document.createElement("template");
  template.content.append(content);

  return {
    html: template.innerHTML,
  };
}

interface AlertKind {
  title: string;
  // Static octicon markup owned by the renderer; never derived from document content.
  icon: string;
}

const ALERT_KINDS: Record<string, AlertKind> = {
  NOTE: {
    title: "Note",
    icon: octicon("info", "M0 8a8 8 0 1 1 16 0A8 8 0 0 1 0 8Zm8-6.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13ZM6.5 7.75A.75.75 0 0 1 7.25 7h1a.75.75 0 0 1 .75.75v2.75h.25a.75.75 0 0 1 0 1.5h-2a.75.75 0 0 1 0-1.5h.25v-2h-.25a.75.75 0 0 1-.75-.75ZM8 6a1 1 0 1 1 0-2 1 1 0 0 1 0 2Z"),
  },
  TIP: {
    title: "Tip",
    icon: octicon("light-bulb", "M8 1.5c-2.363 0-4 1.69-4 3.75 0 .984.424 1.625.984 2.304l.214.253c.223.264.47.556.673.848.284.411.537.896.621 1.49a.75.75 0 0 1-1.484.211c-.04-.282-.163-.547-.37-.847a8.456 8.456 0 0 0-.542-.68c-.084-.1-.173-.205-.268-.32C3.201 7.75 2.5 6.766 2.5 5.25 2.5 2.31 4.863 0 8 0s5.5 2.31 5.5 5.25c0 1.516-.701 2.5-1.328 3.259-.095.115-.184.22-.268.319-.207.245-.383.453-.541.681-.208.3-.33.565-.37.847a.751.751 0 0 1-1.485-.212c.084-.593.337-1.078.621-1.489.203-.292.45-.584.673-.848.075-.088.147-.173.213-.253.561-.679.985-1.32.985-2.304 0-2.06-1.637-3.75-4-3.75ZM5.75 12h4.5a.75.75 0 0 1 0 1.5h-4.5a.75.75 0 0 1 0-1.5ZM6 15.25a.75.75 0 0 1 .75-.75h2.5a.75.75 0 0 1 0 1.5h-2.5a.75.75 0 0 1-.75-.75Z"),
  },
  IMPORTANT: {
    title: "Important",
    icon: octicon("report", "M0 1.75C0 .784.784 0 1.75 0h12.5C15.216 0 16 .784 16 1.75v9.5A1.75 1.75 0 0 1 14.25 13H8.06l-2.573 2.573A1.458 1.458 0 0 1 3 14.543V13H1.75A1.75 1.75 0 0 1 0 11.25Zm1.75-.25a.25.25 0 0 0-.25.25v9.5c0 .138.112.25.25.25h2a.75.75 0 0 1 .75.75v2.19l2.72-2.72a.749.749 0 0 1 .53-.22h6.5a.25.25 0 0 0 .25-.25v-9.5a.25.25 0 0 0-.25-.25Zm7 2.25v2.5a.75.75 0 0 1-1.5 0v-2.5a.75.75 0 0 1 1.5 0ZM9 9a1 1 0 1 1-2 0 1 1 0 0 1 2 0Z"),
  },
  WARNING: {
    title: "Warning",
    icon: octicon("alert", "M6.457 1.047c.659-1.234 2.427-1.234 3.086 0l6.082 11.378A1.75 1.75 0 0 1 14.082 15H1.918a1.75 1.75 0 0 1-1.543-2.575Zm1.763.707a.25.25 0 0 0-.44 0L1.698 13.132a.25.25 0 0 0 .22.368h12.164a.25.25 0 0 0 .22-.368Zm.53 3.996v2.5a.75.75 0 0 1-1.5 0v-2.5a.75.75 0 0 1 1.5 0ZM9 11a1 1 0 1 1-2 0 1 1 0 0 1 2 0Z"),
  },
  CAUTION: {
    title: "Caution",
    icon: octicon("stop", "M4.47.22A.749.749 0 0 1 5 0h6c.199 0 .389.079.53.22l4.25 4.25c.141.14.22.331.22.53v6a.749.749 0 0 1-.22.53l-4.25 4.25a.749.749 0 0 1-.53.22H5a.749.749 0 0 1-.53-.22L.22 11.53A.749.749 0 0 1 0 11V5c0-.199.079-.389.22-.53Zm.84 1.28L1.5 5.31v5.38l3.81 3.81h5.38l3.81-3.81V5.31L10.69 1.5ZM8 4a.75.75 0 0 1 .75.75v3.5a.75.75 0 0 1-1.5 0v-3.5A.75.75 0 0 1 8 4Zm0 8a1 1 0 1 1 0-2 1 1 0 0 1 0 2Z"),
  },
};

const ALERT_MARKER = /^\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\](?:\n|$)/;

// document.TEXT_NODE without relying on the global Node, which the
// measurement harness does not expose.
const TEXT_NODE = 3;

function transformAlerts(content: DocumentFragment): void {
  for (const quote of Array.from(content.querySelectorAll("blockquote"))) {
    // GitHub only recognizes top-level alert markers, not quotes nested in
    // other quotes or in already-converted alerts.
    if (quote.parentElement?.closest("blockquote, .markdown-alert")) {
      continue;
    }
    const paragraph = quote.firstElementChild;
    if (!paragraph || paragraph.tagName !== "P") {
      continue;
    }
    const first = paragraph.firstChild;
    if (!first || first.nodeType !== TEXT_NODE || !first.nodeValue) {
      continue;
    }
    const match = ALERT_MARKER.exec(first.nodeValue);
    const kindKey = match?.[1];
    const kind = kindKey === undefined ? undefined : ALERT_KINDS[kindKey];
    if (!match || kindKey === undefined || kind === undefined) {
      continue;
    }
    const rest = first.nodeValue.slice(match[0].length);
    if (rest.length > 0) {
      first.nodeValue = rest;
    } else {
      first.remove();
      if (!paragraph.hasChildNodes()) {
        paragraph.remove();
      }
    }

    const alert = document.createElement("div");
    alert.className = `markdown-alert markdown-alert-${kindKey.toLowerCase()}`;
    const title = document.createElement("p");
    title.className = "markdown-alert-title";
    title.innerHTML = kind.icon;
    title.append(kind.title);
    alert.append(title);
    while (quote.firstChild) {
      alert.append(quote.firstChild);
    }
    quote.replaceWith(alert);
  }
}

function octicon(name: string, path: string): string {
  return `<svg class="octicon octicon-${name}" viewBox="0 0 16 16" width="16" height="16" fill="currentColor" aria-hidden="true"><path d="${path}"></path></svg>`;
}

function githubSlug(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^\p{L}\p{M}\p{N}\s_-]/gu, "")
    .replace(/\s/g, "-");
}
