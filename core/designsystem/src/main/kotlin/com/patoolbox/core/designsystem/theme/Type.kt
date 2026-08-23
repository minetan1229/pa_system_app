package com.patoolbox.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// フォントは端末標準のみを使う（外部フォントを同梱しない）。
// 商用配布でフォントのライセンス確認を増やさないための判断。
//
// 大きい文字ほど字間を詰める。既定のままだと見出しが間延びして、
// 本文との差が「大きさ」だけになり、締まらない。
//
// 一番大きい見出し（display*）だけ明朝／セリフにしている。Claude の見出しと同じ考え方で、
// 「読み物の見出し」と「操作するUIの文字」を書体で分けるためのもの。
// title 以下をセリフにしないのは、表・ボタン・単位のような小さい文字で
// セリフの飾りが潰れて読みにくくなるため。

/**
 * 見出し用のセリフ体。端末標準のセリフ（日本語なら明朝）をそのまま使う。
 *
 * **本文や操作の文字には使わないこと。** 小さい字ではセリフの飾りが潰れ、
 * 屋外や暗所で読み取りが遅くなる。
 */
val PaSerif: FontFamily = FontFamily.Serif

/**
 * 数値表示は等幅にする。プロポーショナルだと桁が変わるたびに数字が横に揺れて、
 * ライブ中の SPL メーターが読みづらくなる。
 */
val ReadoutTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 72.sp,
    letterSpacing = (-1).sp,
)

val PaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PaSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 54.sp,
        letterSpacing = (-1.0).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = PaSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.6).sp,
    ),
    /** 見出しの絵に添える題。ホームの一番上で使う */
    displaySmall = TextStyle(
        fontFamily = PaSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    /**
     * バッジや単位などの小さいラベル。等幅にしているのは
     * "SPL" "1/3" "Ω" のような記号混じりが縦に揃うようにするため。
     */
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp,
    ),
)
