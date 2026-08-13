package com.patoolbox.core.reference

sealed interface TroubleshootStep {
    val id: String
}

/** はい／いいえで分岐する質問。 */
data class TroubleshootQuestion(
    override val id: String,
    val text: String,
    val yesId: String,
    val noId: String,
) : TroubleshootStep

/** 行き着いた原因と対処。 */
data class TroubleshootResolution(
    override val id: String,
    val cause: String,
    val actions: List<String>,
) : TroubleshootStep

data class TroubleshootFlow(
    val title: String,
    val summary: String,
    val startId: String,
    val steps: List<TroubleshootStep>,
) {
    private val byId: Map<String, TroubleshootStep> = steps.associateBy { it.id }

    fun step(id: String): TroubleshootStep? = byId[id]

    val start: TroubleshootStep get() = byId.getValue(startId)
}

/**
 * トラブルの切り分け。
 *
 * 現場で焦っているときほど「上流から順に潰す」ができなくなるので、
 * 分岐を用意して機械的に辿れるようにしている。
 * 各分岐は「触れば結果がすぐ分かる」ことだけを聞く（推測を聞かない）。
 */
object Troubleshooting {

    val NO_SOUND = TroubleshootFlow(
        title = "音が出ない",
        summary = "上流から順に切り分ける。まず卓に信号が来ているかを見る",
        startId = "meter",
        steps = listOf(
            TroubleshootQuestion(
                id = "meter",
                text = "卓の該当チャンネルのメーターは振れていますか？",
                yesId = "fader",
                noId = "gain",
            ),
            TroubleshootQuestion(
                id = "gain",
                text = "ゲインを上げても振れませんか？（ファンタムが要るマイクなら48Vも確認）",
                yesId = "cable",
                noId = "gain_low",
            ),
            TroubleshootResolution(
                id = "gain_low",
                cause = "ゲイン不足、またはファンタム未供給",
                actions = listOf(
                    "適正レベルまでゲインを上げる",
                    "コンデンサマイク・アクティブDIなら48Vを入れる",
                ),
            ),
            TroubleshootQuestion(
                id = "cable",
                text = "ケーブルを別のものに替えると振れますか？",
                yesId = "cable_bad",
                noId = "source",
            ),
            TroubleshootResolution(
                id = "cable_bad",
                cause = "ケーブルの断線・接触不良",
                actions = listOf(
                    "そのケーブルを回線から外す（後で必ず処分か修理に回す）",
                    "同じ束の他の線も疑う",
                ),
            ),
            TroubleshootQuestion(
                id = "source",
                text = "マイク・楽器側を別のものに替えると振れますか？",
                yesId = "source_bad",
                noId = "patch",
            ),
            TroubleshootResolution(
                id = "source_bad",
                cause = "マイクまたは楽器側の故障",
                actions = listOf("予備に交換する", "楽器側の電池・シールドも確認する"),
            ),
            TroubleshootResolution(
                id = "patch",
                cause = "パッチの間違い、またはステージボックスの回線違い",
                actions = listOf(
                    "パッチ表と実際の差し込み位置を突き合わせる",
                    "マルチの番号とステージボックスの番号がずれていないか確認する",
                ),
            ),
            TroubleshootQuestion(
                id = "fader",
                text = "フェーダーとミュート、アサイン（LR / マトリクス）は正しいですか？",
                yesId = "amp",
                noId = "routing",
            ),
            TroubleshootResolution(
                id = "routing",
                cause = "卓内のルーティング",
                actions = listOf(
                    "ミュートを解除し、LRアサインを入れる",
                    "マスターフェーダーとVCA/DCAも確認する",
                ),
            ),
            TroubleshootQuestion(
                id = "amp",
                text = "アンプ・パワードスピーカーの電源とレベルは入っていますか？",
                yesId = "speaker_cable",
                noId = "amp_off",
            ),
            TroubleshootResolution(
                id = "amp_off",
                cause = "アンプ側の電源・レベル",
                actions = listOf(
                    "電源とプロテクトのランプを確認する",
                    "アッテネータが絞り切られていないか見る",
                ),
            ),
            TroubleshootResolution(
                id = "speaker_cable",
                cause = "スピーカーケーブル、またはスピーカー本体",
                actions = listOf(
                    "別の回線に繋いで切り分ける",
                    "アンプ出力にマイクケーブルを使っていないか確認する",
                ),
            ),
        ),
    )

