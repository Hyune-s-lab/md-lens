import { viteSingleFile } from "vite-plugin-singlefile";
import { defineConfig } from "vitest/config";

export default defineConfig({
  root: "renderer",
  plugins: [viteSingleFile()],
  build: {
    outDir: "../build/generated/renderer",
    emptyOutDir: true,
    target: "es2022",
  },
  test: {
    environment: "jsdom",
    include: ["**/*.test.ts"],
  },
});
