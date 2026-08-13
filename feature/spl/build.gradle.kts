plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.spl"
}

dependencies {
    implementation(project(":core:audio"))
    implementation(project(":core:export"))
    implementation(libs.androidx.activity.compose)
}
