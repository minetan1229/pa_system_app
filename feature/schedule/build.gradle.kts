plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.schedule"
}

dependencies {
    implementation(project(":core:export"))
    implementation(libs.androidx.activity.compose)
}
