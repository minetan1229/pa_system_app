package com.patoolbox.core.reference

/** 信号の流れのどこで起きるか。上流から順に並んでいる。 */
enum class DegradationStage(val label: String, val description: String) {
    SOURCE("音源・マイク", "ここで失った音は、後段で何をしても戻らない"),
    CABLE("ケーブル・接続", "地味だが積み上がる。1本ずつは小さくても経路全体で効く"),
    GAIN("ゲイン構成", "劣化の原因として最も多い。つまみの位置だけで決まる"),
    PROCESSING("卓の処理", "良くするための操作が、そのまま劣化の原因になる"),
    TRANSPORT("伝送・デジタル", "無線とデジタルは「劣化しない」ではなく「別の壊れ方をする」"),
    OUTPUT("アンプ・スピーカー", "限界を超えたときの音。機材は守られるが音は失われる"),
    VENUE("会場・空気", "距離と空気は必ず高域を削る。EQ で戻す前提で組む"),
    RECORD("録音・書き出し", "後で聞き返す音の質。撮り直しはできない"),
}

/** どのくらい深刻か。手当ての優先順を決めるために持たせている。 */
enum class DegradationSeverity(val label: String, val note: String) {
    /** 元に戻せない。起こさないことしか対策がない */
    FATAL("不可逆", "起きた時点で音は失われる。防ぐしかない"),

    /** 大きく効くが、原因を直せば戻る */
    HEAVY("大きい", "原因を直せば元に戻る"),

    /** 気づきにくく、少しずつ積み上がる */
    CREEPING("じわじわ", "1つでは気づかない。合計で効く"),
}

/**
 * 音質が落ちる原因1つぶん。
 *
 * 「〜すると劣化する」で終わらせない。現場で必要なのは
 * **どう聞こえるか（症状から逆引きできる）** と
 * **どのくらいの量か（対処する価値があるか）** の2つ。
 */
data class DegradationItem(
    val title: String,
    val stage: DegradationStage,
    val severity: DegradationSeverity,
    /** どう聞こえるか。症状から引くための文 */
    val symptom: String,
    /** 何が起きているか */
    val mechanism: String,
    /** どのくらいの量か。数字で書く */
    val amount: String,
    /** いま現場でできる手当て */
    val fixes: List<String>,
    /** 次からの防ぎ方。現場対処と同じなら null */
    val prevention: String?,
) {
    val searchText: String
        get() = buildString {
            append(title).append(' ')
            append(stage.label).append(' ')
            append(symptom).append(' ')
            append(mechanism).append(' ')
            append(amount).append(' ')
            fixes.forEach { append(it).append(' ') }
            append(prevention.orEmpty())
        }.lowercase()
}

/**
 * 音質劣化の一覧。
 *
 * この app の他のリファレンスと違って、並び順は「信号の流れ」にしている。
 * 現場で音が悪いとき、人は最後（スピーカー）から疑うが、
 * 実際の原因はほぼ上流（ゲインとケーブル）にある。
 * 上から読めば上流から確かめることになる、という並べ方をしている。
 */
object SignalDegradation {

