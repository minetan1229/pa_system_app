import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import pa.lib
import pa.libs

/**
 * :feature:* モジュール共通の設定。
 * これ1つ適用すれば Android ライブラリ + Compose + Hilt + core への依存が揃う。
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("pa.android.library")
            pluginManager.apply("pa.android.compose")
            pluginManager.apply("pa.android.hilt")

            dependencies {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
                add("implementation", project(":core:data"))
                add("implementation", project(":core:billing"))

                add("implementation", libs.lib("androidx-core-ktx"))
                add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.lib("androidx-navigation-compose"))
                add("implementation", libs.lib("androidx-hilt-viewmodel-compose"))
                add("implementation", libs.lib("kotlinx-coroutines-android"))

                add("testImplementation", project(":core:testing"))
                add("testImplementation", libs.lib("turbine"))
            }
        }
    }
}
