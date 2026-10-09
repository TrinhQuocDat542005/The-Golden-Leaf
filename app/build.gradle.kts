import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") // version đã declare ở root
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
    id("com.google.gms.google-services") // ✅ thêm dòng này

    kotlin("plugin.compose") // bắt buộc với Kotlin 2.0 + Compose
}

val debugApiBaseUrl = providers.gradleProperty("API_BASE_URL")
    .orElse(providers.environmentVariable("API_BASE_URL"))
    .orElse("http://10.0.2.2:8080/")

val releaseApiBaseUrl = providers.gradleProperty("PRODUCTION_API_BASE_URL")
    .orElse(providers.environmentVariable("PRODUCTION_API_BASE_URL"))
    .orElse("https://api.example.invalid/")

val weatherApiKey = providers.gradleProperty("WEATHER_API_KEY")
    .orElse(providers.environmentVariable("WEATHER_API_KEY"))
    .orElse("")

val demoApiBaseUrl = providers.gradleProperty("DEMO_API_BASE_URL").orElse("http://10.0.2.2:8080/")
require(demoApiBaseUrl.get() in setOf("http://10.0.2.2:8080/", "http://10.0.2.2:18082/", "http://127.0.0.1:8080/", "http://127.0.0.1:18082/")) {
    "Demo must use a loopback/emulator origin on port 8080 or the isolated fixture port 18082."
}

// Only the disposable component/API test build disables Firebase's auto-init provider.
val week8IsolatedTests = providers.gradleProperty("week8IsolatedTests").map { it == "true" }.orElse(false)


android {
// ... (phần android block giữ nguyên)
    namespace = "com.example.giaodien"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.giaodien"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = if (providers.gradleProperty("demoInstrumentation").orNull == "true")
            "androidx.test.runner.AndroidJUnitRunner" else "com.example.giaodien.Week8TestRunner"
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "DEMO_MODE", "false")
            manifestPlaceholders["allowCleartext"] = "true"
            manifestPlaceholders["firebaseInitEnabled"] = (!week8IsolatedTests.get()).toString()
            buildConfigField("boolean", "WEEK8_ISOLATED_TESTS", week8IsolatedTests.get().toString())
            buildConfigField("String", "API_BASE_URL", "\"${debugApiBaseUrl.get()}\"")
            buildConfigField("String", "WEATHER_API_KEY", "\"${weatherApiKey.get()}\"")
        }
        release {
            buildConfigField("boolean", "DEMO_MODE", "false")
            manifestPlaceholders["allowCleartext"] = "false"
            manifestPlaceholders["firebaseInitEnabled"] = "true"
            buildConfigField("boolean", "WEEK8_ISOLATED_TESTS", "false")
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"${releaseApiBaseUrl.get()}\"")
            buildConfigField("String", "WEATHER_API_KEY", "\"${weatherApiKey.get()}\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    if (providers.gradleProperty("demoInstrumentation").orNull == "true") testBuildType = "demo"
    buildTypes.create("demo") {
        initWith(buildTypes.getByName("debug"))
        applicationIdSuffix = ".demo"
        versionNameSuffix = "-demo"
        matchingFallbacks += "debug"
        manifestPlaceholders["firebaseInitEnabled"] = "false"
        buildConfigField("boolean", "DEMO_MODE", "true")
        buildConfigField("boolean", "WEEK8_ISOLATED_TESTS", "false")
        buildConfigField("String", "API_BASE_URL", "\"${demoApiBaseUrl.get()}\"")
        buildConfigField("String", "WEATHER_API_KEY", "\"\"")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packagingOptions {
        resources {
            // Loại bỏ các file metadata trùng lặp
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/io.netty.versions.properties"
            )
        }
    }

}

configurations.all {
    resolutionStrategy {
        force("com.google.protobuf:protobuf-javalite:3.25.5")
        // Loại bỏ protobuf-java trùng
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    // ---------------------------------------------
    // CORE & COMPOSE
    // ---------------------------------------------
    implementation("androidx.core:core-ktx:1.13.1")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    // ---------------------------------------------
    // FIREBASE
    // ---------------------------------------------
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))

    implementation("com.google.firebase:firebase-firestore-ktx") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
    implementation("com.google.firebase:firebase-auth-ktx") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
    implementation("com.google.firebase:firebase-messaging-ktx") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }

    implementation("com.google.android.gms:play-services-auth:20.7.0") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }

    // ---------------------------------------------
    // HILT
    // ---------------------------------------------
    implementation("com.google.dagger:hilt-android:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")

    // ---------------------------------------------
    // NETWORK & SERIALIZATION
    // ---------------------------------------------
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.4")

// Hoặc phiên bản mới nhất
    // ---------------------------------------------
    // TESTING
    // ---------------------------------------------
    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

kapt {
    correctErrorTypes = true
}

val validateReleaseEndpoint by tasks.registering {
    doLast {
        val uri = URI(releaseApiBaseUrl.get())
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() &&
            !uri.host.endsWith(".invalid") && uri.userInfo == null &&
            uri.query == null && uri.fragment == null && uri.path.endsWith("/")) {
            "Release requires a real HTTPS PRODUCTION_API_BASE_URL ending in / (no credentials/query/fragment)."
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validateReleaseEndpoint) }
// The synthetic package has no Firebase project/client and never initializes Firebase.
tasks.matching { it.name == "processDemoGoogleServices" }.configureEach { enabled = false }
