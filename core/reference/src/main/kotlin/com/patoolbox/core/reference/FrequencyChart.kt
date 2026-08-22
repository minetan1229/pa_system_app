package com.patoolbox.core.reference

import kotlin.math.sqrt

/**
 * その帯域に対して普通どちら向きに動かすか。
 *
 * 色とラベルで出し分けるために持たせている。「触ると何か変わる帯域」を並べても
 * 初めての人は上げるのか削るのか分からず、結果として全部上げてしまう。
 */
enum class BandAction(val label: String) {
    /** 削るのが基本。上げても良くならない帯域 */
    CUT("削る"),

    /** 足したいときに触る帯域 */
    BOOST("上げる"),

    /** どちらもあり得る。まず聴いて決める */
    EITHER("聴いて決める"),

    /** ハウリング・耳への刺激。上げてはいけない */
    WATCH("触るなら下げる"),
}

/** 帯域ごとの効き方。EQ を触る前の当たりをつけるために使う。 */
data class BandTip(
    val label: String,
    val fromHz: Double,
    val toHz: Double,
    /** その帯域を動かすと何が変わるか */
    val effect: String,
    /**
     * ワンアドバイス。迷ったときにまず試す一手。
     *
     * 「ここが効く」だけでは卓の前で手が止まるので、**中心周波数・Q・量**まで書く。
     * 数字が入っていない助言は現場では使えない。
     */
    val advice: String,
    val action: BandAction,
) {
    /** 帯域の中心（対数軸上）。図の上に点を置くときに使う */
    val centerHz: Double get() = sqrt(fromHz * toHz)
}

/** 楽器の大分類。数が増えたので、探すときはここで絞る。 */
enum class InstrumentGroup(val label: String, val description: String) {
    DRUMS("ドラム", "打面ごとに帯域が違う。ひとまとめに作ると全部こもる"),
    BASS("低音", "キックとの住み分けで決まる"),
    GUITAR("ギター", "ボーカルと同じ帯域を取り合う"),
    VOCAL("ボーカル・声", "最優先で通す帯域。他の楽器がここを避ける"),
    KEYS("鍵盤", "帯域が広いので、削って他の場所を空ける"),
    WIND_STRINGS("管・弦", "生音が大きい。かぶりの扱いが本題になる"),
    PERCUSSION("パーカッション・和楽器", "アタックと胴鳴りの2点で決まる"),
    PLAYBACK("再生・その他", "完成した音が来る。足すより整える"),
}

/**
 * 楽器1つぶんの帯域と作り方。
 *
 * 帯域だけを並べても卓の前では足りない。同じ 200Hz でも、
 * マイクの位置を 3cm 動かせば EQ を触らずに済むことがある。
 * だから「どこにマイクを置くか」「何とぶつかるか」まで1枚に載せている。
 */
data class InstrumentBands(
    val instrument: String,
    val group: InstrumentGroup,
    /** アンサンブルの中でこの楽器が担う役割。1行 */
    val role: String,
    /** 基音のおおよその範囲 */
    val fundamentalFromHz: Double,
    val fundamentalToHz: Double,
    /** まず入れておくハイパスの目安。null は「入れない方が良い」 */
    val highPassHz: Int?,
    /** マイキング。EQ より先に効く */
    val micTip: String,
    /** コンプ・ゲート・ディエッサーの入口の値 */
    val dynamicsTip: String,
    /** ぶつかる相手と、どちらをどう逃がすか */
    val conflicts: List<String>,
    /** 現場でやりがちな失敗 */
    val pitfalls: List<String>,
    val tips: List<BandTip>,
) {
    /** 検索用。楽器名だけでは「ざらつき」「ピエゾ」のような引き方に当たらない */
    val searchText: String
        get() = buildString {
            append(instrument).append(' ')
            append(group.label).append(' ')
            append(role).append(' ')
            append(micTip).append(' ')
            append(dynamicsTip).append(' ')
            conflicts.forEach { append(it).append(' ') }
            pitfalls.forEach { append(it).append(' ') }
            tips.forEach { append(it.label).append(' ').append(it.effect).append(' ').append(it.advice).append(' ') }
        }.lowercase()
}

/**
 * 楽器ごとの帯域チャート。
 *
 * 「ここを上げれば良くなる」ではなく「ここを触ると何が変わるか」を書いている。
 * 実際にどう転ぶかは会場と演者で変わるので、断定するとかえって邪魔になる。
 * ただし **最初の一手だけは数字で書く**。方向が分からないまま
 * つまみを回すのが一番音を悪くするので、出発点は与える。
 */
object FrequencyChart {

