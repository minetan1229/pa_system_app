package com.patoolbox.core.model

/** 端末ローカルの設定。クラウドには送らない。 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** よく使うツール。ホームの先頭に出す */
    val favoriteToolIds: Set<String> = emptySet(),
    /** 計測中に画面を消さない */
    val keepScreenOnWhileMeasuring: Boolean = true,
    /** 開発用の Pro 強制 ON。デバッグビルドでしか効かせない */
    val debugProOverride: Boolean = false,
) {
    companion object {
        val Default = UserPreferences()
    }
}
