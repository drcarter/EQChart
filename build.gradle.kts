import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask
import org.gradle.api.tasks.Exec

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.dokka)
}

apply(from = "gradle/publish.gradle.kts")
apply(from = "gradle/jacoco.gradle.kts")

val documentedModules = setOf(
    ":EQChart-common",
    ":EQChart",
    ":EQChart-3d-core",
    ":EQChart-compose",
)

val moduleDisplayNames = mapOf(
    ":EQChart-common" to "EQChart Common",
    ":EQChart" to "EQChart Views",
    ":EQChart-3d-core" to "EQChart 3D Core",
    ":EQChart-compose" to "EQChart Compose",
)

val moduleDocPaths = mapOf(
    ":EQChart-common" to "eqchart-common",
    ":EQChart" to "eqchart-views",
    ":EQChart-3d-core" to "eqchart-3d-core",
    ":EQChart-compose" to "eqchart-compose",
)

dependencies {
    documentedModules.forEach { modulePath ->
        add("dokka", project(modulePath))
    }
}

extensions.configure<DokkaExtension>("dokka") {
    moduleName.set("EQChart Reference")
    dokkaPublications.named("html") {
        moduleName.set("EQChart Reference")
        outputDirectory.set(layout.projectDirectory.dir("docs/reference"))
    }
}

subprojects {
    if (path !in documentedModules) {
        return@subprojects
    }

    apply(plugin = "org.jetbrains.dokka")

    extensions.configure<DokkaExtension>("dokka") {
        modulePath.set(moduleDocPaths.getValue(project.path))
        dokkaPublications.named("html") {
            moduleName.set(moduleDisplayNames.getValue(project.path))
        }
        dokkaSourceSets.configureEach {
            jdkVersion.set(17)
            skipEmptyPackages.set(true)
            suppressGeneratedFiles.set(true)

            val sourceDirectories = listOf(
                "src/main/java",
                "src/main/kotlin",
            )

            sourceDirectories.forEach { relativePath ->
                val sourceDirectory = project.file(relativePath)
                if (sourceDirectory.exists()) {
                    sourceLink {
                        localDirectory.set(sourceDirectory)
                        remoteUrl.set(
                            uri(
                                "https://github.com/drcarter/EQChart/tree/main/" +
                                    "${project.name}/$relativePath",
                            ),
                        )
                        remoteLineSuffix.set("#L")
                    }
                }
            }

            perPackageOption {
                matchingRegex.set(".*\\.internal(\\..*)?")
                suppress.set(true)
            }
        }
    }

    tasks.register("dokkaHtml") {
        group = "documentation"
        description = "Compatibility alias for Dokka HTML publication generation."
        dependsOn("dokkaGeneratePublicationHtml")
    }
}

tasks.named<DokkaGeneratePublicationTask>("dokkaGeneratePublicationHtml") {
    group = "documentation"
    description = "Builds aggregated HTML API reference docs for all published library modules."
    doFirst {
        delete(outputDirectory)
    }
}

tasks.register("dokkaHtmlMultiModule") {
    group = "documentation"
    description = "Compatibility alias for the aggregated Dokka HTML reference docs."
    dependsOn("dokkaGeneratePublicationHtml")
}

tasks.register("referenceDocs") {
    group = "documentation"
    description = "Builds Dokka HTML reference docs into docs/reference."
    dependsOn("dokkaGeneratePublicationHtml", "mergeStandaloneModuleDocsIntoReferenceDocs")
    doLast {
        val indexFile = layout.projectDirectory.file("docs/reference/index.html").asFile
        println("Reference docs file: ${indexFile.absolutePath}")
        println("Preview in browser: ./gradlew referenceDocsPreview")
    }
}

