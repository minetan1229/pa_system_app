plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.showrunner"
}

dependencies {
    // SE パッド・同期音源の再生（SoundCuePlayer）と、その保存先（SoundCueRepository は core:data 側）
    implementation(project(":core:audio"))
}
