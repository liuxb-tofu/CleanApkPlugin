# CleanApkPlugin
*其他语言版本: [English](README.md), [简体中文](README_zh.md).*

一个Apk清理插件。通过编辑生成的Apk，删除里面的资源文件，达到减少apk大小的目的。
## 功能
1. 可移除apk中的任意文件。
2. 可将项目中的png转成webp。
## 特点
1. 支持productFlavor、buildType多维度配置规则。
2. 支持独立任务和内嵌action两种运行方式。
## 使用方式

`build.gradle.kts`

```kotlin
plugins {
    id("io.github.blookliu.clean-apk-plugin")
}

cleanApk {
    // 是否覆盖原来的apk
    overwriteOutput = false
    // 是否使用独立的gradle task，cleanApk${variantName}
    useSingleTask = false
    // 支持配置flavor、buildType、flavor+buildType
    // 支持通配符，规则参考packingOptions
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
        // 支持配置flavor、buildType、flavor+buildType
        // includes优先级高于excludes，都不配置默认全部转换
        // 如果转换后的webp大于原始图片，则不会转换
        create("debug") {
            enable = true
            quality = 75f // [0, 100] 默认75
            drawableIncludes = listOf("close") // 图片名，不带后缀
            drawableExcludes = listOf()
            mipmapIncludes = listOf()
            mipmapExcludes = listOf()
        }
    }
}
```

## 致谢
[AndResguard](https://github.com/shwenzhang/AndResGuard) 使用了apksigner部分的代码

[Android-ArscBlamer](https://github.com/google/android-arscblamer) 使用来解析arsc文件