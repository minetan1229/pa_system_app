// スクリプト内の `java` は Gradle の拡張を指すので、完全修飾では書けない
import java.util.Properties

plugins {
    alias(libs.plugins.pa.android.application)
    alias(libs.plugins.pa.android.compose)
    alias(libs.plugins.pa.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.licensee)
}

/**
 * 署名鍵の読み込み。
 *
 * keystore.properties があればそれを使い、無ければデバッグ鍵で署名する。
 * 未署名の APK は端末にインストールできないので、鍵を用意していない段階でも
 * とりあえず動かせることを優先した。Play へ出す前には必ず本番鍵に差し替える
 * （手順は docs/PHASE5.md）。
 */
val keystoreProperties = rootProject.file("keystore.properties").takeIf { it.exists() }?.let {
    Properties().apply { it.inputStream().use(::load) }
}

android {
    namespace = "com.patoolbox"

    defaultConfig {
        // 公開前に自分のドメインに合わせて変更する（applicationId は後から変えられない）
        applicationId = "com.patoolbox"
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("upload") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
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
            signingConfig = if (keystoreProperties != null) {
                signingConfigs.getByName("upload")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    /**
     * 課金が入るまでの暫定的な全機能開放。
     *
     * Play Billing はまだ入っていないので、この値が false だと Pro のツールが
     * どのビルドでも一切開けない。**Phase 5 で課金を実装したら false にすること。**
     * 消し忘れると有料機能が無料で出続ける。
     */
    buildTypes.configureEach {
        buildConfigField("boolean", "PRE_RELEASE_UNLOCK", "true")
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
    implementation(project(":feature:spl"))
    implementation(project(":feature:calibration"))
    implementation(project(":feature:siggen"))
    implementation(project(":feature:rta"))
    implementation(project(":feature:tuner"))
    implementation(project(":feature:metronome"))
    implementation(project(":feature:calc"))
    implementation(project(":feature:reference"))
    implementation(project(":feature:feedback"))
    implementation(project(":feature:measure"))
    implementation(project(":feature:analyzer"))
    implementation(project(":feature:stageplot"))
    implementation(project(":feature:wireless"))
    implementation(project(":feature:recorder"))
    implementation(project(":feature:business"))
    implementation(project(":feature:patch"))
    implementation(project(":feature:showtimer"))
    implementation(project(":feature:sfx"))
    implementation(project(":feature:job"))
    implementation(project(":feature:schedule"))

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
