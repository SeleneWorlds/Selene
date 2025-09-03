plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

dependencies {
    // Kotlin Compiler Frontend
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.21")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.21")
    
    // JSON Serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

application {
    mainClass.set("world.selene.analyzer.LuaModuleAnalyzerKt")
}

tasks.register<JavaExec>("analyzeLuaModules") {
    group = "analysis"
    description = "Analyzes all LuaModule implementations and generates JSON report"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("world.selene.analyzer.LuaModuleAnalyzerKt")
}
