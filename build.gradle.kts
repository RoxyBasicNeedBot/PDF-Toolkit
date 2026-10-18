plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
}

subprojects {
    tasks.matching { it.name.startsWith("check") && it.name.endsWith("AarMetadata") }.configureEach {
        enabled = false
    }

    tasks.matching { it.name.contains("Lint") || it.name.contains("lint") }.configureEach {
        enabled = false
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xskip-metadata-version-check",
                "-Xskip-prerelease-check"
            )
        }
    }
}

