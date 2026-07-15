import assert from "node:assert/strict";

const expectedTheme = process.argv[2] ?? "dark";
const port = process.env.JCEF_DEBUG_PORT ?? "9222";
const targets = await fetch(`http://127.0.0.1:${port}/json/list`).then((response) => response.json());
const target = targets.find((candidate) => candidate.title === "MdLens");

assert(
  target,
  `MdLens page not found. Open the MdLens editor tab first. Found: ${targets.map((item) => item.title).join(", ")}`,
);

const result = await evaluate(target.webSocketDebuggerUrl, `(() => {
  const viewer = document.getElementById("viewer");
  const themeStyle = document.head.querySelector("style[data-md-lens-theme]");
  return {
    theme: document.documentElement.dataset.theme,
    rootBackground: getComputedStyle(document.documentElement).backgroundColor,
    viewerBackground: viewer === null ? null : getComputedStyle(viewer).backgroundColor,
    viewerColor: viewer === null ? null : getComputedStyle(viewer).color,
    themeCssBytes: themeStyle?.textContent?.length ?? 0,
  };
})()`);

const expectedBackground = expectedTheme === "dark" ? "rgb(13, 17, 23)" : "rgb(255, 255, 255)";
assert.equal(result.theme, expectedTheme);
assert.equal(result.rootBackground, expectedBackground);
assert.equal(result.viewerBackground, expectedBackground);
assert(result.themeCssBytes > 1_000, "Theme CSS was not injected");

console.log(JSON.stringify(result, null, 2));

function evaluate(webSocketUrl, expression) {
  return new Promise((resolve, reject) => {
    const socket = new WebSocket(webSocketUrl);
    const timeout = setTimeout(() => {
      socket.close();
      reject(new Error("Timed out while inspecting the MdLens page"));
    }, 5_000);

    socket.addEventListener("open", () => {
      socket.send(JSON.stringify({
        id: 1,
        method: "Runtime.evaluate",
        params: { expression, returnByValue: true },
      }));
    });
    socket.addEventListener("message", (event) => {
      const message = JSON.parse(event.data);
      if (message.id !== 1) {
        return;
      }
      clearTimeout(timeout);
      socket.close();
      if (message.result.exceptionDetails !== undefined) {
        reject(new Error(message.result.exceptionDetails.text));
        return;
      }
      resolve(message.result.result.value);
    });
    socket.addEventListener("error", () => {
      clearTimeout(timeout);
      reject(new Error("Unable to connect to the MdLens DevTools target"));
    });
  });
}
