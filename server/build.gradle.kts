import java.time.Instant

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    `maven-publish`
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("world.selene.server.ServerBootstrapKt")
}

dependencies {
    implementation(project(":common"))

    implementation(libs.bundles.ktor.server)

    implementation(libs.jline)
}

tasks.register("generateLibrariesJson") {
    description = "Generates libraries.json with runtime classpath dependencies"
    group = "build"

    val outputFile = layout.buildDirectory.file("libs/libraries.json")
    outputs.file(outputFile)

    doLast {
        val runtimeClasspath = configurations.getByName("runtimeClasspath")
        val libraries = mutableListOf<Map<String, String>>()

        runtimeClasspath.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            val moduleVersion = artifact.moduleVersion.id

            // Skip project dependencies since they are embedded in the shadow JAR
            if (moduleVersion.group != project.group.toString()) {
                libraries.add(
                    mapOf(
                        "group" to moduleVersion.group,
                        "name" to moduleVersion.name,
                        "version" to moduleVersion.version,
                        "classifier" to (artifact.classifier ?: ""),
                        "extension" to (artifact.extension ?: ""),
                        "file" to artifact.file.name
                    )
                )
            }
        }

        val metadata = mapOf(
            "version" to project.version.toString(),
            "generatedAt" to Instant.now().toString(),
            "libraries" to libraries
        )

        outputFile.get().asFile.parentFile.mkdirs()
        outputFile.get().asFile.writeText(
            groovy.json.JsonBuilder(metadata).toPrettyString()
        )

        println("Generated libraries.json with ${libraries.size} runtime dependencies")
    }
}

tasks.named("build") {
    dependsOn("generateLibrariesJson")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    // We only include project dependencies in the shadow JAR.
    // This is to simplify libraries.json generation, since we don't know SNAPSHOT versions of project dependencies
    // in advance. This way, we don't have to worry about them.
    // Other libraries we'll still have downloaded separately to reduce build size.
    configurations = listOf(project.configurations.runtimeClasspath.get())
    dependencies {
        include(project(":common"))
        exclude("*:*") // Exclude all other dependencies
    }

    archiveClassifier.set("dist")

    mergeServiceFiles()
    manifest {
        attributes(
            "Main-Class" to "world.selene.server.ServerBootstrapKt"
        )
    }
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "world.selene.server.ServerBootstrapKt"
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            artifactId = "selene-server"
            version = project.version.toString()

            // This is used by the launcher to download dependencies.
            artifact(tasks.named("generateLibrariesJson").get().outputs.files.singleFile) {
                classifier = "libraries"
                extension = "json"
                builtBy(tasks.named("generateLibrariesJson"))
            }

            pom {
                name.set("Selene Server")
                description.set("Selene game server application")
                url.set("https://github.com/SeleneWorlds/Selene")
            }
        }
    }

    repositories {
        maven {
            var releasesRepoUrl = "https://maven.twelveiterations.com/repository/maven-releases/"
            var snapshotsRepoUrl = "https://maven.twelveiterations.com/repository/maven-snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            name = "nexus"
            credentials(PasswordCredentials::class)
        }
    }
}