plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("world.selene.server.ServerBootstrapKt")
}

dependencies {
    implementation(project(":common"))

    val ktorVersion by properties
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    implementation("org.jline:jline:3.30.4")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "world.selene.server.ServerBootstrapKt"
    }
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}