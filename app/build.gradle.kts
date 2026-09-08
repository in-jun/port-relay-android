import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Release signing, per https://developer.android.com/studio/publish/app-signing:
// keystore.properties (storeFile, storePassword, keyAlias, keyPassword) is loaded
// from the project root, or from ~/.android/release/ on a developer machine. Neither
// file is committed; without one the release build is produced unsigned.
val keystorePropertiesFile = listOf(
    rootProject.file("keystore.properties"),
    File(System.getProperty("user.home"), ".android/release/keystore.properties"),
).firstOrNull { it.isFile }
val keystoreProperties = Properties().apply {
    keystorePropertiesFile?.inputStream()?.use(::load)
}

// Release builds take their version from the git tag: release.yml passes
// -PreleaseVersion=1.2.3 for tag v1.2.3. versionCode is derived so it stays monotonic.
val releaseVersion = providers.gradleProperty("releaseVersion").orNull
fun versionCodeOf(version: String): Int {
    val (major, minor, patch) = version.split(".").map { it.toInt() }
    require(minor < 100 && patch < 100) { "version components must stay below 100: $version" }
    return major * 10_000 + minor * 100 + patch
}

android {
    namespace = "dev.injun.portrelay"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.injun.portrelay"
        minSdk = 26
        targetSdk = 37
        versionCode = releaseVersion?.let(::versionCodeOf) ?: 1
        versionName = releaseVersion ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        keystorePropertiesFile?.let { propsFile ->
            create("release") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = File(keystoreProperties["storeFile"] as String)
                    .let { if (it.isAbsolute) it else File(propsFile.parentFile, it.path) }
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
        aidl = false
        buildConfig = false
        shaders = false
    }
    lint {
        // Warnings are defects: fail the build instead of accumulating a report.
        warningsAsErrors = true
        abortOnError = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Arch Components
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Instrumented tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Local tests: jUnit, coroutines, Android runner
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests: jUnit rules and runners
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
