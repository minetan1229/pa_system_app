plugins {
    alias(libs.plugins.pa.android.library)
    alias(libs.plugins.pa.android.hilt)
}

android {
    namespace = "com.patoolbox.core.audio"
}

dependencies {
    api(project(":core:model"))
    // 数値処理は core:dsp に置き、このモジュールは Android の音声API との接続だけを持つ
    api(project(":core:dsp"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
