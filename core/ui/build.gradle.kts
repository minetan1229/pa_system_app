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

    implementation(libs.androidx.core.ktx)
    // 権限リクエスト（rememberLauncherForActivityResult）に使う
    implementation(libs.androidx.activity.compose)
}
