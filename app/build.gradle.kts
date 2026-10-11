import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
}

// Release signing is optional: the project builds without a keystore so that
// contributors and CI can compile it. keystore.properties is gitignored.
val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "dev.danielclements.puck"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.danielclements.puck"
        minSdk = 26
        targetSdk = 35
        // Restarted at 1: this is a distinct app/listing from upstream's
        // dev.atvremote.app, not an update to it.
        versionCode = 4
        versionName = "1.3.0"

        // English-only: keeps any translated strings a dependency ships
        // (e.g. a library's own "OK"/"Cancel") out of the APK too, not just
        // our own resources.
        resourceConfigurations += "en"
    }

    // Wire logging dumps frame hex, including pairing traffic. Off unless
    // explicitly requested: ./gradlew assembleDebug -PwireLogging=true
    val wireLogging = (project.findProperty("wireLogging") as String?) ?: "false"

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // material-icons-extended ships thousands of unused vectors; R8
            // strips them and takes the APK from ~19 MB to a few MB.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("boolean", "WIRE_LOGGING", "false")
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            isMinifyEnabled = false
            buildConfigField("boolean", "WIRE_LOGGING", wireLogging)
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

    packaging {
        resources.excludes += setOf("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Otherwise every release and debug build produces an identically named
// app-release.apk / app-debug.apk, indistinguishable once it's off the
// build machine — a GitHub release asset with no version in its filename.
// outputFileName is only settable on the impl class; the public
// VariantOutput interface exposes it read-only.
androidComponents {
    onVariants { variant ->
        val suffix = if (variant.buildType == "debug") "-debug" else ""
        variant.outputs.forEach { output ->
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)
                ?.outputFileName?.set("puck-${android.defaultConfig.versionName}$suffix.apk")
        }
    }
}

dependencies {
    implementation(project(":protocol"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    // Foldable posture (FoldingFeature / WindowLayoutInfo) for the remote
    // screen's Tabletop/Book layouts — DESIGN_SPEC.md §8.
    implementation("androidx.window:window:1.3.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Material 3 Expressive (MaterialExpressiveTheme, new shapes/motion) needs
    // material3 1.5.0, which pulls in compileSdk 37 and AGP 9.1+ transitively
    // — too far ahead of stable for an app whose job is working every day.
    // As of late 2026 even the *stable* Compose track has moved onto that
    // same compileSdk 37 requirement, so this stays pinned to the last BOM
    // that targets compileSdk 35 — which already has dynamic (Material You)
    // color; that API has been stable since Material 3 1.0.
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
