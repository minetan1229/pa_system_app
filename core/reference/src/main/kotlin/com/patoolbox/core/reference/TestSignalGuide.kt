package com.patoolbox.core.reference

/** テスト信号の種類。探すときの入口になる。 */
enum class TestSignalKind(val label: String, val description: String) {
    NOISE("ノイズ", "全帯域を一度に鳴らす。系全体の特性を見るのに使う"),
    TONE("単音・波形", "1つの周波数だけを鳴らす。レベルと歪みを追い込むのに使う"),
    SWEEP("スイープ", "周波数を動かしながら鳴らす。応答と遅延を測るのに使う"),
    SPECIAL("確認用", "極性・結線・チャンネルの取り違えを見つけるための信号"),
}

/**
 * テスト信号1つぶんの説明。
 *
 * @param slopeDbPerOctave 1オクターブあたりのレベルの傾き。ノイズの色を数字で示す。
 *   白 = 0、ピンク = -3、ブラウン = -6、青 = +3。null は傾きで表せない信号
 * @param soundsLike どう聞こえるか。耳で区別できるようにするための1行
 * @param whatItIs 何が起きているか。仕組みの説明
 * @param useFor 何に使うか
 * @param cautions 事故と誤解のもと
 * @param levelTip レベルの決め方。クレストファクタが信号ごとに違うので必ず書く
 * @param inThisApp この app のどこで出せるか。null なら app には入っていない
 */
data class TestSignalEntry(
    val name: String,
    val english: String,
    val kind: TestSignalKind,
    val slopeDbPerOctave: Double?,
    val soundsLike: String,
    val whatItIs: String,
    val useFor: List<String>,
    val cautions: List<String>,
    val levelTip: String,
    val inThisApp: String? = null,
    val aliases: List<String> = emptyList(),
) {
    val searchText: String
        get() = buildString {
            append(name).append(' ')
            append(english).append(' ')
            append(kind.label).append(' ')
            append(soundsLike).append(' ')
            append(whatItIs).append(' ')
            useFor.forEach { append(it).append(' ') }
            cautions.forEach { append(it).append(' ') }
            append(levelTip).append(' ')
            append(inThisApp.orEmpty()).append(' ')
            aliases.forEach { append(it).append(' ') }
        }.lowercase()
}

/**
 * テスト信号の辞典。
 *
 * 「ピンクノイズを流してください」と言われて流せる人は多いが、
 * **なぜピンクなのか**を説明できる人は少ない。理由が分かっていないと、
 * ホワイトノイズで RTA を見て「高域が出すぎている」と誤診する事故が起きる。
 *
 * そこで色（傾き）を数字で示し、どう聞こえるか・何に使うか・何を間違えやすいかを
 * 1枚に並べている。
 */
object TestSignalGuide {

