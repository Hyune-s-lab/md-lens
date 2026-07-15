import { beforeEach, describe, expect, it, vi } from "vitest";

describe("loadRuntime", () => {
  beforeEach(() => {
    vi.resetModules();
    delete window.mdLensRuntimes;
  });

  it("requests one runtime for concurrent diagram renders", async () => {
    const { loadRuntime, runtimeReady } = await import("./load-runtime");
    const requestRuntime = vi.fn();
    const first = loadRuntime("mermaid", requestRuntime);
    const second = loadRuntime("mermaid", requestRuntime);
    const runtime = { initialize: vi.fn(), render: vi.fn() };

    window.mdLensRuntimes = { mermaid: runtime };
    runtimeReady("mermaid");

    await expect(first).resolves.toBe(runtime);
    await expect(second).resolves.toBe(runtime);
    expect(requestRuntime).toHaveBeenCalledOnce();
    expect(requestRuntime).toHaveBeenCalledWith("mermaid");
  });

  it("allows a retry after loading fails", async () => {
    const { loadRuntime, runtimeFailed } = await import("./load-runtime");
    const requestRuntime = vi.fn();
    const first = loadRuntime("mermaid", requestRuntime);

    runtimeFailed("mermaid", "Unavailable");

    await expect(first).rejects.toThrow("Unavailable");
    loadRuntime("mermaid", requestRuntime).catch(() => undefined);
    expect(requestRuntime).toHaveBeenCalledTimes(2);
  });
});
