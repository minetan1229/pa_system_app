plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.business"
}

dependencies {
    // 金額・稼働時間の計算
    implementation(project(":core:calc"))
    // 請求書の PDF 出力
    implementation(project(":core:export"))
}
