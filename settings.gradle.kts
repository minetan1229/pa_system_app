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
include(":core:designsystem")
include(":core:ui")
include(":core:data")
include(":core:database")
include(":core:billing")
include(":core:testing")

// feature: 画面単位。Phase が進むごとに増える（:feature:spl, :feature:rta, ...）
include(":feature:home")
include(":feature:settings")
