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
    ":EQChart-compose",
)

val moduleDisplayNames = mapOf(
    ":EQChart-common" to "EQChart Common",
    ":EQChart" to "EQChart Views",
    ":EQChart-compose" to "EQChart Compose",
)

val moduleDocPaths = mapOf(
    ":EQChart-common" to "eqchart-common",
    ":EQChart" to "eqchart-views",
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
            jdkVersion.set(11)
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
    dependsOn("dokkaGeneratePublicationHtml", "mergeViewModuleIntoReferenceDocs")
    doLast {
        val indexFile = layout.projectDirectory.file("docs/reference/index.html").asFile
        println("Reference docs file: ${indexFile.absolutePath}")
        println("Preview in browser: ./gradlew referenceDocsPreview")
    }
}

tasks.register("mergeViewModuleIntoReferenceDocs") {
    group = "documentation"
    description = "Adds the EQChart Views Dokka publication into the root aggregated reference docs."
    dependsOn(":EQChart:dokkaGeneratePublicationHtml", "dokkaGeneratePublicationHtml")

    doLast {
        val rootReferenceDir = layout.projectDirectory.dir("docs/reference").asFile
        val viewPublicationDir = layout.projectDirectory.dir("EQChart/build/dokka/html").asFile
        val targetViewDir = rootReferenceDir.resolve("eqchart-views")

        copy {
            from(viewPublicationDir)
            into(targetViewDir)
        }

        val rootNavigationFile = rootReferenceDir.resolve("navigation.html")
        val rootIndexFile = rootReferenceDir.resolve("index.html")
        val viewNavigationFile = viewPublicationDir.resolve("navigation.html")

        val viewNavigationBlock = viewNavigationFile
            .readText()
            .replace("href=\"", "href=\"eqchart-views/")

        val rootNavigation = rootNavigationFile.readText()
        if (!rootNavigation.contains("EQChart Views-nav-submenu")) {
            rootNavigationFile.writeText("$viewNavigationBlock\n$rootNavigation")
        }

        val viewModuleRow = """
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
""".trimIndent()

        val rootIndex = rootIndexFile.readText()
        if (!rootIndex.contains("EQChart Views</a>")) {
            rootIndexFile.writeText(
                rootIndex.replace(
                    """<div class="table">""",
                    """<div class="table">
$viewModuleRow
""",
                ),
            )
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
