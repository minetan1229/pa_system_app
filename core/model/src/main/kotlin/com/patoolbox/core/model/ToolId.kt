package com.patoolbox.core.model

/**
 * アプリに入っている全ツールのカタログ。
 *
 * ここが「全部入り」の唯一の定義。画面を追加するときは
 * 1. ここに enum を足す（または [implemented] を true にする）
 * 2. core:ui の strings.xml に表示名を足す
 * 3. app の PaNavHost の ToolDestination に画面を繋ぐ
 * の3ステップで済むようにしている。
 *
 * @param category 分類。ホームの入口と道具一覧のグループ分けに使う
 * @param access 無料で使えるか、Pro が必要か
 * @param badge カードに出す2〜4文字のラベル。アイコンではなく文字にしているのは、
 *   暗いFOHでも屋外でも識別できて、かつ material-icons-extended（約30MB）を
 *   抱えなくて済むため。
 * @param level 使うのに要る前提知識の量。[ExperienceLevel] に応じた並べ方に使う
 * @param phase 実装フェーズ（プラン準拠）。未実装ツールの「準備中」表示に使う。
 * @param implemented 実画面が繋がっているか
 */
enum class ToolId(
    val category: ToolCategory,
    val access: ToolAccess,
    val badge: String,
    val level: ToolLevel,
    val phase: Int,
    val implemented: Boolean = false,
) {
    // --- A. 計測 ---
    SPL_METER(ToolCategory.MEASURE, ToolAccess.FREE_LIMITED, "SPL", level = ToolLevel.BASIC, phase = 1, implemented = true),
    SPL_LOGGER(ToolCategory.MEASURE, ToolAccess.PRO, "LOG", level = ToolLevel.INTERMEDIATE, phase = 4, implemented = true),
    RTA(ToolCategory.MEASURE, ToolAccess.FREE_LIMITED, "RTA", level = ToolLevel.INTERMEDIATE, phase = 1, implemented = true),
    FFT(ToolCategory.MEASURE, ToolAccess.PRO, "FFT", level = ToolLevel.ADVANCED, phase = 4, implemented = true),
    SPECTROGRAM(ToolCategory.MEASURE, ToolAccess.PRO, "SPEC", level = ToolLevel.ADVANCED, phase = 4, implemented = true),
    SIGNAL_GENERATOR(ToolCategory.MEASURE, ToolAccess.FREE_LIMITED, "GEN", level = ToolLevel.INTERMEDIATE, phase = 1, implemented = true),
    FEEDBACK_FINDER(ToolCategory.MEASURE, ToolAccess.PRO, "FB", level = ToolLevel.INTERMEDIATE, phase = 4, implemented = true),
    DELAY_FINDER(ToolCategory.MEASURE, ToolAccess.PRO, "DLY", level = ToolLevel.ADVANCED, phase = 4, implemented = true),
    POLARITY_CHECK(ToolCategory.MEASURE, ToolAccess.PRO, "POL", level = ToolLevel.ADVANCED, phase = 4, implemented = true),
    ROOM_MEASURE(ToolCategory.MEASURE, ToolAccess.PRO, "IR", level = ToolLevel.ADVANCED, phase = 4, implemented = true),
    TUNER(ToolCategory.MEASURE, ToolAccess.FREE, "TUN", level = ToolLevel.BASIC, phase = 1, implemented = true),
    METRONOME(ToolCategory.MEASURE, ToolAccess.FREE, "CLK", level = ToolLevel.BASIC, phase = 1, implemented = true),
    RECORDER(ToolCategory.MEASURE, ToolAccess.PRO, "REC", level = ToolLevel.BASIC, phase = 6, implemented = true),

    // --- B. 計算・リファレンス ---
    DELAY_CALC(ToolCategory.CALC, ToolAccess.FREE, "ms", level = ToolLevel.BASIC, phase = 2, implemented = true),
    BPM_CALC(ToolCategory.CALC, ToolAccess.FREE, "BPM", level = ToolLevel.BASIC, phase = 2, implemented = true),
    DB_CALC(ToolCategory.CALC, ToolAccess.FREE, "dB", level = ToolLevel.BASIC, phase = 2, implemented = true),
    IMPEDANCE_CALC(ToolCategory.CALC, ToolAccess.FREE, "Ω", level = ToolLevel.INTERMEDIATE, phase = 2, implemented = true),
    POWER_CALC(ToolCategory.CALC, ToolAccess.PRO, "kW", level = ToolLevel.INTERMEDIATE, phase = 4, implemented = true),
    COVERAGE_CALC(ToolCategory.CALC, ToolAccess.PRO, "COV", level = ToolLevel.INTERMEDIATE, phase = 4, implemented = true),
    CONNECTOR_REF(ToolCategory.CALC, ToolAccess.FREE, "PIN", level = ToolLevel.BASIC, phase = 2, implemented = true),
    FREQ_CHART(ToolCategory.CALC, ToolAccess.FREE, "EQ", level = ToolLevel.BASIC, phase = 2, implemented = true),
    SIGNAL_QUALITY(ToolCategory.CALC, ToolAccess.FREE, "劣化", level = ToolLevel.BASIC, phase = 2, implemented = true),
    TROUBLESHOOT(ToolCategory.CALC, ToolAccess.FREE_LIMITED, "FIX", level = ToolLevel.BASIC, phase = 2, implemented = true),
    GLOSSARY(ToolCategory.CALC, ToolAccess.FREE, "用語", level = ToolLevel.BASIC, phase = 2, implemented = true),
    TEST_SIGNALS(ToolCategory.CALC, ToolAccess.FREE, "信号", level = ToolLevel.BASIC, phase = 2, implemented = true),
    WIRELESS_COORD(ToolCategory.CALC, ToolAccess.PRO, "MHz", level = ToolLevel.ADVANCED, phase = 4, implemented = true),

    // --- C. 現場ドキュメント ---
    PATCH_SHEET(ToolCategory.DOCUMENT, ToolAccess.FREE_LIMITED, "CH", level = ToolLevel.BASIC, phase = 3, implemented = true),
    STAGE_PLOT(ToolCategory.DOCUMENT, ToolAccess.PRO, "図", level = ToolLevel.BASIC, phase = 4, implemented = true),
    PDF_EXPORT(ToolCategory.DOCUMENT, ToolAccess.PRO, "PDF", level = ToolLevel.BASIC, phase = 3, implemented = true),
    RUN_SHEET(ToolCategory.DOCUMENT, ToolAccess.FREE_LIMITED, "進行", level = ToolLevel.BASIC, phase = 3, implemented = true),
    SHOW_TIMER(ToolCategory.DOCUMENT, ToolAccess.FREE, "TMR", level = ToolLevel.BASIC, phase = 3, implemented = true),
    SHOW_RUNNER(ToolCategory.DOCUMENT, ToolAccess.FREE_LIMITED, "本番", level = ToolLevel.BASIC, phase = 3, implemented = true),
    SFX_PADS(ToolCategory.DOCUMENT, ToolAccess.FREE_LIMITED, "SE", level = ToolLevel.BASIC, phase = 6, implemented = true),
    JOB_MANAGER(ToolCategory.DOCUMENT, ToolAccess.FREE_LIMITED, "案件", level = ToolLevel.BASIC, phase = 3, implemented = true),
    SNAPSHOT(ToolCategory.DOCUMENT, ToolAccess.PRO, "再現", level = ToolLevel.INTERMEDIATE, phase = 6, implemented = true),
    GEAR_INVENTORY(ToolCategory.DOCUMENT, ToolAccess.PRO, "台帳", level = ToolLevel.BASIC, phase = 6, implemented = true),

    // --- D. ビジネス ---
    INVOICE(ToolCategory.BUSINESS, ToolAccess.PRO, "見積", level = ToolLevel.BASIC, phase = 6, implemented = true),
    WORK_LOG(ToolCategory.BUSINESS, ToolAccess.PRO, "稼働", level = ToolLevel.BASIC, phase = 6, implemented = true),
    CLOUD_BACKUP(ToolCategory.BUSINESS, ToolAccess.PRO, "同期", level = ToolLevel.BASIC, phase = 6, implemented = true),
    ;

    /** Pro が無いと一切開けないツールか。FREE_LIMITED は開けるが機能に制限がある。 */
    val requiresPro: Boolean get() = access == ToolAccess.PRO

    companion object {
        fun fromIdOrNull(id: String): ToolId? = entries.firstOrNull { it.name == id }
    }
}

