package com.patoolbox.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 現場前提の寸法。
 *
 * 余白は 4dp グリッドに乗せている。画面ごとに 6dp / 10dp / 14dp と散らすと、
 * 一つ一つは正しく見えても並べたときに揃わず、雑な印象だけが残る。
 * [minTouch] を 48dp 以上に保つのは、暗所・手袋・揺れる足場でも押せるようにするため。
 *
 * 角丸は Claude に合わせて深めにしてある（カード 14dp / 入力 8dp）。
 * 以前は 8dp / 4dp だったが、紙のような温かい面には角の立った矩形が合わず、
 * 面と面の境目だけが硬く見えていた。表の詰まり具合は角丸ではなく
 * [spaceMd] 以下の余白で作る。
 */
@Immutable
data class PaDimens(
    val minTouch: Dp = 48.dp,

    // --- 余白（4dp グリッド） ---
    val spaceXs: Dp = 4.dp,
    val spaceSm: Dp = 8.dp,
    val spaceMd: Dp = 12.dp,
    val space: Dp = 16.dp,
    val spaceLg: Dp = 24.dp,
    val spaceXl: Dp = 32.dp,

    /** 画面の左右余白 */
    val gutter: Dp = 16.dp,
    val gutterSmall: Dp = 8.dp,

    // --- 角丸 ---
    /** バッジ・入力欄・絞り込みの札 */
    val cornerSmall: Dp = 8.dp,

    /** カード・パネル */
    val cardCorner: Dp = 14.dp,

    /** 大きな面。ボトムシートなど */
    val cornerLarge: Dp = 22.dp,

    /** ピル（状態ラベル）。完全な丸 */
    val cornerPill: Dp = 999.dp,

    val toolCardMinHeight: Dp = 124.dp,

    /** 説明を2行に詰めたカード（上級者の表示）。説明そのものは消えない */
    val toolCardCompactMinHeight: Dp = 112.dp,

    /**
     * ツールカードの文字バッジ。
     * 36dp から広げたのは、離れた場所から色と3文字だけで当たりを付けられるようにするため。
     */
    val badgeSize: Dp = 44.dp,

    /** 枠線。影ではなく線で面を分けるので、太らせないこと */
    val hairline: Dp = 1.dp,

    /** パネル左端の識別帯。色でカテゴリを示すのに使う */
    val railWidth: Dp = 3.dp,

    /**
     * 一覧のカードに敷く挿絵の高さ。
     * これ以上低いと絵が潰れて何の絵か分からなくなり、
     * これ以上高いと1画面に並ぶカードが3枚を切る。
     */
    val illustrationRow: Dp = 60.dp,

    /** 見出しの挿絵。ホームの一番上に1枚だけ置く */
    val illustrationHero: Dp = 148.dp,
)

val LocalPaDimens = staticCompositionLocalOf { PaDimens() }