    val ALL: List<TestSignalEntry> = listOf(

        // ------------------------------------------------------------------
        // ノイズ
        // ------------------------------------------------------------------

        TestSignalEntry(
            name = "ピンクノイズ",
            english = "Pink noise (1/f noise)",
            kind = TestSignalKind.NOISE,
            slopeDbPerOctave = -3.0,
            aliases = listOf("pink", "1/f", "ピンク"),
            soundsLike = "滝の音、遠くの雨。ホワイトノイズより低く落ち着いて聞こえる",
            whatItIs = "1オクターブごとにエネルギーが等しくなるノイズ。" +
                "オクターブが上がるたびに帯域幅は倍になるので、" +
                "エネルギーを一定に保つには 1Hz あたりの密度を半分（-3dB/oct）にする必要がある。" +
                "人の耳は周波数を対数で捉えるため、この配り方が「どの帯域も同じ量」に聞こえる",
            useFor = listOf(
                "RTA でシステムの周波数特性を見る（1/3オクターブ表示で平らになるのが基準）",
                "スピーカーの左右・上下の音色を揃える",
                "会場の暗騒音より十分大きいレベルで、部屋の癖を掴む",
                "アンプとスピーカーの通電確認（音楽より断線に気づきやすい）",
            ),
            cautions = listOf(
                "1/1 や 1/3 オクターブ表示では平らになるが、FFT（等間隔）表示では右下がりに見える。" +
                    "表示方式を確認せずに「高域が足りない」と判断しない",
                "実際の音楽より高域のエネルギーが多い。ツイータを焼く事故はたいていこれ",
                "測定用のマイクとスピーカーの向きで結果が変わる。1本の測定で結論を出さない",
            ),
            levelTip = "クレストファクタは約 4:1（12dB）。" +
                "ピークが -6dBFS に収まるよう、実効値で -18dBFS 前後に置く。" +
                "現場では会場の暗騒音より 20dB 以上高く、かつ客席で 85dB(A) を超えない範囲",
            inThisApp = "シグナルジェネレータ → ピンクノイズ。RTA と並べて使う",
        ),

        TestSignalEntry(
            name = "ホワイトノイズ",
            english = "White noise",
            kind = TestSignalKind.NOISE,
            slopeDbPerOctave = 0.0,
            aliases = listOf("white", "ホワイト"),
            soundsLike = "テレビの砂嵐、蒸気。ピンクノイズより明らかに高くシャーッとする",
            whatItIs = "1Hz あたりのエネルギーがどの周波数でも等しいノイズ。" +
                "つまり 100〜200Hz（100Hz 幅）と 10k〜10.1kHz（100Hz 幅）に同じだけ入っている。" +
                "オクターブで見ると上に行くほど帯域幅が倍々になるので、" +
                "1オクターブあたりでは +3dB/oct の右上がりになる",
            useFor = listOf(
                "電子回路・機器単体の特性測定（人の聴感ではなく物理量を見るとき）",
                "ノイズゲートやディエッサーの動作確認",
                "高域側の異常（ツイータの断線、ネットワークの不良）を耳で見つける",
            ),
            cautions = listOf(
                "**スピーカーの試験に使わない。** 高域のエネルギーが音楽よりはるかに多く、" +
                    "ツイータを短時間で焼く。同じ表示レベルでもピンクより危険",
                "RTA で見ると必ず右上がりになる。それはシステムの異常ではなく信号の性質",
                "耳への刺激が強い。長時間流すと聴力の判断が鈍る",
            ),
            levelTip = "クレストファクタはピンクとほぼ同じ（約12dB）だが、" +
                "高域に集中しているぶん実効値を 6dB 低く扱うくらいで丁度よい",
            inThisApp = "シグナルジェネレータ → ホワイトノイズ",
        ),

        TestSignalEntry(
            name = "ブラウンノイズ",
            english = "Brown / Red noise",
            kind = TestSignalKind.NOISE,
            slopeDbPerOctave = -6.0,
            aliases = listOf("brown", "red", "レッド", "茶"),
            soundsLike = "滝の音をさらに低くしたもの。風呂場の換気扇や遠雷に近い",
            whatItIs = "ホワイトノイズを積分した信号（ブラウン運動に由来。色の茶色ではない）。" +
                "-6dB/oct で、ピンクよりさらに低域に寄っている",
            useFor = listOf(
                "サブウーファーの動作確認と位置決め",
                "低域だけの共振・ビビりを探す（会場の建具が鳴る場所を見つける）",
            ),
            cautions = listOf(
                "低域に全エネルギーが集中するので、ウーファーのストロークを使い切りやすい",
                "小さい音量に聞こえても、アンプは大きな電力を出している",
            ),
            levelTip = "耳での大きさとアンプの負担が最も食い違う信号。" +
                "メーターで -20dBFS を超えない範囲から始める",
        ),

        TestSignalEntry(
            name = "ブルーノイズ・バイオレットノイズ",
            english = "Blue / Violet noise",
            kind = TestSignalKind.NOISE,
            slopeDbPerOctave = 3.0,
            aliases = listOf("blue", "violet", "青", "紫"),
            soundsLike = "ホワイトノイズよりさらに細く鋭い「シー」",
            whatItIs = "ブルーが +3dB/oct、バイオレットが +6dB/oct。" +
                "ピンクとブラウンをそれぞれ裏返した関係にある",
            useFor = listOf(
                "ディザ（量子化ノイズを聴こえにくい帯域へ追いやる）の理屈の説明",
                "高域の測定分解能を稼ぐ用途。PA の現場で出す場面はほぼ無い",
            ),
            cautions = listOf(
                "PA でスピーカーに入れる理由がない。ツイータを壊すだけ",
            ),
            levelTip = "現場では使わない。使うなら回路の入力までで止める",
        ),

        TestSignalEntry(
            name = "帯域制限ノイズ",
            english = "Band-limited / Band-passed noise",
            kind = TestSignalKind.NOISE,
            slopeDbPerOctave = null,
            aliases = listOf("バンドパス", "オクターブノイズ"),
            soundsLike = "ノイズだが、幅の狭い帯域だけが鳴っている「フー」",
            whatItIs = "ピンクノイズを1/1 または 1/3 オクターブで切り出したもの。" +
                "その帯域だけにエネルギーを集める",
            useFor = listOf(
                "会場のどの帯域で定在波が立つかを1つずつ確かめる",
                "ハウリングしやすい帯域を安全に探る（狭いのでゲインを追い込みやすい）",
                "スピーカーごとの位相合わせ（クロスオーバー周辺の帯域だけで見る）",
            ),
            cautions = listOf(
                "低い帯域を選ぶとウーファーだけに全電力が入る。レベルは毎回入れ直す",
                "1点で測ると部屋のモードで大きく変わる。客席で3点以上動いて確かめる",
            ),
            levelTip = "全帯域ノイズと同じ表示レベルでも、" +
                "1つのスピーカーに集中するぶん負担は大きい。10dB 下げて始める",
        ),

        // ------------------------------------------------------------------
        // 単音・波形
        // ------------------------------------------------------------------

        TestSignalEntry(
            name = "サイン波（正弦波）",
            english = "Sine wave",
            kind = TestSignalKind.TONE,
            slopeDbPerOctave = null,
            aliases = listOf("sine", "正弦", "単音", "純音"),
            soundsLike = "「ポー」という濁りのない1つの音。倍音が全く無い",
            whatItIs = "1つの周波数だけのもっとも単純な信号。" +
                "倍音を含まないので、出てきた音に倍音があれば、それは全部システムの歪み",
            useFor = listOf(
                "1kHz 基準でゲイン構成を組む（各段のメーターを合わせていく）",
                "歪みの確認（音が濁ったらその段で歪んでいる）",
                "ビビり・共振の発生源を特定する（周波数を動かして鳴る場所を探す）",
                "チューナー・メーターの動作確認",
            ),
            cautions = listOf(
                "**連続して出すとツイータとホーンが最も壊れやすい。** " +
                    "音楽と違い休みが無いので、ボイスコイルが冷える時間がない",
                "低い音ほど大きく聞こえないが、ウーファーは大きく動いている",
                "定在波の腹と節で音量が場所ごとに大きく変わる。1点で判断しない",
            ),
            levelTip = "クレストファクタは 3dB（√2）しかない。" +
                "同じピーク値ならノイズより実効値が 9dB 高い＝アンプの負担が大きい。" +
                "スピーカーに入れるときは -20dBFS から始めて、短時間で切る",
            inThisApp = "シグナルジェネレータ → サイン波。周波数を直接入力できる",
        ),

        TestSignalEntry(
            name = "1kHz 基準トーン",
            english = "1 kHz reference tone",
            kind = TestSignalKind.TONE,
            slopeDbPerOctave = null,
            aliases = listOf("基準信号", "テストトーン", "リファレンス"),
            soundsLike = "電話の話中音に近い「プー」",
            whatItIs = "レベルの基準として世界中で使われている周波数。" +
                "1kHz を選ぶのは、人の聴感の重み付け（A特性）がほぼ 0dB になり、" +
                "機材の特性も最も素直な帯域だから",
            useFor = listOf(
                "卓・アンプ・録音機のレベルを合わせる（0dBu = 0.775V が基準）",
                "回線の導通確認。どこまで信号が来ているかを1段ずつ追う",
                "納品する音声ファイルの頭に付ける基準トーン",
            ),
            cautions = listOf(
                "0VU の位置は機材で違う（+4dBu 基準と -10dBV 基準で 11.8dB ずれる）",
                "スピーカーから長時間出すとかなり耳障り。回線確認はヘッドホンで行う",
            ),
            levelTip = "デジタルでは -20dBFS または -18dBFS を 0VU とする流儀がある。" +
                "現場に入る前にどちらの基準か確認する",
        ),

        TestSignalEntry(
            name = "方形波・三角波・ノコギリ波",
            english = "Square / Triangle / Sawtooth",
            kind = TestSignalKind.TONE,
            slopeDbPerOctave = null,
            aliases = listOf("square", "矩形波", "saw"),
            soundsLike = "方形波はブザー、ノコギリ波は弦楽器に近いざらついた音",
            whatItIs = "倍音を規則的に含む波形。方形波は奇数倍音のみ、" +
                "ノコギリ波は全ての倍音を含む。倍音の減り方は -6dB/oct（方形・ノコギリ）",
            useFor = listOf(
                "アンプの立ち上がり（スルーレート）とリンギングを見る",
                "デジタル機器の折り返しノイズを見つける",
                "シンセの音作りの説明",
            ),
            cautions = listOf(
                "**高域のエネルギーが極端に多い。スピーカーに入れない。** " +
                    "特に方形波は電力の点でアンプの限界を超えやすい",
                "測定はオシロかアナライザで行い、スピーカーからは出さない",
            ),
            levelTip = "方形波のクレストファクタは 0dB（ピーク＝実効値）。" +
                "同じピークならサイン波より 3dB、ノイズより 12dB 電力が大きい",
        ),

        // ------------------------------------------------------------------
        // スイープ
        // ------------------------------------------------------------------

        TestSignalEntry(
            name = "ログスイープ",
            english = "Logarithmic sine sweep (ESS)",
            kind = TestSignalKind.SWEEP,
            slopeDbPerOctave = -3.0,
            aliases = listOf("sweep", "スイープ", "ess", "対数スイープ"),
            soundsLike = "低い音から高い音へ「ヒューン」と上がる。低い側にゆっくり留まる",
            whatItIs = "周波数を対数で（1オクターブあたり同じ時間で）動かすスイープ。" +
                "結果としてエネルギーの配り方はピンクノイズと同じ -3dB/oct になる。" +
                "録った音を逆フィルタで畳み込むとインパルス応答が得られ、" +
                "**歪みの成分が時間軸で分離される**のが最大の利点",
            useFor = listOf(
                "インパルス応答の測定（残響 RT60・反射の把握）",
                "スピーカーまでの距離＝ディレイ時間の実測",
                "極性の確認",
                "暗騒音がある会場でも S/N を稼げる（同じ測定を何度も足し合わせられる）",
            ),
            cautions = listOf(
                "**大音量が出る。** PA に繋いだ状態で不用意に鳴らさない",
                "測定中に人が動くと結果が濁る。静かな時間を確保して行う",
                "スイープが短すぎると低域の分解能が足りない。低域を見るなら 5 秒以上",
            ),
            levelTip = "ピークは一定で、実効値はサイン波と同じ。" +
                "測定は -12dBFS 前後のピークで始め、暗騒音との差を見て上げる",
            inThisApp = "ディレイ実測・極性チェック・残響測定の3つが、" +
                "どれも内部ではこのスイープを鳴らして録っている",
        ),

        TestSignalEntry(
            name = "リニアスイープ",
            english = "Linear sweep",
            kind = TestSignalKind.SWEEP,
            slopeDbPerOctave = 0.0,
            aliases = listOf("リニア", "linear sweep"),
            soundsLike = "低い側をあっという間に通り過ぎ、高域に長く留まる「ピューン」",
            whatItIs = "周波数を等間隔（Hz 単位）で動かすスイープ。" +
                "エネルギーの配り方はホワイトノイズと同じ平坦になる",
            useFor = listOf(
                "高域の細かい特性を見たいとき",
                "機器単体の測定（スピーカーを介さない測定）",
            ),
            cautions = listOf(
                "低域に時間を割かないので、部屋の測定には向かない",
                "高域にエネルギーが偏る。スピーカーで長く鳴らさない",
            ),
            levelTip = "ログスイープと同じ扱い。高域側の負担だけ大きい",
        ),

        TestSignalEntry(
            name = "MLS（最大長系列）",
            english = "Maximum Length Sequence",
            kind = TestSignalKind.SWEEP,
            slopeDbPerOctave = 0.0,
            aliases = listOf("mls", "疑似ランダム"),
            soundsLike = "ホワイトノイズと区別がつかない「シャー」",
            whatItIs = "決まった長さで繰り返す疑似ランダム信号。" +
                "自己相関を取るとインパルス応答が出る。計算が軽いので古くから使われた",
            useFor = listOf(
                "インパルス応答の測定（現在はログスイープの方が一般的）",
            ),
            cautions = listOf(
                "歪みが測定結果全体に散らばる。ログスイープのように分離できない",
                "測定中に温度や風で音速が変わると結果が崩れる",
            ),
            levelTip = "ノイズと同じ扱い。実効値で -18dBFS 前後",
        ),

        // ------------------------------------------------------------------
        // 確認用
        // ------------------------------------------------------------------

        TestSignalEntry(
            name = "極性チェック信号",
            english = "Polarity test pulse",
            kind = TestSignalKind.SPECIAL,
            slopeDbPerOctave = null,
            aliases = listOf("ポラリティ", "位相チェック", "逆相"),
            soundsLike = "「ポコッ」という非対称なパルスの繰り返し",
            whatItIs = "波形の＋側と−側が非対称なパルス。" +
                "受け側で最初の振れがどちらへ出たかを見て、経路のどこかで＋−が入れ替わっていないかを判定する",
            useFor = listOf(
                "スピーカーの結線ミス（＋−逆）を見つける",
                "複数のスピーカー・マイクの向きを揃える",
            ),
            cautions = listOf(
                "極性（＋−の向き）と位相（時間のずれ）は別物。" +
                    "極性が合っていても距離差があれば打ち消しは起きる",
                "マルチウェイのスピーカーは帯域ごとに極性が違う設計もある",
            ),
            levelTip = "小さい音で足りる。-30dBFS でも判定できる",
            inThisApp = "極性チェック。スイープを鳴らして応答の向きから判定する",
        ),

        TestSignalEntry(
            name = "L/R 識別信号",
            english = "Channel identification",
            kind = TestSignalKind.SPECIAL,
            slopeDbPerOctave = null,
            aliases = listOf("チャンネル確認", "左右"),
            soundsLike = "「レフト」「ライト」の音声、または片チャンネルだけのノイズ",
            whatItIs = "片方のチャンネルだけに信号を入れて、どちらのスピーカーから出るかを確かめる",
            useFor = listOf(
                "配線の左右取り違えを見つける（本番直前に必ず確認する項目）",
                "マトリクス・ゾーンごとの送り先の確認",
            ),
            cautions = listOf(
                "モノラル送りになっていると左右の違いが出ない。まずモノ／ステレオの設定を確認",
            ),
            levelTip = "客席で普通に聞こえる程度で十分",
        ),

        TestSignalEntry(
            name = "2信号（混変調テスト）",
            english = "Two-tone IMD test",
            kind = TestSignalKind.SPECIAL,
            slopeDbPerOctave = null,
            aliases = listOf("imd", "混変調", "相互変調"),
            soundsLike = "2つの音が同時に鳴り、うなりが聞こえる",
            whatItIs = "周波数の違う2つのサイン波を同時に入れる。" +
                "系が非線形だと、元の信号に無い和と差の周波数が生まれる。" +
                "その量が混変調歪み（IMD）",
            useFor = listOf(
                "アンプ・卓の歪みの確認",
                "ワイヤレスの周波数配置の考え方の説明（同じ理屈で相互変調が起きる）",
            ),
            cautions = listOf(
                "スピーカーで測ると部屋の影響が混ざる。機器単体で測る",
            ),
            levelTip = "2波の合計でクリップしないよう、それぞれ 6dB 下げて入れる",
            inThisApp = "ワイヤレス周波数調整。無線側の相互変調を計算で出す",
        ),
    )

    fun byKind(kind: TestSignalKind): List<TestSignalEntry> = ALL.filter { it.kind == kind }

    fun search(query: String): List<TestSignalEntry> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return ALL
        return ALL.filter { it.searchText.contains(normalized) }
    }
}
