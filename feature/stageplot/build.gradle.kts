plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.stageplot"
}

dependencies {
    // 描画コードを PDF と共有している（画面と配布物がずれないようにするため）
    implementation(project(":core:export"))
}
