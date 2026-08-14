package com.patoolbox.core.model

/**
 * 案件（現場）。パッチ表・進行表・測定結果はすべてこれに紐づく。
 *
 * 時刻は epoch ミリ秒で持ち、表示側で端末のタイムゾーンに変換する。
 */
data class Job(
    val id: Long = 0,
    val name: String,
    val venueName: String = "",
    val eventDateEpochMs: Long? = null,
    val loadInAtEpochMs: Long? = null,
    val showAtEpochMs: Long? = null,
    val clientName: String = "",
    val contact: String = "",
    val notes: String = "",
    val createdAtEpochMs: Long = 0,
    val updatedAtEpochMs: Long = 0,
)

/**
 * パッチ表（インプットリスト）。
 * [jobId] が null なら案件に紐づかないテンプレート。
 */
data class PatchSheet(
    val id: Long = 0,
    val jobId: Long? = null,
    val name: String,
    val isTemplate: Boolean = false,
    val rows: List<PatchRow> = emptyList(),
    val updatedAtEpochMs: Long = 0,
)

/**
 * パッチ表の1行 = 卓の1ch。
 *
 * ファンタムは行ごとに持つ。コンデンサマイクとDIが混ざる現場で
 * 「どのchに48Vを入れるか」が一目で分かることが実用上重要。
 */
data class PatchRow(
    val id: Long = 0,
    val channel: Int,
    val source: String = "",
    val micModel: String = "",
    val standType: String = "",
    val phantom: Boolean = false,
    val multiNumber: String = "",
    val notes: String = "",
) {
    val isEmpty: Boolean
        get() = source.isBlank() && micModel.isBlank() && notes.isBlank()
}

/** 進行表の1項目。 */
data class ScheduleItem(
    val id: Long = 0,
    val jobId: Long,
    val title: String,
    val startAtEpochMs: Long? = null,
    val durationMinutes: Int = 0,
    val owner: String = "",
) {
    /** 開始時刻と長さから終了時刻を出す。 */
    val endAtEpochMs: Long?
        get() = startAtEpochMs?.let { it + durationMinutes * MILLIS_PER_MINUTE }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}

/**
 * ステージプロット（配置図）。
 *
 * ステージの実寸を持たせているのは、図に縮尺を出すため。
 * 「モニターを4本」とだけ書かれた図をもらっても、置けるかどうかは寸法が無いと分からない。
 */
data class StagePlot(
    val id: Long = 0,
    val jobId: Long? = null,
    val name: String,
    val stageWidthMeters: Double = DEFAULT_WIDTH_METERS,
    val stageDepthMeters: Double = DEFAULT_DEPTH_METERS,
    val notes: String = "",
    val items: List<StageItem> = emptyList(),
    val updatedAtEpochMs: Long = 0,
) {
    /** 図の縦横比。描画側はこれに合わせて枠を取る */
    val aspectRatio: Float
        get() = (stageWidthMeters / stageDepthMeters).toFloat()

    companion object {
        const val DEFAULT_WIDTH_METERS = 8.0
        const val DEFAULT_DEPTH_METERS = 5.0
        const val MIN_METERS = 1.0
        const val MAX_METERS = 40.0
    }
}

/**
 * ステージ上に置いた1つの記号。
 *
 * 座標は **ステージの左上を (0,0)、右下を (1,1) とした相対値**。
 * 画面のサイズにも用紙のサイズにも依存しないので、スマホで作った図が
 * そのまま A4 でも同じ配置で出る。ピクセルで持つとここが破綻する。
 */
data class StageItem(
    val id: Long = 0,
    val symbol: StageSymbol,
    /** 空なら [StageSymbol.defaultLabel] を表示する */
    val label: String = "",
    val x: Float = 0.5f,
    val y: Float = 0.5f,
) {
    val displayLabel: String get() = label.ifBlank { symbol.defaultLabel }
}

/** 記号の見た目。文字だけだと図として読めないので、形でも区別する。 */
enum class StageShape {
    RECT,
    CIRCLE,
    /** 客席側が広い台形。モニターの向きが図で分かる */
    WEDGE,
}

/**
 * ステージプロットに置ける記号。
 *
 * バッジは2〜3文字。アイコン画像を持たないのはアプリ全体の方針と同じで、
 * 暗いFOHでも読めることと、素材のライセンス確認を増やさないことが理由。
 */
enum class StageSymbol(
    val badge: String,
    val defaultLabel: String,
    val shape: StageShape,
    /** ステージ幅に対する既定の大きさ */
    val widthRatio: Float,
    val heightRatio: Float,
) {
    WEDGE_MONITOR("MON", "モニター", StageShape.WEDGE, 0.10f, 0.07f),
    SIDE_FILL("SF", "サイドフィル", StageShape.WEDGE, 0.12f, 0.10f),
    IN_EAR("IEM", "イヤモニ", StageShape.RECT, 0.07f, 0.05f),
    MAIN_SPEAKER("PA", "メインSP", StageShape.RECT, 0.08f, 0.12f),
    DRUMS("Dr", "ドラム", StageShape.CIRCLE, 0.18f, 0.18f),
    BASS_AMP("Ba", "ベースアンプ", StageShape.RECT, 0.09f, 0.07f),
    GUITAR_AMP("Gt", "ギターアンプ", StageShape.RECT, 0.08f, 0.06f),
    KEYBOARD("Key", "キーボード", StageShape.RECT, 0.16f, 0.06f),
    VOCAL("Vo", "ボーカル", StageShape.CIRCLE, 0.07f, 0.07f),
    MIC("Mic", "マイク", StageShape.CIRCLE, 0.05f, 0.05f),
    DI("DI", "DI", StageShape.RECT, 0.05f, 0.04f),
    RISER("台", "平台", StageShape.RECT, 0.22f, 0.16f),
    POWER("電", "電源", StageShape.RECT, 0.05f, 0.04f),
    PERFORMER("人", "演者", StageShape.CIRCLE, 0.06f, 0.06f),
    MUSIC_STAND("譜", "譜面台", StageShape.RECT, 0.05f, 0.04f),
    ;

    companion object {
        /** 追加パレットの並び。使用頻度の高い順 */
        val PALETTE: List<StageSymbol> = entries
    }
}

/** スタンドの種類。パッチ表で選ぶ頻度が高いので定型を用意する。 */
object StandTypes {
    const val TALL_BOOM = "ブーム(背高)"
    const val SHORT_BOOM = "ブーム(卓上)"
    const val STRAIGHT = "ストレート"
    const val CLIP = "クリップ"
    const val NONE = "なし"

    val ALL = listOf(TALL_BOOM, SHORT_BOOM, STRAIGHT, CLIP, NONE)
}
