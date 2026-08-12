plugins {
    alias(libs.plugins.pa.android.library)
    alias(libs.plugins.pa.android.hilt)
}

android {
    namespace = "com.patoolbox.core.billing"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:data"))

    implementation(libs.kotlinx.coroutines.android)

    // Phase 5 で Play Billing 9 を入れる（サブスク＋買い切り、オフラインキャッシュ付き）。
    // それまでは LocalProGate だけで Free/Pro を切り替える。
}
