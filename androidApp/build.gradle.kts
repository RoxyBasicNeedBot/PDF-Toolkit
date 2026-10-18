plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.roxy.pdftoolkit"
    compileSdk = 35

    val envVersionName = System.getenv("APP_VERSION_NAME")
    val envVersionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull()

    defaultConfig {
        applicationId = "com.roxy.pdftoolkit"
        minSdk = 26
        targetSdk = 35
        versionCode = envVersionCode ?: 1
        versionName = envVersionName ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    flavorDimensions += listOf("abi")
    productFlavors {
        create("universal") {
            dimension = "abi"
            isDefault = true
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            }
            buildConfigField("String", "ARCHITECTURE", "\"universal\"")
        }
        create("arm64") {
            dimension = "abi"
            ndk { abiFilters += "arm64-v8a" }
            buildConfigField("String", "ARCHITECTURE", "\"arm64\"")
        }
        create("armeabi") {
            dimension = "abi"
            ndk { abiFilters += "armeabi-v7a" }
            buildConfigField("String", "ARCHITECTURE", "\"armeabi\"")
        }
        create("x86_64") {
            dimension = "abi"
            ndk { abiFilters += "x86_64" }
            buildConfigField("String", "ARCHITECTURE", "\"x86_64\"")
        }
    }

    val releaseKeystoreFile = sequenceOf(
        file("roxy.keystore"),
        file("roxypdftoolkit.keystore"),
        file("release.keystore"),
        file("release.jks"),
        rootProject.file("roxy.keystore"),
        rootProject.file("roxypdftoolkit.keystore"),
        rootProject.file("release.keystore"),
        rootProject.file("release.jks")
    ).firstOrNull { it.exists() }

    val hasReleaseKeystore = releaseKeystoreFile != null

    val resolvedStorePassword = System.getenv("KEYSTORE_PASSWORD")
        ?: System.getenv("KEY_STORE_PASSWORD")
        ?: System.getenv("STORE_PASSWORD")
        ?: "roxy_release_pass"

    val resolvedKeyAlias = System.getenv("KEY_ALIAS")
        ?: System.getenv("ALIAS")
        ?: "roxypdftoolkit"

    val resolvedKeyPassword = System.getenv("KEY_PASSWORD")
        ?: resolvedStorePassword

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = releaseKeystoreFile
                storePassword = resolvedStorePassword
                keyAlias = resolvedKeyAlias
                keyPassword = resolvedKeyPassword
            } else {
                initWith(signingConfigs.getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            keepDebugSymbols += listOf(
                "**/libandroidx.graphics.path.so",
                "**/libtensorflowlite_jni.so"
            )
        }
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/NOTICE.md",
                "META-INF/CONTRIBUTORS.md",
                "META-INF/LICENSE.md",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/INDEX.LIST",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.koin.android)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xskip-metadata-version-check",
            "-Xskip-prerelease-check"
        )
    }
}
