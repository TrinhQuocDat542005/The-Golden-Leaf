buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.2")
    }
}

plugins {
    kotlin("plugin.serialization") version "2.0.21" apply false

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}
