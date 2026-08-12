plugins {
    alias(libs.plugins.pa.android.library)
    alias(libs.plugins.pa.android.hilt)
}

android {
    namespace = "com.patoolbox.core.data"
}

dependencies {
    api(project(":core:model"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
