import java.util.Properties

plugins {
    // No org.jetbrains.kotlin.android here: AGP 9 compiles Kotlin itself.
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// google-services only applies when a real config file is present, so a fresh
// clone still syncs before Firebase is set up.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Maps key and environment configuration are read from local.properties and .env
// so secrets and sensitive credentials never land in version control.
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { stream -> load(stream) }
}

val envProperties = Properties().apply {
    val rootEnv = rootProject.file("../.env")
    if (rootEnv.exists()) {
        rootEnv.inputStream().use { stream -> load(stream) }
    }
    val appEnv = rootProject.file(".env")
    if (appEnv.exists()) {
        appEnv.inputStream().use { stream -> load(stream) }
    }
}

val storeOwnerEmails: String = envProperties.getProperty("STORE_OWNER_EMAILS")
    ?: localProperties.getProperty("STORE_OWNER_EMAILS")
    ?: System.getenv("STORE_OWNER_EMAILS")
    ?: ""

android {
    namespace = "com.princesanitary.grahak"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.princesanitary.grahak"
        minSdk = 24
        targetSdk = 37

        versionCode = 12
        versionName = "2.2.0"

        manifestPlaceholders["MAPS_API_KEY"] =
            localProperties.getProperty("MAPS_API_KEY", "")

        buildConfigField(
            "String",
            "STORE_OWNER_EMAILS",
            "\"${storeOwnerEmails.replace("\"", "\\\"")}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }

        /*
         * Release-like build for judging real UI performance on a device.
         *
         * A `debug` build runs Compose with extra instrumentation and no
         * optimisation, so scrolling and screen transitions are noticeably
         * slower than what users will actually get. This variant flips
         * debuggable off (the setting responsible for most of that overhead)
         * while staying unminified, so it installs alongside nothing special
         * and needs no release keystore.
         *
         *   ./gradlew installPerf
         */
        create("perf") {
            initWith(getByName("debug"))
            isDebuggable = false
            isMinifyEnabled = false
            matchingFallbacks += listOf("debug")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core + Compose
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.10.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")

    // Google sign-in
    implementation("com.google.android.gms:play-services-auth:21.5.1")

    // Maps + location: used by the "pin your site" step when booking a plumber.
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:maps-compose:6.7.0")

    // Images
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Coroutines <-> Play Services Task interop (Firebase .await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    // Annotations only (@Inject / @Singleton markers on repositories). 2 KB jar,
    // no annotation processing - this used to arrive transitively via Hilt.
    implementation("javax.inject:javax.inject:1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
