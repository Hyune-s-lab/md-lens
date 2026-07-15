import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const mermaidPath = resolve(root, "node_modules/mermaid/dist/mermaid.min.js");
const iconsPath = resolve(root, "node_modules/@iconify-json/mdi/icons.json");
const outputPath = resolve(root, "build/generated/mermaid/runtime-mermaid.js");
const selectedIconNames = [
  "account",
  "api",
  "cloud",
  "cog",
  "database",
  "message-processing",
  "server",
  "web",
];

const [mermaidSource, iconSetSource] = await Promise.all([
  readFile(mermaidPath, "utf8"),
  readFile(iconsPath, "utf8"),
]);
const iconSet = JSON.parse(iconSetSource);
const selectedIcons = Object.fromEntries(
  selectedIconNames.map((name) => {
    const icon = iconSet.icons[name];
    if (icon === undefined) {
      throw new Error(`Missing Material Design icon: ${name}`);
    }
    return [name, icon];
  }),
);
const bundledIconPack = {
  prefix: "mdi",
  width: iconSet.width,
  height: iconSet.height,
  icons: selectedIcons,
};
const globalExport = 'globalThis["mermaid"] = globalThis.__esbuild_esm_mermaid_nm["mermaid"].default;';
if (!mermaidSource.includes(globalExport)) {
  throw new Error("Mermaid runtime export format changed; update the JCEF global export transform");
}
const runtime = [
  "/*! Mermaid 11.16.0 (MIT) with a curated Material Design Icons subset (Apache-2.0). */",
  mermaidSource
    .replace(/^\/\/# sourceMappingURL=.*$/gm, "")
    .replace(globalExport, 'globalThis["mermaid"] = __esbuild_esm_mermaid_nm["mermaid"].default;'),
  "window.mdLensRuntimes = window.mdLensRuntimes || {};",
  "window.mdLensRuntimes.mermaid = mermaid;",
  `window.mdLensRuntimes.mermaid.registerIconPacks([{name:"mdi",icons:${JSON.stringify(bundledIconPack)}}]);`,
  "",
].join("\n");

await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, runtime);