tasks.register("mergeStandaloneModuleDocsIntoReferenceDocs") {
    group = "documentation"
    description = "Adds standalone library module Dokka publications into the root aggregated reference docs."
    dependsOn(
        ":EQChart:dokkaGeneratePublicationHtml",
        ":EQChart-3d-core:dokkaGeneratePublicationHtml",
        "dokkaGeneratePublicationHtml",
    )

    doLast {
        val rootReferenceDir = layout.projectDirectory.dir("docs/reference").asFile
        val rootNavigationFile = rootReferenceDir.resolve("navigation.html")
        val rootIndexFile = rootReferenceDir.resolve("index.html")

        data class StandaloneDocModule(
            val projectDir: String,
            val targetDir: String,
            val navigationMarker: String,
            val indexMarker: String,
            val indexRow: String,
        )

        val standaloneModules = listOf(
            StandaloneDocModule(
                projectDir = "EQChart",
                targetDir = "eqchart-views",
                navigationMarker = "EQChart Views-nav-submenu",
                indexMarker = "EQChart Views</a>",
                indexRow = """
<a data-name="eqchart-views%2FMain%2F0" anchor-label="EQChart Views" id="eqchart-views%2FMain%2F0" data-filterable-set=""></a>
      <div class="table-row">
        <div class="main-subrow ">
          <div class="w-100"><span class="inline-flex">
              <div><a href="eqchart-views/index.html">EQChart Views</a></div>
<span class="anchor-wrapper"><span class="anchor-icon" pointing-to="eqchart-views%2FMain%2F0"></span>
                <div class="copy-popup-wrapper "><span class="copy-popup-icon"></span><span>Link copied to clipboard</span></div>
              </span></span></div>
          <div><span class="brief-comment">
              <p class="paragraph">Android View-based chart components for XML layouts and classic view hierarchies.</p>
            </span></div>
        </div>
      </div>
""".trimIndent(),
            ),
            StandaloneDocModule(
                projectDir = "EQChart-3d-core",
                targetDir = "eqchart-3d-core",
                navigationMarker = "EQChart 3D Core-nav-submenu",
                indexMarker = "EQChart 3D Core</a>",
                indexRow = """
<a data-name="eqchart-3d-core%2FMain%2F0" anchor-label="EQChart 3D Core" id="eqchart-3d-core%2FMain%2F0" data-filterable-set=""></a>
      <div class="table-row">
        <div class="main-subrow ">
          <div class="w-100"><span class="inline-flex">
              <div><a href="eqchart-3d-core/index.html">EQChart 3D Core</a></div>
<span class="anchor-wrapper"><span class="anchor-icon" pointing-to="eqchart-3d-core%2FMain%2F0"></span>
                <div class="copy-popup-wrapper "><span class="copy-popup-icon"></span><span>Link copied to clipboard</span></div>
              </span></span></div>
          <div><span class="brief-comment">
              <p class="paragraph">OpenGL-backed true 3D chart surfaces and rendering primitives shared across UI entry points.</p>
            </span></div>
        </div>
      </div>
""".trimIndent(),
            ),
        )

        standaloneModules.forEach { module ->
            val publicationDir = layout.projectDirectory.dir("${module.projectDir}/build/dokka/html").asFile
            val targetDir = rootReferenceDir.resolve(module.targetDir)

            copy {
                from(publicationDir)
                into(targetDir)
            }

            val navigationBlock = publicationDir
                .resolve("navigation.html")
                .readText()
                .replace("href=\"", "href=\"${module.targetDir}/")

            val rootNavigation = rootNavigationFile.readText()
            if (!rootNavigation.contains(module.navigationMarker)) {
                rootNavigationFile.writeText("$navigationBlock\n$rootNavigation")
            }

            val rootIndex = rootIndexFile.readText()
            if (!rootIndex.contains(module.indexMarker)) {
                rootIndexFile.writeText(
                    rootIndex.replace(
                        """<div class="table">""",
                        """<div class="table">
${module.indexRow}
""",
                    ),
                )
            }
        }
    }
}

tasks.register<Exec>("referenceDocsPreview") {
    group = "documentation"
    description = "Builds Dokka reference docs, starts a local preview server if needed, and opens the browser."
    dependsOn("referenceDocs")
    workingDir = layout.projectDirectory.asFile
    commandLine("bash", layout.projectDirectory.file("scripts/reference-docs-preview.sh").asFile.absolutePath)
}

tasks.register<Exec>("referenceDocsStop") {
    group = "documentation"
    description = "Stops the local preview server used for Dokka reference docs."
    workingDir = layout.projectDirectory.asFile
    commandLine("bash", layout.projectDirectory.file("scripts/reference-docs-preview.sh").asFile.absolutePath, "stop")
}
