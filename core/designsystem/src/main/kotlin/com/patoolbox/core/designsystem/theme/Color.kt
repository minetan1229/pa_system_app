package com.patoolbox.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// 配色の考え方（Claude をモチーフに組み直したもの）
//
// 1. **地は白ではなく生成りの紙**。#F0EEE6 を地に、#FAF9F5 をカードに使う。
//    暗色側も純黒ではなく炭色（#1F1E1D）まで。長時間見る道具なので、
//    青白い面や純黒より、紙と炭の面の方が目が疲れない。
// 2. **色は粘土色（clay, #D97757）を1色だけ効かせる**。帯・下線・バッジの地・図の線に使う。
//    **白文字は乗せない**——コントラストが 3.1 しか出ず、屋外で読めない。
//    白文字が要るボタンには暗く落とした [PaBrand.clayInk] を使う（白に対して 6.1）。
// 3. 面の分け方は **影ではなく「明度差 + 1dp の枠線」**。
//    暗所モードでは影が一切見えず、屋外モードでは影が飛ぶので、
//    4つのテーマすべてで同じ作りが成立するのは線で分ける方だけ。
// 4. 意味の色（藍・緑・赤・琥珀）も彩度を落として紙の面に馴染ませる。
//    ただし文字として使う値はすべて 4.5:1 以上を確認してある。
//
// surfaceContainer 系まで明示的に指定しているのは、省くと Material のベースライン
// （紫がかったグレー）が残り、特に NIGHT_RED でカードだけ灰色に浮くため。

/**
 * テーマに依存しないブランド色。
 *
 * 色面や図の線に使う。**文字色として使わないこと**
 * （明色テーマの紙の面では [clay] のコントラストが 3.0 しか出ない）。
 */
object PaBrand {
    /** 粘土色。帯・下線・バッジの地・図の線に使う。黒文字なら乗せられる（6.7） */
    val clay = Color(0xFFD97757)

    /** 白文字を乗せる必要があるとき用。白に対して約 6.1:1 */
    val clayInk = Color(0xFFA6431E)

    /** 粘土色の淡い地。バッジやお知らせの背景 */
    val clayTintLight = Color(0xFFF5E4DA)
    val clayTintDark = Color(0xFF3A211A)

    /** リンクと情報。紙の面に合うよう彩度を落とした藍 */
    val blue = Color(0xFF2C5C8F)
    val blueTintLight = Color(0xFFE3E9F0)
    val blueTintDark = Color(0xFF17273A)

    val green = Color(0xFF2F6B47)
    val greenTintLight = Color(0xFFE1EDE4)
    val greenTintDark = Color(0xFF16301F)

    val red = Color(0xFFB03A2E)
    val redTintLight = Color(0xFFF7E3DE)
    val redTintDark = Color(0xFF3A1512)

    /** 注意。麻色寄りの琥珀 */
    val amber = Color(0xFF7A5210)
    val amberTintLight = Color(0xFFF3E7CE)
    val amberTintDark = Color(0xFF33280F)
}

/**
 * 明色。既定。
 *
 * 地を生成り（#F0EEE6）、カードを紙の白（#FAF9F5）にして、
 * 明度差 + 枠線だけで面を分けている。地まで純白にすると、この作りが成立しない。
 *
 * 無彩色の灰ではなく黄み寄りの灰を通しているのは、粘土色を1色だけ効かせる配色で
 * 面が青白いと、差し色だけが浮いて安っぽく見えるため。
 */
internal val PaLightColors = lightColorScheme(
    primary = PaBrand.clayInk,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = PaBrand.clayTintLight,
    onPrimaryContainer = Color(0xFF5C2410),
    secondary = PaBrand.blue,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = PaBrand.blueTintLight,
    onSecondaryContainer = Color(0xFF123A63),
    tertiary = PaBrand.green,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = PaBrand.greenTintLight,
    onTertiaryContainer = Color(0xFF143A26),
    error = PaBrand.red,
    onError = Color(0xFFFFFFFF),
    errorContainer = PaBrand.redTintLight,
    onErrorContainer = Color(0xFF4E120C),
    background = Color(0xFFF0EEE6),
    onBackground = Color(0xFF1F1E1D),
    surface = Color(0xFFF0EEE6),
    onSurface = Color(0xFF1F1E1D),
    surfaceVariant = Color(0xFFE8E6DC),
    onSurfaceVariant = Color(0xFF63615A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDFCF8),
    surfaceContainer = Color(0xFFFAF9F5),
    surfaceContainerHigh = Color(0xFFEDEBE1),
    surfaceContainerHighest = Color(0xFFE3E0D3),
    outline = Color(0xFF6E6C64),
    outlineVariant = Color(0xFFDCD9CB),
)

