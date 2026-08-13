package com.patoolbox.core.reference

/** 帯域ごとの効き方。EQ を触る前の当たりをつけるために使う。 */
data class BandTip(
    val label: String,
    val fromHz: Double,
    val toHz: Double,
    val effect: String,
)

data class InstrumentBands(
    val instrument: String,
    /** 基音のおおよその範囲 */
    val fundamentalFromHz: Double,
    val fundamentalToHz: Double,
    val tips: List<BandTip>,
)

/**
 * 楽器ごとの帯域チャート。
 *
 * 「ここを上げれば良くなる」ではなく「ここを触ると何が変わるか」を書いている。
 * 実際にどう転ぶかは会場と演者で変わるので、断定するとかえって邪魔になる。
 */
object FrequencyChart {

    val ALL: List<InstrumentBands> = listOf(
        InstrumentBands(
            instrument = "キック",
            fundamentalFromHz = 40.0,
            fundamentalToHz = 100.0,
            tips = listOf(
                BandTip("重心", 50.0, 80.0, "低域の量。出しすぎると客席で膨らむ"),
                BandTip("箱鳴り", 200.0, 400.0, "こもりの原因。切ると輪郭が出る"),
                BandTip("アタック", 2_500.0, 5_000.0, "ビーターの当たる音。抜けを作る"),
            ),
        ),
        InstrumentBands(
            instrument = "スネア",
            fundamentalFromHz = 100.0,
            fundamentalToHz = 250.0,
            tips = listOf(
                BandTip("胴鳴り", 120.0, 250.0, "太さ。出しすぎると重くなる"),
                BandTip("ボックス感", 400.0, 800.0, "詰まった感じ。切ると抜ける"),
                BandTip("抜け", 3_000.0, 6_000.0, "スティックの当たり"),
                BandTip("スナッピー", 8_000.0, 12_000.0, "ざらつき・空気感"),
            ),
        ),
        InstrumentBands(
            instrument = "ハイハット・シンバル",
            fundamentalFromHz = 300.0,
            fundamentalToHz = 1_000.0,
            tips = listOf(
                BandTip("不要な低域", 20.0, 300.0, "ハイパスで切る。かぶりが減る"),
                BandTip("耳につく帯域", 2_000.0, 4_000.0, "うるさく感じたらここ"),
                BandTip("きらびやかさ", 10_000.0, 16_000.0, "上げすぎると耳が痛い"),
            ),
        ),
        InstrumentBands(
            instrument = "タム",
            fundamentalFromHz = 80.0,
            fundamentalToHz = 300.0,
            tips = listOf(
                BandTip("胴鳴り", 80.0, 200.0, "太さ"),
                BandTip("こもり", 300.0, 600.0, "切ると前に出る"),
                BandTip("アタック", 4_000.0, 6_000.0, "スティックの当たり"),
            ),
        ),
        InstrumentBands(
            instrument = "ベース",
            fundamentalFromHz = 40.0,
            fundamentalToHz = 400.0,
            tips = listOf(
                BandTip("重心", 60.0, 120.0, "キックと取り合いになる帯域"),
                BandTip("輪郭", 700.0, 1_200.0, "音程感。小さいスピーカーでも聞こえる"),
                BandTip("指・ピックの音", 2_000.0, 4_000.0, "アタックの明瞭さ"),
            ),
        ),
        InstrumentBands(
            instrument = "エレキギター",
            fundamentalFromHz = 80.0,
            fundamentalToHz = 1_200.0,
            tips = listOf(
                BandTip("不要な低域", 20.0, 100.0, "ハイパスで切るとベースと分離する"),
                BandTip("body", 200.0, 500.0, "厚み。多いとこもる"),
                BandTip("前に出る帯域", 1_500.0, 3_000.0, "抜け。ボーカルとぶつかりやすい"),
                BandTip("耳障り", 4_000.0, 6_000.0, "歪みのざらつき"),
            ),
        ),
        InstrumentBands(
            instrument = "アコースティックギター",
            fundamentalFromHz = 80.0,
            fundamentalToHz = 1_200.0,
            tips = listOf(
                BandTip("ボディの膨らみ", 100.0, 250.0, "ピエゾだと出すぎることが多い"),
                BandTip("箱鳴り", 300.0, 600.0, "切るとすっきりする"),
                BandTip("ピック・弦の音", 3_000.0, 6_000.0, "明瞭さ"),
                BandTip("空気感", 10_000.0, 15_000.0, "上げすぎるとピエゾ臭くなる"),
            ),
        ),
        InstrumentBands(
            instrument = "男性ボーカル",
            fundamentalFromHz = 85.0,
            fundamentalToHz = 350.0,
            tips = listOf(
                BandTip("ハイパス", 20.0, 80.0, "ハンドリングノイズと吹かれを切る"),
                BandTip("胸声・近接効果", 100.0, 250.0, "近づくと増える。多いとこもる"),
                BandTip("鼻づまり感", 500.0, 900.0, "切ると抜ける"),
                BandTip("明瞭度", 2_000.0, 4_000.0, "言葉の輪郭。ハウりやすい帯域でもある"),
                BandTip("歯擦音", 5_000.0, 8_000.0, "サ行のきつさ。ディエッサーの対象"),
            ),
        ),
        InstrumentBands(
            instrument = "女性ボーカル",
            fundamentalFromHz = 165.0,
            fundamentalToHz = 700.0,
            tips = listOf(
                BandTip("ハイパス", 20.0, 100.0, "吹かれ対策"),
                BandTip("厚み", 200.0, 400.0, "薄いと感じたらここ"),
                BandTip("明瞭度", 2_500.0, 5_000.0, "言葉の輪郭"),
                BandTip("歯擦音", 6_000.0, 10_000.0, "サ行のきつさ"),
            ),
        ),
        InstrumentBands(
            instrument = "ホーン（サックス・トランペット）",
            fundamentalFromHz = 120.0,
            fundamentalToHz = 1_200.0,
            tips = listOf(
                BandTip("太さ", 200.0, 500.0, "痩せていたらここ"),
                BandTip("鳴り", 1_000.0, 2_000.0, "前に出る帯域"),
                BandTip("きつさ", 3_000.0, 6_000.0, "大音量で耳につく"),
            ),
        ),
        InstrumentBands(
            instrument = "ピアノ",
            fundamentalFromHz = 27.5,
            fundamentalToHz = 4_186.0,
            tips = listOf(
                BandTip("低域の膨らみ", 100.0, 250.0, "バンドでは切ることが多い"),
                BandTip("こもり", 300.0, 600.0, "抜けが悪いときに切る"),
                BandTip("アタック", 3_000.0, 6_000.0, "ハンマーの当たり"),
            ),
        ),
    )

    /** その周波数を含む楽器の帯域を探す。RTA で暴れている帯域から当たりをつける用。 */
    fun tipsAt(frequencyHz: Double): List<Pair<InstrumentBands, BandTip>> =
        ALL.flatMap { instrument ->
            instrument.tips
                .filter { frequencyHz >= it.fromHz && frequencyHz <= it.toHz }
                .map { instrument to it }
        }
}
