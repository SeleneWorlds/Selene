plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

dependencies {
    api(project.dependencies.platform(libs.koin.bom))
    api(libs.bundles.koin)

    api(libs.bundles.jackson)

    api(libs.netty.all)

    api(libs.bundles.lua)
    runtimeOnly(libs.lua54.platform) {
        artifact {
            classifier = "natives-desktop"
        }
    }

    api(libs.hoplite.core)

    api(libs.slf4j.api)
    api(libs.bundles.log4j)

    api(libs.java.jwt)

    api(libs.arrow.core)

    api(libs.brigadier)

    api(libs.guava)

    api(libs.bundles.ktor.client)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            artifactId = "selene-common"
            version = project.version.toString()
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