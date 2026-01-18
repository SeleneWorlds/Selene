plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    repositories {
        mavenCentral()

        maven {
            url = uri("https://libraries.minecraft.net")
            content {
                includeGroup("com.mojang")
            }
        }
    }
}
