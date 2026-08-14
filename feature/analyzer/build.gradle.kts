plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.analyzer"
}

dependencies {
    implementation(project(":core:audio"))
}
