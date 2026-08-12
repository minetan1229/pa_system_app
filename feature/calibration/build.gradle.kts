plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.calibration"
}

dependencies {
    implementation(project(":core:audio"))
}
