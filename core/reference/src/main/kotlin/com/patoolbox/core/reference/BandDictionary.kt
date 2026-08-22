package com.patoolbox.core.reference

import kotlin.math.sqrt

/** その帯域がハウリングしやすいか。対処の順番を決めるために持たせている。 */
enum class FeedbackRisk(val label: String, val note: String) {
    /** ハウリングの常連。ここが出たら真っ先に疑う */
    HIGH("回りやすい", "この帯域の発振はまず疑う。ノッチが効く"),

    /** 条件が揃うと回る */
    MEDIUM("条件次第", "モニターの位置・マイクの持ち方で回る"),

    /** ここでの発振はまれ。別の原因を疑う */
    LOW("まれ", "ここが鳴り続けるならハウリング以外を疑う"),
}

/**
 * 帯域1つぶんの辞書項目。
 *
 * 楽器別の[FrequencyChart]と役割が違う。あちらは「キックをどう作るか」、
 * こちらは **「いま見えている 250Hz が何なのか」** を引くための表。
 * ハウリング検出やアナライザが出した数字から逆に引く使い方を想定している。
 *
 * @param oneLiner 1行の要約。他の画面（ハウリング検出の一覧など）に差し込む用
 * @param lives この帯域に何が居るか。楽器名だけでなく「電源ハムの3倍音」も入れる
 * @param boost 上げると何が起きるか。数字（中心周波数・量）を必ず入れる
 * @param cut 下げると何が起きるか。同上
 * @param problems この帯域が原因で起きる代表的な症状
 * @param feedbackNote ここで発振したときの一手
 * @param checkTone 耳で確かめるときの手がかり。null なら特に無し
 */
data class BandEntry(
    val label: String,
    val nickname: String,
    val fromHz: Double,
    val toHz: Double,
    val oneLiner: String,
    val lives: List<String>,
    val boost: String,
    val cut: String,
    val problems: List<String>,
    val feedbackRisk: FeedbackRisk,
    val feedbackNote: String,
    val checkTone: String? = null,
) {
    /** 帯域の中心（対数軸上）。図に点を置くときに使う */
    val centerHz: Double get() = sqrt(fromHz * toHz)

    fun contains(hz: Double): Boolean = hz >= fromHz && hz < toHz

    val searchText: String
        get() = buildString {
            append(label).append(' ')
            append(nickname).append(' ')
            append(oneLiner).append(' ')
            lives.forEach { append(it).append(' ') }
            append(boost).append(' ')
            append(cut).append(' ')
            problems.forEach { append(it).append(' ') }
            append(feedbackNote).append(' ')
            append(checkTone.orEmpty())
        }.lowercase()
}

/**
 * 帯域の辞書。
 *
 * 20Hz から 20kHz を 11 に切って、それぞれ「何が居て、上げ下げすると何が起きるか」を書く。
 * 区切りは 1 オクターブ前後（低域は 2 オクターブ）。等分ではないのは、
 * 人の聴感が対数で、しかも 200Hz〜5kHz に判断材料が集中しているため。
 *
 * この表がハウリング検出とアナライザの受け皿になる。
 * 「630Hz が鳴っている」と数字だけ出しても、それが何なのか分からなければ手が打てない。
 */
object BandDictionary {

