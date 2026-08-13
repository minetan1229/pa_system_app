plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.reference"
}

dependencies {
    implementation(project(":core:reference"))
}