    val ALL: List<DegradationItem> = listOf(

        // --- 音源・マイク ---

        DegradationItem(
            title = "複数マイクのかぶり（コムフィルタ）",
            stage = DegradationStage.SOURCE,
            severity = DegradationSeverity.HEAVY,
            symptom = "1本ずつは良い音なのに、混ぜると薄く・スカスカになる。" +
                "フェーダーを上げると逆に音が細くなる",
            mechanism = "同じ音を距離の違う2本のマイクが拾うと、時間差のぶんだけ" +
                "特定の周波数で打ち消しが起きる。音速 340m/s なので " +
                "34cm の距離差 = 1ms = 500Hz とその奇数倍が消える",
            amount = "深いところで -20dB 以上。EQ では絶対に埋められない",
            fixes = listOf(
                "使わないマイクのフェーダーを下げる。混ぜる本数を減らすのが最も効く",
                "2本のマイクを「近づける」か「はっきり離す」。中間が最悪",
                "極性スイッチを切り替えて、良い方を選ぶ",
                "距離差があるなら卓のディレイで合わせる（1cm ≒ 0.03ms）",
            ),
            prevention = "3:1 の原則。マイク間の距離を、マイクから音源までの距離の3倍以上にする",
        ),

        DegradationItem(
            title = "マイクの握り込み・グリルの汚れ",
            stage = DegradationStage.SOURCE,
            severity = DegradationSeverity.HEAVY,
            symptom = "同じ歌い手なのに日によってハウリングが止まらない。高域が曇る",
            mechanism = "単一指向性マイクは、グリル後方の音響的な穴で指向性を作っている。" +
                "手で覆うとその穴が塞がり、指向性が全方向に近づく。" +
                "グリル内の唾と埃は高域から先に減衰させる",
            amount = "握り込みで背面の感度が 10〜15dB 上がる。" +
                "汚れたグリルは 8kHz 以上で 3〜6dB の損失",
            fixes = listOf(
                "持ち方を演者に伝える。EQ より効く",
                "グリルを外して中の防塵スポンジを洗う（乾かしてから戻す）",
                "予備のマイクに交換して比較する",
            ),
            prevention = "本番前にグリルの中を確認する。使い回しのマイクは定期的に洗う",
        ),

        // --- ケーブル・接続 ---

        DegradationItem(
            title = "長いアンバランスケーブルの高域損失",
            stage = DegradationStage.CABLE,
            severity = DegradationSeverity.HEAVY,
            symptom = "同じギターなのに、長いシールドにすると曇って痩せる",
            mechanism = "ケーブルは 1m あたり 80〜150pF の静電容量を持つ。" +
                "出力インピーダンスの高い機器（ギター・ピエゾ）と組むと、" +
                "その容量がローパスフィルタになる",
            amount = "10m（約 1nF）とギター（約 10kΩ 相当）で -3dB が 16kHz 付近。" +
                "ピエゾ（数百kΩ）だと数kHz まで落ちてくる",
            fixes = listOf(
                "楽器のすぐ近くで DI に入れ、そこから先をバランス（マイクケーブル）にする",
                "アクティブ回路（プリアンプ内蔵）の楽器はこの影響をほぼ受けない",
                "余ったケーブルを巻いて長さを稼がない。短いものに替える",
            ),
            prevention = "ハイインピーダンスの信号を 5m 以上引き回さない設計にする",
        ),

        DegradationItem(
            title = "アンバランス接続とグラウンドループのハム",
            stage = DegradationStage.CABLE,
            severity = DegradationSeverity.HEAVY,
            symptom = "「ブーン」という持続音。楽器に触ると変化する。" +
                "特定の機材を繋いだときだけ出る",
            mechanism = "アンバランス接続はノイズを打ち消す仕組みを持たない。" +
                "また2つの機材が別のコンセントから電源を取ると、" +
                "グラウンド間の電位差で 50Hz（東日本）/60Hz（西日本）とその倍音が流れる",
            amount = "ハムは 50/60Hz と 100/120/150/180Hz。" +
                "条件が悪いと信号より大きくなる",
            fixes = listOf(
                "電源を同じ系統（同じ電源タップ）にまとめる",
                "アイソレーショントランス入りの DI を挟む（グラウンドを切る）",
                "DI のグラウンドリフトスイッチを切り替える",
                "GEQ で 50/60Hz とその倍音をノッチするのは応急処置。原因は残る",
            ),
            prevention = "PA 系統と楽器・映像系統の電源を最初から分けない。分けるならトランスで絶縁する",
        ),

        DegradationItem(
            title = "接触不良・断線寸前のケーブル",
            stage = DegradationStage.CABLE,
            severity = DegradationSeverity.HEAVY,
            symptom = "「ガリッ」というノイズ、突然の音切れ、片チャンネルだけ小さい",
            mechanism = "XLR のピンの緩み、TRS のリング接点の酸化、" +
                "コネクタ根元の芯線が半分切れている状態。動かすと変化するのが特徴",
            amount = "完全に落ちるか、片側だけ 6dB 落ちる（ステレオがモノになる）",
            fixes = listOf(
                "ケーブルを揺すって再現するか確かめる。再現したらその場で交換する",
                "コネクタを別のチャンネルに挿し替えて、卓側かケーブル側かを切り分ける",
                "疑わしいケーブルは即座に外して、後で直すために印を付ける",
            ),
            prevention = "怪しいケーブルを箱に戻さない。その場でテープを巻いて隔離する",
        ),

        DegradationItem(
            title = "インピーダンス不整合（直入れ）",
            stage = DegradationStage.CABLE,
            severity = DegradationSeverity.HEAVY,
            symptom = "ギターを卓に直接挿したら、音が小さく細く、高域が無い",
            mechanism = "パッシブのギター・ベース・ピエゾは出力インピーダンスが数十kΩ〜数百kΩ。" +
                "卓のライン入力（10kΩ程度）で受けると信号が分圧され、" +
                "同時に高域が落ちる",
            amount = "レベルで 10〜20dB、高域で数dBの損失。音量を上げても元の音には戻らない",
            fixes = listOf(
                "DI（インピーダンス変換）を挟む。これ以外に解決策はない",
                "手元に無ければ、楽器のアンプのライン出力から受ける",
            ),
            prevention = "パッシブ楽器は必ず DI 経由。パッチ表の段階で DI の数を数えておく",
        ),

        // --- ゲイン構成 ---

        DegradationItem(
            title = "ゲイン不足をフェーダーで補う",
            stage = DegradationStage.GAIN,
            severity = DegradationSeverity.HEAVY,
            symptom = "静かな曲で「サー」というノイズが聞こえる。" +
                "フェーダーを上げるほどノイズも一緒に上がる",
            mechanism = "プリアンプのゲインが足りないまま後段で持ち上げると、" +
                "信号とノイズが同じ量だけ増える。S/N は入力段で決まってしまう",
            amount = "プリのゲインを 20dB 下げてフェーダーで戻すと、S/N はそのまま 20dB 悪化する",
            fixes = listOf(
                "プリアンプのゲインをやり直す。ピークが -12〜-6dBFS に来るように合わせる",
                "フェーダーは 0dB 付近（ユニティ）で使えるゲインにする",
            ),
            prevention = "リハーサルで最も大きい部分を鳴らしてゲインを決める。" +
                "サウンドチェックの小さい音で決めない",
        ),

        DegradationItem(
            title = "入力段のクリップ（歪み）",
            stage = DegradationStage.GAIN,
            severity = DegradationSeverity.FATAL,
            symptom = "大きい部分だけ「ジリッ」と割れる。EQ を触っても消えない。" +
                "録音にも同じ歪みが残っている",
            mechanism = "プリアンプまたは AD 変換の上限を超えた波形は、上が平らに潰される。" +
                "潰れた形は情報として失われるので、後段では復元できない",
            amount = "0dBFS に 1サンプル張り付いた時点で発生する。" +
                "3次・5次といった高次倍音が可聴域全体に広がるので、" +
                "EQ で削れる範囲に収まらない",
            fixes = listOf(
                "ゲインを下げる。フェーダーではなくゲイン（一番上流）を下げる",
                "20dB のパッドを入れる。トランペット・和太鼓・キックでは常用する",
                "既に録れてしまった音は直せない。撮り直すしかない",
            ),
            prevention = "リハで決めたゲインから 6dB の余裕を残す。本番はリハより大きくなる",
        ),

        DegradationItem(
            title = "デジタル卓の 0dBFS 天井",
            stage = DegradationStage.GAIN,
            severity = DegradationSeverity.FATAL,
            symptom = "アナログ卓と同じ感覚でメーターを振らせたら、突然ひどく割れた",
            mechanism = "アナログ卓は 0VU を超えても数dBは緩やかに歪むだけだが、" +
                "デジタルは 0dBFS が絶対の壁で、超えた瞬間から真っ平らに潰れる",
            amount = "アナログの +4dBu を -18〜-20dBFS に置くのが標準。" +
                "つまり **平均で -18dBFS、ピークで -6dBFS** が目安",
            fixes = listOf(
                "メーターの見方を変える。0 まで振らせない",
                "各チャンネルにリミッターを -6dBFS で置く",
                "マスターにもリミッターを置く（最後の防波堤）",
            ),
            prevention = "デジタル卓では「メーターが振れている」ではなく「余裕が何dB あるか」で見る",
        ),

        // --- 卓の処理 ---

        DegradationItem(
            title = "EQ のブーストの積み上げ",
            stage = DegradationStage.PROCESSING,
            severity = DegradationSeverity.CREEPING,
            symptom = "1本ずつ良くしたはずなのに、全部上げたらマスターが赤くなり、" +
                "音は前より悪い",
            mechanism = "ブーストはそのぶんヘッドルームを食う。" +
                "各チャンネルで +4dB ずつ上げれば、合わさったときには" +
                "その帯域だけが過剰になる",
            amount = "同じ帯域を8チャンネルで +3dB 上げると、合計ではおよそ +12dB になる",
            fixes = listOf(
                "上げたい帯域ではなく、邪魔な帯域を下げる。「足すより引く」",
                "上げた量の合計を数える。1チャンネルで +6dB を超えたら考え直す",
                "チャンネルの出力（トリム）を下げて全体を揃え直す",
            ),
            prevention = "探すときはブースト、直すときはカット。探し終わったらブーストを戻す",
        ),

        DegradationItem(
            title = "EQ の掛けすぎによる位相回転",
            stage = DegradationStage.PROCESSING,
            severity = DegradationSeverity.CREEPING,
            symptom = "1本では良い音なのに、他と混ぜると芯が消える。" +
                "EQ を全部バイパスすると、なぜかまとまりが良い",
            mechanism = "一般的な EQ（ミニマムフェイズ）は、動かした周波数の周辺で位相も回す。" +
                "同じ音を含む別チャンネルとの間で打ち消しが起きる",
            amount = "Q が高く量が大きいほど回る。Q4.0 で 12dB 動かすと、" +
                "その周辺で ±90度近く回ることがある",
            fixes = listOf(
                "深く狭く削るのをやめ、浅く広く（Q1.0〜2.0 で -3dB）に置き換える",
                "同じ音を拾っている複数チャンネルには同じ EQ を入れる（ずれを揃える）",
                "1つの EQ で 12dB 動かすより、原因（マイク位置）を直す",
            ),
            prevention = "1バンドの補正量を -6dB 以内にとどめる。それ以上必要なら原因が別にある",
        ),

        DegradationItem(
            title = "コンプの掛けすぎ",
            stage = DegradationStage.PROCESSING,
            severity = DegradationSeverity.HEAVY,
            symptom = "音量は揃ったが、抑揚が無く平坦。曲の隙間で「サー」が持ち上がる。" +
                "ドラムが入るたびに他の楽器が沈む",
            mechanism = "大きい部分だけを圧縮すると、相対的に小さい部分（ノイズ・かぶり）が" +
                "持ち上がる。アタックを短く設定すると立ち上がりそのものが潰れる",
            amount = "ゲインリダクション 10dB を超えると、ノイズフロアも同じだけ上がる",
            fixes = listOf(
                "レシオを 3:1 まで戻し、ゲインリダクションを 4〜6dB に収める",
                "アタックを 5〜15ms に伸ばす。1ms 以下は打楽器の芯を消す",
                "リリースを曲のテンポに合わせる（速すぎるとポンピングする）",
            ),
            prevention = "コンプは「音量を揃える」道具。音を良くする道具として使わない",
        ),

        DegradationItem(
            title = "ノイズゲートの締めすぎ",
            stage = DegradationStage.PROCESSING,
            severity = DegradationSeverity.HEAVY,
            symptom = "語頭の子音が消える。ドラムのロールが途切れる。" +
                "小さい音のフレーズが丸ごと聞こえない",
            mechanism = "しきい値より小さい信号を落とす仕組みなので、" +
                "「小さいが必要な音」も一緒に落ちる。開くまでの時間のぶん頭も削れる",
            amount = "しきい値を 6dB 上げるだけで、弱打が全部落ちることがある",
            fixes = listOf(
                "しきい値を下げ、レンジ（減衰量）を -60dB ではなく -12dB にする",
                "ホールドを伸ばす（タムなら 60〜120ms）",
                "スピーチのマイクにはゲートを使わない",
            ),
            prevention = "ゲートはかぶりを減らす道具。無音を作る道具ではない",
        ),

        DegradationItem(
            title = "ハウリング対策のノッチのやりすぎ",
            stage = DegradationStage.PROCESSING,
            severity = DegradationSeverity.CREEPING,
            symptom = "ハウリングは止まったが、声が細く不自然。" +
                "気づけば GEQ のスライダーが何本も下がっている",
            mechanism = "ハウリングする帯域はその音の主要な帯域でもある。" +
                "自動ハウリング抑制は止め続けるためにノッチを増やし続ける",
            amount = "Q10 のノッチ1本なら影響は小さいが、5本を超えると声の質感が変わる。" +
                "-12dB を超えるノッチは必ず聞こえる",
            fixes = listOf(
                "ノッチを一度全部戻し、原因（マイクとスピーカーの位置関係）から手を付ける",
                "モニターの向きを 15度変える方が、ノッチ3本より効く",
                "自動抑制は「リハで場所を探すため」に使い、本番は固定ノッチにする",
            ),
            prevention = "マイクをスピーカーの後ろ（指向性の外）に置く。位置で稼いだ余裕は音質を損なわない",
        ),

        // --- 伝送・デジタル ---

        DegradationItem(
            title = "Bluetooth 経由の再エンコード",
            stage = DegradationStage.TRANSPORT,
            severity = DegradationSeverity.FATAL,
            symptom = "高域が曇る。映像とずれる。曲間で「プツッ」と切れる",
            mechanism = "Bluetooth は非可逆圧縮（SBC / AAC など）で送る。" +
                "元がすでに圧縮音源なら二重に圧縮される。" +
                "さらに送受信のバッファのぶん遅延が発生する",
            amount = "SBC で 150〜250ms の遅延。高域は 16kHz あたりから落ちる。" +
                "混雑した会場では切断も起きる",
            fixes = listOf(
                "有線に替える。3.5mm でもよいが、DI かバランス出力が望ましい",
                "どうしても無線なら、事前に同じ会場・同じ端末で通しで試す",
            ),
            prevention = "本番の再生に Bluetooth を使わない。電波環境は当日まで分からない",
        ),

        DegradationItem(
            title = "圧縮音源の再圧縮と低ビットレート",
            stage = DegradationStage.TRANSPORT,
            severity = DegradationSeverity.FATAL,
            symptom = "シンバルやリバーブの余韻が「シュワシュワ」する。" +
                "大音量にすると急に安っぽく聞こえる",
            mechanism = "MP3/AAC は聞こえにくい成分を捨てて容量を減らしている。" +
                "小さいスピーカーでは分からないが、PA の音量では捨てた跡が聞こえる",
            amount = "MP3 128kbps は 16kHz 以上をほぼカット。" +
                "圧縮を重ねるたびに劣化が積み上がる",
            fixes = listOf(
                "WAV かロスレスで用意し直す。これ以外に手はない",
                "手元にそれしかない場合は、高域を上げて補おうとしない（アラが目立つだけ）",
            ),
            prevention = "本番で流す音源は WAV で受け取る。パッチ表と一緒にフォーマットも確認する",
        ),

        DegradationItem(
            title = "サンプリング周波数とクロックのずれ",
            stage = DegradationStage.TRANSPORT,
            severity = DegradationSeverity.HEAVY,
            symptom = "数秒〜数十秒おきに「プツッ」と入る。" +
                "音は出ているのに時々ノイズが混ざる",
            mechanism = "44.1kHz の機器と 48kHz の機器を繋ぐ、または同じ 48kHz でも" +
                "別々のクロックで動かすと、少しずつサンプルがずれて" +
                "定期的に取りこぼしが起きる",
            amount = "48k と 44.1k を混ぜると再生速度が 8.8% ずれる。" +
                "同じ周波数でもクロックが別だと数十秒に1回のクリックになる",
            fixes = listOf(
                "系全体のサンプリング周波数を揃える（48kHz が現場の標準）",
                "クロックのマスターを1台に決めて、他は同期に設定する",
                "揃えられない機器の間はアナログで繋ぐ（AD/DA が1回増えるが確実）",
            ),
            prevention = "デジタルで繋ぐ機器のクロック設定を、リハの最初に全部確認する",
        ),

        DegradationItem(
            title = "無線マイクのコンパンダーとドロップアウト",
            stage = DegradationStage.TRANSPORT,
            severity = DegradationSeverity.HEAVY,
            symptom = "有線と比べて高域が硬い。小さい音のときに背景がざわつく。" +
                "演者が動くと一瞬「ザッ」と切れる",
            mechanism = "アナログ無線は電波に載せるために圧縮・伸張（コンパンダー）を通す。" +
                "そのため小音量部でノイズが揺らぐ。" +
                "受信レベルが落ちるとドロップアウトが起きる",
            amount = "コンパンダーの影響は 8kHz 以上と小音量部で聞こえる。" +
                "ドロップアウトは受信バーが2本以下で頻発する",
            fixes = listOf(
                "アンテナを演者の見通し線上に置く。人の体は電波を大きく遮る",
                "アンテナと送信機の距離を 3m 以上離す（近すぎても飽和する）",
                "電池を新品にする。送信出力の低下がドロップアウトの主因",
            ),
            prevention = "本番前にステージ全域を歩いて受信レベルを確認する。" +
                "重要なチャンネルは有線にする",
        ),

        DegradationItem(
            title = "AD/DA の往復を重ねる",
            stage = DegradationStage.TRANSPORT,
            severity = DegradationSeverity.CREEPING,
            symptom = "1台通すごとに少しずつ曇る。遅延も増えていく",
            mechanism = "デジタル卓からアナログで外部機器へ出し、また戻すと" +
                "そのたびに DA と AD を通る。変換ごとにノイズと遅延が足される",
            amount = "1往復で 0.5〜2ms の遅延。ノイズは1回では気づかないが、" +
                "3往復すると分かる",
            fixes = listOf(
                "卓の内蔵エフェクトで済むものは内蔵で済ませる",
                "外部機器はデジタル接続（AES/EBU・Dante）で繋ぐ",
                "遅延が問題になるチャンネルは、卓のディレイで他を揃える",
            ),
            prevention = "系統図を書いて、変換の回数を数えておく",
        ),

        DegradationItem(
            title = "オーディオネットワークのパケットロス",
            stage = DegradationStage.TRANSPORT,
            severity = DegradationSeverity.HEAVY,
            symptom = "全チャンネルが同時に一瞬途切れる。または片方の卓だけ音が来ない",
            mechanism = "Dante などは通常の Ethernet に音を流している。" +
                "他のトラフィック（映像・PC のバックアップ）や" +
                "対応していないスイッチを混ぜると取りこぼしが起きる",
            amount = "レイテンシ設定 1ms は経路が短いときだけ成立する。" +
                "スイッチを2段以上通るなら 2〜5ms に上げる",
            fixes = listOf(
                "音声用のネットワークを他の用途から物理的に分ける",
                "レイテンシ設定を1段上げる",
                "省電力機能（EEE）付きのスイッチを外す。取りこぼしの典型的な原因",
            ),
            prevention = "音声ネットワークに PC や映像機器を挿さない。" +
                "スイッチは動作実績のある機種を使う",
        ),

        // --- アンプ・スピーカー ---

        DegradationItem(
            title = "リミッターの常時作動",
            stage = DegradationStage.OUTPUT,
            severity = DegradationSeverity.HEAVY,
            symptom = "フェーダーを上げても音量が増えない。" +
                "低域が入るたびに全体が沈む。曲の後半で音が小さく感じる",
            mechanism = "アンプやパワードスピーカーの保護リミッターが常時働くと、" +
                "低域の大きい成分に合わせて全帯域が押し下げられる",
            amount = "リミッターの LED が点灯し続ける状態では、実質 3〜6dB 圧縮されている",
            fixes = listOf(
                "サブローをハイパスで切る。40Hz 以下を削るとリミッターが外れる",
                "スピーカーを増やす。1台の限界は EQ では超えられない",
                "全体の音量を 3dB 下げる。客席では気づかれないことが多い",
            ),
            prevention = "会場のシステムの再生下限と最大音圧を、仕込みの前に確認する",
        ),

        DegradationItem(
            title = "パワーコンプレッション（ボイスコイルの発熱）",
            stage = DegradationStage.OUTPUT,
            severity = DegradationSeverity.CREEPING,
            symptom = "ライブの後半になるほど音が詰まる。" +
                "休憩明けに戻る。リハでは良かったのに本番で足りない",
            mechanism = "ボイスコイルが熱くなると直流抵抗が上がり、同じ電力でも音圧が下がる。" +
                "低域ユニットで顕著",
            amount = "定格付近で連続運転すると 2〜4dB、限界近くで 6dB 落ちることがある",
            fixes = listOf(
                "音量を下げる。冷える時間を作る",
                "スピーカーを増やして1台あたりの負担を減らす",
                "高域を上げて補おうとしない（詰まりの原因は低域側にある）",
            ),
            prevention = "リハで出せた音量が本番の上限とは限らない。" +
                "余裕を持ったスピーカーの台数で組む",
        ),

        DegradationItem(
            title = "スピーカーユニットの経年劣化",
            stage = DegradationStage.OUTPUT,
            severity = DegradationSeverity.CREEPING,
            symptom = "左右で音色が違う。特定の1台だけ低域が緩い、" +
                "または高域が出ていない",
            mechanism = "ウレタン製のエッジは 10年ほどで劣化して硬化・崩壊する。" +
                "ツイーターのダイアフラムは焼けると能率が落ちる",
            amount = "エッジ劣化で低域が 3〜6dB 減り、共振周波数が上がる。" +
                "ツイーターは焼けると高域が 6dB 以上落ちる",
            fixes = listOf(
                "左右を入れ替えて、症状がスピーカー側かどうかを切り分ける",
                "ピンクノイズを流して RTA で1台ずつ測り、差を見る",
                "その日は EQ で応急処置し、修理に出す印を付ける",
            ),
            prevention = "常設のシステムは年に1回、1台ずつ RTA で測って記録を残す",
        ),

        // --- 会場・空気 ---

        DegradationItem(
            title = "距離による空気吸収",
            stage = DegradationStage.VENUE,
            severity = DegradationSeverity.HEAVY,
            symptom = "前方はきれいなのに、後方だけ曇って聞こえる",
            mechanism = "空気そのものが高域を吸収する。距離に比例して増え、" +
                "湿度が低いほど強い。距離による -6dB/倍距離とは別に効く",
            amount = "20℃・湿度70% で 8kHz は 50m で約 3dB、100m で約 7dB 減る。" +
                "16kHz ではその倍以上",
            fixes = listOf(
                "遠くを狙うスピーカーだけ高域をシェルフで +3dB する",
                "ディレイスピーカーを立てて距離そのものを短くする",
                "屋外の乾燥した日は、想定より高域を足す",
            ),
            prevention = "スピーカーの配置を距離で分ける設計にする。1台で 50m を狙わない",
        ),

        DegradationItem(
            title = "ディレイスピーカーのタイミングずれ",
            stage = DegradationStage.VENUE,
            severity = DegradationSeverity.HEAVY,
            symptom = "後方でエコーのように二重に聞こえる。" +
                "言葉が聞き取りにくいのに音量は足りている",
            mechanism = "メインとディレイスピーカーの到達時間が合っていないと、" +
                "同じ音が時間差で2回届く。30ms を超えると別の音として聞こえる",
            amount = "音速 340m/s なので 1m = 2.9ms。" +
                "20m 後方なら 58ms のディレイが必要",
            fixes = listOf(
                "実測する（このアプリのディレイ実測ツール）。巻き尺の計算値は空調と気温でずれる",
                "計算するなら 距離[m] ÷ 340 × 1000 = ms",
                "合わせた後で、ディレイ側の音量を 3dB 下げると自然になる",
            ),
            prevention = "ディレイスピーカーは立てた日に必ず実測する。前回の設定値を流用しない",
        ),

        DegradationItem(
            title = "客入りによる会場特性の変化",
            stage = DegradationStage.VENUE,
            severity = DegradationSeverity.HEAVY,
            symptom = "リハでちょうどよかったのに、開場したら高域が足りず、" +
                "低域だけ膨らんで聞こえる",
            mechanism = "人体は中高域をよく吸収する。空席のときの反射が無くなり、" +
                "残響時間が短くなる。低域はあまり吸われないので相対的に増える",
            amount = "満席で中高域の残響が 30〜50% 短くなることがある。" +
                "体感では高域が 2〜3dB 減る",
            fixes = listOf(
                "開場後に1曲めで作り直す前提で構える",
                "リハの時点で高域を作り込みすぎない",
                "低域を 2dB 下げ、8kHz 以上を 2dB 上げるところから調整する",
            ),
            prevention = "リハの設定をそのまま本番に持ち込まない。客入り後の再調整を予定に入れる",
        ),

        // --- 録音・書き出し ---

        DegradationItem(
            title = "録音レベルの取りすぎ・取り足りなさ",
            stage = DegradationStage.RECORD,
            severity = DegradationSeverity.FATAL,
            symptom = "後で聞いたら、盛り上がった部分だけ割れている。" +
                "または全体が小さく、上げるとノイズだらけ",
            mechanism = "16bit の録音は 0dBFS を超えた瞬間に潰れる。" +
                "逆に小さく録るとビットを使い切れず、上げたときにノイズが目立つ",
            amount = "ピークを -6dBFS、平均を -18dBFS に置くのが目安。" +
                "16bit の理論ダイナミックレンジは 96dB",
            fixes = listOf(
                "録りながらピークメーターを見る。クリップ表示が出たらその場でゲインを下げる",
                "割れた録音は直せない。同じ曲をもう一度録るしかない",
            ),
            prevention = "本番の最も大きい部分を想定して 6dB の余裕を残す。" +
                "リハの音量でレベルを決めない",
        ),

        DegradationItem(
            title = "録った音の再圧縮と渡し方",
            stage = DegradationStage.RECORD,
            severity = DegradationSeverity.FATAL,
            symptom = "渡した音源が、自分が聞いた音より軽い",
            mechanism = "WAV で録った音をメールやチャットで送るときに" +
                "自動的に圧縮・変換されることがある。" +
                "一度圧縮されたものを WAV に戻しても中身は戻らない",
            amount = "多くのメッセージアプリは 128kbps 前後の AAC に変換する",
            fixes = listOf(
                "WAV のまま渡す。ファイル共有かストレージ経由にする",
                "容量が問題ならロスレス（FLAC）にする。非可逆圧縮にはしない",
            ),
            prevention = "渡す経路を先に決めてから録音のフォーマットを決める",
        ),
    )

    fun byStage(stage: DegradationStage): List<DegradationItem> =
        ALL.filter { it.stage == stage }

    fun bySeverity(severity: DegradationSeverity): List<DegradationItem> =
        ALL.filter { it.severity == severity }

    /**
     * 症状から引く。
     *
     * 現場で分かっているのは「ブーンと鳴る」「後ろだけ曇る」といった症状だけなので、
     * 原因名ではなく症状の言葉で当たるようにしている。
     */
    fun search(query: String): List<DegradationItem> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return ALL
        return ALL.filter { it.searchText.contains(needle) }
    }
}
