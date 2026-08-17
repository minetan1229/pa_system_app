plugins {
    alias(libs.plugins.pa.android.library)
    alias(libs.plugins.pa.android.compose)
}

android {
    namespace = "com.patoolbox.core.ui"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:designsystem"))
    // 各画面の「？」から開く解説の内容。全 feature から参照するのでここに api で通す
    api(project(":core:reference"))

    implementation(libs.androidx.core.ktx)
    // 権限リクエスト（rememberLauncherForActivityResult）に使う
    implementation(libs.androidx.activity.compose)
}
