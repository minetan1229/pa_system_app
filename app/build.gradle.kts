plugins {
    alias(libs.plugins.pa.android.application)
    alias(libs.plugins.pa.android.compose)
    alias(libs.plugins.pa.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.licensee)
}

android {
    namespace = "com.patoolbox"

    defaultConfig {
        // 公開前に自分のドメインに合わせて変更する（applicationId は後から変えられない）
        applicationId = "com.patoolbox"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Pro 判定のデバッグトグルはこのビルドでのみ効く
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // 署名は Play App Signing 前提。ローカルでは未署名APKが出る。
        }
    }
}

/**
 * 商用配布の前提条件その1: 依存ライブラリのライセンスを機械的に縛る。
 * 許可リストに無いライセンスが混ざると `:app:licensee`（= check）が落ちる。
 * GPL / AGPL / LGPL / 非商用限定を「気づかずに」混ぜる事故を防ぐのが目的。
 */
licensee {
    allow("Apache-2.0")
    // protobuf-javalite（DataStore の推移依存）
    allow("BSD-3-Clause")

    // 実際に使っているものだけを書く（未使用の許可は licensee が警告を出す）。
    // 追加してよいのは MIT / BSD-2-Clause / CC0-1.0 まで。GPL / AGPL / LGPL /
    // 非商用限定は絶対に許可しない（THIRD_PARTY.md 参照）。
    // Phase 5 で Play Billing を入れるときは Android SDK 利用規約の allowUrl を足す。
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:billing"))

    implementation(project(":feature:home"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.viewmodel.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
