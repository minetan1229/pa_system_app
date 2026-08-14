pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "pa-toolbox"

include(":app")

// core: 全 feature が共有する土台
include(":core:model")
// 計測の数値処理。Android に依存しない純 Kotlin なので、JVM テストで理論値と突き合わせられる
include(":core:dsp")
// マイク入力とスピーカー出力。core:dsp を Android の音声APIに繋ぐ層
include(":core:audio")
// 計算機の数値処理。こちらも Android 非依存で JVM テスト対象
include(":core:calc")
include(":core:reference")
include(":core:designsystem")
include(":core:ui")
include(":core:data")
include(":core:database")
include(":core:export")
include(":core:billing")
include(":core:testing")

// feature: 画面単位。Phase が進むごとに増える
include(":feature:home")
include(":feature:settings")
include(":feature:spl")
include(":feature:calibration")
include(":feature:siggen")
include(":feature:rta")
include(":feature:tuner")
include(":feature:metronome")
include(":feature:calc")
include(":feature:reference")
include(":feature:feedback")
// スイープ測定（ディレイ実測・極性・残響）。3ツールで測定行為が同じなので1モジュール
include(":feature:measure")
// FFT アナライザとスペクトログラム。同じ解析結果の別の見せ方なので1モジュール
include(":feature:analyzer")
// ステージプロット。描画コードは core:export に置いて PDF と共有している
include(":feature:stageplot")
// ワイヤレスの混変調計算。法令上の可否は扱わない（一次資料を確認できないため）
include(":feature:wireless")
// 録音。WAV の読み書きは core:export、再生は既存の AudioPlaybackEngine を使う
include(":feature:recorder")
// Phase 6 の運営ツール群（機材台帳・スナップショット・見積/請求・稼働記録・バックアップ）。
// どれも「一覧して足して消す」形で独自性が薄いため、1モジュールにまとめている
include(":feature:business")
include(":feature:patch")
include(":feature:showtimer")
include(":feature:job")
include(":feature:schedule")
