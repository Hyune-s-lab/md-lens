export type DocumentType = "markdown" | "mermaid";
export type RenderProfile = "compact" | "spacious";
export type RenderTheme = "light" | "dark";

export interface RenderRequest {
  version: 4;
  source: string;
  baseUrl: string;
  documentType: DocumentType;
  theme: RenderTheme;
  profile: RenderProfile;
  bodyFontFamily: string;
  codeFontFamily: string;
  fontScale: number;
  maxContentWidth?: number | null;
  accentHeadings?: boolean;
  accentBold?: boolean;
  accentInlineCode?: boolean;
}
