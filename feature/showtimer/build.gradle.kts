plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.showtimer"
}

dependencies {
    // 本番中のレベル/スペクトラム監視。時間だけの画面から離れられないと使い物にならない
    implementation(project(":core:audio"))
}
