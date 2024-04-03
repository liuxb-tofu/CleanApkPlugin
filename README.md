# CleanApkPlugin
*Read this in other languages: [English](README.md), [简体中文](README_zh.md).*

CleanApkPlugin is an Apk shrink plugin for Gradle. It helps you reduce the size of your Apk by editing the generated Apk and removing unnecessary resource files, such as images, audio and video files, and other files that do not affect the functionality of your app.

## Features
1. Remove any file in the Apk.
2. Convert PNG images in your project to WebP format.

## Highlights
1. Support multi-dimensional configuration rules based on productFlavor, buildType, etc.
2. Support two running modes: independent task and embedded action.

## How to use

`build.gradle.kts`

```kotlin
plugins {
    id("io.github.blookliu.clean-apk-plugin")
}

cleanApk {
    // Whether to overwrite the original apk
    overwriteOutput = false
    // Whether to use an independent gradle task, cleanApk${variantName}
    useSingleTask = false
    // Support configuring rules based on flavor, buildType, flavor+buildType
    // Support wildcards, rules refer to packingOptions
    excludeFiles {
        create("debug") {
            excludes = listOf("assets/guetzli", "/DebugProbesKt.bin")
        }

        create("release") {
            excludes = listOf("assets/pngquant", "/DebugProbesKt.bin")
        }

        create("flavor1") {
            excludes = listOf()
        }

        create("flavor2Debug") {
            excludes = listOf()
        }
    }

    webp {
        // Support configuring rules based on flavor, buildType, flavor+buildType
        // includes have higher priority than excludes, if neither is configured, all images will be converted by default
        // If the converted webp is larger than the original image, it will not be converted
        create("debug") {
            enable = true
            quality = 75f // [0, 100] default 75
            drawableIncludes = listOf("close") // image name, without extension
            drawableExcludes = listOf()
            mipmapIncludes = listOf()
            mipmapExcludes = listOf()
        }
    }
}
```

## Acknowledgments
[AndResguard](https://github.com/shwenzhang/AndResGuard): Used the code of apksigner

[Android-ArscBlamer](https://github.com/google/android-arscblamer): Used to parse arsc files