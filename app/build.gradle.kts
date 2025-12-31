plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") // Indispensable pour Firebase
}

android {
    namespace = "com.example.numidiapath"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.numidiapath"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Bibliothèques de base AndroidX
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // --- FIREBASE (Gestion via BOM) ---
    // Le BOM permet de ne pas spécifier les versions pour chaque module Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")      // Auth
    implementation("com.google.firebase:firebase-firestore") // Base de données (Texte)
    implementation("com.google.firebase:firebase-storage")   // Stockage (Photos)

    // --- AFFICHAGE D'IMAGES (Glide) ---
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // --- CARTOGRAPHIE ---
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // --- TESTS ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // ML Kit Image Labeling (Indispensable pour corriger l'erreur d'import)
    implementation("com.google.mlkit:image-labeling:17.0.7")
// Pour la gestion de l'image (InputImage)
    implementation("com.google.android.gms:play-services-mlkit-image-labeling:16.0.8")
    // Ajoutez cette ligne pour la Database en temps réel
    implementation("com.google.firebase:firebase-database:21.0.0")
}