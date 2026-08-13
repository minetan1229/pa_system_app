package com.patoolbox.core.reference

enum class ConnectorCategory(val label: String) {
    LINE("ライン・マイク"),
    SPEAKER("スピーカー"),
    UTILITY("その他"),
}

data class PinAssignment(val pin: String, val signal: String)

data class Connector(
    val name: String,
    val category: ConnectorCategory,
    val summary: String,
    val pins: List<PinAssignment>,
    /** 現場で事故になりやすい点。ここが本体 */
    val cautions: List<String> = emptyList(),
)

/**
 * コネクタのピン配列と注意点。
 *
 * ピン番号そのものより「間違えると何が起きるか」を書くことを優先している。
 * 配線図は調べれば出てくるが、現場で必要なのは
 * 「これを繋ぐと壊れるのか、音が出ないだけなのか」の判断のため。
 */
object Connectors {

    val ALL: List<Connector> = listOf(
        Connector(
            name = "XLR（3ピン）",
            category = ConnectorCategory.LINE,
            summary = "バランス接続の基本。マイクとライン両方に使う",
            pins = listOf(
                PinAssignment("1", "グランド（シールド）"),
                PinAssignment("2", "ホット（+ / 正相）"),
                PinAssignment("3", "コールド（− / 逆相）"),
            ),
            cautions = listOf(
                "古い日本製機材には2番コールドのものがある。混在すると片方だけ逆相になる",
                "1番は必ずシャーシに落とす。浮かせるとノイズを拾う",
                "ファンタムは2番・3番に同電位で乗る。1番との間に電圧がかかる",
            ),
        ),
        Connector(
            name = "TRS フォン（バランス）",
            category = ConnectorCategory.LINE,
            summary = "バランスのライン接続。XLR と同じ信号を3極で送る",
            pins = listOf(
                PinAssignment("T（チップ）", "ホット（+）"),
                PinAssignment("R（リング）", "コールド（−）"),
                PinAssignment("S（スリーブ）", "グランド"),
            ),
            cautions = listOf(
                "同じ形状でステレオ（T=L, R=R）にも使う。挿す先で意味が変わる",
                "バランス出力にステレオケーブルを挿すと片チャンネルが逆相で回る",
            ),
        ),
        Connector(
            name = "TS フォン（アンバランス）",
            category = ConnectorCategory.LINE,
            summary = "楽器用。ノイズに弱いので長く引かない",
            pins = listOf(
                PinAssignment("T（チップ）", "信号"),
                PinAssignment("S（スリーブ）", "グランド"),
            ),
            cautions = listOf(
                "5m を超えると高域が落ち、ノイズを拾いやすくなる。DI を使うこと",
            ),
        ),
        Connector(
            name = "インサート（TRS 1本）",
            category = ConnectorCategory.LINE,
            summary = "1本で送りと戻りを兼ねる。卓のインサート端子に使う",
            pins = listOf(
                PinAssignment("T（チップ）", "センド（多くの機種）"),
                PinAssignment("R（リング）", "リターン（多くの機種）"),
                PinAssignment("S（スリーブ）", "グランド"),
            ),
            cautions = listOf(
                "T と R が逆の機種がある。卓の取説を必ず確認する",
                "半挿しでセンドだけ取る使い方があるが、接触不良の原因になる",
            ),
        ),
        Connector(
            name = "RCA（ピン）",
            category = ConnectorCategory.LINE,
            summary = "民生機器の接続。レベルが -10dBV でプロ機材より低い",
            pins = listOf(
                PinAssignment("中心", "信号"),
                PinAssignment("外周", "グランド"),
            ),
            cautions = listOf(
                "+4dBu の機材と繋ぐと約12dB のレベル差が出る。DI かマッチング機器を挟む",
            ),
        ),
        Connector(
            name = "Speakon NL4",
            category = ConnectorCategory.SPEAKER,
            summary = "スピーカー接続の標準。2系統（4極）を1本で送れる",
            pins = listOf(
                PinAssignment("1+", "1系統目 ＋"),
                PinAssignment("1−", "1系統目 −"),
                PinAssignment("2+", "2系統目 ＋（バイアンプ時の高域など）"),
                PinAssignment("2−", "2系統目 −"),
            ),
            cautions = listOf(
                "NL2 のプラグは NL4 のジャックに挿さる（1系統目のみ使用）",
                "バイアンプ機をパッシブで鳴らすと高域ドライバを飛ばすことがある。結線を必ず確認",
                "アンプ出力にマイクケーブル（XLR）を使わない。電流を流せない",
            ),
        ),
        Connector(
            name = "Speakon NL2",
            category = ConnectorCategory.SPEAKER,
            summary = "2極のスピーカー接続",
            pins = listOf(
                PinAssignment("1+", "＋"),
                PinAssignment("1−", "−"),
            ),
        ),
        Connector(
            name = "DI ボックス",
            category = ConnectorCategory.UTILITY,
            summary = "アンバランスをバランスに変換して長距離に送る",
            pins = listOf(
                PinAssignment("INPUT", "楽器から（TS）"),
                PinAssignment("THRU", "アンプへ（並列出力）"),
                PinAssignment("OUTPUT", "卓へ（XLR バランス）"),
            ),
            cautions = listOf(
                "グランドリフトはハムが出るときだけ持ち上げる。常時リフトはノイズの元",
                "アクティブDIはファンタムが要る。卓側の48Vを確認する",
                "パッドは楽器のレベルが高い（アクティブベースなど）ときに入れる",
            ),
        ),
        Connector(
            name = "ファンタム電源（48V）",
            category = ConnectorCategory.UTILITY,
            summary = "コンデンサマイクとアクティブDIに電源を送る",
            pins = listOf(
                PinAssignment("2番・3番", "+48V（同電位）"),
                PinAssignment("1番", "グランド（帰り）"),
            ),
            cautions = listOf(
                "リボンマイクに掛けると壊れることがある。結線ミスや断線時は特に危険",
                "抜き差しは必ずファンタムを切ってから。突入電流でスピーカーからノイズが出る",
                "ダイナミックマイクは掛かっていても基本は問題ないが、断線したケーブルでは壊れ得る",
            ),
        ),
    )

    fun byCategory(category: ConnectorCategory): List<Connector> =
        ALL.filter { it.category == category }
}
