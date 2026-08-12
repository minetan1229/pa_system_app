import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import pa.JAVA_VERSION
import pa.int
import pa.libs

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // AGP 9 は Kotlin を内蔵している。org.jetbrains.kotlin.android は適用しない。
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                compileSdk = libs.int("compileSdk")

                defaultConfig {
                    minSdk = libs.int("minSdk")
                    targetSdk = libs.int("targetSdk")
                }

                // Kotlin の jvmTarget は targetCompatibility から自動で決まるので個別設定は不要
                compileOptions {
                    sourceCompatibility = JAVA_VERSION
                    targetCompatibility = JAVA_VERSION
                }
            }
        }
    }
}
