import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import pa.JAVA_VERSION
import pa.configureKotlinJvmTarget
import pa.lib
import pa.libs

/** Android に依存しない純 Kotlin モジュール（:core:model など）。JVM テストが速い。 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }

            configureKotlinJvmTarget()

            dependencies {
                add("testImplementation", libs.lib("junit4"))
                add("testImplementation", libs.lib("truth"))
            }
        }
    }
}
