plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// El plugin de Google Services solo se aplica si existe app/google-services.json
// (descargado de la consola de Firebase). Sin el archivo, la app compila igual y
// se conecta al Firebase Emulator Suite local (ver README).
val hasGoogleServices = file("google-services.json").exists()
if (hasGoogleServices) {
    apply(plugin = "com.google.gms.google-services")
}

/** Lee una propiedad de Gradle (-P o gradle.properties) con un valor por defecto. */
fun prop(name: String, default: String): String =
    (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() } ?: default

android {
    namespace = "com.ucompensar.ecomarket"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ucompensar.ecomarket"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"

        // Firebase: emulador local cuando no hay google-services.json o cuando se pide con -Pecomarket.useEmulator=true
        val useEmulator = !hasGoogleServices || prop("ecomarket.useEmulator", "false").toBoolean()
        buildConfigField("boolean", "USE_FIREBASE_EMULATOR", useEmulator.toString())
        buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"${prop("ecomarket.emulatorHost", "10.0.2.2")}\"")

        // ePayco
        buildConfigField("String", "EPAYCO_PUBLIC_KEY", "\"${prop("EPAYCO_PUBLIC_KEY", "")}\"")
        buildConfigField("boolean", "EPAYCO_TEST", prop("EPAYCO_TEST", "true"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Firma de depuración para que el APK de validación se instale sin keystore propio
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // AndroidX y Material Design 3
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.google.android.material:material:1.12.0")

    // Firebase: autenticación, Firestore y Storage
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Geolocalización (Google Play Services)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Corrutinas y puente con las Task de Google Play Services
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Carga de imágenes remotas
    implementation("io.coil-kt:coil:2.7.0")
}
