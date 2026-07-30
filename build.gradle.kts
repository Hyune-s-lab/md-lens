import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

dependencies {
    testImplementation(kotlin("test"))

    intellijPlatform {
        intellijIdea("2026.1.4")
        testFramework(TestFrameworkType.Platform)
    }
}

kotlin {
    jvmToolchain(21)
}

val npmInstall by tasks.registering(Exec::class) {
    inputs.files("package.json", "package-lock.json")
    outputs.dir("node_modules")
    commandLine("npm", "ci")
}

val typecheckRenderer by tasks.registering(Exec::class) {
    dependsOn(npmInstall)
    inputs.dir("renderer")
    inputs.file("tsconfig.json")
    commandLine("npm", "run", "typecheck:renderer")
}

val testRenderer by tasks.registering(Exec::class) {
    dependsOn(npmInstall)
    inputs.dir("renderer")
    inputs.file("vite.config.ts")
    commandLine("npm", "run", "test:renderer")
}

val buildRenderer by tasks.registering(Exec::class) {
    dependsOn(npmInstall)
    inputs.dir("renderer")
    inputs.files("tsconfig.json", "vite.config.ts")
    outputs.file(layout.buildDirectory.file("generated/renderer/index.html"))
    commandLine("npm", "run", "build:renderer")
}

val buildMermaidRuntime by tasks.registering(Exec::class) {
    dependsOn(npmInstall)
    inputs.files("package.json", "package-lock.json", "scripts/build-mermaid-runtime.mjs")
    outputs.file(layout.buildDirectory.file("generated/mermaid/runtime-mermaid.js"))
    commandLine("npm", "run", "build:mermaid-runtime")
}

val buildHighlightRuntime by tasks.registering(Exec::class) {
    dependsOn(npmInstall)
    inputs.files(
        "package.json",
        "package-lock.json",
        "scripts/build-highlight-runtime.mjs",
        "renderer/runtime/highlight.ts",
    )
    outputs.file(layout.buildDirectory.file("generated/highlight/runtime-highlight.js"))
    commandLine("npm", "run", "build:highlight-runtime")
}

val buildKatexRuntime by tasks.registering(Exec::class) {
    dependsOn(npmInstall)
    inputs.files(
        "package.json",
        "package-lock.json",
        "scripts/build-katex-runtime.mjs",
        "renderer/runtime/katex.ts",
    )
    outputs.file(layout.buildDirectory.file("generated/katex/runtime-katex.js"))
    commandLine("npm", "run", "build:katex-runtime")
}

val buildRuntimeLicenses by tasks.registering(Exec::class) {
    dependsOn(npmInstall)
    inputs.files("package-lock.json", "scripts/build-runtime-licenses.mjs")
    inputs.dir("licenses")
    outputs.file(layout.buildDirectory.file("generated/licenses/NPM-RUNTIME-LICENSES.txt"))
    commandLine("npm", "run", "build:runtime-licenses")
}

val testMermaidRuntime by tasks.registering(Exec::class) {
    dependsOn(buildMermaidRuntime, npmInstall)
    inputs.files("scripts/test-mermaid-runtime.mjs")
    inputs.file(layout.buildDirectory.file("generated/mermaid/runtime-mermaid.js"))
    commandLine("npm", "run", "test:mermaid-runtime")
}

val testHighlightRuntime by tasks.registering(Exec::class) {
    dependsOn(buildHighlightRuntime, npmInstall)
    inputs.files("scripts/test-highlight-runtime.mjs")
    inputs.file(layout.buildDirectory.file("generated/highlight/runtime-highlight.js"))
    commandLine("npm", "run", "test:highlight-runtime")
}

val testKatexRuntime by tasks.registering(Exec::class) {
    dependsOn(buildKatexRuntime, npmInstall)
    inputs.files("scripts/test-katex-runtime.mjs")
    inputs.file(layout.buildDirectory.file("generated/katex/runtime-katex.js"))
    commandLine("npm", "run", "test:katex-runtime")
}

