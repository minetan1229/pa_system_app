package com.patoolbox.core.model

/**
 * アプリに入っている全ツールのカタログ。
 *
 * ここが「全部入り」の唯一の定義。画面を追加するときは
 * 1. ここに enum を足す（または [implemented] を true にする）
 * 2. core:ui の strings.xml に表示名を足す
 * 3. app の ToolRouting に画面を繋ぐ
 * の3ステップで済むようにしている。
 *
 * @param category ホーム画面のグループ分け
 * @param access 無料で使えるか、Pro が必要か
 * @param badge カードに出す2〜4文字のラベル。アイコンではなく文字にしているのは、
 *   暗いFOHでも屋外でも識別できて、かつ material-icons-extended（約30MB）を
 *   抱えなくて済むため。
 * @param phase 実装フェーズ（プラン準拠）。未実装ツールの「準備中」表示に使う。
 * @param implemented 実画面が繋がっているか
 */
enum class ToolId(
    val category: ToolCategory,
    val access: ToolAccess,
    val badge: String,
    val phase: Int,
    val implemented: Boolean = false,
) {
    // --- A. 計測 ---
    SPL_METER(ToolCategory.MEASURE, ToolAccess.FREE_LIMITED, "SPL", phase = 1, implemented = true),
    SPL_LOGGER(ToolCategory.MEASURE, ToolAccess.PRO, "LOG", phase = 4, implemented = true),
    RTA(ToolCategory.MEASURE, ToolAccess.FREE_LIMITED, "RTA", phase = 1, implemented = true),
    FFT(ToolCategory.MEASURE, ToolAccess.PRO, "FFT", phase = 4, implemented = true),
    SPECTROGRAM(ToolCategory.MEASURE, ToolAccess.PRO, "SPEC", phase = 4, implemented = true),
    SIGNAL_GENERATOR(ToolCategory.MEASURE, ToolAccess.FREE_LIMITED, "GEN", phase = 1, implemented = true),
    FEEDBACK_FINDER(ToolCategory.MEASURE, ToolAccess.PRO, "FB", phase = 4, implemented = true),
    DELAY_FINDER(ToolCategory.MEASURE, ToolAccess.PRO, "DLY", phase = 4, implemented = true),
    POLARITY_CHECK(ToolCategory.MEASURE, ToolAccess.PRO, "POL", phase = 4, implemented = true),
    ROOM_MEASURE(ToolCategory.MEASURE, ToolAccess.PRO, "IR", phase = 4, implemented = true),
    TUNER(ToolCategory.MEASURE, ToolAccess.FREE, "TUN", phase = 1, implemented = true),
    METRONOME(ToolCategory.MEASURE, ToolAccess.FREE, "CLK", phase = 1, implemented = true),
    RECORDER(ToolCategory.MEASURE, ToolAccess.PRO, "REC", phase = 6, implemented = true),

    // --- B. 計算・リファレンス ---
    DELAY_CALC(ToolCategory.CALC, ToolAccess.FREE, "ms", phase = 2, implemented = true),
    BPM_CALC(ToolCategory.CALC, ToolAccess.FREE, "BPM", phase = 2, implemented = true),
    DB_CALC(ToolCategory.CALC, ToolAccess.FREE, "dB", phase = 2, implemented = true),
    IMPEDANCE_CALC(ToolCategory.CALC, ToolAccess.FREE, "Ω", phase = 2, implemented = true),
    POWER_CALC(ToolCategory.CALC, ToolAccess.PRO, "kW", phase = 4, implemented = true),
    COVERAGE_CALC(ToolCategory.CALC, ToolAccess.PRO, "COV", phase = 4, implemented = true),
    CONNECTOR_REF(ToolCategory.CALC, ToolAccess.FREE, "PIN", phase = 2, implemented = true),
    FREQ_CHART(ToolCategory.CALC, ToolAccess.FREE, "EQ", phase = 2, implemented = true),
    SIGNAL_QUALITY(ToolCategory.CALC, ToolAccess.FREE, "劣化", phase = 2, implemented = true),
    TROUBLESHOOT(ToolCategory.CALC, ToolAccess.FREE_LIMITED, "FIX", phase = 2, implemented = true),
    GLOSSARY(ToolCategory.CALC, ToolAccess.FREE, "用語", phase = 2, implemented = true),
    TEST_SIGNALS(ToolCategory.CALC, ToolAccess.FREE, "信号", phase = 2, implemented = true),
    WIRELESS_COORD(ToolCategory.CALC, ToolAccess.PRO, "MHz", phase = 4, implemented = true),

    // --- C. 現場ドキュメント ---
    PATCH_SHEET(ToolCategory.DOCUMENT, ToolAccess.FREE_LIMITED, "CH", phase = 3, implemented = true),
    STAGE_PLOT(ToolCategory.DOCUMENT, ToolAccess.PRO, "図", phase = 4, implemented = true),
    PDF_EXPORT(ToolCategory.DOCUMENT, ToolAccess.PRO, "PDF", phase = 3, implemented = true),
    RUN_SHEET(ToolCategory.DOCUMENT, ToolAccess.FREE_LIMITED, "進行", phase = 3, implemented = true),
    SHOW_TIMER(ToolCategory.DOCUMENT, ToolAccess.FREE, "TMR", phase = 3, implemented = true),
    SFX_PADS(ToolCategory.DOCUMENT, ToolAccess.FREE_LIMITED, "SE", phase = 6, implemented = true),
    JOB_MANAGER(ToolCategory.DOCUMENT, ToolAccess.FREE_LIMITED, "案件", phase = 3, implemented = true),
    SNAPSHOT(ToolCategory.DOCUMENT, ToolAccess.PRO, "再現", phase = 6, implemented = true),
    GEAR_INVENTORY(ToolCategory.DOCUMENT, ToolAccess.PRO, "台帳", phase = 6, implemented = true),

    // --- D. ビジネス ---
    INVOICE(ToolCategory.BUSINESS, ToolAccess.PRO, "見積", phase = 6, implemented = true),
    WORK_LOG(ToolCategory.BUSINESS, ToolAccess.PRO, "稼働", phase = 6, implemented = true),
    CLOUD_BACKUP(ToolCategory.BUSINESS, ToolAccess.PRO, "同期", phase = 6, implemented = true),
    ;

    /** Pro が無いと一切開けないツールか。FREE_LIMITED は開けるが機能に制限がある。 */
    val requiresPro: Boolean get() = access == ToolAccess.PRO

    companion object {
        fun fromIdOrNull(id: String): ToolId? = entries.firstOrNull { it.name == id }
    }
}

/** ホーム画面のグループ。 */
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

/** 課金による利用範囲。 */
enum class ToolAccess {
    /** 制限なしで無料 */
    FREE,

    /** 無料で開けるが、記録・出力・保存件数などに制限がある */
    FREE_LIMITED,

    /** Pro 専用 */
    PRO,
}
