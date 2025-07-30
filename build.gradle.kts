plugins {
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.20" apply false
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
