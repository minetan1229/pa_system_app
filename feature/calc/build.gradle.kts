plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.calc"
}

dependencies {
    implementation(project(":core:calc"))
}
