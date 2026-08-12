plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.spl"
}

dependencies {
    implementation(project(":core:audio"))
}
