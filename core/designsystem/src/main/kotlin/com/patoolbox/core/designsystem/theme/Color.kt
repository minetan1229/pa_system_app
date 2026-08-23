package com.patoolbox.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// 配色の考え方（Cloudflare のダッシュボードを参照して組み直したもの）
//
// 1. **面はほぼ無彩色**にする。白と灰色の段差だけで構造を作り、
//    色は「意味があるところ」にしか使わない。38ツールぶんの画面を並べたときに
//    落ち着いて見えるのは、色数を絞った方だった。
// 2. **ブランド色（橙）は文字を乗せない**。帯・下線・バッジ・図の線に使う。
//    橙の上に白文字を置くとコントラストが 3:1 前後しか出ず、屋外で読めない。
//    文字を乗せる必要があるボタンには、暗く落とした [PaBrand.orangeInk] を使う。
// 3. 面の分け方は **影ではなく「明度差 + 1dp の枠線」**。
//    暗所モードでは影が一切見えず、屋外モードでは影が飛ぶので、
//    4つのテーマすべてで同じ作りが成立するのは線で分ける方だけ。
//
// surfaceContainer 系まで明示的に指定しているのは、省くと Material のベースライン
// （紫がかったグレー）が残り、特に NIGHT_RED でカードだけ灰色に浮くため。

/**
 * テーマに依存しないブランド色。
 *
 * 色面や図の線に使う。**文字色として使わないこと**
 * （明色テーマの白背景では [orange] のコントラストが足りない）。
 */
object PaBrand {
    /** ブランドの橙。帯・下線・バッジの地・図の線に使う */
    val orange = Color(0xFFF6821F)

    /** 橙の上に白文字を置く必要があるとき用。白に対して約 5:1 */
    val orangeInk = Color(0xFFB8500A)

    /** 橙の淡い地色。バッジやお知らせの背景 */
    val orangeTintLight = Color(0xFFFDF0E2)
    val orangeTintDark = Color(0xFF3A2109)

    /** リンクと情報。Cloudflare のドキュメントリンクと同系 */
    val blue = Color(0xFF0055CC)
    val blueTintLight = Color(0xFFE4EDFB)
    val blueTintDark = Color(0xFF10243F)

    val green = Color(0xFF0F7B4F)
    val greenTintLight = Color(0xFFE1F3EA)
    val greenTintDark = Color(0xFF0E2A1E)

    val red = Color(0xFFBE3B34)
    val redTintLight = Color(0xFFFBE7E5)
    val redTintDark = Color(0xFF33110F)

    val amber = Color(0xFF8A5B00)
    val amberTintLight = Color(0xFFFBF0DA)
    val amberTintDark = Color(0xFF2E2206)
}

/**
 * 明色。既定。
 *
 * 背景をわずかに温かい灰色（#F5F3EF）にして、カードの白を浮かせている。
 * 背景まで白にすると、枠線だけで面を分ける作りが成立しない。
 *
 * 青みの灰色（#F6F6F7）から温色側に振ってあるのは、長時間見る画面で
 * 冷たい灰色が硬く感じられたため。明度はほぼ変えていないので、
 * 文字と枠線のコントラストは元のまま。
 */
internal val PaLightColors = lightColorScheme(
    primary = PaBrand.orangeInk,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = PaBrand.orangeTintLight,
    onPrimaryContainer = Color(0xFF4A2000),
    secondary = PaBrand.blue,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = PaBrand.blueTintLight,
    onSecondaryContainer = Color(0xFF00265C),
    tertiary = PaBrand.green,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = PaBrand.greenTintLight,
    onTertiaryContainer = Color(0xFF04301D),
    error = PaBrand.red,
    onError = Color(0xFFFFFFFF),
    errorContainer = PaBrand.redTintLight,
    onErrorContainer = Color(0xFF450F0B),
    background = Color(0xFFF5F3EF),
    onBackground = Color(0xFF1D1F20),
    surface = Color(0xFFF5F3EF),
    onSurface = Color(0xFF1D1F20),
    surfaceVariant = Color(0xFFEDEAE3),
    onSurfaceVariant = Color(0xFF5F6469),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDFCFA),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1EEE8),
    surfaceContainerHighest = Color(0xFFE8E4DC),
    outline = Color(0xFF74797E),
    outlineVariant = Color(0xFFDFDAD1),
)

/**
 * 暗色。FOH の卓まわりで一番長く見る画面なので、ここを基準に作ってある。
 * 背景をほぼ黒に落とし、面は明度をわずかに上げるだけで分ける。
 */
