import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

// Firebase is optional: the google-services plugin is applied only when the
// developer has dropped app/google-services.json into place. Without it the
// app still builds and runs fully — family sync just stays in local mode.
val googleServicesFile = file("google-services.json")
val firebaseEnabled = googleServicesFile.exists()
if (firebaseEnabled) {
    apply(plugin = "com.google.gms.google-services")
}

// Optional release signing. Create keystore.properties next to this file with
// storeFile / storePassword / keyAlias / keyPassword to sign release builds.
// AI keys live in local.properties (git-ignored) rather than in source.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val groqKeys = localProps.getProperty("groq.keys", "")
val groqModel = localProps.getProperty("groq.model", "openai/gpt-oss-120b")

// Supabase powers the in-app updater. The anon key is safe to ship — row level
// security is what actually protects the data.
val supabaseUrl = localProps.getProperty("supabase.url", "")
val supabaseAnonKey = localProps.getProperty("supabase.anonKey", "")

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.familymoney"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.familymoney"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField("boolean", "FIREBASE_ENABLED", firebaseEnabled.toString())
        buildConfigField("String", "GROQ_KEYS", "\"" + groqKeys + "\"")
        buildConfigField("String", "GROQ_MODEL", "\"" + groqModel + "\"")
        buildConfigField("String", "SUPABASE_URL", "\"" + supabaseUrl + "\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"" + supabaseAnonKey + "\"")
        resourceConfigurations += listOf("he", "en")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // Fall back to the debug key so `assembleRelease` still yields an
                // installable APK for personal / sideload use.
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/DEPENDENCIES",
            "META-INF/INDEX.LIST"
        )
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    sourceSets {
        getByName("main") {
            // Firebase-specific implementation only compiles when the SDK is on
            // the classpath (i.e. when google-services.json exists).
            java.srcDir(if (firebaseEnabled) "src/firebase/java" else "src/nofirebase/java")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.navigation:navigation-compose:2.8.5")

    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.google.zxing:core:3.5.3")

    if (firebaseEnabled) {
        implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
        implementation("com.google.firebase:firebase-firestore-ktx")
        implementation("com.google.firebase:firebase-auth-ktx")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
