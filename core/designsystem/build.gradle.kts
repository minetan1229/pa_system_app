plugins {
    alias(libs.plugins.pa.android.library)
    alias(libs.plugins.pa.android.compose)
}

android {
    namespace = "com.patoolbox.core.designsystem"
}

dependencies {
    api(project(":core:model"))
}
