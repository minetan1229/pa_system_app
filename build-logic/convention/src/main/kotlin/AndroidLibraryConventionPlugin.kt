import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import pa.JAVA_VERSION
import pa.int
import pa.lib
import pa.libs

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // AGP 9 は Kotlin を内蔵している。org.jetbrains.kotlin.android は適用しない。
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk = libs.int("compileSdk")

                defaultConfig {
                    minSdk = libs.int("minSdk")
                    // ライブラリモジュールに targetSdk は無い（AGP 9 で廃止済み）
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    sourceCompatibility = JAVA_VERSION
                    targetCompatibility = JAVA_VERSION
                }
            }

            dependencies {
                add("testImplementation", libs.lib("junit4"))
                add("testImplementation", libs.lib("truth"))
                add("testImplementation", libs.lib("kotlinx-coroutines-test"))
            }
        }
    }
}