    val ALL: List<BandEntry> = listOf(

        BandEntry(
            label = "超低域",
            nickname = "サブロー / インフラ",
            fromHz = 20.0,
            toHz = 40.0,
            oneLiner = "音程ではなく体で感じる帯域。ほとんどの会場では邪魔にしかならない",
            lives = listOf(
                "5弦ベースの最低音（B0 = 31Hz）とシンセの最低音",
                "キックのビーターの風、ステージの床鳴り、空調とトラックの走行音",
                "マイクスタンドを蹴った衝撃、ケーブルの取り回しノイズ",
            ),
            boost = "上げても客席では量感がほとんど増えず、アンプの余力だけを食う。" +
                "31.5Hz を +3dB するとサブの消費電力は倍になる",
            cut = "30〜40Hz に 12〜24dB/oct のハイパスを入れる。" +
                "これだけでシステム全体のヘッドルームが 3〜6dB 増え、中低域が澄む",
            problems = listOf(
                "ステージの床が揺れて、全部のマイクにかぶりが乗る",
                "サブのリミッターが先に働き、キックのアタックまで潰れる",
                "客席では聞こえないのにメーターだけ振れる",
            ),
            feedbackRisk = FeedbackRisk.LOW,
            feedbackNote = "この帯域が鳴り続けるならハウリングではなく、" +
                "空調・振動・風、またはマイクケーブルの取り回しを疑う",
            checkTone = "手を叩いても出ない。出ているならほぼ機械の音",
        ),

        BandEntry(
            label = "低域の重心",
            nickname = "ロー / 重さ",
            fromHz = 40.0,
            toHz = 80.0,
            oneLiner = "曲の重さが決まる帯域。キックとベースがここを取り合う",
            lives = listOf(
                "キックの胴鳴り（50〜70Hz）、ベースの低い方の基音（41Hz = E1）",
                "フロアタムの胴（60〜80Hz）、シンセベース",
                "会場の一番低い定在波（20m の部屋で 8.5Hz の倍数）",
            ),
            boost = "60Hz を Q1.4 で +2dB まで。3dB 以上上げたくなったら、" +
                "先に 200〜400Hz を削って場所を空ける方が正しい",
            cut = "客席後方だけ膨らむときは 63Hz を Q2 で -3dB。" +
                "部屋のモードに当たっているので、EQ より配置の方が効くことも多い",
            problems = listOf(
                "キックとベースが同じ 60Hz で重なって、どちらも輪郭を失う",
                "後方の壁際だけ低音が溜まる（部屋のモード）",
                "サブのゲインを上げると全体が眠くなる",
            ),
            feedbackRisk = FeedbackRisk.LOW,
            feedbackNote = "ここでの発振はまれ。出るならモニターとマイクの近接効果か、" +
                "サブがステージに回り込んでいる。ハイパスと配置で対処する",
            checkTone = "胸に来る感じ。音程は聞き取りにくい",
        ),

        BandEntry(
            label = "低域の量感",
            nickname = "ベース帯 / 太さ",
            fromHz = 80.0,
            toHz = 160.0,
            oneLiner = "ベースの音程が聞こえる帯域。ここが濁ると何の曲か分からなくなる",
            lives = listOf(
                "ベースの基音の中心（80〜120Hz）、キックのアタック手前",
                "男声の基音（100〜140Hz）、バスドラムの張り",
                "電源ハムの2倍音（100 / 120Hz）",
            ),
            boost = "ベースを前に出したいときは 100Hz ではなく 800Hz〜1kHz を上げる。" +
                "100Hz を +3dB すると量は増えるが音程は逆に分からなくなる",
            cut = "ボーカルやギターのハイパスを 100〜120Hz に入れて、" +
                "この帯域をベースとキックに明け渡す。混ざりが一気に良くなる",
            problems = listOf(
                "全チャンネルの低域が積み上がって、ミックスがもたつく",
                "100Hz / 120Hz のハムが混ざる（アースの取り方を疑う）",
                "ハイパスを入れていない MC マイクが低域を濁らせる",
            ),
            feedbackRisk = FeedbackRisk.MEDIUM,
            feedbackNote = "手持ちマイクの近接効果で回りやすい。" +
                "125Hz を Q4 で -4dB か、ハイパスを 120Hz に上げる方が副作用が少ない",
            checkTone = "ハムなら 100Hz か 120Hz ちょうど。音程が動かないのが手がかり",
        ),

        BandEntry(
            label = "ふくらみ",
            nickname = "こもりの入口 / 箱鳴り",
            fromHz = 160.0,
            toHz = 320.0,
            oneLiner = "こもりの正体。削ると音量を上げずに前に出る、最も費用対効果の高い帯域",
            lives = listOf(
                "女声の基音（200〜300Hz）、スネアの胴、ギターの箱鳴り",
                "小さい部屋の定在波が集中する場所",
                "マイクを近づけたときの近接効果の山",
            ),
            boost = "痩せた音に厚みを足すとき 250Hz を Q1.0 で +2dB。" +
                "ただし2本以上のチャンネルで同じことをすると必ずこもる",
            cut = "こもりには 250Hz を Q2.0 で -4dB から。-6dB を超えると痩せるのでそこで止める。" +
                "10チャンネル全部で 2dB 削ると、合計では大きな変化になる",
            problems = listOf(
                "「音は出ているのに何を言っているか分からない」の主犯",
                "スピーチが眠くなる（250Hz を -4dB で解決することが多い）",
                "楽器を足すたびに全体がこもっていく",
            ),
            feedbackRisk = FeedbackRisk.MEDIUM,
            feedbackNote = "モニターを床置きしたときに回りやすい（床の反射で +6dB 増える）。" +
                "250Hz を Q4 で -5dB、それでも回るならモニターを1つ減らす",
            checkTone = "話し声を手で囲んだときの音",
        ),

        BandEntry(
            label = "中低域",
            nickname = "濁り / 段ボール",
            fromHz = 320.0,
            toHz = 630.0,
            oneLiner = "楽器が最も密集する帯域。ここの整理がミックスの明暗を分ける",
            lives = listOf(
                "ギター・キーボード・ホーンの下の方、スネアの本体（400Hz 付近）",
                "男声の第2倍音、タムの胴",
                "会場のモードが密になり始める境目",
            ),
            boost = "太さが足りないときに 400Hz を +2dB。" +
                "ただしここを上げると、たいてい他の楽器が隠れる",
            cut = "「段ボールを叩いた音」がするときは 400〜500Hz を Q2 で -3dB。" +
                "バンド全体でこの帯域を 2〜3dB 空けると、音量を上げずに全部が聞こえるようになる",
            problems = listOf(
                "全部の楽器が同じ場所で鳴って、個々が聞こえない",
                "PA の音が「安っぽい」と言われるときの正体",
                "モニターがここで溜まって演者が音量を要求する",
            ),
            feedbackRisk = FeedbackRisk.HIGH,
            feedbackNote = "モニターのハウリングで最も多い帯域の一つ。" +
                "500Hz を Q6 以上の狭いノッチで -6dB。広く削ると声が細くなる",
            checkTone = "電話の声に近い帯域",
        ),

        BandEntry(
            label = "中域の芯",
            nickname = "鼻づまり / 芯",
            fromHz = 630.0,
            toHz = 1_250.0,
            oneLiner = "声と楽器の芯。触ると音の性格が最も大きく変わる",
            lives = listOf(
                "ボーカルの倍音の中心、スネアの「バシッ」、ギターの芯",
                "ベースの音程感（800Hz を上げると小さいスピーカーでもベースが聞こえる）",
                "1kHz は測定と校正の基準点",
            ),
            boost = "ベースやキックを小型スピーカーでも聞かせたいとき 800Hz を +3dB。" +
                "低域を足すより確実に「聞こえる」ようになる",
            cut = "鼻をつまんだような声は 800Hz〜1kHz を Q3 で -4dB。" +
                "削りすぎると声が遠くなるので -6dB を超えないこと",
            problems = listOf(
                "「鼻づまり」「箱の中で歌っている」と言われる",
                "ここを上げすぎると耳が疲れて、後半で音量を下げたくなる",
            ),
            feedbackRisk = FeedbackRisk.HIGH,
            feedbackNote = "マイクの指向性が崩れ始める帯域で、回るとかなり大きい音になる。" +
                "1kHz を Q8 で -6dB のノッチ。ゲインを 2dB 下げる方が先",
            checkTone = "1kHz は「プー」という基準音。耳で覚えておくと現場で速い",
        ),

        BandEntry(
            label = "存在感",
            nickname = "前に出る帯域 / 耳につく",
            fromHz = 1_250.0,
            toHz = 2_500.0,
            oneLiner = "人の耳が最も敏感な入口。少し触るだけで印象が大きく変わる",
            lives = listOf(
                "子音の始まり、ギターのピッキング、スネアのアタック",
                "人の耳の感度が上がり始める帯域（等ラウドネス曲線の谷の手前）",
                "ハウリングの常連",
            ),
            boost = "声を前に出したいとき 2kHz を Q1.5 で +2dB。" +
                "3dB を超えると刺さり始めるので、その手前で止める",
            cut = "耳が疲れる・きついときは 2kHz を Q2 で -3dB。" +
                "客席の後方だけきつい場合は EQ ではなくスピーカーの角度を疑う",
            problems = listOf(
                "上げると最初は「良くなった」と感じるが、30分で耳が疲れる",
                "複数チャンネルで上げると全体がきつくなる",
            ),
            feedbackRisk = FeedbackRisk.HIGH,
            feedbackNote = "ハウリングの中心帯域。" +
                "2kHz を Q8 で -6dB を1点入れ、それ以上はマイクの向きで対処する",
            checkTone = "耳が痛くなる手前の「キーン」の始まり",
        ),

        BandEntry(
            label = "プレゼンス",
            nickname = "明瞭度 / 抜け",
            fromHz = 2_500.0,
            toHz = 5_000.0,
            oneLiner = "言葉が聞き取れるかどうかを決める帯域。同時にハウリングが最も多い",
            lives = listOf(
                "子音（t / k / s の頭）、シンバルのアタック",
                "人の耳が最も敏感（3〜4kHz は外耳道の共鳴で +10dB 前後になる）",
                "ハウリングの最頻出帯域",
            ),
            boost = "言葉が聞き取れないとき 3.15kHz を Q1.5 で +2〜3dB。" +
                "音量を上げるより確実に明瞭度が上がる",
            cut = "刺さる・耳が痛いときは 4kHz を Q3 で -3dB。" +
                "削りすぎると言葉が埋もれるので、明瞭度と引き換えなのを忘れないこと",
            problems = listOf(
                "この帯域を上げすぎた PA は「うるさいのに聞き取れない」",
                "高齢の客層が多い会場ではここが足りないと言葉が届かない",
            ),
            feedbackRisk = FeedbackRisk.HIGH,
            feedbackNote = "最優先で疑う。3.15kHz か 4kHz を Q8〜10 の狭いノッチで -6dB。" +
                "2点まで。3点目が要るならマイクとモニターの位置を直す",
            checkTone = "「シー」ではなく「チッ」に近い、子音のきつさ",
        ),

        BandEntry(
            label = "歯擦音",
            nickname = "サ行 / シャリ",
            fromHz = 5_000.0,
            toHz = 8_000.0,
            oneLiner = "サ行のきつさとシンバルの明るさ。ここは EQ よりディエッサーが効く",
            lives = listOf(
                "s / sh の歯擦音（6〜8kHz）、ハイハットのアタック",
                "アコースティックギターのピック、弦の擦れ",
            ),
            boost = "きらびやかにしたいとき 6.3kHz を Q1 で +2dB。" +
                "ただし歯擦音も同じだけ上がるので、ディエッサーとセットで考える",
            cut = "サ行がきついときは EQ で削る前にディエッサーを 6.3kHz / -4dB で入れる。" +
                "EQ で切ると、歯擦音の無い部分まで曇る",
            problems = listOf(
                "歌い手が変わると急にサ行だけ刺さる",
                "安いマイクほどこの帯域に癖の山がある",
            ),
            feedbackRisk = FeedbackRisk.MEDIUM,
            feedbackNote = "回ると「ピー」ではなく「シー」に近い音になる。" +
                "6.3kHz を Q8 で -5dB。イヤモニ運用では出ない",
            checkTone = "「サシスセソ」を言ったときの息の音",
        ),

        BandEntry(
            label = "空気感",
            nickname = "きらめき / シャリつき",
            fromHz = 8_000.0,
            toHz = 12_500.0,
            oneLiner = "距離と湿度で最初に失われる帯域。客席後方には届いていない",
            lives = listOf(
                "シンバルの余韻、ブラシ、弦の倍音",
                "テープやアナログ機材のヒスノイズ",
            ),
            boost = "遠くの客席で音がこもるとき 10kHz を +2dB。" +
                "空気による減衰（100m で 6〜10dB）を戻す意味がある",
            cut = "ノイズが気になるときは 10kHz を -3dB。" +
                "ヒスは下げられるが、シンバルの余韻も一緒に消える",
            problems = listOf(
                "近くのモニターでは足りているのに、客席後方では足りない",
                "湿度が低い日ほど届く（乾燥時の方が高域の減衰は小さい）",
            ),
            feedbackRisk = FeedbackRisk.LOW,
            feedbackNote = "この帯域だけで発振することはまれ。" +
                "鳴っているならワイヤレスの高周波ノイズかデジタルの折り返しを疑う",
            checkTone = "シンバルを鳴らして「シャーン」の後ろに残る成分",
        ),

        BandEntry(
            label = "エア",
            nickname = "超高域",
            fromHz = 12_500.0,
            toHz = 20_000.0,
            oneLiner = "大人にはほとんど聞こえない。切っても失うものは少ない",
            lives = listOf(
                "シンバルの最上部の倍音、ノイズ",
                "スイッチング電源やモニター機器の漏れ（15.7kHz など）",
                "40歳を超えると多くの人が 14kHz 以上を聞き取れない",
            ),
            boost = "録音や配信では 16kHz を +1〜2dB で空気感が出る。" +
                "生の PA では上げてもほとんど誰にも届かない",
            cut = "16kHz 以上をシェルビングで -3dB。" +
                "ノイズとツイータの負担が減り、音の印象はほとんど変わらない",
            problems = listOf(
                "ここを上げてもツイータの発熱だけが増える",
                "デジタルの折り返し（エイリアス）がここに現れることがある",
            ),
            feedbackRisk = FeedbackRisk.LOW,
            feedbackNote = "ここが鳴り続けるのはハウリングではなく機器由来のノイズ。" +
                "ケーブルと電源、ワイヤレスの受信機を先に確認する",
            checkTone = "若い人にしか聞こえない。テレビの電子音のような高さ",
        ),
    )

    /** その周波数が属する帯域。可聴域の外では null */
    fun at(hz: Double): BandEntry? = ALL.firstOrNull { it.contains(hz) }

    fun search(query: String): List<BandEntry> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return ALL
        // 数字で引かれたら周波数として扱う。「250」と打つのは症状ではなく周波数
        normalized.toDoubleOrNull()?.let { hz -> return listOfNotNull(at(hz)) }
        return ALL.filter { it.searchText.contains(normalized) }
    }

    fun byRisk(risk: FeedbackRisk): List<BandEntry> = ALL.filter { it.feedbackRisk == risk }
}