/** 道具の分類。ホームの入口と一覧の見出しに使う。 */
enum class ToolCategory {
    /** A. マイク/スピーカーを使う計測 */
    MEASURE,

    /** B. 計算機とリファレンス */
    CALC,

    /** C. 現場ドキュメント・運営 */
    DOCUMENT,

    /** D. 見積・稼働管理 */
    BUSINESS,
}

/**
 * 使うのに要る前提知識の量。
 *
 * 課金の区分（[ToolAccess]）とは無関係。無料でも読み違えやすい道具はあるし、
 * Pro でも押すだけの道具はある。
 *
 * **この区分で道具を隠さない。** 隠すと「入っていない」と思われる。
 * 変えるのは並べる順と、注意の札を出すかどうかだけ。
 */
enum class ToolLevel {
    /** 前提知識がほとんど要らない。手順どおりに押せば使える */
    BASIC,

    /** 現場の用語と手順を知っている前提 */
    INTERMEDIATE,

    /** 測定の理屈が分かっていないと数字を誤読する */
    ADVANCED,
}

/** 課金による利用範囲。 */
enum class ToolAccess {
    /** 制限なしで無料 */
    FREE,

    /** 無料で開けるが、記録・出力・保存件数などに制限がある */
    FREE_LIMITED,

    /** Pro 専用 */
    PRO,
}
