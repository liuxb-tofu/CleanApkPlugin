plugins {
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "io.github.blookliu"
version = "1.0.0"
repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle:8.2.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
    implementation("commons-io:commons-io:2.6")
    implementation("com.google.guava:guava:30.1.1-jre")
}

gradlePlugin {
    website = "https://github.com/liuxb-tofu/CleanApkPlugin"
    vcsUrl = "https://github.com/liuxb-tofu/CleanApkPlugin.git"
    plugins {
        create("CleanApkPlugin") {
            id = "io.github.blookliu.clean-apk-plugin"
            displayName = "Plugin for clean Apk"
            description = "A plugin for cleaning Apk"
            tags = listOf("android", "compile", "build")
            implementationClass = "io.github.blookliu.clean_apk_plugin.CleanApkPlugin"
        }
    }
}

publishing{
    repositories {
        maven {
            name = "localPluginRepository"
            url = uri("../local-plugin-repository")
        }
    }
}

tasks.withType(Javadoc::class.java).all { enabled = false }

