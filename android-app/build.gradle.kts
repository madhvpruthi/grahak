// Top-level build file.
//
// AGP 9.x ships "built-in Kotlin": the org.jetbrains.kotlin.android plugin must
// NOT be applied any more (AGP fails the build if it is). AGP pulls in its own
// Kotlin Gradle Plugin, so to control the Kotlin version we override it on the
// buildscript classpath, per the AGP 9 upgrade notes. The Compose compiler
// ships inside Kotlin, so its plugin version must match KOTLIN_VERSION exactly.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}

plugins {
    // 9.3.2 is the newest AGP that runs on the already-cached Gradle 9.5.1.
    // AGP 9.4.0 would require a fresh Gradle 9.6.0 download.
    id("com.android.application") version "9.3.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}
