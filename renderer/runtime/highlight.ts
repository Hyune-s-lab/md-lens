import hljs from "highlight.js/lib/common";
import dart from "highlight.js/lib/languages/dart";
import dockerfile from "highlight.js/lib/languages/dockerfile";
import groovy from "highlight.js/lib/languages/groovy";
import nginx from "highlight.js/lib/languages/nginx";
import nix from "highlight.js/lib/languages/nix";
import pgsql from "highlight.js/lib/languages/pgsql";
import powershell from "highlight.js/lib/languages/powershell";
import protobuf from "highlight.js/lib/languages/protobuf";
import scala from "highlight.js/lib/languages/scala";

declare global {
  interface Window {
    mdLensRuntimes?: Record<string, unknown>;
  }
}

hljs.registerLanguage("dart", dart);
hljs.registerLanguage("dockerfile", dockerfile);
hljs.registerLanguage("groovy", groovy);
hljs.registerLanguage("nginx", nginx);
hljs.registerLanguage("nix", nix);
hljs.registerLanguage("pgsql", pgsql);
hljs.registerLanguage("powershell", powershell);
hljs.registerLanguage("protobuf", protobuf);
hljs.registerLanguage("scala", scala);

hljs.registerAliases("psql", { languageName: "pgsql" });
hljs.registerAliases(["mysql", "mariadb", "plsql", "sqlite"], { languageName: "sql" });

window.mdLensRuntimes = window.mdLensRuntimes ?? {};
window.mdLensRuntimes.highlight = hljs;
