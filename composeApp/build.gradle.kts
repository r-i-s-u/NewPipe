/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.jetbrains.compose.multiplatform)
    alias(libs.plugins.koin)
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }

    android {
        namespace = "net.newpipe.app"
        compileSdk {
            version = release(36) {
                minorApiLevel = 1
            }
        }
        minSdk = 23
        androidResources {
            enable = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.resources)
            implementation(libs.jetbrains.compose.preview)

            implementation(libs.jetbrains.lifecycle.viewmodel)

            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.lifecycle.navigation3)

            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.annotations)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.jetbrains.compose.preview)
            implementation(libs.androidx.activity)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.jetbrains.coroutines.swing)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.jetbrains.compose.tooling)
}

compose.desktop {
    application {
        mainClass = "net.newpipe.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "net.newpipe.app"
            packageVersion = "1.0.0"
        }
    }
}

koinCompiler {
    userLogs = true // See what the compiler plugin detects
}
