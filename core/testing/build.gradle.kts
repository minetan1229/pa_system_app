plugins {
    alias(libs.plugins.pa.android.library)
}

android {
    namespace = "com.patoolbox.core.testing"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:data"))
    api(project(":core:billing"))

    api(libs.junit4)
    api(libs.truth)
    api(libs.kotlinx.coroutines.test)
}
