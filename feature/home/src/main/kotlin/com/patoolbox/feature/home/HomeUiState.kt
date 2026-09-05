package com.patoolbox.feature.home

import com.patoolbox.core.model.CalibrationConfidence
import com.patoolbox.core.model.ConsoleType
import com.patoolbox.core.model.ExperienceLevel
import com.patoolbox.core.model.FieldProfile
import com.patoolbox.core.model.PlannedShow
import com.patoolbox.core.model.ProStatus
import com.patoolbox.core.model.ToolId

/**
 * ホーム画面の状態。
 *
 * 検索の絞り込みは ViewModel ではなく Composable 側で行う。
 * ツール名・説明が string リソースなので、その方がロケール切り替えに自然に追従でき、
 * ViewModel を Context 非依存に保てる（＝Robolectric 無しで単体テストできる）。
 */
data class HomeUiState(
    val query: String = "",
    val proStatus: ProStatus = ProStatus.Free,
    val favoriteTools: List<ToolId> = emptyList(),
    val calibration: CalibrationSummary = CalibrationSummary(),
    val profile: FieldProfile = FieldProfile.Default,
    /** false なら、慣れの度合いをまだ自分で選んでいない。ホームの最初の案内を出す */
    val hasChosenExperienceLevel: Boolean = false,
    /**
     * 今日の日付が入っている進行表。本番万能コントローラーが自動で読み込むのと同じもの。
     *
     * 「いま始まっているか」はここでは判定しない（時計が進んでも状態は流れてこない）。
     * 判定は画面側で今の時刻を見ながら [PlannedShow.isRunningAt] などで行う。
     */
    val todayShows: List<PlannedShow> = emptyList(),
) {
    val level: ExperienceLevel get() = profile.level

    /** いま出すべき今日の1本。進行中のもの → 次に始まるもの の順 */
    fun showForNow(nowEpochMs: Long): PlannedShow? = PlannedShow.pick(todayShows, nowEpochMs)
}

/**
 * 保存済みの校正値をまとめたもの。
 *
 * ホームでは「どの入力を何 dB ずらしているか」までは出さない。
 * 出しても現場で判断に使えないうえ、未校正のまま使い続けている人に
 * 気づいてもらうのが目的なので、状態を1段階だけ持つ。
 *
 * @param calibratedCount 校正済みの入力の数（内蔵マイクと USB マイクは別に数える）
 * @param bestConfidence 保存済みのうち一番信頼できるもの。無ければ未校正
 */
data class CalibrationSummary(
    val calibratedCount: Int = 0,
    val bestConfidence: CalibrationConfidence = CalibrationConfidence.UNCALIBRATED,
) {
    val isCalibrated: Boolean get() = calibratedCount > 0
}

/**
 * お気に入りが空のときにホームへ出す4つ。
 *
 * どの段でも「音を測る」を先頭に置き、残り3つを慣れの度合いで入れ替える。
 * 選ぶ基準は「その段の人が、その日いちばん最初に開くもの」。
 *
 * - [ExperienceLevel.BEGINNER] — 分からないことを調べる道具（用語・ピン配置・仕込みの表）
 * - [ExperienceLevel.INTERMEDIATE] — 音を出して測る一式
 * - [ExperienceLevel.ADVANCED] — 追い込みに使う道具
 *
 * トラブルシュート（症状から原因を切り分ける道具）はここに固定で置かない。
 * ホームの重要な位置に常駐させると、使わない現場では場所を取るだけの邪魔になるため。
 * 初心者向けの案内文（[R.string.home_beginner_guide_body]）から導線を張るのと、
 * CALC 分類からはいつでも開ける。
 *
 * 初心者・中級者の4つは Pro 無しで開けるものだけにしてある
 * （何も分からない段階で、開いた先が「買え」になる導線を最初の画面に置かない）。
 * 上級者だけは Pro 専用の道具を入れている——その段の人が最初に開くのは
 * ハウリング検出とディレイ実測で、無料の道具に差し替えると
 * 「自分向けではない画面」になるため。★で自分の4つに変えられる前提でもある。
 *
 * 卓の種類は4枠目だけを差し替える。上級者では差し替えないのは、
 * その段の人は★で自分の4つを決めているため——勝手に入れ替わる方が邪魔になる。
 */
fun starterTools(profile: FieldProfile): List<ToolId> {
    val base = when (profile.level) {
        ExperienceLevel.BEGINNER -> listOf(
            ToolId.SPL_METER,
            ToolId.GLOSSARY,
            ToolId.CONNECTOR_REF,
            ToolId.PATCH_SHEET,
        )

        ExperienceLevel.INTERMEDIATE -> listOf(
            ToolId.SPL_METER,
            ToolId.RTA,
            ToolId.SIGNAL_GENERATOR,
            ToolId.DELAY_CALC,
        )

        ExperienceLevel.ADVANCED -> listOf(
            ToolId.SPL_METER,
            ToolId.RTA,
            ToolId.FEEDBACK_FINDER,
            ToolId.DELAY_FINDER,
        )
    }

    val consoleTool = when {
        profile.level == ExperienceLevel.ADVANCED -> null
        // 手で EQ を触るので、どの帯域が何かを引く早見表を出す
        profile.console == ConsoleType.ANALOG -> ToolId.FREQ_CHART
        // 卓側に入力の割り当てがあるので、当日の表を出す
        profile.console == ConsoleType.DIGITAL -> ToolId.PATCH_SHEET
        else -> null
    }

    if (consoleTool == null || consoleTool in base.take(3)) return base
    return base.take(3) + consoleTool
}
