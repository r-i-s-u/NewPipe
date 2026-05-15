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
    alias(libs.plugins.jetbrains.kotlinx.serialization)
    alias(libs.plugins.about.libraries)
}

// Better than adding a third-party dependency for something as simple as this
// https://stackoverflow.com/a/74771876/8446131
val buildConfigGenerator by tasks.registering(Sync::class) {
    val buildConfigPackage = "net.newpipe.app"
    val rawClass = """
        package $buildConfigPackage

        object BuildConfig {
            const val VERSION_NAME = "$NEWPIPE_VERSION_NAME"
        }
    """.trimIndent()
    from(resources.text.fromString(rawClass)) {
        rename { "BuildConfig.kt" }
        into(buildConfigPackage.replace(".", "/"))
    }
    into(layout.buildDirectory.dir("generated/kotlin/"))
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes"
        )
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }

    android {
        namespace = "net.newpipe.app"
        compileSdk {
            version = release(NEWPIPE_VERSION_SDK_COMPILE_MAJOR) {
                minorApiLevel = NEWPIPE_VERSION_SDK_COMPILE_MINOR
            }
        }
        minSdk {
            version = release(NEWPIPE_VERSION_SDK_MINOR)
        }
        androidResources {
            enable = true
        }

        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-proguard-rules.pro")
            }
        }

        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        commonMain {
            kotlin.srcDir(buildConfigGenerator.map { it.destinationDir })
            dependencies {
                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.material3)
                implementation(libs.jetbrains.compose.ui)
                implementation(libs.jetbrains.compose.resources)
                implementation(libs.jetbrains.compose.preview)

                implementation(libs.jetbrains.lifecycle.viewmodel)

                // Use API as java compiler cannot see NavKey for some reason
                api(libs.jetbrains.navigation3.ui)

                implementation(libs.jetbrains.lifecycle.navigation3)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.koin.compose.viewmodel)
                implementation(libs.koin.annotations)

                implementation(libs.russhwolf.settings)

                implementation(libs.touchlab.kermit)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test.core)
            implementation(libs.jetbrains.compose.test.ui)
        }
        androidMain.dependencies {
            implementation(libs.jetbrains.compose.preview)
            implementation(libs.androidx.activity)
            implementation(libs.androidx.preference)
            implementation(libs.androidx.browser)
        }
        val androidDeviceTest by getting {
            dependencies {
                implementation(libs.androidx.compose.test.ui.manifest)
                implementation(libs.androidx.compose.test.ui.junit)

                // Needed because androidx.compose.test.ui.junit pulls an older dependency
                // which crashes on new Android versions
                implementation(libs.androidx.test.espresso.core)
            }
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.jetbrains.coroutines.swing)
        }
        val jvmTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
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
            packageName = NEWPIPE_APPLICATION_ID_NEW
            packageVersion = NEWPIPE_VERSION_NAME
        }
    }
}

koinCompiler {
    userLogs = true // See what the compiler plugin detects
}

// Run ./gradlew exportLibraryDefinitions to generate/update the libraries and license definitions
aboutLibraries {
    export {
        outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")
        prettyPrint = true
    }
}
