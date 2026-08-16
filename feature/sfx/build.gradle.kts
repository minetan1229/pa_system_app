plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.sfx"
}

dependencies {
    // SE の再生（SoundCuePlayer）
    implementation(project(":core:audio"))
}