    val ALL: List<InstrumentBands> = listOf(

        // ------------------------------------------------------------------
        // ドラム
        // ------------------------------------------------------------------

        InstrumentBands(
            instrument = "キック（バスドラム）",
            group = InstrumentGroup.DRUMS,
            role = "曲の芯。客席で感じる低域のほとんどはこれとベースで決まる",
            fundamentalFromHz = 40.0,
            fundamentalToHz = 100.0,
            highPassHz = 30,
            micTip = "打面の穴から 5〜10cm 内側、ビーターの当たる点に向ける。" +
                "奥に入れるほどアタックが増えて低域が減る。胴の外に出すほど low が増えるが箱鳴りも増える",
            dynamicsTip = "ゲートは スレッショルド -30dB / ホールド 40ms / リリース 150ms から。" +
                "リリースを短くすると余韻が切れて安いドラムマシンの音になる。" +
                "コンプは 4:1 / アタック 10ms / リリース 100ms で 3dB 程度",
            conflicts = listOf(
                "ベースと 60〜120Hz で正面衝突する。キックを 60Hz、ベースを 90〜100Hz に振り分ける",
                "フロアタムの 80Hz とかぶる。フロアタム側を削って場所を譲る",
            ),
            pitfalls = listOf(
                "低域が足りないときに 60Hz を上げると、客席後方だけが膨らむ。まず 300Hz を削る",
                "サブロー（40Hz以下）を出すとステージが揺れて、他のマイク全部にかぶりが乗る",
            ),
            tips = listOf(
                BandTip(
                    "床鳴り・風", 20.0, 35.0,
                    "音楽的な成分は入っていない。床の振動とビーターの風だけ",
                    "35Hz で 12dB/oct のハイパス。切っても客席の量感は減らない",
                    BandAction.CUT,
                ),
                BandTip(
                    "重心", 50.0, 80.0,
                    "低域の量。出しすぎると客席で膨らんで曲が重くなる",
                    "60Hz を Q1.4 で +2dB まで。3dB 以上上げたくなったら箱鳴りを削る方が先",
                    BandAction.BOOST,
                ),
                BandTip(
                    "箱鳴り・段ボール感", 200.0, 400.0,
                    "こもりの原因。切ると輪郭が出て、音量を上げずに前に出る",
                    "300Hz を Q2.0 で -4dB から。-6dB を超えると痩せるので、そこで止める",
                    BandAction.CUT,
                ),
                BandTip(
                    "アタック", 2_500.0, 5_000.0,
                    "ビーターが当たる音。客席で「ドスッ」と聞こえるかはここで決まる",
                    "4kHz を Q1.0 で +3dB。PA を通すと最初に失われるのがこの帯域",
                    BandAction.BOOST,
                ),
                BandTip(
                    "シンバルのかぶり", 8_000.0, 16_000.0,
                    "キックのマイクに乗ったハイハットとシンバル。ゲートが誤動作する",
                    "10kHz から上をシェルフで -6dB。キックに高域は要らない",
                    BandAction.CUT,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "スネア（トップ）",
            group = InstrumentGroup.DRUMS,
            role = "曲のタイムを決める音。客席の手拍子はこれに合う",
            fundamentalFromHz = 100.0,
            fundamentalToHz = 250.0,
            highPassHz = 80,
            micTip = "リムから 2〜3cm 内側、打面の 1〜2cm 上。ハイハットに背を向ける角度で置く。" +
                "中心に近づけるほど太くなるが、スティックのアタックが減る",
            dynamicsTip = "コンプは 4:1 / アタック 5〜10ms / リリース 80ms で 4dB。" +
                "アタックを 1ms まで詰めると芯が消えて「パスッ」になる。ゲートは -25dB / ホールド 30ms",
            conflicts = listOf(
                "ボーカルの 200〜250Hz とかぶる。スネアを削る側にする",
                "ハイタムと 150〜200Hz が近い。片方を 180Hz で -3dB して分ける",
            ),
            pitfalls = listOf(
                "抜けが足りないときに 3kHz を上げると、ハイハットのかぶりも一緒に上がる。まずマイクの角度を直す",
                "ゲートをきつく掛けるとロールとゴーストノートが全部消える",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 80.0,
                    "キックのかぶりと床の振動。スネアの音は入っていない",
                    "80Hz でハイパス。ここを残すとドラム全体が濁る",
                    BandAction.CUT,
                ),
                BandTip(
                    "胴鳴り・太さ", 120.0, 250.0,
                    "音の太さ。出しすぎると重くなり、曲のテンポが遅く聞こえる",
                    "180Hz を Q1.4 で ±3dB。細いと感じたら上げる前に打面を見る（緩んでいることが多い）",
                    BandAction.EITHER,
                ),
                BandTip(
                    "ボックス感・詰まり", 400.0, 800.0,
                    "箱に入ったような詰まった感じ。ここを切ると音量を上げずに抜ける",
                    "500Hz を Q2.0 で -3dB。それでも詰まるなら 700Hz も同量",
                    BandAction.CUT,
                ),
                BandTip(
                    "スティックの当たり", 3_000.0, 6_000.0,
                    "抜け。客席後方まで届くかはこの帯域で決まる",
                    "4kHz を Q1.0 で +3dB。上げすぎるとハイハットのかぶりが目立つ",
                    BandAction.BOOST,
                ),
                BandTip(
                    "空気感・ざらつき", 8_000.0, 12_000.0,
                    "スナッピーのざらつきと胴の空気感",
                    "10kHz をシェルフで +2dB。ここは 3dB を超えると耳が疲れる",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "スネア（ボトム）",
            group = InstrumentGroup.DRUMS,
            role = "スナッピーの「ジャッ」だけを足すマイク。単体では使わない",
            fundamentalFromHz = 200.0,
            fundamentalToHz = 800.0,
            highPassHz = 200,
            micTip = "裏面のスナッピーに 3〜5cm。**極性を反転する**。" +
                "トップと向かい合うので、反転しないと胴鳴りが打ち消される",
            dynamicsTip = "トップのゲートに連動させる（キーイン）。単独でゲートを掛けると" +
                "トップと開くタイミングがずれて「ジャッ」が遅れて聞こえる",
            conflicts = listOf(
                "トップとの合成でコムフィルタが出る。極性反転とフェーダー量で確認する",
                "ハイハットのかぶりが最も多いマイク。使わない曲では落としておく",
            ),
            pitfalls = listOf(
                "極性反転を忘れると、上げるほどスネアが痩せる。原因が分からず EQ を触りがちな典型",
                "トップと同じ量まで上げると、ざらつきだけの薄い音になる",
            ),
            tips = listOf(
                BandTip(
                    "胴鳴り（不要）", 20.0, 200.0,
                    "トップと打ち消し合う帯域。このマイクには要らない",
                    "200Hz で 12dB/oct のハイパス。太さはトップから取る",
                    BandAction.CUT,
                ),
                BandTip(
                    "ジャッという芯", 2_000.0, 4_000.0,
                    "スナッピーが鳴っている実体",
                    "3kHz を Q1.4 で +2dB。これ以上はノイズが増えるだけ",
                    BandAction.BOOST,
                ),
                BandTip(
                    "ざらつき", 6_000.0, 14_000.0,
                    "このマイクの主役。トップに足すのはここだけ",
                    "8kHz をシェルフで +4dB。上げても耳につかないのはトップより量が少ないため",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "ハイタム",
            group = InstrumentGroup.DRUMS,
            role = "フィルの入口。スネアより高く、ロータムへ降りていく音",
            fundamentalFromHz = 150.0,
            fundamentalToHz = 300.0,
            highPassHz = 100,
            micTip = "リムから 2cm 内側、打面の 2〜3cm 上。ヘッドの中心を狙わない" +
                "（中心は倍音が少なく「ボコッ」になる）。シンバルに背を向ける角度で",
            dynamicsTip = "ゲートは -28dB / ホールド 60ms / リリース 250ms。" +
                "3点のタムで同じ設定を使い回すと、ハイタムだけ余韻が切れる（音が高く減衰が速いため）",
            conflicts = listOf(
                "スネアの胴鳴り 150〜250Hz と重なる。ハイタムを 200Hz で -2dB して分ける",
                "エレキギターの body 200〜500Hz とぶつかる。フィルのときだけ勝てば良いので、ギター側は触らない",
            ),
            pitfalls = listOf(
                "3点まとめて同じ EQ を入れると、ハイタムだけ低域が足りず「カン」と鳴る",
                "ゲートを閉め気味にすると、ロールの粒が抜ける",
            ),
            tips = listOf(
                BandTip(
                    "キックのかぶり", 20.0, 100.0,
                    "タムの音ではない。ドラム全体の濁りの原因",
                    "100Hz でハイパス。ハイタムは 150Hz より下に基音を持たない",
                    BandAction.CUT,
                ),
                BandTip(
                    "胴鳴り・太さ", 180.0, 300.0,
                    "タムらしい太さ。ここが痩せるとオモチャの音になる",
                    "220Hz を Q1.4 で +2dB。ロータムより高めを狙うのがコツ",
                    BandAction.BOOST,
                ),
                BandTip(
                    "こもり", 400.0, 700.0,
                    "抜けの悪さ。切ると前に出る",
                    "500Hz を Q2.0 で -4dB。フィルが埋もれるときはここ",
                    BandAction.CUT,
                ),
                BandTip(
                    "アタック", 3_000.0, 6_000.0,
                    "スティックの当たり。粒が立つかどうか",
                    "5kHz を Q1.0 で +3dB。ロータムより高い周波数を選ぶ",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "ロータム",
            group = InstrumentGroup.DRUMS,
            role = "フィルの中間。ハイタムとフロアタムの橋渡し",
            fundamentalFromHz = 100.0,
            fundamentalToHz = 200.0,
            highPassHz = 70,
            micTip = "ハイタムと同じ角度・同じ距離で揃える。" +
                "距離が違うとフィルを回したときに音量と音色が段でずれる",
            dynamicsTip = "ゲートは -28dB / ホールド 80ms / リリース 350ms。" +
                "ハイタムよりホールドとリリースを伸ばす。同じ値だと余韻が足りない",
            conflicts = listOf(
                "ベースの輪郭 700Hz〜とは離れているので競合しない。ぶつかるのは 150Hz 付近のキック",
                "ハイタムと 180〜200Hz で重なる。ロータムは 140Hz 側、ハイタムは 220Hz 側に振る",
            ),
            pitfalls = listOf(
                "ハイタムのコピー設定を入れると 220Hz が過剰になり、フィルの真ん中だけ膨らむ",
                "ハイパスを 100Hz に上げると基音が削れて薄くなる。70Hz までにする",
            ),
            tips = listOf(
                BandTip(
                    "キックのかぶり", 20.0, 70.0,
                    "ロータムの基音より下。残す意味がない",
                    "70Hz でハイパス。ハイタムより低く設定するのを忘れやすい",
                    BandAction.CUT,
                ),
                BandTip(
                    "胴鳴り・太さ", 120.0, 220.0,
                    "音の重心。フィルが降りていく感じはここで作る",
                    "150Hz を Q1.4 で +2dB。ハイタムより 70Hz ほど低い点を選ぶ",
                    BandAction.BOOST,
                ),
                BandTip(
                    "こもり", 350.0, 600.0,
                    "抜けの悪さ",
                    "450Hz を Q2.0 で -4dB。ハイタムより少し低い点",
                    BandAction.CUT,
                ),
                BandTip(
                    "アタック", 2_500.0, 5_000.0,
                    "スティックの当たり",
                    "4kHz を Q1.0 で +3dB。ハイタムより低い点にすると3点が階段になる",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "フロアタム",
            group = InstrumentGroup.DRUMS,
            role = "フィルの着地点。低い胴鳴りで曲の展開を作る",
            fundamentalFromHz = 60.0,
            fundamentalToHz = 150.0,
            highPassHz = 50,
            micTip = "リムから 3cm 内側、打面の 3〜5cm 上。" +
                "上から離すほど胴鳴りが増える。床に近いので、床の反射でコムフィルタが出やすい",
            dynamicsTip = "ゲートは -30dB / ホールド 120ms / リリース 600ms。" +
                "ここを短くすると「ドーン」が「ドッ」になる。3点でいちばん長く開けておく",
            conflicts = listOf(
                "キックの 60〜80Hz と正面衝突する。フロアタムを 80Hz で -3dB して譲る",
                "ベースの重心 60〜120Hz とも重なる。フィルの一瞬だけなので、ベース側は触らない",
            ),
            pitfalls = listOf(
                "低域を足すとキックが濁る。太さは 100Hz、量ではなく余韻の長さで作る",
                "ハイパスを 80Hz に入れると基音が消える。50Hz までにとどめる",
            ),
            tips = listOf(
                BandTip(
                    "床の振動", 20.0, 50.0,
                    "ステージの振動が乗るだけの帯域",
                    "50Hz でハイパス。フロアタムの基音は 60Hz より上にある",
                    BandAction.CUT,
                ),
                BandTip(
                    "重心・胴鳴り", 70.0, 150.0,
                    "「ドーン」の量。上げすぎるとキックと取り合いになる",
                    "100Hz を Q1.4 で +2dB。キックと同じ 60Hz は狙わない",
                    BandAction.BOOST,
                ),
                BandTip(
                    "こもり", 300.0, 500.0,
                    "モコモコした感じ",
                    "400Hz を Q2.0 で -4dB。3点で最も削って良い帯域",
                    BandAction.CUT,
                ),
                BandTip(
                    "アタック", 2_000.0, 4_000.0,
                    "スティックの当たり。低い太鼓ほど輪郭が必要",
                    "3kHz を Q1.0 で +3dB。これが無いと客席で「ボワッ」としか聞こえない",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "ハイハット",
            group = InstrumentGroup.DRUMS,
            role = "曲の細かい刻み。上げなくても聞こえるので、下げる方に手が伸びる",
            fundamentalFromHz = 300.0,
            fundamentalToHz = 1_000.0,
            highPassHz = 300,
            micTip = "エッジではなくベルとエッジの中間、上から 10cm。" +
                "エッジを狙うとシンバルの風切り音（チャフ）が乗る。スネアに背を向ける",
            dynamicsTip = "コンプは掛けない。掛けるとかぶりの音量が上がってドラム全体が近くなる。" +
                "ゲートも使わない（開閉が耳につく）",
            conflicts = listOf(
                "スネアマイクへのかぶりが最大の問題。EQ ではなくマイクの角度で減らす",
                "ボーカルの歯擦音 6〜8kHz と重なる。ハイハットを 7kHz で -2dB してボーカルを通す",
            ),
            pitfalls = listOf(
                "客席では生音が十分に聞こえていることが多い。上げる前に PA を止めて確かめる",
                "10kHz を上げると耳が痛くなるが、しばらく聞くと慣れて更に上げてしまう",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域とかぶり", 20.0, 300.0,
                    "キック・スネア・タムのかぶり。ハイハットの音は入っていない",
                    "300Hz で 12dB/oct のハイパス。ここを切るだけでドラムが整理される",
                    BandAction.CUT,
                ),
                BandTip(
                    "こもり・ぼやけ", 400.0, 900.0,
                    "刻みが不明瞭になる帯域",
                    "600Hz を Q2.0 で -3dB。細かいリズムが見えてくる",
                    BandAction.CUT,
                ),
                BandTip(
                    "耳につく帯域", 2_000.0, 4_000.0,
                    "「うるさい」と感じたときの正体はほぼここ。刺激だけで情報は少ない",
                    "3kHz を Q2.0 で -3dB。音量を下げるより先にここを削る",
                    BandAction.WATCH,
                ),
                BandTip(
                    "きらびやかさ", 10_000.0, 16_000.0,
                    "空気感。上げすぎると耳が痛くなり、長い本番で聞き疲れる",
                    "12kHz をシェルフで +2dB まで。屋外では +3dB まで許容",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "オーバーヘッド（シンバル・全体）",
            group = InstrumentGroup.DRUMS,
            role = "ドラムを1つの楽器としてまとめる。個別マイクはここに足す考え方",
            fundamentalFromHz = 200.0,
            fundamentalToHz = 2_000.0,
            highPassHz = 200,
            micTip = "スネアの中心から左右のマイクまでの距離を**巻き尺で合わせる**。" +
                "揃っていないとスネアが中央に来ない。2本の間隔は片方からスネアまでの距離の1.4倍以内",
            dynamicsTip = "コンプは 2:1 / アタック 30ms で 2〜3dB。強く掛けるとシンバルが" +
                "止まったときに他の楽器が浮き上がる（ポンピング）",
            conflicts = listOf(
                "全部のドラムが入っているので、個別マイクとの位相が問題になる。極性を1本ずつ確かめる",
                "ボーカルマイクへのかぶりを増やす主因でもある。ハウリングの余裕はここで削られる",
            ),
            pitfalls = listOf(
                "スネアが左右にずれているのに EQ で直そうとする。原因はマイクの距離差",
                "低域を残すとキックが2重に聞こえる。200Hz より下は要らない",
            ),
            tips = listOf(
                BandTip(
                    "キックのかぶり", 20.0, 200.0,
                    "キックマイクと時間差で重なり、輪郭を消す",
                    "200Hz でハイパス。ドラム全体の低域はキックとフロアタムに任せる",
                    BandAction.CUT,
                ),
                BandTip(
                    "こもり", 300.0, 800.0,
                    "タムとシンバルの中間で濁る帯域",
                    "500Hz を Q2.0 で -3dB。ドラムセットの見通しが良くなる",
                    BandAction.CUT,
                ),
                BandTip(
                    "刺激・チャフ", 3_000.0, 6_000.0,
                    "シンバルの風切り音。うるさく感じる帯域",
                    "4kHz を Q1.4 で -3dB。ここを削ると音量を上げられる",
                    BandAction.WATCH,
                ),
                BandTip(
                    "空気感", 8_000.0, 16_000.0,
                    "ドラムが「その場にある」感じ",
                    "10kHz をシェルフで +2dB。ここだけでドラム全体が新しく聞こえる",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "ライドシンバル",
            group = InstrumentGroup.DRUMS,
            role = "ベルとカップの刻み。オーバーヘッドだけでは足りないときに立てる",
            fundamentalFromHz = 300.0,
            fundamentalToHz = 1_200.0,
            highPassHz = 300,
            micTip = "ベルから 10〜15cm、カップの内側寄り。真上から狙うと" +
                "サスティンばかりで刻みが見えない",
            dynamicsTip = "コンプもゲートも掛けない。サスティンが長いので、" +
                "ゲートは必ず途中で閉じて不自然になる",
            conflicts = listOf(
                "オーバーヘッドと同じ音を拾うので、位相で音色が変わる。フェーダーを上げながら確認する",
                "ハイハットと帯域がほぼ同じ。両方上げると高域が過剰になる",
            ),
            pitfalls = listOf(
                "ベルの「カン」を出そうとして 4kHz を上げると、シンバル全体がうるさくなる",
                "サスティンが長いので、上げると曲の隙間が埋まって曲が重くなる",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 300.0,
                    "タムとキックのかぶり",
                    "300Hz でハイパス",
                    BandAction.CUT,
                ),
                BandTip(
                    "ベルの芯", 1_500.0, 3_000.0,
                    "「カン」という刻みの実体",
                    "2kHz を Q1.4 で +2dB。刻みが見えないときはここ",
                    BandAction.BOOST,
                ),
                BandTip(
                    "うるさい帯域", 4_000.0, 7_000.0,
                    "サスティンの刺激。長く聞くと疲れる",
                    "5kHz を Q2.0 で -3dB",
                    BandAction.WATCH,
                ),
                BandTip(
                    "きらめき", 10_000.0, 16_000.0,
                    "余韻の質感",
                    "12kHz をシェルフで +2dB",
                    BandAction.BOOST,
                ),
            ),
        ),

        // ------------------------------------------------------------------
        // 低音
        // ------------------------------------------------------------------

        InstrumentBands(
            instrument = "エレキベース",
            group = InstrumentGroup.BASS,
            role = "曲の土台と音程。キックと役割を分けるのがすべて",
            fundamentalFromHz = 40.0,
            fundamentalToHz = 400.0,
            highPassHz = 40,
            micTip = "DI が基本。アンプも録るなら、DI とマイクの**極性と時間差**を必ず確認する" +
                "（数cmの差で 200Hz 付近が消える）。マイクはコーンの中心から 5cm 外し",
            dynamicsTip = "コンプは 4:1 / アタック 20ms / リリース 150ms で 4〜6dB。" +
                "ベースはコンプで音量を揃えると客席での安定感が大きく変わる。" +
                "スラップがある曲だけリミッターを -6dBFS に置く",
            conflicts = listOf(
                "キックと 60〜120Hz を取り合う。キック 60Hz / ベース 90〜100Hz に分ける",
                "小型スピーカーでは低域が再生されない。700Hz〜1.2kHz の輪郭で音程を聞かせる",
            ),
            pitfalls = listOf(
                "低域を足すと客席で膨らむだけで音程は見えない。輪郭は 800Hz で作る",
                "アンプの音がステージで大きいと、卓のフェーダーを下げても客席の低域は減らない",
            ),
            tips = listOf(
                BandTip(
                    "不要な最低域", 20.0, 40.0,
                    "4弦ベースの最低音（E＝41Hz）より下。ノイズと振動だけ",
                    "40Hz でハイパス。5弦なら 30Hz に下げる（B＝31Hz）",
                    BandAction.CUT,
                ),
                BandTip(
                    "重心", 60.0, 120.0,
                    "低域の量。キックと取り合いになる帯域",
                    "90Hz を Q1.4 で +2dB。キックが 60Hz なら 100Hz を選ぶ",
                    BandAction.BOOST,
                ),
                BandTip(
                    "こもり", 200.0, 400.0,
                    "モコモコする帯域。ここが多いと音程が消える",
                    "250Hz を Q2.0 で -3dB。ベースが埋もれる原因の半分はここ",
                    BandAction.CUT,
                ),
                BandTip(
                    "輪郭・音程感", 700.0, 1_200.0,
                    "音程が見える帯域。小さいスピーカーでもここは鳴る",
                    "800Hz を Q1.4 で +3dB。「ベースが聞こえない」と言われたらまずここ",
                    BandAction.BOOST,
                ),
                BandTip(
                    "指・ピックの音", 2_000.0, 4_000.0,
                    "アタックの明瞭さ。スラップの「バキ」もここ",
                    "2.5kHz を Q1.4 で +2dB。指弾きの曲では上げない方が良いことも多い",
                    BandAction.EITHER,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "ウッドベース（コントラバス）",
            group = InstrumentGroup.BASS,
            role = "生音の低音。ハウリングとかぶりが本題になる",
            fundamentalFromHz = 40.0,
            fundamentalToHz = 300.0,
            highPassHz = 40,
            micTip = "ピエゾ（ピックアップ）と、駒の脇 10〜20cm のマイクを混ぜる。" +
                "ピエゾは輪郭、マイクは胴鳴りを担当させる。f 孔の正面は低域が過剰になる",
            dynamicsTip = "コンプは 3:1 / アタック 30ms でごく浅く 2〜3dB。" +
                "強く掛けると弓の立ち上がりが潰れて、ジャズでは使えない音になる",
            conflicts = listOf(
                "ハウリングの起点になりやすい。100〜250Hz にモードが立つので GEQ でノッチする",
                "ドラムのかぶりが胴に入る。ステージ位置をドラムから離す方が EQ より効く",
            ),
            pitfalls = listOf(
                "ピエゾだけで作ると「ボコボコ」した音になる。1〜3kHz が痩せている",
                "低域を上げるとステージ上でハウリングし、そこから先は何も上げられなくなる",
            ),
            tips = listOf(
                BandTip(
                    "不要な最低域", 20.0, 40.0,
                    "ステージの振動が乗るだけ",
                    "40Hz でハイパス。低音楽器でもここは切って良い",
                    BandAction.CUT,
                ),
                BandTip(
                    "胴鳴り", 80.0, 200.0,
                    "楽器の量感。ハウリングもここから始まる",
                    "120Hz を Q1.4 で ±2dB。ハウるなら Q4.0 で -4dB のノッチに切り替える",
                    BandAction.EITHER,
                ),
                BandTip(
                    "ボコボコ感", 250.0, 500.0,
                    "ピエゾ特有の詰まり",
                    "350Hz を Q2.0 で -4dB。ピエゾを使うなら必ず触る帯域",
                    BandAction.CUT,
                ),
                BandTip(
                    "指と弦の音", 1_000.0, 3_000.0,
                    "音程と発音が見える帯域",
                    "1.5kHz を Q1.4 で +3dB。ピエゾはここが足りない",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "シンセベース",
            group = InstrumentGroup.BASS,
            role = "完成した低音が来る。足すのではなく、他と衝突しないよう削る",
            fundamentalFromHz = 30.0,
            fundamentalToHz = 300.0,
            highPassHz = 30,
            micTip = "ライン受け。バランス出力が無い機材はDIを通す。" +
                "ステレオで来ても低域は片チャンネルに寄せて確認する（打ち消しが起きていないか）",
            dynamicsTip = "リミッターだけ。-6dBFS に置く。コンプで整えるとシンセ側の" +
                "エンベロープが崩れて、作った人の意図が消える",
            conflicts = listOf(
                "サブロー（30〜50Hz）がサブウーファーの上限とぶつかる。会場のシステム上限を先に確認する",
                "キックと完全に同じ帯域を占めることがある。どちらかをサイドチェインで沈ませる",
            ),
            pitfalls = listOf(
                "30Hz が入っていてもスピーカーが出せず、アンプの余力だけ食う",
                "ステレオの低域は打ち消しが起きる。モノで確認しないと客席で消える",
            ),
            tips = listOf(
                BandTip(
                    "サブロー", 30.0, 50.0,
                    "体で感じる帯域。会場の再生能力次第で無駄になる",
                    "システムの下限が 45Hz なら 40Hz でハイパス。出ない帯域は切る",
                    BandAction.CUT,
                ),
                BandTip(
                    "重心", 60.0, 120.0,
                    "量感の本体",
                    "80Hz を Q1.4 で ±2dB。ここはシンセ側の音作りを尊重する",
                    BandAction.EITHER,
                ),
                BandTip(
                    "濁り", 200.0, 400.0,
                    "他の楽器と最もぶつかる帯域",
                    "300Hz を Q2.0 で -3dB。ボーカルの通りが良くなる",
                    BandAction.CUT,
                ),
                BandTip(
                    "輪郭", 800.0, 2_000.0,
                    "小型スピーカーで音程を伝える帯域",
                    "1kHz を Q1.4 で +2dB",
                    BandAction.BOOST,
                ),
            ),
        ),

        // ------------------------------------------------------------------
        // ギター
        // ------------------------------------------------------------------

        InstrumentBands(
            instrument = "エレキギター（クリーン・クランチ）",
            group = InstrumentGroup.GUITAR,
            role = "中域のコード感。ボーカルの帯域を取り合う相手",
            fundamentalFromHz = 80.0,
            fundamentalToHz = 1_200.0,
            highPassHz = 100,
            micTip = "コーンのセンターから 2〜3cm 外し、グリルに 2〜5cm。" +
                "センター真正面は高域が刺さり、外に行くほど丸くなる。**5cm 動かす方が EQ より効く**",
            dynamicsTip = "コンプは掛けないか 2:1 で 2dB まで。ギタリスト側で" +
                "既に掛かっていることが多く、二重に掛けるとピッキングの表情が消える",
            conflicts = listOf(
                "ボーカルの 1.5〜3kHz と正面衝突する。ギター側を 2.5kHz で -3dB する",
                "ベースの 200〜400Hz と重なる。ギターを 150Hz でハイパスして譲る",
            ),
            pitfalls = listOf(
                "アンプの音がステージで大きいと卓では何も制御できない。まず音量を交渉する",
                "低域を残すとベースが見えなくなる。ギターに 100Hz 以下は要らない",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 100.0,
                    "アンプの箱鳴りとステージの振動。ベースと衝突する",
                    "100Hz でハイパス。切ってもギターは痩せない",
                    BandAction.CUT,
                ),
                BandTip(
                    "body・厚み", 200.0, 500.0,
                    "音の厚み。多いとこもり、少ないと薄くなる",
                    "300Hz を Q1.4 で -2dB から。バンドで埋もれるときは削る方が効く",
                    BandAction.EITHER,
                ),
                BandTip(
                    "前に出る帯域", 1_500.0, 3_000.0,
                    "抜け。ここはボーカルの明瞭度と同じ場所",
                    "2.5kHz を Q1.4 で -3dB。ボーカルが聞こえないときは、まずギターのここを削る",
                    BandAction.WATCH,
                ),
                BandTip(
                    "ざらつき・刺さり", 4_000.0, 6_000.0,
                    "耳に刺さる帯域。歪みが強いほど増える",
                    "5kHz を Q2.0 で -3dB",
                    BandAction.CUT,
                ),
                BandTip(
                    "空気感", 8_000.0, 12_000.0,
                    "きらめき。クリーンのアルペジオで効く",
                    "10kHz をシェルフで +2dB。歪んでいる音では上げない（ノイズが増える）",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "エレキギター（ハイゲイン・歪み）",
            group = InstrumentGroup.GUITAR,
            role = "壁のような中域。上げなくても聞こえるので、削って場所を作る",
            fundamentalFromHz = 80.0,
            fundamentalToHz = 1_200.0,
            highPassHz = 120,
            micTip = "クリーンより外側（センターから 4〜5cm 外し）。" +
                "歪みは高域が過剰なので、外すだけで扱いやすくなる。2本使うなら位相を必ず確認",
            dynamicsTip = "コンプは不要。歪みそのものが最大のコンプになっている。" +
                "リミッターだけ -6dBFS に置いてピークを止める",
            conflicts = listOf(
                "ボーカルを完全に覆う。2〜4kHz を -4dB 削るのが前提",
                "2本のギターが同じ帯域だと団子になる。片方を 800Hz、片方を 2kHz で分ける",
            ),
            pitfalls = listOf(
                "「厚みが足りない」と言われて低域を足すと、客席では濁るだけ。厚みは 800Hz 付近",
                "歪みの音は生音がステージに漏れているので、卓で下げても客席で減らない",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 120.0,
                    "ベースの居場所。歪みギターの基音より下",
                    "120Hz でハイパス。クリーンより高く設定する",
                    BandAction.CUT,
                ),
                BandTip(
                    "こもり", 250.0, 500.0,
                    "壁のように詰まる帯域",
                    "400Hz を Q2.0 で -4dB。歪み2本のときは必ず削る",
                    BandAction.CUT,
                ),
                BandTip(
                    "厚み", 700.0, 1_000.0,
                    "歪みギターの「太さ」の実体。低域ではなくここ",
                    "800Hz を Q1.4 で +2dB",
                    BandAction.BOOST,
                ),
                BandTip(
                    "ボーカルとの衝突", 2_000.0, 4_000.0,
                    "ボーカルの明瞭度と同じ帯域を埋める",
                    "3kHz を Q1.4 で -4dB。歌モノなら最初から削っておく",
                    BandAction.WATCH,
                ),
                BandTip(
                    "ジリジリ", 5_000.0, 8_000.0,
                    "歪みのノイズ成分。情報は無く刺激だけ",
                    "6kHz を Q2.0 で -4dB。もしくは 8kHz からシェルフで落とす",
                    BandAction.CUT,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "アコースティックギター（ピエゾ）",
            group = InstrumentGroup.GUITAR,
            role = "コードと刻み。ピエゾ特有の癖を取るのが仕事",
            fundamentalFromHz = 80.0,
            fundamentalToHz = 1_200.0,
            highPassHz = 90,
            micTip = "ライブではピエゾ（DI）が基本。マイクを足すなら 12〜14フレット付近に 15cm。" +
                "サウンドホール正面は 100〜250Hz が過剰でハウリングの原因になる",
            dynamicsTip = "コンプは 3:1 / アタック 15ms で 3dB。" +
                "ストロークの粒が揃う。掛けすぎるとピックのアタックが消える",
            conflicts = listOf(
                "ボーカルとの 2〜4kHz。弾き語りではギターを -2dB 削る",
                "ハイハットの 8kHz 以上と重なる。両方上げると高域が過剰になる",
            ),
            pitfalls = listOf(
                "ピエゾ臭さを取るために高域を切ると、こんどは曇る。原因は 2〜3kHz の硬さ",
                "サウンドホール由来の 100〜200Hz でハウる。ハイパスだけでは止まらないのでノッチする",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 90.0,
                    "6弦の基音（E＝82Hz）より下。ハンドリングノイズと足音",
                    "90Hz でハイパス。弾き語りなら 100Hz まで上げて良い",
                    BandAction.CUT,
                ),
                BandTip(
                    "ボディの膨らみ", 100.0, 250.0,
                    "ピエゾだと過剰になりやすい。ハウリングの起点",
                    "180Hz を Q2.0 で -3dB。ハウるなら Q4.0 で -6dB のノッチ",
                    BandAction.CUT,
                ),
                BandTip(
                    "箱鳴り・こもり", 300.0, 600.0,
                    "切るとすっきりする。刻みが見えてくる",
                    "400Hz を Q2.0 で -3dB",
                    BandAction.CUT,
                ),
                BandTip(
                    "ピエゾの硬さ", 2_000.0, 3_500.0,
                    "「ピエゾ臭い」と言われる正体。高域ではなくここ",
                    "2.5kHz を Q2.0 で -3dB。高域を削る前にここを試す",
                    BandAction.CUT,
                ),
                BandTip(
                    "ピック・弦の音", 4_000.0, 7_000.0,
                    "明瞭さ。ストロークの粒",
                    "5kHz を Q1.4 で +2dB",
                    BandAction.BOOST,
                ),
                BandTip(
                    "空気感", 10_000.0, 15_000.0,
                    "生々しさ。上げすぎるとピエゾ臭さが戻る",
                    "12kHz をシェルフで +2dB まで",
                    BandAction.BOOST,
                ),
            ),
        ),

        // ------------------------------------------------------------------
        // ボーカル・声
        // ------------------------------------------------------------------

        InstrumentBands(
            instrument = "男性ボーカル",
            group = InstrumentGroup.VOCAL,
            role = "最優先で客席に届ける音。他の楽器はここを避けて作る",
            fundamentalFromHz = 85.0,
            fundamentalToHz = 350.0,
            highPassHz = 80,
            micTip = "口から 3〜5cm。離れると近接効果が減って薄くなり、モニターのかぶりが増える。" +
                "**マイクを握り込む持ち方は指向性を壊す**ので、ハウリングが止まらないときは持ち方を見る",
            dynamicsTip = "コンプは 3:1 / アタック 5ms / リリース 100ms で 4〜6dB。" +
                "ディエッサーは 6〜8kHz を 4dB。強く掛けると声がこもるので、" +
                "サ行が気になる曲だけ深くする",
            conflicts = listOf(
                "エレキギターの 2〜3kHz と正面衝突する。**ギター側を削る**（ボーカルを上げない）",
                "スネアの 200〜250Hz と重なる。スネアを削る",
            ),
            pitfalls = listOf(
                "聞こえないときにフェーダーを上げるとハウる。まず 2〜4kHz を占めている楽器を削る",
                "2kHz を上げると明瞭になるが、同じ帯域でハウリングも起きる。上げ幅は +3dB まで",
            ),
            tips = listOf(
                BandTip(
                    "ハンドリング・吹かれ", 20.0, 80.0,
                    "マイクを持つ音、足音、風。声の成分は入っていない",
                    "80Hz でハイパス（12dB/oct）。全チャンネルで最初に入れる設定",
                    BandAction.CUT,
                ),
                BandTip(
                    "胸声・近接効果", 100.0, 250.0,
                    "近づくと増える帯域。多いとこもり、少ないと薄くなる",
                    "200Hz を Q1.4 で -2dB から。こもるなら -4dB まで",
                    BandAction.EITHER,
                ),
                BandTip(
                    "鼻づまり感", 500.0, 900.0,
                    "「モワッ」とした詰まり。切ると抜ける",
                    "700Hz を Q2.0 で -3dB。これだけで前に出ることが多い",
                    BandAction.CUT,
                ),
                BandTip(
                    "明瞭度", 2_000.0, 4_000.0,
                    "言葉の輪郭。同時にハウリングが最も起きやすい帯域",
                    "3kHz を Q1.4 で +2dB まで。それ以上要るなら他の楽器を削る",
                    BandAction.WATCH,
                ),
                BandTip(
                    "歯擦音（サ行）", 5_000.0, 8_000.0,
                    "「サ」「シ」のきつさ。ディエッサーの対象",
                    "ディエッサーを 6.5kHz / 4dB。EQ で削ると全体が曇る",
                    BandAction.WATCH,
                ),
                BandTip(
                    "空気感", 10_000.0, 16_000.0,
                    "息の生々しさ。近くにいる感じ",
                    "12kHz をシェルフで +2dB。ハウリングには効かない安全な帯域",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "女性ボーカル",
            group = InstrumentGroup.VOCAL,
            role = "最優先で通す音。男性より基音が高く、ハイパスを高く取れる",
            fundamentalFromHz = 165.0,
            fundamentalToHz = 700.0,
            highPassHz = 100,
            micTip = "口から 3〜5cm。男性より高い声なので、離れたときの痩せ方が大きい。" +
                "ハンドマイクなら、サビで離す癖があるかリハーサルで見ておく",
            dynamicsTip = "コンプは 3:1 / アタック 5ms / リリース 80ms で 4〜6dB。" +
                "ディエッサーは 7〜9kHz を 4dB（男性より高い点）",
            conflicts = listOf(
                "シンセパッドとストリングスの 500Hz〜2kHz に埋もれやすい。伴奏側を削る",
                "エレキギターの 2〜4kHz と衝突する。ギター側を -4dB",
            ),
            pitfalls = listOf(
                "男性と同じ 80Hz ハイパスだと低域のかぶりが残る。100〜120Hz まで上げて良い",
                "「細い」と感じて 200Hz を足すとこもる。厚みは 300Hz 付近",
            ),
            tips = listOf(
                BandTip(
                    "吹かれ・かぶり", 20.0, 100.0,
                    "声の成分は無い。ドラムのかぶりが入る",
                    "120Hz でハイパス。男性より高く取れる",
                    BandAction.CUT,
                ),
                BandTip(
                    "厚み", 200.0, 400.0,
                    "薄いと感じたときに触る帯域",
                    "300Hz を Q1.4 で +2dB。200Hz ではなく 300Hz を選ぶ",
                    BandAction.EITHER,
                ),
                BandTip(
                    "詰まり", 600.0, 1_000.0,
                    "抜けの悪さ",
                    "800Hz を Q2.0 で -3dB",
                    BandAction.CUT,
                ),
                BandTip(
                    "明瞭度", 2_500.0, 5_000.0,
                    "言葉の輪郭。硬さもここから出る",
                    "3.5kHz を Q1.4 で +2dB。硬いと言われたら逆に -2dB",
                    BandAction.WATCH,
                ),
                BandTip(
                    "歯擦音（サ行）", 6_000.0, 10_000.0,
                    "男性より高い位置にある",
                    "ディエッサーを 8kHz / 4dB",
                    BandAction.WATCH,
                ),
                BandTip(
                    "空気感", 10_000.0, 16_000.0,
                    "息の質感",
                    "12kHz をシェルフで +2dB",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "コーラス",
            group = InstrumentGroup.VOCAL,
            role = "リードの後ろに敷く声。リードと同じ作りにすると前に出すぎる",
            fundamentalFromHz = 120.0,
            fundamentalToHz = 700.0,
            highPassHz = 120,
            micTip = "リードより離して 5〜10cm。楽器を弾きながら歌うので、" +
                "マイクスタンドの位置と高さをリハーサルで決めておく（本番で動かせない）",
            dynamicsTip = "コンプは 4:1 で 6dB とリードより深く。" +
                "音量が揃うことでリードの後ろに安定して敷ける",
            conflicts = listOf(
                "リードと同じ帯域なので、明瞭度（3kHz）を上げるとリードが負ける",
                "楽器のかぶりが多い。ハウリングの余裕を最も食うチャンネル",
            ),
            pitfalls = listOf(
                "リードと同じ EQ をコピーすると、どちらが主役か分からなくなる",
                "使わない曲でフェーダーを上げたままにすると、かぶりだけが客席に出る",
            ),
            tips = listOf(
                BandTip(
                    "かぶり・低域", 20.0, 120.0,
                    "楽器のかぶりが主。声の成分は少ない",
                    "120Hz でハイパス。リードより高く設定する",
                    BandAction.CUT,
                ),
                BandTip(
                    "こもり", 300.0, 600.0,
                    "リードを覆う帯域",
                    "400Hz を Q2.0 で -3dB。リードの居場所を空ける",
                    BandAction.CUT,
                ),
                BandTip(
                    "明瞭度（控える）", 2_000.0, 4_000.0,
                    "上げるとリードと同じ位置に来る",
                    "3kHz は触らないか -2dB。ここを譲るのがコーラスの作り方",
                    BandAction.WATCH,
                ),
                BandTip(
                    "空気感", 8_000.0, 14_000.0,
                    "リードと差を付けられる帯域",
                    "10kHz をシェルフで +2dB。明瞭度ではなくここで存在感を作る",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "MC・アナウンス（スピーチ）",
            group = InstrumentGroup.VOCAL,
            role = "内容が伝わることだけが目的。音楽的な良さは要らない",
            fundamentalFromHz = 85.0,
            fundamentalToHz = 300.0,
            highPassHz = 120,
            micTip = "話者は歌手ほどマイクに近づかない。20cm 離れる前提で" +
                "指向性の強いマイクを選ぶ。演台マイクは口の高さに合わせる",
            dynamicsTip = "コンプは 6:1 / アタック 3ms で 8dB と深く。" +
                "話者の距離が変わっても音量が保てる。ゲートは使わない（語頭が消える）",
            conflicts = listOf(
                "客席のざわめきと競合する。音量ではなく 2〜4kHz の明瞭度で勝つ",
                "会場の残響が長いと言葉が潰れる。低域を削るほど聞き取れる",
            ),
            pitfalls = listOf(
                "音楽用の EQ を流用すると低域が多くて聞き取りにくい。スピーチは 120Hz 以下不要",
                "音量を上げると残響も一緒に増えて、かえって聞き取れなくなる",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 120.0,
                    "内容に寄与しない。残響を増やすだけ",
                    "120Hz でハイパス。150Hz まで上げても言葉は損なわれない",
                    BandAction.CUT,
                ),
                BandTip(
                    "こもり", 200.0, 500.0,
                    "「モゴモゴ」の正体",
                    "300Hz を Q2.0 で -4dB。スピーチでは大きく削って良い",
                    BandAction.CUT,
                ),
                BandTip(
                    "明瞭度（子音）", 2_000.0, 4_000.0,
                    "言葉が分かるかどうかを決める帯域",
                    "3kHz を Q1.4 で +4dB。スピーチでは音楽より積極的に上げる",
                    BandAction.BOOST,
                ),
                BandTip(
                    "歯擦音", 6_000.0, 9_000.0,
                    "きつさ。ディエッサーで抑える",
                    "ディエッサーを 7kHz / 3dB",
                    BandAction.WATCH,
                ),
                BandTip(
                    "不要な高域", 12_000.0, 20_000.0,
                    "言葉の情報は無い。ハウリングとノイズだけ",
                    "12kHz からシェルフで -6dB。スピーチでは切って良い",
                    BandAction.CUT,
                ),
            ),
        ),

        // ------------------------------------------------------------------
        // 鍵盤
        // ------------------------------------------------------------------

        InstrumentBands(
            instrument = "グランドピアノ",
            group = InstrumentGroup.KEYS,
            role = "帯域が最も広い楽器。バンドでは削って他の場所を空ける",
            fundamentalFromHz = 27.5,
            fundamentalToHz = 4_186.0,
            highPassHz = 60,
            micTip = "低音弦側と高音弦側に2本、ハンマーから 20〜30cm 上。" +
                "蓋を閉めるとハウリングに強くなるが箱鳴りが増える。" +
                "バンドではPZM（境界マイク）を蓋の裏に貼るとかぶりが減る",
            dynamicsTip = "コンプは 3:1 / アタック 20ms で 3dB。ソロでは掛けない。" +
                "バンドの中ではコンプで音量を揃えた方が埋もれない",
            conflicts = listOf(
                "ボーカル・ギター・ベースの全部と重なる。バンドでは低域と 2〜4kHz を削る",
                "低音弦マイクとベースが 60〜120Hz で衝突する。ピアノ側を切る",
            ),
            pitfalls = listOf(
                "ソロの設定のままバンドに入れると、他が全部埋もれる",
                "蓋を開けたままドラムの近くに置くと、ドラムのかぶりでハウる",
            ),
            tips = listOf(
                BandTip(
                    "不要な最低域", 20.0, 60.0,
                    "ステージの振動。バンドではベースの居場所",
                    "ソロなら 30Hz、バンドなら 100Hz でハイパス。用途で変える",
                    BandAction.CUT,
                ),
                BandTip(
                    "低域の膨らみ", 100.0, 250.0,
                    "量感。バンドでは切ることが多い",
                    "150Hz を Q1.4 で -3dB。ベースと同じ場所を譲る",
                    BandAction.CUT,
                ),
                BandTip(
                    "こもり", 300.0, 600.0,
                    "抜けの悪さ",
                    "400Hz を Q2.0 で -3dB",
                    BandAction.CUT,
                ),
                BandTip(
                    "硬さ・耳につく", 1_500.0, 3_000.0,
                    "バンドで前に出る帯域。同時にボーカルとぶつかる",
                    "歌モノなら 2.5kHz を -3dB。ソロなら触らない",
                    BandAction.EITHER,
                ),
                BandTip(
                    "ハンマーのアタック", 3_000.0, 6_000.0,
                    "粒立ち。刻みが見える",
                    "4kHz を Q1.4 で +2dB",
                    BandAction.BOOST,
                ),
                BandTip(
                    "空気感", 8_000.0, 16_000.0,
                    "響きの広がり",
                    "10kHz をシェルフで +2dB",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "エレピ（ローズ系）",
            group = InstrumentGroup.KEYS,
            role = "中域を埋める伴奏。ボーカルの後ろに敷く",
            fundamentalFromHz = 55.0,
            fundamentalToHz = 2_000.0,
            highPassHz = 80,
            micTip = "ライン受け。ステレオで来たら位相を確認する。" +
                "アンプシミュレータのノイズが多い機種があるので、無音時のノイズフロアを見ておく",
            dynamicsTip = "コンプは 3:1 / アタック 10ms で 3dB。" +
                "ベルの「キン」が強い曲だけディエッサー的に 4kHz を抑える",
            conflicts = listOf(
                "ボーカルの 500Hz〜1kHz を覆う。エレピ側を -3dB",
                "エレキギターと帯域がほぼ同じ。どちらかを削らないと団子になる",
            ),
            pitfalls = listOf(
                "気持ちよく聞こえる楽器なので上げすぎる。ボーカルが埋もれてから気づく",
                "トレモロが掛かっていると音量が揺れる。コンプが誤動作する",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 80.0,
                    "ベースの居場所",
                    "80Hz でハイパス",
                    BandAction.CUT,
                ),
                BandTip(
                    "こもり", 200.0, 500.0,
                    "他の楽器と最もぶつかる帯域",
                    "350Hz を Q2.0 で -3dB",
                    BandAction.CUT,
                ),
                BandTip(
                    "ボーカルを覆う帯域", 600.0, 1_200.0,
                    "エレピの気持ちよさの本体。同時にボーカルの居場所",
                    "800Hz を Q1.4 で -2dB。歌モノでは譲る",
                    BandAction.WATCH,
                ),
                BandTip(
                    "ベルの「キン」", 3_000.0, 6_000.0,
                    "アタックの硬さ",
                    "4kHz を Q2.0 で ±2dB。強く弾く曲では削る",
                    BandAction.EITHER,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "シンセ・パッド",
            group = InstrumentGroup.KEYS,
            role = "隙間を埋める面。存在に気づかれないのが正しい状態",
            fundamentalFromHz = 60.0,
            fundamentalToHz = 8_000.0,
            highPassHz = 150,
            micTip = "ライン受け。ステレオ幅が広い音は、モノにしたときに消える成分がある。" +
                "客席の端では片側しか聞こえないので、モノで一度確認する",
            dynamicsTip = "コンプは掛けない。持続音なのでコンプが常時働き、" +
                "他の楽器が入るたびに音量が揺れる",
            conflicts = listOf(
                "ボーカルの帯域を全部覆う。150Hz ハイパス + 1〜3kHz を削るのが前提",
                "ストリングスと役割が重複する。両方入れるなら帯域を上下で分ける",
            ),
            pitfalls = listOf(
                "帯域が広いので、少し上げるだけで全体が曇る",
                "ステレオ幅の広い音はモノの客席端で消える。頼りにすると穴が空く",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 150.0,
                    "ベースとキックの居場所を奪う",
                    "150Hz でハイパス。パッドに低域は要らない",
                    BandAction.CUT,
                ),
                BandTip(
                    "濁り", 200.0, 500.0,
                    "全体を曇らせる帯域",
                    "300Hz を Q2.0 で -4dB",
                    BandAction.CUT,
                ),
                BandTip(
                    "ボーカルとの衝突", 1_000.0, 3_000.0,
                    "歌の明瞭度と同じ場所",
                    "2kHz を Q1.4 で -3dB。歌モノでは常に削っておく",
                    BandAction.WATCH,
                ),
                BandTip(
                    "広がり", 8_000.0, 16_000.0,
                    "空間の広さ。ここだけで存在感が出る",
                    "10kHz をシェルフで +3dB。中域を削って高域で存在させる",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "オルガン（ハモンド系）",
            group = InstrumentGroup.KEYS,
            role = "持続する中低域。バンドの隙間を埋めるが、埋めすぎる",
            fundamentalFromHz = 30.0,
            fundamentalToHz = 4_000.0,
            highPassHz = 80,
            micTip = "ロータリースピーカーは上下2つのユニットに別のマイク。" +
                "上（ホーン）に2本でステレオ、下（ローター）に1本。回転で音量が揺れるのが正常",
            dynamicsTip = "コンプは掛けない。ロータリーの音量変化そのものが楽器の表情なので、" +
                "コンプで均すと回転が止まって聞こえる",
            conflicts = listOf(
                "ベースの帯域を完全に覆う。ベースペダルを弾く曲では役割を決めておく",
                "持続音なのでボーカルの隙間を全部埋める。500Hz〜2kHz を削る",
            ),
            pitfalls = listOf(
                "ドローバーの設定で帯域が大きく変わる。リハと本番で別の音になることがある",
                "ロータリーのマイクは音量が揺れるので、ゲートが誤動作する",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 80.0,
                    "ベースと衝突する",
                    "80Hz でハイパス。ベースペダルを使う曲では 50Hz まで",
                    BandAction.CUT,
                ),
                BandTip(
                    "こもり", 200.0, 500.0,
                    "持続音の濁り",
                    "300Hz を Q2.0 で -3dB",
                    BandAction.CUT,
                ),
                BandTip(
                    "存在感", 800.0, 2_000.0,
                    "オルガンらしさ。同時にボーカルの居場所",
                    "1.2kHz を Q1.4 で ±2dB。歌モノでは削る",
                    BandAction.EITHER,
                ),
                BandTip(
                    "ホーンのざらつき", 4_000.0, 8_000.0,
                    "ロータリーの回転音とホーンの刺激",
                    "5kHz を Q2.0 で -3dB",
                    BandAction.CUT,
                ),
            ),
        ),

        // ------------------------------------------------------------------
        // 管・弦
        // ------------------------------------------------------------------

        InstrumentBands(
            instrument = "サックス（アルト・テナー）",
            group = InstrumentGroup.WIND_STRINGS,
            role = "中域のソロ。生音が大きいので、足すより整える",
            fundamentalFromHz = 120.0,
            fundamentalToHz = 1_000.0,
            highPassHz = 100,
            micTip = "ベルの正面ではなくベルの端から 15〜20cm、キーの側に少し寄せる。" +
                "正面はキーの息とリードのノイズが過剰。クリップマイクは常に近接効果が乗る",
            dynamicsTip = "コンプは 3:1 / アタック 15ms で 4dB。" +
                "ソロで吹き込んだときのピークを止める。掛けすぎると吹き方の表情が消える",
            conflicts = listOf(
                "ボーカルと同じ 1〜3kHz を占める。歌の裏では -3dB 下げる",
                "生音が客席に届いているので、卓のフェーダーだけでは音量が制御できない",
            ),
            pitfalls = listOf(
                "クリップマイクは近すぎて低域が過剰。100Hz ハイパスは必須",
                "ソロで前に出そうとして 3kHz を上げると、耳に刺さって長く聞けない",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 100.0,
                    "近接効果とハンドリング",
                    "100Hz でハイパス。クリップマイクなら 150Hz まで",
                    BandAction.CUT,
                ),
                BandTip(
                    "太さ", 200.0, 500.0,
                    "痩せていると感じたときに触る",
                    "300Hz を Q1.4 で +2dB。テナーは 250Hz、アルトは 400Hz 寄り",
                    BandAction.EITHER,
                ),
                BandTip(
                    "鳴り・前に出る帯域", 1_000.0, 2_000.0,
                    "サックスらしい鳴り",
                    "1.5kHz を Q1.4 で +2dB",
                    BandAction.BOOST,
                ),
                BandTip(
                    "きつさ", 3_000.0, 6_000.0,
                    "大音量で耳につく。リードのノイズもここ",
                    "4kHz を Q2.0 で -3dB。ソロで上げるのは我慢する",
                    BandAction.WATCH,
                ),
                BandTip(
                    "息・空気感", 8_000.0, 14_000.0,
                    "吹いている感じ",
                    "10kHz をシェルフで +2dB",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "トランペット",
            group = InstrumentGroup.WIND_STRINGS,
            role = "最も遠くまで届く楽器。PA で足す必要が最も少ない",
            fundamentalFromHz = 165.0,
            fundamentalToHz = 1_000.0,
            highPassHz = 120,
            micTip = "ベル正面を**外す**。正面から 20〜30cm、軸から 20〜30度。" +
                "正面はピークが大きすぎてマイクとプリアンプが飽和する。ミュートを使うと更に指向性が鋭くなる",
            dynamicsTip = "リミッターを -8dBFS に置く。コンプは 4:1 / アタック 10ms で 4dB。" +
                "ハイノートのピークは他の楽器の 10dB 以上上に出ることがある",
            conflicts = listOf(
                "生音が最も大きい。卓で下げても客席の音量は変わらない",
                "ボーカルの明瞭度帯域を貫く。歌の裏では -4dB",
            ),
            pitfalls = listOf(
                "リハの音量でゲインを決めると、本番のハイノートでクリップする。6dB の余裕を残す",
                "ベル正面に立てると、マイクの入力段で歪んで後から直せない",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 120.0,
                    "トランペットの基音より下",
                    "120Hz でハイパス",
                    BandAction.CUT,
                ),
                BandTip(
                    "太さ", 200.0, 500.0,
                    "痩せて聞こえるときに触る",
                    "350Hz を Q1.4 で +2dB",
                    BandAction.EITHER,
                ),
                BandTip(
                    "鳴り", 800.0, 2_000.0,
                    "前に出る帯域",
                    "1.2kHz を Q1.4 で +2dB。上げなくても十分聞こえることが多い",
                    BandAction.EITHER,
                ),
                BandTip(
                    "刺さり", 3_000.0, 8_000.0,
                    "客席で痛くなる帯域。ハイノートでここが暴れる",
                    "5kHz を Q2.0 で -4dB。トランペットで最も重要な処理",
                    BandAction.WATCH,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "トロンボーン",
            group = InstrumentGroup.WIND_STRINGS,
            role = "ホーンセクションの下支え。トランペットの下に敷く",
            fundamentalFromHz = 80.0,
            fundamentalToHz = 600.0,
            highPassHz = 80,
            micTip = "ベルから 20〜30cm、軸から 15度外し。スライドが動くので" +
                "マイクスタンドの位置に余裕を持たせる（演奏者が動く）",
            dynamicsTip = "コンプは 4:1 / アタック 15ms で 4dB。" +
                "トランペットと同じ設定にすると、セクションの音量バランスが揃う",
            conflicts = listOf(
                "ベースとギターの 200〜500Hz にぶつかる。セクションでは 300Hz を整理する",
                "トランペットと 1〜2kHz が重なる。トロンボーンを下、トランペットを上に振り分ける",
            ),
            pitfalls = listOf(
                "低域が出る楽器なのでハイパスを浅くしがちだが、80Hz は切って良い",
                "セクションで全員に同じ EQ を入れると、トロンボーンだけこもる",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 80.0,
                    "ステージの振動",
                    "80Hz でハイパス",
                    BandAction.CUT,
                ),
                BandTip(
                    "太さ", 150.0, 400.0,
                    "セクションの土台",
                    "250Hz を Q1.4 で +2dB",
                    BandAction.BOOST,
                ),
                BandTip(
                    "こもり", 400.0, 800.0,
                    "抜けの悪さ",
                    "600Hz を Q2.0 で -3dB",
                    BandAction.CUT,
                ),
                BandTip(
                    "鳴り", 1_000.0, 2_500.0,
                    "前に出る帯域",
                    "1.5kHz を Q1.4 で +2dB",
                    BandAction.BOOST,
                ),
                BandTip(
                    "刺さり", 4_000.0, 8_000.0,
                    "強く吹いたときの刺激",
                    "5kHz を Q2.0 で -3dB",
                    BandAction.WATCH,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "バイオリン",
            group = InstrumentGroup.WIND_STRINGS,
            role = "高い旋律。ハウリングに最も弱い楽器",
            fundamentalFromHz = 196.0,
            fundamentalToHz = 3_000.0,
            highPassHz = 150,
            micTip = "駒の上 20〜30cm、f 孔を外す。クリップマイクは駒の脇に付けるが、" +
                "近すぎて 2〜4kHz が過剰になる。ピックアップだけだと「ギシギシ」した音になる",
            dynamicsTip = "コンプは 3:1 / アタック 30ms でごく浅く 2dB。" +
                "弓の立ち上がりを潰すと表情が全部消える。掛けないのが基本",
            conflicts = listOf(
                "ボーカルの明瞭度帯域と完全に重なる。歌の裏では -4dB",
                "2〜4kHz でハウリングしやすい。上げられる量が最も少ない楽器",
            ),
            pitfalls = listOf(
                "明瞭度を上げようとするとハウる。上げるなら 8kHz 以上",
                "ピックアップの「ギシギシ」を高域を切って直そうとすると曇る。原因は 2.5kHz",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 150.0,
                    "G線の基音（196Hz）より下。ステージの振動だけ",
                    "150Hz でハイパス。切っても楽器は痩せない",
                    BandAction.CUT,
                ),
                BandTip(
                    "胴鳴り", 250.0, 600.0,
                    "楽器の量感",
                    "350Hz を Q1.4 で +2dB。細いと感じたらここ",
                    BandAction.EITHER,
                ),
                BandTip(
                    "ギシギシ・硬さ", 2_000.0, 4_000.0,
                    "ピックアップ特有の硬さ。ハウリングの起点でもある",
                    "2.5kHz を Q2.0 で -3dB。ここを削ると音量を上げられる",
                    BandAction.WATCH,
                ),
                BandTip(
                    "松脂・弓の音", 6_000.0, 10_000.0,
                    "弓が弦を擦る音。生々しさ",
                    "8kHz をシェルフで +2dB。ハウリングに強い安全な帯域",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "フルート",
            group = InstrumentGroup.WIND_STRINGS,
            role = "高く細い旋律。生音が小さいのでハウリングと戦う",
            fundamentalFromHz = 262.0,
            fundamentalToHz = 2_000.0,
            highPassHz = 200,
            micTip = "歌口（吹き口）から 15〜20cm、息が直接当たらない角度。" +
                "正面は吹く息のノイズが全部入る。管の中央付近に向けると息が減る",
            dynamicsTip = "コンプは 3:1 / アタック 20ms で 3dB。" +
                "生音が小さくゲインを高く取るので、コンプでピークを止めないとフィードバックに近づく",
            conflicts = listOf(
                "ゲインを高く取るためハウリングの起点になりやすい。モニターの位置が重要",
                "ボーカルの 2〜4kHz と重なる",
            ),
            pitfalls = listOf(
                "生音が小さいのでゲインを上げすぎ、ドラムのかぶりだけが増える",
                "息のノイズを高域カットで消そうとすると、楽器の輝きも消える。マイクの角度で解決する",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 200.0,
                    "最低音（C＝262Hz）より下。かぶりだけ",
                    "200Hz でハイパス。高めに取れる",
                    BandAction.CUT,
                ),
                BandTip(
                    "太さ", 300.0, 700.0,
                    "低音域の量感",
                    "400Hz を Q1.4 で +2dB",
                    BandAction.EITHER,
                ),
                BandTip(
                    "息のノイズ", 1_000.0, 2_500.0,
                    "「フー」という息。マイクが近いと過剰",
                    "1.5kHz を Q2.0 で -2dB。まずマイクの角度を直す",
                    BandAction.CUT,
                ),
                BandTip(
                    "輝き", 4_000.0, 10_000.0,
                    "フルートらしい抜け",
                    "6kHz を Q1.4 で +2dB",
                    BandAction.BOOST,
                ),
            ),
        ),

        // ------------------------------------------------------------------
        // パーカッション・和楽器
        // ------------------------------------------------------------------

        InstrumentBands(
            instrument = "和太鼓",
            group = InstrumentGroup.PERCUSSION,
            role = "低域の圧。1台でシステムの余力を全部使う",
            fundamentalFromHz = 40.0,
            fundamentalToHz = 200.0,
            highPassHz = 30,
            micTip = "打面から 30〜50cm 離す。近いと風圧でマイクが飽和する。" +
                "胴の横に1本足すと低域の余韻が取れる。**低域用マイクは風圧に耐える機種を選ぶ**",
            dynamicsTip = "リミッターを -10dBFS に置く。コンプは 4:1 / アタック 20ms で 4dB。" +
                "ピークが他の楽器より 15dB 以上高いので、リミッターなしではシステムが保護に入る",
            conflicts = listOf(
                "キックとベースの帯域を丸ごと占める。共演するなら和太鼓を主役にして他を削る",
                "低域の圧でステージが揺れ、全マイクにかぶる",
            ),
            pitfalls = listOf(
                "リハの音量でゲインを決めると本番で必ずクリップする。10dB の余裕を残す",
                "低域を足すとサブウーファーのリミッターが働き、他の曲の低域まで減る",
            ),
            tips = listOf(
                BandTip(
                    "床の振動", 20.0, 35.0,
                    "音楽的成分は少なく、システムの余力を食う",
                    "35Hz でハイパス。屋外でも切って良い",
                    BandAction.CUT,
                ),
                BandTip(
                    "圧・重心", 50.0, 120.0,
                    "体に来る帯域。和太鼓の本体",
                    "80Hz を Q1.4 で +2dB まで。それ以上はシステムが保護に入る",
                    BandAction.BOOST,
                ),
                BandTip(
                    "胴鳴り・こもり", 200.0, 500.0,
                    "「ボワン」とした余韻",
                    "300Hz を Q2.0 で -4dB。輪郭が出る",
                    BandAction.CUT,
                ),
                BandTip(
                    "バチの当たり", 2_000.0, 5_000.0,
                    "打点。客席で「ドン」と聞こえるかを決める",
                    "3kHz を Q1.4 で +3dB。低域より先にここを作る",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "カホン",
            group = InstrumentGroup.PERCUSSION,
            role = "アコースティック編成のドラム代わり。低音と打面を1本で拾う",
            fundamentalFromHz = 60.0,
            fundamentalToHz = 300.0,
            highPassHz = 50,
            micTip = "背面の穴に 10cm でキック役、打面の斜め前 20cm でスネア役。" +
                "2本使えるなら分ける。1本なら打面の下の角に置いて両方を拾う",
            dynamicsTip = "コンプは 4:1 / アタック 10ms で 3dB。ゲートは使わない" +
                "（低音と打面が同じマイクなので、必ずどちらかが切れる）",
            conflicts = listOf(
                "アコギの 100〜250Hz とぶつかる。カホンを主役にしてアコギを削る",
                "床に置くので足音と振動を拾う。ステージの共振も入る",
            ),
            pitfalls = listOf(
                "低域が欲しくてマイクを穴に近づけると、打面の「パン」が消える",
                "ゲートを掛けると弱いゴーストノートが全部消えて、リズムが平坦になる",
            ),
            tips = listOf(
                BandTip(
                    "床の振動", 20.0, 50.0,
                    "足音とステージの共振",
                    "50Hz でハイパス",
                    BandAction.CUT,
                ),
                BandTip(
                    "低音（キック役）", 70.0, 150.0,
                    "背面の穴から出る低音",
                    "100Hz を Q1.4 で +2dB",
                    BandAction.BOOST,
                ),
                BandTip(
                    "こもり", 250.0, 500.0,
                    "箱鳴り",
                    "350Hz を Q2.0 で -4dB",
                    BandAction.CUT,
                ),
                BandTip(
                    "打面のパン", 2_000.0, 5_000.0,
                    "手が当たる音。スネア役",
                    "3kHz を Q1.4 で +3dB",
                    BandAction.BOOST,
                ),
                BandTip(
                    "ざらつき", 6_000.0, 12_000.0,
                    "内蔵の弦（スナッピー）のざらつき",
                    "8kHz をシェルフで +2dB",
                    BandAction.BOOST,
                ),
            ),
        ),

        InstrumentBands(
            instrument = "パーカッション（シェイカー・タンバリン）",
            group = InstrumentGroup.PERCUSSION,
            role = "高域の刻み。低域は一切要らない",
            fundamentalFromHz = 1_000.0,
            fundamentalToHz = 8_000.0,
            highPassHz = 400,
            micTip = "20〜30cm 離す。近いと音量差が激しくて扱えない。" +
                "演奏者が動くので、指向性の広いマイクの方が安定する",
            dynamicsTip = "コンプは 4:1 / アタック 5ms で 4dB。" +
                "シェイカーは音量差が大きいので、コンプで揃えないと刻みが聞こえない",
            conflicts = listOf(
                "ハイハットと完全に同じ帯域。両方入れると高域が過剰になる",
                "ボーカルの歯擦音と重なる。6〜8kHz を -2dB",
            ),
            pitfalls = listOf(
                "低域を残すとステージのかぶりだけが増える。400Hz 以下は不要",
                "高域を上げると耳につく。刻みが見えないのは 2〜4kHz が足りないため",
            ),
            tips = listOf(
                BandTip(
                    "不要な低域", 20.0, 400.0,
                    "楽器の音は入っていない。かぶりだけ",
                    "400Hz でハイパス。思い切って切って良い",
                    BandAction.CUT,
                ),
                BandTip(
                    "刻みの芯", 2_000.0, 5_000.0,
                    "リズムが見える帯域",
                    "3kHz を Q1.4 で +3dB。高域より先にここ",
                    BandAction.BOOST,
                ),
                BandTip(
                    "ざらつき", 6_000.0, 12_000.0,
                    "シェイカーの砂の音",
                    "8kHz をシェルフで +2dB",
                    BandAction.BOOST,
                ),
            ),
        ),

        // ------------------------------------------------------------------
        // 再生・その他
        // ------------------------------------------------------------------

        InstrumentBands(
            instrument = "トラック再生（PC・DJ）",
            group = InstrumentGroup.PLAYBACK,
            role = "完成品が来る。整えるだけで、作り直さない",
            fundamentalFromHz = 30.0,
            fundamentalToHz = 16_000.0,
            highPassHz = null,
            micTip = "ライン受け。**イヤホン端子ではなくバランス出力かDIを使う**。" +
                "ノートPCの端子はノイズとインピーダンスの両方で不利。音量は8割固定にして卓で調整する",
            dynamicsTip = "何も掛けない。既にマスタリングされているので、" +
                "コンプを足すと作った人の意図した音圧感が崩れる。リミッターだけ -3dBFS",
            conflicts = listOf(
                "生楽器と混ぜると帯域が全部埋まる。トラック側を削って生楽器の場所を作る",
                "会場の低域特性と合わないことがある。EQ ではなく会場側のシステム EQ で直す",
            ),
            pitfalls = listOf(
                "「音が悪い」と感じてEQを触ると、元より悪くなることが多い。まず接続と音量を見る",
                "PC のシステム音量を下げると S/N が悪くなる。8割で固定して卓で下げる",
            ),
            tips = listOf(
                BandTip(
                    "サブロー", 30.0, 50.0,
                    "会場が出せない帯域。アンプの余力だけ食う",
                    "システムの下限に合わせてハイパス。40Hz 前後が目安",
                    BandAction.CUT,
                ),
                BandTip(
                    "低域の量", 60.0, 150.0,
                    "会場によって過剰になる帯域",
                    "100Hz を Q1.4 で -2dB。会場が響くほど削る",
                    BandAction.EITHER,
                ),
                BandTip(
                    "こもり", 200.0, 500.0,
                    "生楽器とぶつかる帯域",
                    "300Hz を Q2.0 で -2dB。生楽器と混ぜるときだけ",
                    BandAction.CUT,
                ),
                BandTip(
                    "刺激", 3_000.0, 6_000.0,
                    "大音量で耳につく帯域",
                    "4kHz を Q1.4 で -2dB。長時間の再生では効く",
                    BandAction.EITHER,
                ),
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

    fun byGroup(group: InstrumentGroup): List<InstrumentBands> =
        ALL.filter { it.group == group }

    /**
     * 楽器・帯域名・助言の本文から探す。
     *
     * 現場で引くのは「ピエゾ」「ハウリング」「こもる」のような症状の言葉で、
     * 楽器名で引くとは限らない。
     */
    fun search(query: String): List<InstrumentBands> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return ALL
        return ALL.filter { it.searchText.contains(needle) }
    }
}
