plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.measure"
}

dependencies {
    implementation(project(":core:audio"))
    // 遅延を距離に直すのに音速が要る
    implementation(project(":core:calc"))
}
