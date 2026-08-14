plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.recorder"
}

dependencies {
    implementation(project(":core:audio"))
}