internal val PaDarkColors = darkColorScheme(
    primary = PaBrand.orange,
    onPrimary = Color(0xFF2B1400),
    primaryContainer = PaBrand.orangeTintDark,
    onPrimaryContainer = Color(0xFFFFC894),
    secondary = Color(0xFF7FAEFF),
    onSecondary = Color(0xFF002451),
    secondaryContainer = PaBrand.blueTintDark,
    onSecondaryContainer = Color(0xFFCEDFFB),
    tertiary = Color(0xFF57C48D),
    onTertiary = Color(0xFF00351F),
    tertiaryContainer = PaBrand.greenTintDark,
    onTertiaryContainer = Color(0xFFB8E9CE),
    error = Color(0xFFFF8F86),
    onError = Color(0xFF5A0F0A),
    errorContainer = PaBrand.redTintDark,
    onErrorContainer = Color(0xFFFFD6D2),
    background = Color(0xFF121314),
    onBackground = Color(0xFFE9EAEB),
    surface = Color(0xFF121314),
    onSurface = Color(0xFFE9EAEB),
    surfaceVariant = Color(0xFF2B2D2F),
    onSurfaceVariant = Color(0xFFA9AEB3),
    surfaceContainerLowest = Color(0xFF0C0D0E),
    surfaceContainerLow = Color(0xFF16181A),
    surfaceContainer = Color(0xFF1C1E20),
    surfaceContainerHigh = Color(0xFF24262A),
    surfaceContainerHighest = Color(0xFF2D3033),
    outline = Color(0xFF878C92),
    outlineVariant = Color(0xFF32353A),
)

/**
 * 暗所（FOH）用。赤系だけで描画して暗順応を壊さない。
 * 青系の光を一切出さないのが目的なので、彩度のある色を足さないこと。
 */
internal val PaNightRedColors = darkColorScheme(
    primary = Color(0xFFFF5533),
    onPrimary = Color(0xFF1A0000),
    primaryContainer = Color(0xFF3D0F00),
    onPrimaryContainer = Color(0xFFFF8566),
    secondary = Color(0xFFCC4629),
    onSecondary = Color(0xFF1A0000),
    secondaryContainer = Color(0xFF2E0A00),
    onSecondaryContainer = Color(0xFFFF7A5C),
    tertiary = Color(0xFFB33D22),
    onTertiary = Color(0xFF1A0000),
    tertiaryContainer = Color(0xFF260800),
    onTertiaryContainer = Color(0xFFFF6B4A),
    error = Color(0xFFFFAA8F),
    onError = Color(0xFF330500),
    errorContainer = Color(0xFF5C1200),
    onErrorContainer = Color(0xFFFFC7B5),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE04B2B),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE04B2B),
    surfaceVariant = Color(0xFF260800),
    onSurfaceVariant = Color(0xFFB33D22),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF080000),
    surfaceContainer = Color(0xFF120000),
    surfaceContainerHigh = Color(0xFF1C0300),
    surfaceContainerHighest = Color(0xFF260500),
    outline = Color(0xFF802B18),
    outlineVariant = Color(0xFF4D1A0E),
)

/** 屋外・直射日光用。コントラスト最大。 */
internal val PaOutdoorColors = lightColorScheme(
    primary = Color(0xFF00344D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF00344D),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF1A1A1A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF3D2E00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF000000),
    error = Color(0xFF8B0000),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFD6D6),
    onErrorContainer = Color(0xFF000000),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF2F2F2),
    surfaceContainerHigh = Color(0xFFEAEAEA),
    surfaceContainerHighest = Color(0xFFE0E0E0),
    outline = Color(0xFF000000),
    outlineVariant = Color(0xFF8A8A8A),
)

/**
 * カテゴリの識別色。
 *
 * アイコンを使わない方針なので、色 + 文字バッジでツールを見分ける。
 * ブランド色（橙）を計測に当てず現場ドキュメントに残しているのは、
 * 橙が「アプリの色」として画面のあちこちに出るため、
 * カテゴリの識別としては別の色相に散らした方が見分けやすいから。
 */
object PaCategoryColors {
    val measure = Color(0xFF2C7683)
    val calc = Color(0xFF4A6FB5)
    val document = PaBrand.orange
    val business = Color(0xFF7565AC)
}

/**
 * 挿絵（[com.patoolbox.core.designsystem.component.PaIllustration]）の色。
 *
 * 計測画面の色は「値の意味」を持つので強い。挿絵は意味を持たないので、
 * 彩度を落として面だけで見せる。ここを強い色で塗ると、
 * 隣に並ぶ実測値のグラフより挿絵の方が目立ってしまう。
 *
 * **呼び出し側でこの object を直接使わないこと。**
 * 暗所モード（赤以外の光を出せない）と屋外モード（淡色が飛ぶ）では別の値が要るので、
 * 必ず `paIllustrationPalette()` を通す。
 */
internal object PaSoft {
    // 明色。温かい砂色を地にして、水色と若草色を面に置く
    val lineLight = Color(0xFF7A7264)
    val sandLight = Color(0xFFF0EBE1)
    val mutedLight = Color(0xFFE1DACC)
    val skyLight = Color(0xFFBFCFDD)
    val sageLight = Color(0xFFC3D2C4)
    val clayLight = Color(0xFFE3A672)

    // 暗色。同じ色相のまま明度を落とす（色相を変えると別のアプリに見える）
    val lineDark = Color(0xFF9BA0A6)
    val sandDark = Color(0xFF1E2022)
    val mutedDark = Color(0xFF2B2E31)
    val skyDark = Color(0xFF3A4C5F)
    val sageDark = Color(0xFF3D4E44)
    val clayDark = Color(0xFFB9803F)
}
