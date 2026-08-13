plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.feedback"
}

dependencies {
    implementation(project(":core:audio"))
}
