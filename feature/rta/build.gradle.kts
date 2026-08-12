plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.rta"
}

dependencies {
    implementation(project(":core:audio"))
}
