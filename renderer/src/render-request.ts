export type DocumentType = "markdown" | "mermaid";
export type RenderProfile = "compact" | "spacious";
export type RenderTheme = "light" | "dark";

export interface RenderRequest {
  version: 5;
  source: string;
  baseUrl: string;
  documentType: DocumentType;
  theme: RenderTheme;
  profile: RenderProfile;
  fontFamily: string;
  fontSize: number;
  maxContentWidth?: number | null;
  accentHeadings?: boolean;
  accentBold?: boolean;
  accentInlineCode?: boolean;
}
