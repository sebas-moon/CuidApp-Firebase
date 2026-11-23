// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // Plugin que lee google-services.json y conecta la app con TU proyecto de Firebase
    alias(libs.plugins.google.gms.google.services) apply false
}