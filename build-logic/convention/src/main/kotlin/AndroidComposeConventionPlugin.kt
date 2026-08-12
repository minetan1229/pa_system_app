import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import pa.lib
import pa.libs

/**
 * Compose を使うモジュールに適用する。
 * `pa.android.application` か `pa.android.library` の後に適用すること。
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.findByType(ApplicationExtension::class.java)?.apply {
                buildFeatures { compose = true }
            }
            extensions.findByType(LibraryExtension::class.java)?.apply {
                buildFeatures { compose = true }
            }

            dependencies {
                val bom = libs.lib("androidx-compose-bom")
                add("implementation", platform(bom))
                add("androidTestImplementation", platform(bom))

                add("implementation", libs.lib("androidx-compose-foundation"))
                add("implementation", libs.lib("androidx-compose-material3"))
                add("implementation", libs.lib("androidx-compose-ui"))
                add("implementation", libs.lib("androidx-compose-ui-graphics"))
                add("implementation", libs.lib("androidx-compose-ui-tooling-preview"))
                add("debugImplementation", libs.lib("androidx-compose-ui-tooling"))
            }
        }
    }
}
