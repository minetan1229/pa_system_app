plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.siggen"
}

dependencies {
    implementation(project(":core:audio"))
}