    val FEEDBACK = TroubleshootFlow(
        title = "ハウリングする",
        summary = "まず利得を下げてから原因を探す。EQで削るのは最後",
        startId = "which",
        steps = listOf(
            TroubleshootQuestion(
                id = "which",
                text = "どのマイクで起きているか特定できていますか？",
                yesId = "position",
                noId = "isolate",
            ),
            TroubleshootResolution(
                id = "isolate",
                cause = "発生源が未特定",
                actions = listOf(
                    "チャンネルを1本ずつ下げて、止まったところが原因",
                    "本アプリのRTAで発振している帯域を確認する",
                ),
            ),
            TroubleshootQuestion(
                id = "position",
                text = "マイクがスピーカーの正面や近くにありませんか？",
                yesId = "move",
                noId = "gain_structure",
            ),
            TroubleshootResolution(
                id = "move",
                cause = "配置（最も効く対策）",
                actions = listOf(
                    "マイクをスピーカーの指向から外す",
                    "モニターの角度を変える。演者に近づけて音量を下げる",
                ),
            ),
            TroubleshootQuestion(
                id = "gain_structure",
                text = "ゲインが高すぎませんか？（フェーダーが下がりきっている）",
                yesId = "gain_down",
                noId = "eq",
            ),
            TroubleshootResolution(
                id = "gain_down",
                cause = "ゲイン構成",
                actions = listOf(
                    "ゲインを下げてフェーダーを定位置に戻す",
                    "必要なら演者にマイクへ近づいてもらう",
                ),
            ),
            TroubleshootResolution(
                id = "eq",
                cause = "特定帯域の共振",
                actions = listOf(
                    "発振している帯域をQ狭めで数dBだけ切る",
                    "切りすぎると音が痩せる。3〜4箇所までにとどめる",
                    "モニターとメインで原因帯域が違うことが多い。別々に処理する",
                ),
            ),
        ),
    )

    val HUM = TroubleshootFlow(
        title = "ハムノイズが出る",
        summary = "50/60Hz 系ならグランド、高い方なら別の原因を疑う",
        startId = "pitch",
        steps = listOf(
            TroubleshootQuestion(
                id = "pitch",
                text = "低い「ブーン」という音ですか？（ジーではなく）",
                yesId = "single",
                noId = "buzz",
            ),
            TroubleshootQuestion(
                id = "single",
                text = "特定のチャンネルだけで起きていますか？",
                yesId = "di",
                noId = "power",
            ),
            TroubleshootResolution(
                id = "di",
                cause = "その回線のグランドループ",
                actions = listOf(
                    "DIのグランドリフトを持ち上げる",
                    "楽器アンプと卓の電源系統を揃える",
                    "ケーブルを別経路に振る（電源ケーブルと並走させない）",
                ),
            ),
            TroubleshootResolution(
                id = "power",
                cause = "系統全体のグランドループ",
                actions = listOf(
                    "全機材の電源を同じ系統から取る",
                    "アース棒・アース端子の接続を確認する",
                    "照明・映像と電源を分ける",
                ),
            ),
            TroubleshootQuestion(
                id = "buzz",
                text = "調光された照明が入っていますか？",
                yesId = "dimmer",
                noId = "rf",
            ),
            TroubleshootResolution(
                id = "dimmer",
                cause = "調光器のノイズ",
                actions = listOf(
                    "音響と照明の電源系統を分ける",
                    "音声ケーブルを調光線から離す",
                ),
            ),
            TroubleshootResolution(
                id = "rf",
                cause = "高周波の飛び込み、または機器の故障",
                actions = listOf(
                    "携帯電話・無線機を卓とマイクケーブルから離す",
                    "ケーブルを1本ずつ抜いて発生源を特定する",
                ),
            ),
        ),
    )

    val ALL: List<TroubleshootFlow> = listOf(NO_SOUND, FEEDBACK, HUM)
}
