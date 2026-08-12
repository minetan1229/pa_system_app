plugins {
    alias(libs.plugins.pa.android.library)
    alias(libs.plugins.pa.android.hilt)
    alias(libs.plugins.pa.android.room)
}

android {
    namespace = "com.patoolbox.core.database"
}

dependencies {
    api(project(":core:model"))

    implementation(libs.kotlinx.coroutines.android)
}
