package com.patoolbox.core.model

/**
 * 現場の環境に合わせた表示モード。
 * 「暗いFOHで眩しくない」「屋外の直射日光で見える」は実用上の必須要件なので、
 * 単なるライト/ダークではなく専用モードを用意している。
 */
enum class ThemeMode {
    /** 端末の設定に従う */
    SYSTEM,

    LIGHT,

    DARK,

    /** 暗所用。暗順応を壊さないよう赤系のみで描画する */
    NIGHT_RED,

    /** 屋外用。コントラスト最大・白背景・太字 */
    OUTDOOR,
}
