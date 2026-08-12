plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.metronome"
}

dependencies {
    implementation(project(":core:audio"))
}
