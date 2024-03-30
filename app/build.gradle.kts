plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.blookliu.clean-apk-plugin")
}

android {
    namespace = "com.blookliu.cleanapk"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.blookliu.cleanapk"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    flavorDimensions += "channel"

    productFlavors {
        create("flavor1") {
            dimension = "channel"
        }
        create("flavor2") {
            dimension = "channel"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
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