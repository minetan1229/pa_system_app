plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.tuner"
}

dependencies {
    implementation(project(":core:audio"))
}
