plugins {
    `java-platform`
    id("maven-publish")
}

dependencies {
    constraints {
        api(project(":EQChart-common"))
        api(project(":EQChart"))
        api(project(":EQChart-compose"))
    }
}

publishing {
    publications {
        create<MavenPublication>("bom") {
            from(components["javaPlatform"])
            artifactId = "eqchart-bom"

            pom {
                name.set("EQChart BOM")
                description.set("Bill of materials for aligning EQChart module versions.")
            }
        }
    }
}
