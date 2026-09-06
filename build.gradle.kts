plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

// The project lives inside a OneDrive-synced folder, where the sync client holds
// locks on files Gradle needs to delete between builds. Redirecting build output
// to a plain local directory avoids "Unable to delete directory" failures.
val outputRoot: java.io.File = File(System.getProperty("user.home"), "AndroidBuilds/FamilyMoney")

rootProject.layout.buildDirectory.set(File(outputRoot, "root"))
subprojects {
    layout.buildDirectory.set(File(outputRoot, name))
}
