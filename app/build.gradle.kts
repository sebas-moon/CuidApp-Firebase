plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services) // Firebase
}

android {
    // Paquete de la app. Reemplaza al atributo package="" del AndroidManifest.xml
    namespace = "com.santo_tomas.cuidapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.santo_tomas.cuidapp"
        minSdk = 31      // Android 12 o superior
        targetSdk = 36   // Android 16
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)          // AppCompatActivity
    implementation(libs.material)           // MaterialButton, MaterialCardView, BottomNavigationView, Material 3
    implementation(libs.activity)           // Activity Result API (registerForActivityResult)
    implementation(libs.constraintlayout)   // ConstraintLayout y Flow
    implementation(platform(libs.firebase.bom)) // BoM: elige versiones compatibles de Firebase
    implementation(libs.firebase.firestore) // Cloud Firestore: CRUD de pacientes en la nube
    implementation(libs.firebase.auth)      // Firebase Authentication: registro e inicio de sesión
    implementation(libs.mlkit.text.recognition) // ML Kit: lee el texto de la cédula en el teléfono (sin internet)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}