val generatePluginVersion by tasks.registering {
    val pluginVersion = project.version.toString()
    val outputFile = layout.buildDirectory.file("generated/pluginVersion/mdlens/plugin-version.txt")
    outputs.file(outputFile)
    doLast {
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(pluginVersion)
        }
    }
}

val prepareRendererResources by tasks.registering(Copy::class) {
    dependsOn(buildHighlightRuntime, buildKatexRuntime, buildMermaidRuntime, buildRenderer, buildRuntimeLicenses, generatePluginVersion, npmInstall)
    into(layout.buildDirectory.dir("generated/rendererResources"))
    from(layout.buildDirectory.file("generated/renderer/index.html")) {
        into("mdlens")
        rename { "viewer.html" }
    }
    from(layout.buildDirectory.file("generated/mermaid/runtime-mermaid.js")) {
        into("mdlens")
    }
    from(layout.buildDirectory.file("generated/highlight/runtime-highlight.js")) {
        into("mdlens")
    }
    from(layout.buildDirectory.file("generated/katex/runtime-katex.js")) {
        into("mdlens")
    }
    from(layout.buildDirectory.file("generated/pluginVersion/mdlens/plugin-version.txt")) {
        into("mdlens")
    }
    from("LICENSE") {
        into("META-INF")
        rename { "MDLENS-LICENSE.txt" }
    }
    from("docs/assets/plugin-icon.svg") {
        into("META-INF")
        rename { "pluginIcon.svg" }
    }
    from("docs/assets/plugin-icon-dark.svg") {
        into("META-INF")
        rename { "pluginIcon_dark.svg" }
    }
    from(layout.buildDirectory.file("generated/licenses/NPM-RUNTIME-LICENSES.txt")) {
        into("META-INF/licenses")
    }
}

sourceSets.main {
    resources.srcDir(layout.buildDirectory.dir("generated/rendererResources"))
}

tasks.processResources {
    dependsOn(prepareRendererResources)
}

tasks.check {
    dependsOn(testHighlightRuntime, testKatexRuntime, testMermaidRuntime, testRenderer, typecheckRenderer)
}

intellijPlatform {
    pluginConfiguration {
        id = "dev.hyunelab.mdlens"
        name = "MdLens"
        version = project.version.toString()
        changeNotes = """
            <h3>Bug Fixes</h3>
            <ul>
              <li>Block <code>foreignObject</code> and <code>script</code> tags in sanitized SVG to prevent XSS via inline SVG.</li>
              <li>Constrain inline SVG diagrams to the container width so they no longer stretch edge-to-edge.</li>
            </ul>
            <br/>
            <p>See the <a href="https://github.com/Hyune-s-lab/md-lens/releases/tag/v0.6.1">GitHub release notes</a>.</p>
        """.trimIndent()

        ideaVersion {
            sinceBuild = "251"
        }

        vendor {
            name = "Hyune-s-lab"
            url = "https://github.com/Hyune-s-lab"
        }
    }

    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdeaUltimate, "2025.1")
            create(IntelliJPlatformType.IntellijIdeaUltimate, "2025.2")
            create(IntelliJPlatformType.IntellijIdeaUltimate, "2025.3")
            create(IntelliJPlatformType.IntellijIdeaUltimate, "2026.1.4")
            latest {
                types = listOf(IntelliJPlatformType.IntellijIdeaUltimate)
                channels = listOf(ProductRelease.Channel.RELEASE)
            }
        }
    }
}

tasks.register("verifyRelease") {
    group = "verification"
    description = "Builds and verifies the Marketplace distribution."
    dependsOn(
        tasks.check,
        tasks.buildPlugin,
        tasks.verifyPluginProjectConfiguration,
        tasks.verifyPluginStructure,
        tasks.verifyPlugin,
    )
}
