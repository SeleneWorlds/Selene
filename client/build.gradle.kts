import java.time.Instant

plugins {
    kotlin("jvm")
    application
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("world.selene.client.ClientBootstrapKt")
}

dependencies {
    implementation(project(":common"))

    val gdxVersion by properties
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:${gdxVersion}")
    implementation("com.badlogicgames.gdx:gdx-tools:$gdxVersion") {
        exclude("com.badlogicgames.gdx", "gdx-backend-lwjgl")
    }
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    runtimeOnly("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")

    val ktxVersion by properties
    implementation("io.github.libktx:ktx-app:$ktxVersion")
    implementation("io.github.libktx:ktx-assets-async:$ktxVersion")
    implementation("io.github.libktx:ktx-freetype-async:$ktxVersion")

    val visuiVersion by properties
    implementation("com.kotcrab.vis:vis-ui:$visuiVersion")
    val lmlVersion by properties
    implementation("com.crashinvaders.lml:gdx-lml:$lmlVersion")
    implementation("com.crashinvaders.lml:gdx-lml-vis:$lmlVersion")
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
            "Main-Class" to "world.selene.client.ClientBootstrapKt"
        )
    }
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "world.selene.client.ClientBootstrapKt"
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            artifactId = "selene-client"
            version = project.version.toString()

            // This is used by the launcher to download dependencies.
            artifact(tasks.named("generateLibrariesJson").get().outputs.files.singleFile) {
                classifier = "libraries"
                extension = "json"
                builtBy(tasks.named("generateLibrariesJson"))
            }

            pom {
                name.set("Selene Client")
                description.set("Selene game client application")
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