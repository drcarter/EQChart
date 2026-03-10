import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.math.BigDecimal

val coverageModules = setOf(
    ":EQChart",
    ":EQChart-common",
    ":EQChart-compose",
)

subprojects {
    if (path in coverageModules) {
        apply(plugin = "jacoco")

        extensions.configure<JacocoPluginExtension> {
            toolVersion = "0.8.12"
        }

        val coverageExcludes = listOf(
            "**/R.class",
            "**/R\\$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
        )

        tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
            group = "verification"
            description = "Generates JaCoCo coverage reports for debug unit tests."
            dependsOn("testDebugUnitTest")

            classDirectories.setFrom(
                files(
                    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
                        exclude(coverageExcludes)
                    },
                    fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
                        exclude(coverageExcludes)
                    },
                ),
            )

            sourceDirectories.setFrom(
                files(
                    "src/main/java",
                    "src/main/kotlin",
                ),
            )

            executionData.setFrom(
                fileTree(layout.buildDirectory) {
                    include(
                        "jacoco/testDebugUnitTest.exec",
                        "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                    )
                },
            )

            reports {
                xml.required.set(true)
                html.required.set(true)
                csv.required.set(false)
            }
        }

        if (path == ":EQChart-common") {
            tasks.register<JacocoCoverageVerification>("jacocoDebugUnitTestCoverageVerification") {
                group = "verification"
                description = "Verifies JaCoCo instruction coverage for EQChart-common debug unit tests."
                dependsOn("testDebugUnitTest")

                classDirectories.setFrom(
                    files(
                        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
                            exclude(coverageExcludes)
                        },
                        fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
                            exclude(coverageExcludes)
                        },
                    ),
                )

                sourceDirectories.setFrom(
                    files(
                        "src/main/java",
                        "src/main/kotlin",
                    ),
                )

                executionData.setFrom(
                    fileTree(layout.buildDirectory) {
                        include(
                            "jacoco/testDebugUnitTest.exec",
                            "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                        )
                    },
                )

                violationRules {
                    rule {
                        element = "BUNDLE"

                        limit {
                            counter = "INSTRUCTION"
                            value = "COVEREDRATIO"
                            minimum = BigDecimal("1.0")
                        }
                    }
                }
            }
        }

        tasks.register("coverageDebug") {
            group = "verification"
            description = "Runs debug unit tests and generates JaCoCo coverage reports."
            dependsOn(
                buildList {
                    add("testDebugUnitTest")
                    add("jacocoDebugUnitTestReport")
                    if (project.path == ":EQChart-common") {
                        add("jacocoDebugUnitTestCoverageVerification")
                    }
                },
            )
        }

        tasks.withType<Test>().configureEach {
            useJUnit()
        }
    }
}

tasks.register("libraryUnitTest") {
    group = "verification"
    description = "Runs debug unit tests for all library modules."
    dependsOn(coverageModules.map { "$it:testDebugUnitTest" })
}

tasks.register("libraryJacocoReport") {
    group = "verification"
    description = "Generates JaCoCo debug unit test reports for all library modules."
    dependsOn(coverageModules.map { "$it:jacocoDebugUnitTestReport" })
}

tasks.register("libraryCoverage") {
    group = "verification"
    description = "Runs all library unit tests and generates JaCoCo reports."
    dependsOn("libraryUnitTest", "libraryJacocoReport")
}
