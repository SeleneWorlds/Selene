plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    val koinVersion by properties
    api(project.dependencies.platform("io.insert-koin:koin-bom:$koinVersion"))
    api("io.insert-koin:koin-core")
    api("io.insert-koin:koin-logger-slf4j")

    api("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")

    val nettyVersion by properties
    api("io.netty:netty-all:$nettyVersion")

    val luaJavaVersion by properties
    api("party.iroiro.luajava:luajava:$luaJavaVersion")
    api("party.iroiro.luajava:lua54:$luaJavaVersion")
    runtimeOnly("party.iroiro.luajava:lua54-platform:$luaJavaVersion:natives-desktop")

    val hopliteVersion by properties
    api("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")

    val log4jVersion by properties
    api("org.slf4j:slf4j-api:2.0.17")
    api("org.apache.logging.log4j:log4j-api:${log4jVersion}")
    runtimeOnly("org.apache.logging.log4j:log4j-core:${log4jVersion}")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:${log4jVersion}")

    api("com.auth0:java-jwt:4.5.0")

    val arrowVersion by properties
    api("io.arrow-kt:arrow-core:$arrowVersion")

    api("com.mojang:brigadier:1.0.17")

    api("com.google.guava:guava:33.4.8-jre")

    val ktorVersion by properties
    api("io.ktor:ktor-client-core:$ktorVersion")
    api("io.ktor:ktor-client-cio:$ktorVersion")
    api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-serialization-jackson:$ktorVersion")
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