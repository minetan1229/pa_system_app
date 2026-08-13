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
include(":feature:patch")
include(":feature:showtimer")
include(":feature:job")
include(":feature:schedule")
