package pa

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** 全モジュール共通の Java / Kotlin ターゲット。AGP 9 の最低要件は JDK 17。 */
internal val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17

/** convention plugin の中からは `libs` アクセサが使えないため、Version Catalog を手で引く。 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalStateException("libs.versions.toml に library '$alias' が無い") }

internal fun VersionCatalog.int(alias: String): Int =
    findVersion(alias)
        .orElseThrow { IllegalStateException("libs.versions.toml に version '$alias' が無い") }
        .requiredVersion
        .toInt()

/**
 * Kotlin のコンパイルターゲットを揃える。
 * `kotlin { compilerOptions { } }` 拡張の型は AGP/KGP のバージョンで揺れるため、
 * コンパイルタスクを直接設定して壊れにくくしている。
 */
internal fun Project.configureKotlinJvmTarget() {
    tasks.withType<KotlinCompile>().configureEach {
        // プラグインのソース内では kotlin-dsl の `=` 代入オーバーロードが効かないので set() を使う
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
