plugins {
    alias(libs.plugins.pa.jvm.library)
}

// Android に依存させないこと。
// 計測の正しさはここのユニットテストで担保するので、実機やRobolectricを挟まずに
// ミリ秒でテストが回る状態を維持する。