/**
 * 暗色。FOH の卓まわりで一番長く見る画面なので、ここを基準に作ってある。
 *
 * 純黒ではなく炭色（#1F1E1D）で止めている。純黒に白文字だとコントラストが立ちすぎて、
 * 薄暗い場所では文字の縁が滲んで見える。面は明度をわずかに上げるだけで分ける。
 */
internal val PaDarkColors = darkColorScheme(
    primary = PaBrand.clay,
    onPrimary = Color(0xFF2E1109),
    primaryContainer = PaBrand.clayTintDark,
    onPrimaryContainer = Color(0xFFF0B79B),
    secondary = Color(0xFF8FB3DC),
    onSecondary = Color(0xFF0C2039),
    secondaryContainer = PaBrand.blueTintDark,
    onSecondaryContainer = Color(0xFFC9DCF2),
    tertiary = Color(0xFF6FC292),
    onTertiary = Color(0xFF0A2716),
    tertiaryContainer = PaBrand.greenTintDark,
    onTertiaryContainer = Color(0xFFB4E3C7),
    error = Color(0xFFFF9A8E),
    onError = Color(0xFF4A0F09),
    errorContainer = PaBrand.redTintDark,
    onErrorContainer = Color(0xFFFFD3CB),
    background = Color(0xFF1F1E1D),
    onBackground = Color(0xFFF5F4EF),
    surface = Color(0xFF1F1E1D),
    onSurface = Color(0xFFF5F4EF),
    surfaceVariant = Color(0xFF30302E),
    onSurfaceVariant = Color(0xFFA8A69C),
    surfaceContainerLowest = Color(0xFF141413),
    surfaceContainerLow = Color(0xFF1F1E1D),
    surfaceContainer = Color(0xFF262624),
    surfaceContainerHigh = Color(0xFF30302E),
    surfaceContainerHighest = Color(0xFF3A3A37),
    outline = Color(0xFF8A887F),
    outlineVariant = Color(0xFF3D3D3A),
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
 * ブランド色（粘土色）を計測に当てず現場ドキュメントに残しているのは、
 * 粘土色が「アプリの色」として画面のあちこちに出るため、
 * カテゴリの識別としては別の色相に散らした方が見分けやすいから。
 *
 * **明色用と暗色用で別の値を持つ。** バッジは色面の上に文字を置くので、
 * 明色テーマ向けの濃い色をそのまま暗色テーマの炭色のカードに置くと、
 * カードとの明度差が 2.5 まで落ちてバッジの輪郭が消える。
 *
 * 呼び出し側はこの object を直接使わず、`ToolCategory.accentColor()` を通すこと。
 */
object PaCategoryColors {
    // 明色用。白文字が乗る前提（いずれも白に対して 5.5 以上）
    val measure = Color(0xFF35646E)
    val calc = Color(0xFF515EA0)
    val document = PaBrand.clayInk
    val business = Color(0xFF6B5E8E)

    // 暗色用。黒文字が乗る前提（いずれも黒に対して 8.5 以上）
    val measureDark = Color(0xFF6FB3C0)
    val calcDark = Color(0xFF97A2E2)
    val documentDark = PaBrand.clay
    val businessDark = Color(0xFFB3A3D6)
}

/**
 * 挿絵（[com.patoolbox.core.designsystem.component.PaIllustration]）の色。
 *
 * 計測画面の色は「値の意味」を持つので強い。挿絵は意味を持たないので、
 * 彩度を落として面だけで見せる。ここを強い色で塗ると、
 * 隣に並ぶ実測値のグラフより挿絵の方が目立ってしまう。
 *
 * 地を紙・麻・生成りで揃えているのは、挿絵だけ別のアプリから
 * 持ってきたように見えないようにするため。
 *
 * **呼び出し側でこの object を直接使わないこと。**
 * 暗所モード（赤以外の光を出せない）と屋外モード（淡色が飛ぶ）では別の値が要るので、
 * 必ず `paIllustrationPalette()` を通す。
 */
internal object PaSoft {
    // 明色。麻色を地にして、鈍い藍と苔色を面に置く
    val lineLight = Color(0xFF7C7568)
    val sandLight = Color(0xFFEDE7D9)
    val mutedLight = Color(0xFFDCD3C0)
    val skyLight = Color(0xFFBECBD8)
    val sageLight = Color(0xFFC6CDB2)
    val clayLight = Color(0xFFD97757)

    // 暗色。同じ色相のまま明度を落とす（色相を変えると別のアプリに見える）
    val lineDark = Color(0xFF9C978C)
    val sandDark = Color(0xFF262624)
    val mutedDark = Color(0xFF35342F)
    val skyDark = Color(0xFF3E4B58)
    val sageDark = Color(0xFF454A3A)
    val clayDark = Color(0xFFB35F41)
}
