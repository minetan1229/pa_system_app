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
include(":core:designsystem")
include(":core:ui")
include(":core:data")
include(":core:database")
include(":core:billing")
include(":core:testing")

// feature: 画面単位。Phase が進むごとに増える
include(":feature:home")
include(":feature:settings")
include(":feature:spl")
include(":feature:calibration")
