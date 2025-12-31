// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    // Voici la syntaxe correcte pour Kotlin (.kts)
    id("com.google.gms.google-services") version "4.4.0" apply false
}