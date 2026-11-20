/*
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import com.android.build.api.dsl.ApplicationExtension
import java.util.regex.Pattern

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.legacy.kapt)
    alias(libs.plugins.jetbrains.kotlin.parcelize)
    alias(libs.plugins.jetbrains.kotlinx.serialization)
    checkstyle
}

val gitWorkingBranch = providers.exec {
    commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
}.standardOutput.asText.map { it.trim() }
val defaultBranches = listOf("master", "dev")
val workingBranch = gitWorkingBranch.getOrElse("")
val normalizedWorkingBranch = workingBranch
    .replaceFirst("^[^A-Za-z]+".toRegex(), "")
    .replace("[^0-9A-Za-z]+".toRegex(), "")

kotlin {
    jvmToolchain(21)
}

configure<ApplicationExtension> {
    compileSdk {
        version = release(PIPELOAD_VERSION_SDK_COMPILE_MAJOR) {
            minorApiLevel = PIPELOAD_VERSION_SDK_COMPILE_MINOR
        }
    }
    namespace = PIPELOAD_APPLICATION_ID
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    defaultConfig {
        applicationId = PIPELOAD_APPLICATION_ID
        resValue("string", "app_name", "PipeLoad")
        minSdk {
            version = release(PIPELOAD_VERSION_SDK_MIN)
        }
        targetSdk {
            version = release(PIPELOAD_VERSION_SDK_TARGET)
        }

     resourceConfigurations += listOf("en")

        versionCode = System.getProperty("versionCodeOverride")?.toInt() ?: PIPELOAD_VERSION_CODE

        versionName = PIPELOAD_VERSION_NAME
        System.getProperty("versionNameSuffix")?.let { versionNameSuffix = it }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true

            // suffix the app id and the app name with git branch name
            if (normalizedWorkingBranch.isEmpty() || workingBranch in defaultBranches) {
                applicationIdSuffix = ".debug"
                resValue("string", "app_name", "PipeLoad Debug")
            } else {
                applicationIdSuffix = ".debug.$normalizedWorkingBranch"
                resValue("string", "app_name", "PipeLoad $workingBranch")
            }
        }

        release {
            System.getProperty("packageSuffix")?.let { suffix ->
                applicationIdSuffix = suffix
                resValue("string", "app_name", "PipeLoad $suffix")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
}

    lint {
        lintConfig = file("lint.xml")
        // Continue the debug build even when errors are found
        abortOnError = false
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        encoding = "utf-8"
    }

    androidResources {
        generateLocaleConfig = true
        additionalParameters.addAll(listOf("--preferred-density", "xxhdpi"))
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        resValues = true
    }

    packaging {
        resources {
            // remove two files which belong to jsoup
            // no idea how they ended up in the META-INF dir...
            excludes += setOf(
                "META-INF/README.md",
                "META-INF/CHANGES",
                "META-INF/androidx/**", "META-INF/*.version", "META-INF/*.kotlin_module",
                "google/protobuf/**", "kotlin/**", "DebugProbesKt.bin", "kotlin-tooling-metadata.json",
                "META-INF/COPYRIGHT" // "COPYRIGHT" belongs to RxJava...
            )
        }
    }
}


// Custom dependency configuration for ktlint
val ktlint by configurations.creating

checkstyle {
    configDirectory = rootProject.file("checkstyle")
    isIgnoreFailures = false
    isShowViolations = true
    toolVersion = libs.versions.checkstyle.get()
}

tasks.register<Checkstyle>("runCheckstyle") {
    source("src")
    include("**/*.java")
    exclude("**/gen/**")
    exclude("**/R.java")
    exclude("**/BuildConfig.java")
    exclude("main/java/us/shandian/giga/**")

    classpath = configurations.getByName("checkstyle")

    isShowViolations = true

    reports {
        xml.required = true
        html.required = true
    }
}

val outputDir = project.layout.buildDirectory.dir("reports/ktlint/")
val inputFiles = fileTree("src") { include("**/*.kt") }

tasks.register<JavaExec>("runKtlint") {
    inputs.files(inputFiles)
    outputs.dir(outputDir)
    mainClass.set("com.pinterest.ktlint.Main")
    classpath = configurations.getByName("ktlint")
    args = listOf("--editorconfig=../.editorconfig", "src/**/*.kt")
    jvmArgs = listOf("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

tasks.register<JavaExec>("formatKtlint") {
    inputs.files(inputFiles)
    outputs.dir(outputDir)
    mainClass.set("com.pinterest.ktlint.Main")
    classpath = configurations.getByName("ktlint")
    args = listOf("--editorconfig=../.editorconfig", "-F", "src/**/*.kt")
    jvmArgs = listOf("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

tasks.register<CheckDependenciesOrder>("checkDependenciesOrder") {
    tomlFile = layout.projectDirectory.file("../gradle/libs.versions.toml")
}

afterEvaluate {
    tasks.named("preDebugBuild").configure {
        if (!System.getProperties().containsKey("skipFormatKtlint")) {
            dependsOn("formatKtlint")
        }
        dependsOn("runCheckstyle", "runKtlint", "checkDependenciesOrder")
    }
}

dependencies {
    // Desugaring
    coreLibraryDesugaring(libs.android.desugar)

    // NewPipe libraries
    implementation(libs.newpipe.nanojson)
    implementation(libs.newpipe.extractor)
    implementation(libs.newpipe.filepicker)

    // Checkstyle
    checkstyle(libs.puppycrawl.checkstyle)
    ktlint(libs.pinterest.ktlint)

    // AndroidX
    implementation(libs.androidx.appcompat)
    implementation("androidx.cardview:cardview:1.0.0")
    implementation(libs.androidx.core)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.android.material)

    // Coroutines interop
    implementation(libs.kotlinx.coroutines.rx3)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Third-party libraries
    implementation(libs.livefront.bridge)
    implementation(libs.evernote.statesaver.core)
    kapt(libs.evernote.statesaver.compiler)

    // HTML parser
    implementation(libs.jsoup)

    // HTTP client
    implementation(libs.squareup.okhttp)
    implementation(libs.squareup.okhttp.brotli)

    // Reactive extensions for Java VM
    implementation(libs.reactivex.rxjava)
    implementation(libs.reactivex.rxandroid)
    // RxJava binding APIs for Android UI widgets
    implementation(libs.jakewharton.rxbinding)

}

configurations.all {
    exclude(group = "org.mozilla")
}

configurations.configureEach {
    exclude(group = "androidx.webkit", module = "webkit")
}
