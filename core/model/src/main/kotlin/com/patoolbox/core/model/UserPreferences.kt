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
    /** 本番モードで何を止めるか */
    val showMode: ShowModeSettings = ShowModeSettings.Default,
    /** 慣れの度合いと卓の種類。ホームの並べ方と説明の量を決める */
    val profile: FieldProfile = FieldProfile.Default,
    /**
     * 慣れの度合いを一度でも自分で選んだか。
     *
     * false のあいだはホームの最初の1回だけ、大きく尋ねる案内を出す
     * （[FieldProfile.level] の既定は中級だが、それを「選んだ」とは区別する）。
     * 選んだあとは設定画面からいつでも変えられる。
     */
    val hasChosenExperienceLevel: Boolean = false,
) {
    companion object {
        val Default = UserPreferences()
    }
}
