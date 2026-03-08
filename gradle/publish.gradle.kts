import org.gradle.api.publish.PublishingExtension
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Properties

val publishDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
val defaultPublishDate: String = LocalDate
    .now(ZoneId.of("Asia/Seoul"))
    .format(publishDateFormatter)

val publishDate: String = providers
    .gradleProperty("publishDate")
    .orElse(defaultPublishDate)
    .get()

val publishIncrementRaw: String = providers
    .gradleProperty("publishIncrement")
    .orElse("0")
    .get()

fun resolveLibraryVersion(datePart: String, incrementPart: String): String {
    val increment = incrementPart.toIntOrNull() ?: 0
    return if (increment <= 0) {
        datePart
    } else {
        "$datePart.$increment"
    }
}

val resolvedLibraryVersion = resolveLibraryVersion(
    datePart = publishDate,
    incrementPart = publishIncrementRaw,
)

val githubProperties = Properties()
val githubPropertiesFile = rootProject.file("github.properties")
if (githubPropertiesFile.exists()) {
    githubPropertiesFile.inputStream().use(githubProperties::load)
}

val githubFileUser: String = githubProperties
    .getProperty("gpr.user")
    ?.trim()
    .orEmpty()
val githubFileKey: String = githubProperties
    .getProperty("gpr.key")
    ?.trim()
    .orEmpty()

val resolvedGithubUser: String = providers
    .gradleProperty("gpr.user")
    .orElse(providers.environmentVariable("GPR_USER"))
    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
    .orElse(githubFileUser)
    .get()

val resolvedGithubKey: String = providers
    .gradleProperty("gpr.key")
    .orElse(providers.environmentVariable("GPR_KEY"))
    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
    .orElse(githubFileKey)
    .get()

allprojects {
    group = "com.magimon.eq"
    version = resolvedLibraryVersion
}

gradle.taskGraph.whenReady {
    val publishingToGitHub = allTasks.any { it.name.contains("GitHubPackages", ignoreCase = true) }
    if (publishingToGitHub && (resolvedGithubUser.isBlank() || resolvedGithubKey.isBlank())) {
        error(
            "Missing GitHub Packages credentials. " +
                "Set gpr.user/gpr.key in github.properties (project root), " +
                "~/.gradle/gradle.properties, or env vars (GPR_USER/GPR_KEY).",
        )
    }
}

subprojects {
    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension>("publishing") {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/drcarter/EQChart")
                    credentials {
                        username = resolvedGithubUser
                        password = resolvedGithubKey
                    }
                }
            }
        }
    }
}
