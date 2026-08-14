plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.wireless"
}

dependencies {
    implementation(project(":core:calc"))
}
