import type { HighlightApi } from "./render-highlight";
import type { MermaidApi } from "./render-mermaid";

interface RuntimeTypes {
  highlight: HighlightApi;
  mermaid: MermaidApi;
}

type RuntimeName = keyof RuntimeTypes;

interface PendingRuntime {
  promise: Promise<unknown>;
  reject(error: Error): void;
  resolve(runtime: unknown): void;
}

declare global {
  interface Window {
    mdLensRuntimes?: Record<string, unknown>;
  }
}

const pendingRuntimes = new Map<RuntimeName, PendingRuntime>();

export function loadRuntime<Name extends RuntimeName>(
  name: Name,
  requestRuntime: (name: RuntimeName) => void,
): Promise<RuntimeTypes[Name]> {
  const loaded = window.mdLensRuntimes?.[name];
  if (loaded !== undefined) {
    return Promise.resolve(loaded as RuntimeTypes[Name]);
  }

  const pending = pendingRuntimes.get(name);
  if (pending !== undefined) {
    return pending.promise as Promise<RuntimeTypes[Name]>;
  }

  let resolveRuntime: (runtime: unknown) => void = () => undefined;
  let rejectRuntime: (error: Error) => void = () => undefined;
  const promise = new Promise<unknown>((resolve, reject) => {
    resolveRuntime = resolve;
    rejectRuntime = reject;
  });
  pendingRuntimes.set(name, {
    promise,
    reject: rejectRuntime,
    resolve: resolveRuntime,
  });

  try {
    requestRuntime(name);
  } catch (error) {
    runtimeFailed(name, error instanceof Error ? error.message : String(error));
  }
  return promise as Promise<RuntimeTypes[Name]>;
}

export function runtimeReady(name: string): void {
  const pending = pendingRuntimes.get(name as RuntimeName);
  if (pending === undefined) {
    return;
  }
  const runtime = window.mdLensRuntimes?.[name];
  if (runtime === undefined) {
    runtimeFailed(name, `Runtime ${name} did not register its API`);
    return;
  }
  pending.resolve(runtime);
  pendingRuntimes.delete(name as RuntimeName);
}

export function runtimeFailed(name: string, message: string): void {
  const pending = pendingRuntimes.get(name as RuntimeName);
  if (pending === undefined) {
    return;
  }
  pending.reject(new Error(message));
  pendingRuntimes.delete(name as RuntimeName);
}
