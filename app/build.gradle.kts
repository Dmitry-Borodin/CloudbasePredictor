plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val requestedTasks = gradle.startParameter.taskNames

android {
    namespace = "com.cloudbasepredictor"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.cloudbasepredictor"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.cloudbasepredictor.HiltTestRunner"

        when {
            requestedTasks.any { it.endsWith("connectedE2eTest") } ->
                testInstrumentationRunnerArguments["class"] =
                    "com.cloudbasepredictor.e2e.ForecastModelE2eTest"

            requestedTasks.any { it.endsWith("connectedScreenshotTest") } ->
                testInstrumentationRunnerArguments["class"] =
                    "com.cloudbasepredictor.screenshot.ScreenshotCaptureTest"

            else ->
                testInstrumentationRunnerArguments["notClass"] = listOf(
                    "com.cloudbasepredictor.e2e.ForecastModelE2eTest",
                    "com.cloudbasepredictor.screenshot.ScreenshotCaptureTest",
                ).joinToString(",")
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
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

ksp {
    arg("room.generateKotlin", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.retrofit.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.material)
    implementation(libs.maplibre.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.net.sqlcipher)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.security.crypto)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.register("connectedInstrumentationTest") {
    description = "Run instrumentation tests (excluding E2E and screenshot)"
    group = "verification"
    dependsOn("connectedDebugAndroidTest")
}

tasks.register("connectedE2eTest") {
    description = "Run E2E tests (requires emulator/device and network)"
    group = "verification"
    dependsOn("connectedDebugAndroidTest")
}

tasks.register("connectedScreenshotTest") {
    description = "Run screenshot capture tests (requires emulator/device)"
    group = "verification"
    dependsOn("connectedDebugAndroidTest")
}
