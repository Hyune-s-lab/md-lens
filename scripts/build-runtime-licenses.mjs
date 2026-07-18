import { createHash } from "node:crypto";
import { readdir, readFile, writeFile, mkdir } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const lock = JSON.parse(await readFile(resolve(root, "package-lock.json"), "utf8"));
const outputPath = resolve(root, "build/generated/licenses/NPM-RUNTIME-LICENSES.txt");
const materialDesignPath = "node_modules/@iconify-json/mdi";
const packages = Object.entries(lock.packages)
  .filter(([path, metadata]) =>
    path.startsWith("node_modules/") && (!metadata.dev || path === materialDesignPath),
  )
  .sort(([left], [right]) => left.localeCompare(right));
const apacheLicense = (await readFile(resolve(root, "node_modules/dompurify/LICENSE"), "utf8")).trim();
const materialDesignLicense = (
  await readFile(resolve(root, "licenses/MaterialDesignIcons-LICENSE.txt"), "utf8")
).trim();
const markedFootnotePath = "node_modules/marked-footnote";
const markedFootnoteLicense = (
  await readFile(resolve(root, "licenses/marked-footnote-LICENSE.txt"), "utf8")
).trim();
const licenseGroups = new Map();
const packageIndex = [];

for (const [path, metadata] of packages) {
  const name = path.slice("node_modules/".length);
  const packageLabel = `${name}@${metadata.version}`;
  let licenseText;
  if (path === materialDesignPath) {
    licenseText = `${materialDesignLicense}\n\n${apacheLicense}`;
  } else if (path === markedFootnotePath) {
    // The published npm package omits its license file; ship the upstream MIT text.
    licenseText = markedFootnoteLicense;
  } else {
    const entries = await readdir(resolve(root, path));
    const licenseFile = entries.find((entry) => /^(license|licence|copying)(\.|$)/i.test(entry));
    if (licenseFile === undefined) {
      throw new Error(`Missing license file for bundled package: ${packageLabel}`);
    }
    licenseText = (await readFile(resolve(root, path, licenseFile), "utf8")).trim();
  }

  const digest = createHash("sha256").update(licenseText).digest("hex");
  const group = licenseGroups.get(digest) ?? { packages: [], text: licenseText };
  group.packages.push(packageLabel);
  licenseGroups.set(digest, group);
  packageIndex.push(`${packageLabel} — ${metadata.license ?? "see included license text"}`);
}

const sections = [
  "MdLens bundled npm runtime licenses",
  "===========================================",
  "",
  "Packages",
  "--------",
  ...packageIndex,
];
for (const group of licenseGroups.values()) {
  sections.push(
    "",
    "License text for",
    "----------------",
    ...group.packages,
    "",
    group.text,
  );
}

await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, `${sections.join("\n")}\n`);
