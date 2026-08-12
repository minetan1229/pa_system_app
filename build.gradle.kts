// AGP 9 は KGP を内蔵している（既定は AGP がバンドルするバージョン）。
// それより新しい Kotlin / KSP を使うため、ここで classpath を明示的に固定する。
// 参考: https://developer.android.com/build/migrate-to-built-in-kotlin
buildscript {
    dependencies {
        classpath(libs.kotlin.gradlePlugin)
        classpath(libs.ksp.gradlePlugin)
    }
}

// 各プラグインはここで「宣言だけ」して、実際の適用は :build-logic の convention plugin か
// 各モジュールの build.gradle.kts で行う。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.licensee) apply false
}
