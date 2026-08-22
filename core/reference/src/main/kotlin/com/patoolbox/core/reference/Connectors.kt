package com.patoolbox.core.reference

/**
 * コネクタの分け方。
 *
 * 並びは現場で出会う順ではなく **間違えたときの被害の大きさ順** に近づけてある。
 * アナログの入り口を間違えても音が出ないだけだが、スピーカーと電源は機材が壊れる。
 * デジタルとネットワークは「繋がっているのに音が出ない」で時間を溶かす。
 */
enum class ConnectorCategory(val label: String, val description: String) {
    LINE("ライン・マイク", "信号の入り口。ここを間違えても壊れはしないが、音が出ない・ノイズが乗る"),
    SPEAKER("スピーカー", "電流を流す線。間違えると機材が壊れる。迷ったら繋がない"),
    POWER("電源", "落ちると全部止まる。容量と経路を最初に決める"),
    DIGITAL("デジタル音声", "繋がっていても、クロックと周波数が合わないと音が出ない"),
    NETWORK("ネットワーク音響", "Dante / AES67 など。スイッチの設定まで含めて1つの機材と考える"),
    UTILITY("その他・変換", "DI、ファンタム、制御系。事故はたいていここの理解不足から出る"),
}

data class PinAssignment(val pin: String, val signal: String)

/**
 * コネクタ1つぶんの説明。
 *
 * @param cautions 現場で事故になりやすい点。ここが本体
 * @param advanced 上級者向けの補足。知らなくても仕事は回るが、
 *   知っていると原因不明の不具合を1つ潰せる、という粒度で書く
 * @param aliases 検索に引っかけたい別名。カタカナ・英字・通称のずれを吸収する
 */
data class Connector(
    val name: String,
    val category: ConnectorCategory,
    val summary: String,
    val pins: List<PinAssignment>,
    val cautions: List<String> = emptyList(),
    val advanced: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
) {
    internal val searchText: String = buildString {
        append(name).append(' ')
        append(summary).append(' ')
        append(category.label).append(' ')
        pins.forEach { append(it.pin).append(' ').append(it.signal).append(' ') }
        cautions.forEach { append(it).append(' ') }
        advanced.forEach { append(it).append(' ') }
        aliases.forEach { append(it).append(' ') }
    }.lowercase()
}

/**
 * コネクタのピン配列と注意点。
 *
 * ピン番号そのものより「間違えると何が起きるか」を書くことを優先している。
 * 配線図は調べれば出てくるが、現場で必要なのは
 * 「これを繋ぐと壊れるのか、音が出ないだけなのか」の判断のため。
 *
 * [ALL] の並びは重要度順（＝現場で先に知っておくべき順）。
 * カテゴリで畳んでも、その中の順番は保たれる。
 */
object Connectors {

    val ALL: List<Connector> = listOf(
        // ------------------------------------------------------------------
        // ライン・マイク
        // ------------------------------------------------------------------
        Connector(
            name = "XLR（3ピン）",
            category = ConnectorCategory.LINE,
            summary = "バランス接続の基本。マイクとライン両方に使う",
            aliases = listOf("キャノン", "cannon", "xlr3"),
            pins = listOf(
                PinAssignment("1", "グランド（シールド）"),
                PinAssignment("2", "ホット（+ / 正相）"),
                PinAssignment("3", "コールド（− / 逆相）"),
            ),
            cautions = listOf(
                "古い日本製機材には2番コールドのものがある。混在すると片方だけ逆相になる",
                "1番は必ずシャーシに落とす。浮かせるとノイズを拾う",
                "ファンタムは2番・3番に同電位で乗る。1番との間に電圧がかかる",
                "オスが出力、メスが入力。信号は「オス→メス」の向きに流れる",
            ),
            advanced = listOf(
                "バランスがノイズに強いのは、2番と3番に同じように乗った雑音を" +
                    "受け側の差動増幅が引き算で消すため。この効果の大きさを CMRR と呼ぶ",
                "1番を機器の入り口でシャーシに落とすのが正しい（ピン1問題）。" +
                    "基板のグランドに落とす古い設計は、高周波を回路内に引き込んでノイズの原因になる",
                "マイクケーブルは特性インピーダンスを規定していない。" +
                    "AES/EBU をマイクケーブルで送ると距離が伸びないのはこのため",
            ),
        ),
        Connector(
            name = "TRS フォン（バランス）",
            category = ConnectorCategory.LINE,
            summary = "バランスのライン接続。XLR と同じ信号を3極で送る",
            aliases = listOf("ステレオフォン", "trs", "6.3mm", "1/4インチ"),
            pins = listOf(
                PinAssignment("T（チップ）", "ホット（+）"),
                PinAssignment("R（リング）", "コールド（−）"),
                PinAssignment("S（スリーブ）", "グランド"),
            ),
            cautions = listOf(
                "同じ形状でステレオ（T=L, R=R）にも使う。挿す先で意味が変わる",
                "バランス出力にステレオケーブルを挿すと片チャンネルが逆相で回る",
                "抜き差しの途中で一瞬ショートする。必ずフェーダーを下げてから",
            ),
            advanced = listOf(
                "TRS には「バランス」「ステレオ」「インサート」の3つの意味がある。" +
                    "形が同じでも中身が違うので、ケーブルに用途を書いておくと事故が減る",
                "アンバランス機器にバランスケーブルを挿すと、リングがスリーブに落ちて" +
                    "コールド側が短絡する。トランス出力の機材ではこれが発熱の原因になる",
            ),
        ),
        Connector(
            name = "TS フォン（アンバランス）",
            category = ConnectorCategory.LINE,
            summary = "楽器用。ノイズに弱いので長く引かない",
            aliases = listOf("モノフォン", "ts", "シールド", "ギターケーブル"),
            pins = listOf(
                PinAssignment("T（チップ）", "信号"),
                PinAssignment("S（スリーブ）", "グランド"),
            ),
            cautions = listOf(
                "5m を超えると高域が落ち、ノイズを拾いやすくなる。DI を使うこと",
                "スピーカーケーブルとして使わない。細すぎて発熱する",
            ),
            advanced = listOf(
                "高域が落ちるのはケーブルの静電容量とピックアップの内部抵抗で" +
                    "ローパスができるため。パッシブの楽器ほど影響が大きい",
                "アクティブの楽器やエフェクターの後なら、出力インピーダンスが低いので" +
                    "同じ長さでも劣化はずっと小さい",
            ),
        ),
        Connector(
            name = "インサート（TRS 1本）",
            category = ConnectorCategory.LINE,
            summary = "1本で送りと戻りを兼ねる。卓のインサート端子に使う",
            aliases = listOf("insert", "インサーション"),
            pins = listOf(
                PinAssignment("T（チップ）", "センド（多くの機種）"),
                PinAssignment("R（リング）", "リターン（多くの機種）"),
                PinAssignment("S（スリーブ）", "グランド"),
            ),
            cautions = listOf(
                "T と R が逆の機種がある。卓の取説を必ず確認する",
                "半挿しでセンドだけ取る使い方があるが、接触不良の原因になる",
            ),
            advanced = listOf(
                "アンバランスで送り出すので、インサートケーブルは短く。" +
                    "長い外部機器へ送るならインサートではなく AUX とリターンを使う",
            ),
        ),
        Connector(
            name = "RCA（ピン）",
            category = ConnectorCategory.LINE,
            summary = "民生機器の接続。レベルが -10dBV でプロ機材より低い",
            aliases = listOf("ピンジャック", "rca", "cinch"),
            pins = listOf(
                PinAssignment("中心", "信号"),
                PinAssignment("外周", "グランド"),
            ),
            cautions = listOf(
                "+4dBu の機材と繋ぐと約12dB のレベル差が出る。DI かマッチング機器を挟む",
                "挿すとき中心が先に触れる。電源が入ったままだとノイズが出る",
            ),
            advanced = listOf(
                "民生機器とプロ機材をアンバランスで繋ぐと、機器間のグランド電位差が" +
                    "そのままハムになる。アイソレーショントランス入りの DI が確実",
            ),
        ),
        Connector(
            name = "3.5mm ステレオミニ",
            category = ConnectorCategory.LINE,
            summary = "PC・スマホからの音出し。現場で一番よく使う「持ち込み」の口",
            aliases = listOf("ミニプラグ", "mini", "3.5", "イヤホンジャック", "aux"),
            pins = listOf(
                PinAssignment("T（チップ）", "左"),
                PinAssignment("R（リング）", "右"),
                PinAssignment("S（スリーブ）", "グランド"),
            ),
            cautions = listOf(
                "4極（TRRS）はマイク付き。3極の口に挿すと右が出ないことがある",
                "抜けやすい。テープで留めるか、L 字プラグで足を作る",
                "端末側の音量が信号レベルを決める。まず端末を 70〜80% に固定してから卓で取る",
            ),
            advanced = listOf(
                "スマホの出力はアンバランスで、充電しながらだとノイズが乗ることがある。" +
                    "USB オーディオか、アイソレーション付き DI を通すと消える",
                "「本番中に着信で音が止まる」を避けるため、持ち込み端末は機内モードにしてもらう",
            ),
        ),
        Connector(
            name = "マルチケーブル・ステージボックス",
            category = ConnectorCategory.LINE,
            summary = "ステージと卓の間を1本でまとめる。番号の対応が命",
            aliases = listOf("マルチ", "スネーク", "snake", "ステージボックス", "分岐"),
            pins = listOf(
                PinAssignment("IN 1〜n", "ステージ側の入力（メス）"),
                PinAssignment("OUT / RTN", "ステージへ返す出力（オス）"),
            ),
            cautions = listOf(
                "番号は必ずパッチ表と一致させる。現場で番号を読み替え始めると必ず崩れる",
                "戻り（RTN）のオス・メスは入力と逆。挿さらないのは向きが正しい証拠",
                "踏まれる場所は必ずカバーを掛ける。断線は本番中にいちばん直しにくい",
            ),
            advanced = listOf(
                "アナログマルチは1本の中で隣どうしが結合する（クロストーク）。" +
                    "ファンタムを掛けたコンデンサマイクの隣に微弱なリボンを通さない",
                "デジタルスネーク（Dante / MADI など）に替えると本数と重量が激減するが、" +
                    "「電源が要る」「スイッチが要る」「復旧に時間がかかる」が新しく増える",
            ),
        ),

        Connector(
            name = "ミニ XLR（TA3 / TA4 / TA5）",
            category = ConnectorCategory.LINE,
            summary = "ワイヤレス送信機とヘッドセット・ピンマイクを繋ぐ小型コネクタ",
            aliases = listOf("ta3", "ta4", "ta5", "ミニキャノン", "ヘッドセット", "ピンマイク", "ラベリア"),
            pins = listOf(
                PinAssignment("TA3（3ピン）", "1=グランド / 2=バイアス（+5V前後） / 3=信号"),
                PinAssignment("TA4（4ピン）", "1=グランド / 2=バイアス / 3=信号 / 4=識別抵抗"),
                PinAssignment("TA5（5ピン）", "2ch ぶん。2本のマイクを1本で送る"),
            ),
            cautions = listOf(
                "メーカーごとに配線が違う。Shure と Sennheiser の TA4 は互換性がない。" +
                    "挿さるからといって使えるとは限らない",
                "TA4 の4番ピンの抵抗値でマイクの種類を判別する機種がある。" +
                    "自作や変換で抵抗を省くと、音が出ないか歪む",
                "細くて折れやすい。演者に付けたまま引っ張られると根元から断線する",
            ),
            advanced = listOf(
                "ここで送っているのはバランスではなく、バイアス電圧付きのアンバランス。" +
                    "XLR と同じ名前でも中身は別物と考えること",
                "ヘッドセットの断線は「動くと切れる」形で出る。" +
                    "本番前に演者に頭を振ってもらって確かめると事故が減る",
            ),
        ),
        Connector(
            name = "コンボジャック（XLR + TRS）",
            category = ConnectorCategory.LINE,
            summary = "1つの穴に XLR とフォンの両方が挿さる入力。省スペースだが取り違えが起きる",
            aliases = listOf("combo", "コンボ", "ノイトリック", "neutrik"),
            pins = listOf(
                PinAssignment("XLR 側", "マイク入力。ファンタムが送られる"),
                PinAssignment("TRS 側", "ライン入力。機種によりファンタムは来ない"),
            ),
            cautions = listOf(
                "XLR とフォンでゲイン段が違う機種がある。同じつまみの位置でもレベルが変わる",
                "フォンを挿すとファンタムが切れる設計が多い。アクティブ DI をフォンで挿すと動かない",
                "抜き挿しを繰り返すと接点が甘くなる。本番で使う卓のコンボは仕込みで確認する",
            ),
            advanced = listOf(
                "フォン側は TRS と TS の両方を受けるが、TS を挿すとリングがグランドに落ちる。" +
                    "ハムが出るときはここを疑う",
            ),
        ),
        Connector(
            name = "バンタム（TT）・パッチベイ",
            category = ConnectorCategory.LINE,
            summary = "回線を差し替えるための小型フォン。ホールの常設設備で出会う",
            aliases = listOf("tt", "bantam", "パッチ盤", "ジャックフィールド", "パッチベイ"),
            pins = listOf(
                PinAssignment("T（チップ）", "ホット"),
                PinAssignment("R（リング）", "コールド"),
                PinAssignment("S（スリーブ）", "グランド"),
            ),
            cautions = listOf(
                "ノーマル結線（何も挿さないと内部で繋がっている）が生きている盤がある。" +
                    "挿した瞬間に別の回線が切れることがあるので、盤の結線図を先に見る",
                "本番中の抜き挿しは大きなノイズになる。必ず出力をミュートしてから行う",
                "接点が汚れると片側だけ落ちる。ガリが出る盤は挿し直しではなく清掃",
            ),
            advanced = listOf(
                "ハーフノーマルとフルノーマルの違いを押さえる。" +
                    "ハーフは挿しても元の回線が生きたまま分岐し、フルは切り替わる",
            ),
        ),
        Connector(
            name = "EDAC / ELCO（マルチピン）",
            category = ConnectorCategory.LINE,
            summary = "多回線を1つの箱で繋ぐ角形コネクタ。ホールの壁とアンプラックで使う",
            aliases = listOf("edac", "elco", "マルチピン", "角型"),
            pins = listOf(
                PinAssignment("20 / 38 / 56 ピン", "1回線あたり3ピン（ホット・コールド・グランド）"),
                PinAssignment("ガイドピン", "向きを決める突起。無理に挿すとピンが曲がる"),
            ),
            cautions = listOf(
                "ピン配列は施工ごとに違う。同じ形でも会場が変われば中身は別",
                "曲がったピンを無理に挿すと相手側の受けを壊す。入らないときは触らず担当者を呼ぶ",
                "ラッチを締めずに使うと自重で抜ける",
            ),
            advanced = listOf(
                "ホールの壁のコネクタは、卓側の回線番号と一致しないことがある。" +
                    "仕込みの最初に1回線ずつトーンを流して対応を取る",
            ),
        ),

        // ------------------------------------------------------------------
        // スピーカー
        // ------------------------------------------------------------------
        Connector(
            name = "Speakon NL4",
            category = ConnectorCategory.SPEAKER,
            summary = "スピーカー接続の標準。2系統（4極）を1本で送れる",
            aliases = listOf("スピコン", "speakon", "nl4"),
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
                "回してロックするまで挿す。半挿しは接触抵抗で発熱する",
            ),
            advanced = listOf(
                "1本で2台のスピーカーを鳴らす「1系統目＝1台目、2系統目＝2台目」の使い方がある。" +
                    "デイジーチェーンで2台目に送るとき、ケーブル内で 2+/2− を 1+/1− に入れ替える" +
                    "「渡し」のケーブルを使う。これを普通のケーブルと混ぜると音が出ない",
                "太さは 2.0sq 以上を基本に。細い線はダンピングファクタを落とし、低音が緩む",
            ),
        ),
        Connector(
            name = "Speakon NL2 / NL8",
            category = ConnectorCategory.SPEAKER,
            summary = "2極と8極。NL8 は3ウェイ以上のバイアンプ運用で使う",
            aliases = listOf("nl2", "nl8", "スピコン"),
            pins = listOf(
                PinAssignment("1+/1−", "1系統目"),
                PinAssignment("2+/2− …", "2系統目以降（NL8 は4系統）"),
            ),
            cautions = listOf(
                "NL8 のプラグは NL4 のジャックには挿さらない。逆は挿さる",
                "系統の割り当ては機種ごとに違う。取説を見ずに繋がない",
            ),
        ),
        Connector(
            name = "フォン（スピーカー用）・裸線",
            category = ConnectorCategory.SPEAKER,
            summary = "小規模で残っている接続。感電と短絡の危険があるので扱いに注意",
            aliases = listOf("バナナ", "banana", "裸線", "ターミナル"),
            pins = listOf(
                PinAssignment("T（チップ）", "＋"),
                PinAssignment("S（スリーブ）", "−"),
            ),
            cautions = listOf(
                "アンプの電源を入れたまま抜き差ししない。短絡で保護回路が働くか壊れる",
                "裸線はヒゲが出ないよう撚ってから留める。1本触れただけで短絡する",
                "楽器用の TS ケーブルを流用しない。細すぎて発熱し、音も痩せる",
            ),
        ),
        Connector(
            name = "ハイインピーダンス（70V / 100V）",
            category = ConnectorCategory.SPEAKER,
            summary = "館内放送用。長い距離にたくさん繋ぐための方式",
            aliases = listOf("ハイインピ", "ハイイン", "70v", "100v", "constant voltage", "トランス"),
            pins = listOf(
                PinAssignment("＋/−", "アンプの 70V または 100V 出力"),
                PinAssignment("タップ", "各スピーカーで取り出す電力（10W / 5W / 2.5W など）"),
            ),
            cautions = listOf(
                "ローインピーダンスのスピーカーを直結すると壊れる。必ずトランス付きのものを使う",
                "全スピーカーのタップ合計がアンプ出力を超えないこと。80% までで組む",
                "ローインピーダンス用のアンプに 100V スピーカーを繋いでも小さい音しか出ない",
            ),
            advanced = listOf(
                "電圧を高くして電流を減らすので、細い線で長く引ける。" +
                    "電力損失は電流の2乗に比例するため、これが効く",
                "トランスは低域が苦手。100V ラインの系統に低音を求めない",
                "タップを大きくすると音は大きくなるが、その分だけアンプの容量を食う",
            ),
        ),

        // ------------------------------------------------------------------
        // 電源
        // ------------------------------------------------------------------
        Connector(
            name = "powerCON / powerCON TRUE1",
            category = ConnectorCategory.POWER,
            summary = "ロック付きの電源コネクタ。抜けない代わりに扱いに決まりがある",
            aliases = listOf("パワコン", "powercon", "true1", "nac3"),
            pins = listOf(
                PinAssignment("青（IN）", "電源の入り口"),
                PinAssignment("白/グレー（OUT）", "次の機材へ送る（スルー）"),
            ),
            cautions = listOf(
                "無印の powerCON は**通電中の抜き差しが禁止**。必ず電源を切ってから",
                "TRUE1 は通電中でも抜き差しできる規格。見た目が似ているので混ぜない",
                "青と白は形が違って挿さらない。無理に挿そうとしない",
                "スルーで数珠つなぎにするとき、合計電流が1本目のケーブルの容量を超えないこと",
            ),
            advanced = listOf(
                "TRUE1 と TRUE1 TOP は互換が無い（TOP が新しい）。現場で混在すると繋がらない",
                "スルー配線は便利だが、1本目が抜けると下流が全部落ちる。" +
                    "卓と主要な機材は別回路から取る",
            ),
        ),
        Connector(
            name = "電源プラグ（単相100V）",
            category = ConnectorCategory.POWER,
            summary = "日本の一般的な電源。容量と回路の分け方が仕事",
            aliases = listOf("コンセント", "延長", "ドラム", "テーブルタップ", "20a", "15a"),
            pins = listOf(
                PinAssignment("L（短い方）", "ライブ（電圧側）"),
                PinAssignment("N（長い方）", "ニュートラル（接地側）"),
                PinAssignment("E", "アース（3P の場合）"),
            ),
            cautions = listOf(
                "1回路の定格が 15A でも、使うのは 80%（約 12A）まで",
                "ドラムリールは巻いたまま使うと熱がこもる。必ず全部伸ばす",
                "アースを外す変換プラグを常用しない。ハムが出るなら DI のリフトで対処する",
                "照明と音響は回路を分ける。調光のノイズが電源経由で音に乗る",
            ),
            advanced = listOf(
                "機材ごとに違う回路から電源を取ると、機材間にグランドの電位差が生まれて" +
                    "ハムループになる。音響系は可能なかぎり**同じ回路・同じ相**から取る",
                "20A の口（引掛シーリングや L 型）は形が違って一般の 15A プラグが挿さらない。" +
                    "変換を用意しておくと現場で困らない",
                "電圧降下は 3% 以内が目安。50m を超える引き回しは太い線に替えるか、" +
                    "分電盤の近くにアンプを置く",
            ),
        ),

        Connector(
            name = "IEC（C13 / C14 / C19 / C20）",
            category = ConnectorCategory.POWER,
            summary = "機材側の電源コネクタ。ラックの中で最も多い接続",
            aliases = listOf("iec", "c13", "c14", "c19", "インレット", "メガネ"),
            pins = listOf(
                PinAssignment("C13 / C14", "10A まで。卓・エフェクタ・PC が使う"),
                PinAssignment("C19 / C20", "16A まで。大型アンプとパワーディストリビュータ"),
                PinAssignment("L / N / E", "活線 / 中性 / 接地の3ピン"),
            ),
            cautions = listOf(
                "抜けやすい。ラックの中では結束バンドかロック付きのケーブルで固定する",
                "C13 のケーブルで大型アンプを引くと発熱する。容量を確認して使い分ける",
                "接地ピンを浮かせる変換は使わない。ノイズは消えても感電と機材破損の危険が残る",
            ),
            advanced = listOf(
                "ロック機構付き（IEC LOCK など）は形が同じで抜け止めだけが違う。" +
                    "移動を伴う現場ではこちらを標準にすると事故が減る",
            ),
        ),

        // ------------------------------------------------------------------
        // デジタル音声
        // ------------------------------------------------------------------
        Connector(
            name = "AES/EBU（AES3）",
            category = ConnectorCategory.DIGITAL,
            summary = "XLR で送るプロ用のデジタル2ch。形は同じでも中身はアナログと別物",
            aliases = listOf("aes", "aes3", "ebu", "デジタル xlr"),
            pins = listOf(
                PinAssignment("1", "グランド"),
                PinAssignment("2", "信号 +"),
                PinAssignment("3", "信号 −"),
            ),
            cautions = listOf(
                "見た目が XLR なのでアナログ入力に挿さる。挿しても音は出ない（雑音が出る機種もある）",
                "専用の 110Ω ケーブルを使う。マイクケーブルでも短距離なら通るが、伸びない",
                "1本で 2ch。ステレオの片側だけを送ることはできない",
            ),
            advanced = listOf(
                "クロックは信号に埋め込まれている（セルフクロック）ので別線は要らないが、" +
                    "受け側は「どちらを親にするか」を決める必要がある。両方が親だとノイズが出る",
                "サンプリング周波数が合っていないと、繋がっていても音が出ないか" +
                    "「ピリピリ」というノイズになる。まず両側の 48kHz を確認する",
                "ケーブルの特性インピーダンスが 110Ω なのは反射を防ぐため。" +
                    "マイクケーブル（規定なし）だと 50m あたりから崩れ始める",
            ),
        ),
        Connector(
            name = "S/PDIF（同軸・光）",
            category = ConnectorCategory.DIGITAL,
            summary = "民生のデジタル2ch。RCA（同軸75Ω）か TOSLINK（光）",
            aliases = listOf("spdif", "コアキシャル", "toslink", "オプティカル", "光デジタル"),
            pins = listOf(
                PinAssignment("同軸", "RCA・75Ω"),
                PinAssignment("光", "TOSLINK（角型）"),
            ),
            cautions = listOf(
                "AES/EBU とは電気的に別物。変換器なしでは繋がらない（似た信号だが電圧が違う）",
                "光ケーブルは折り曲げに弱い。急に曲げると通らなくなる",
                "同軸には映像用の 75Ω ケーブルが使える。音声用の 50Ω を使わない",
            ),
            advanced = listOf(
                "光は電気的に完全に絶縁されるので、グランドループによるハムを確実に断てる。" +
                    "PC と卓の間のハムに効く",
                "光の方がジッタ（時間の揺れ）は増える傾向がある。" +
                    "受け側が良いクロックを持っていれば実用上は問題にならない",
            ),
        ),
        Connector(
            name = "ADAT（Lightpipe）",
            category = ConnectorCategory.DIGITAL,
            summary = "光1本で 8ch。インターフェースの拡張でよく使う",
            aliases = listOf("adat", "ライトパイプ", "オプティカル 8ch"),
            pins = listOf(
                PinAssignment("TOSLINK", "48kHz までで 8ch"),
            ),
            cautions = listOf(
                "88.2/96kHz では 4ch、176.4/192kHz では 2ch に減る（S/MUX）",
                "S/PDIF と同じ光コネクタなので、挿し間違えても形では気づけない",
            ),
            advanced = listOf(
                "S/MUX（サンプルマルチプレクシング）は高い周波数のデータを" +
                    "複数の ch に分けて送る仕組み。送り側と受け側で同じ設定にしないと音がバラける",
                "ADAT にはクロックが埋め込まれているが、複数系統を使うときは" +
                    "ワードクロックで全体を揃えた方が安定する",
            ),
        ),
        Connector(
            name = "MADI（AES10）",
            category = ConnectorCategory.DIGITAL,
            summary = "1本で 64ch。デジタルスネークの定番",
            aliases = listOf("madi", "aes10", "光 64ch", "bnc madi"),
            pins = listOf(
                PinAssignment("同軸（BNC）", "75Ω・約100m"),
                PinAssignment("光（SC/LC）", "マルチモードで約2km"),
            ),
            cautions = listOf(
                "64ch は 48kHz のとき。96kHz では 32ch に減る",
                "1本が切れると全 ch が落ちる。重要な系統は冗長化する",
            ),
            advanced = listOf(
                "レイテンシが小さく（数サンプル）、ネットワーク機器を介さないので" +
                    "経路が単純。ライブでの信頼性が高いのはこの単純さによる",
                "Dante と比べると、ルーティングの自由度は低いが" +
                    "「繋いだところに繋がっている」ことが物理的に保証される",
            ),
        ),

        Connector(
            name = "HDMI / SDI の埋め込み音声",
            category = ConnectorCategory.DIGITAL,
            summary = "映像信号に音声が乗って来る。映像班との切り分けが必要になる",
            aliases = listOf("hdmi", "sdi", "エンベデッド", "embedded", "映像", "配信"),
            pins = listOf(
                PinAssignment("HDMI", "民生・PC 系。抜けやすく長距離に弱い"),
                PinAssignment("SDI（BNC）", "放送・業務系。100m 単位で引ける"),
                PinAssignment("ディエンベデッダ", "映像から音声を取り出す機材。これが要る"),
            ),
            cautions = listOf(
                "PC の出力先が HDMI になっていると、卓に繋いだ音声端子から音が出ない。" +
                    "「音が出ない」で最も多いパターンの一つ",
                "HDMI は挿し直しで再認識が起き、映像も一瞬切れる。本番中は触らない",
                "サンプリング周波数は 48kHz が基本。44.1kHz の素材はどこかで変換が入る",
            ),
            advanced = listOf(
                "SDI は最大16chの音声を埋め込める。どのペアに何が入っているかは" +
                    "映像側の設定次第なので、仕込みで一覧をもらう",
                "著作権保護（HDCP）が掛かった素材は、経路によって音声も止まる。" +
                    "配信案件では事前に確認する",
            ),
        ),

        // ------------------------------------------------------------------
        // ネットワーク音響
        // ------------------------------------------------------------------
        Connector(
            name = "Dante",
            category = ConnectorCategory.NETWORK,
            summary = "LAN 1本で数百 ch。いま業界の事実上の標準。設定が仕事の半分",
            aliases = listOf("dante", "ダンテ", "audinate", "dante controller", "ネットワークオーディオ"),
            pins = listOf(
                PinAssignment("物理", "Ethernet（RJ45 / etherCON）・Cat5e 以上"),
                PinAssignment("既定のレイテンシ", "1ms（機材と経路により 0.25〜5ms）"),
                PinAssignment("周波数", "48kHz が基本。44.1 / 96kHz も可"),
            ),
            cautions = listOf(
                "全機材のサンプリング周波数を揃える。1台でも違うと、その台だけ音が出ない",
                "クロックの親（Leader / Master）は1台だけ。" +
                    "「Preferred Leader」を明示的に決めておく",
                "IP は既定で自動（リンクローカル 169.254.x.x）。" +
                    "PC を固定 IP にしていると Dante Controller から見えない",
                "スイッチは EEE（省電力イーサネット）を必ず切る。音が途切れる原因の筆頭",
                "本番前に Dante Controller で全機材が見えることを確認する。" +
                    "見えない状態からの復旧は現場でいちばん時間がかかる",
            ),
            advanced = listOf(
                "レイテンシ設定は「経路上の最大」に合わせる。" +
                    "スイッチを1段増やすごとに必要な時間が増えるので、" +
                    "0.25ms のまま段を重ねると音が途切れる。迷ったら 1ms",
                "ユニキャストは1つの送り先に1本ずつ流す方式で、" +
                    "同じ音を4台に送ると帯域を4倍使う。3台以上に配るならマルチキャストにする。" +
                    "ただしマルチキャストは IGMP スヌーピングを設定していないスイッチだと" +
                    "全ポートに垂れ流されてネットワークが詰まる",
                "VLAN で音声と制御を分けるのが定石。照明やカメラと同じ線に相乗りさせない",
                "Dante Domain Manager を使わない限り、ネットワークに繋がった誰でも" +
                    "ルーティングを変えられる。本番前にネットワークを閉じる",
                "Dante Via / Dante Virtual Soundcard は PC を Dante 機材にするソフト。" +
                    "PC の省電力設定と Wi-Fi を必ず切る。無線での Dante は成立しない",
                "AES67 モードに切り替えると他社（Ravenna など）と繋がるが、" +
                    "Dante 独自の機能（サンプリング周波数の自動追従など）は使えなくなる",
            ),
        ),
        Connector(
            name = "etherCON / Cat5e・Cat6",
            category = ConnectorCategory.NETWORK,
            summary = "ネットワーク音響の物理層。ここが弱いと全部落ちる",
            aliases = listOf("ethercon", "rj45", "lan", "cat5", "cat6", "イーサコン"),
            pins = listOf(
                PinAssignment("配線", "T568B（現場の標準）"),
                PinAssignment("距離", "1区間 100m まで"),
                PinAssignment("種別", "Cat5e で 1Gbps まで足りる"),
            ),
            cautions = listOf(
                "現場の LAN は必ずロック付き（etherCON）にする。普通の RJ45 は抜ける",
                "100m を超えるならスイッチを挟むか光に替える。延長ケーブルで繋ぐのは不可",
                "踏まれる場所に平型（フラット）ケーブルを使わない。断線が見えない",
                "自作するなら必ずテスターで通す。8本のうち1本の順番違いで動いたり動かなかったりする",
            ),
            advanced = listOf(
                "STP（シールド付き）は両端が接地されているとグランドループを作る。" +
                    "音響用途では UTP（シールドなし）の方が無難なことが多い",
                "PoE 対応スイッチのポートに非対応機材を挿しても、規格どおりなら壊れない" +
                    "（機器が要求しない限り給電しない）。ただし安価な「パッシブ PoE」は常時給電するので壊れる",
            ),
        ),
        Connector(
            name = "AES67 / AVB / その他のネットワーク方式",
            category = ConnectorCategory.NETWORK,
            summary = "メーカー間をまたぐための共通規格と、その他の方式",
            aliases = listOf("aes67", "avb", "milan", "ravenna", "livewire", "q-lan", "qsc"),
            pins = listOf(
                PinAssignment("AES67", "各方式の相互接続用。Dante / Ravenna / Livewire が対応"),
                PinAssignment("AVB / Milan", "スイッチ側の対応が必須。時刻同期をネットワークが保証する"),
                PinAssignment("Q-LAN", "QSC の方式。Q-SYS 環境で使う"),
            ),
            cautions = listOf(
                "AES67 は「最低限の相互接続」の規格。繋がるが、機能は各社独自のものより減る",
                "AVB は普通のスイッチでは動かない。AVB 対応スイッチが要る",
                "違う方式どうしは、間にゲートウェイ機材を入れないと繋がらない",
            ),
            advanced = listOf(
                "AES67 は PTP（IEEE 1588）で時刻を合わせる。" +
                    "Dante の既定は PTPv1、AES67 は PTPv2 なので、混在時はどちらに揃えるかを決める",
                "AVB / Milan は帯域を予約する仕組みを持つので、輻輳に強い。" +
                    "代わりに経路上の全スイッチが対応している必要がある",
            ),
        ),

        Connector(
            name = "PoE / PoE+（給電付き LAN）",
            category = ConnectorCategory.NETWORK,
            summary = "LAN ケーブルで機材に電源も送る。Dante の小型機材で使う",
            aliases = listOf("poe", "802.3af", "802.3at", "給電"),
            pins = listOf(
                PinAssignment("PoE", "1ポート 15.4W まで（802.3af）"),
                PinAssignment("PoE+", "1ポート 30W まで（802.3at）"),
                PinAssignment("バジェット", "スイッチ全体で使える合計電力。ポート数×最大ではない"),
            ),
            cautions = listOf(
                "スイッチの合計給電量（バジェット）を超えると、後から挿した機材が黙って落ちる",
                "PoE 非対応のスイッチに挿しても給電されない。壊れはしないが動かない",
                "細い Cat5e の長い引き回しは電圧降下で不安定になる。90m を超えないこと",
            ),
            advanced = listOf(
                "音声ネットワークのスイッチは、省電力機能（EEE）と IGMP スヌーピングの設定で" +
                    "挙動が変わる。「繋がっているのに音が出ない」の多くはスイッチ側の設定",
            ),
        ),

        // ------------------------------------------------------------------
        // その他・変換
        // ------------------------------------------------------------------
        Connector(
            name = "DI ボックス",
            category = ConnectorCategory.UTILITY,
            summary = "アンバランスをバランスに変換して長距離に送る",
            aliases = listOf("di", "ダイレクトボックス", "direct box"),
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
            advanced = listOf(
                "パッシブDI（トランス式）は電源が要らず、電気的に絶縁できるので" +
                    "グランドループに強い。高出力の楽器向き",
                "アクティブDI は入力インピーダンスが高く、パッシブのピックアップから" +
                    "高域を吸わない。パッシブベースやエレアコ向き",
                "スピーカー出力から取る「スピーカーDI」は専用品を使う。" +
                    "普通の DI にアンプ出力を入れると壊れる",
            ),
        ),
        Connector(
            name = "ファンタム電源（48V）",
            category = ConnectorCategory.UTILITY,
            summary = "コンデンサマイクとアクティブDIに電源を送る",
            aliases = listOf("ファンタム", "phantom", "48v", "p48"),
            pins = listOf(
                PinAssignment("2番・3番", "+48V（同電位）"),
                PinAssignment("1番", "グランド（帰り）"),
            ),
            cautions = listOf(
                "リボンマイクに掛けると壊れることがある。結線ミスや断線時は特に危険",
                "抜き差しは必ずファンタムを切ってから。突入電流でスピーカーからノイズが出る",
                "ダイナミックマイクは掛かっていても基本は問題ないが、断線したケーブルでは壊れ得る",
                "切ってから 30 秒ほどはコンデンサに電荷が残る。すぐ抜かない",
            ),
            advanced = listOf(
                "2番と3番に同じ電圧を掛けるので、差動で受ける信号には影響しない。" +
                    "これが「見えない（ファンタム）電源」と呼ばれる理由",
                "電流は 1ch あたり最大 10mA 程度。多 ch を一斉にオンにすると" +
                    "卓の電源が足りずレベルが下がる機種がある。数 ch ずつ入れる",
                "T パワー（12V）や プラグインパワー（3〜5V）は別物。" +
                    "ファンタムを掛けると壊れる機材がある",
            ),
        ),
        Connector(
            name = "DMX（照明制御）",
            category = ConnectorCategory.UTILITY,
            summary = "照明の制御信号。XLR と同じ形だが繋いではいけない",
            aliases = listOf("dmx", "dmx512", "照明", "5ピン"),
            pins = listOf(
                PinAssignment("1", "グランド"),
                PinAssignment("2", "データ −"),
                PinAssignment("3", "データ +"),
                PinAssignment("4・5", "予備（5ピンの場合）"),
            ),
            cautions = listOf(
                "3ピン DMX は音声用 XLR と同じ形。挿さるが動かない（機材が壊れることもある）",
                "ケーブルは 110Ω の DMX 用。マイクケーブルで代用すると不安定になる",
                "末端にターミネータ（120Ω）を入れる。無いと信号が反射してちらつく",
            ),
            advanced = listOf(
                "DMX は RS-485 という規格の上に載っている。" +
                    "音声のバランス伝送とは電圧も速度も別物",
                "1系統（ユニバース）で 512ch。足りなければ系統を増やすか Art-Net / sACN を使う",
            ),
        ),
        Connector(
            name = "ワードクロック（BNC）",
            category = ConnectorCategory.UTILITY,
            summary = "デジタル機材の時計を揃える線。合わないと音が出ない・パチパチ鳴る",
            aliases = listOf("ワードクロック", "word clock", "bnc", "クロック", "同期"),
            pins = listOf(
                PinAssignment("BNC", "75Ω・同軸"),
                PinAssignment("終端", "受けの末端で 75Ω 終端"),
            ),
            cautions = listOf(
                "親（マスター）は必ず1台だけ。2台以上あるとノイズが出る",
                "映像用の 75Ω ケーブルが使える。音声用の 50Ω は使わない",
                "スルーで数珠つなぎするときは、最後の機材だけ終端を入れる",
            ),
            advanced = listOf(
                "デジタル音声は「1秒間に何回サンプルを取るか」が全機材で完全に揃っている必要がある。" +
                    "ずれると、少しずつサンプルが足りなくなって「プチッ」という音になる",
                "AES/EBU や ADAT には信号にクロックが埋まっているので、" +
                    "1対1ならワードクロックは要らない。3台以上が絡むときに効いてくる",
            ),
        ),
        Connector(
            name = "USB オーディオ",
            category = ConnectorCategory.UTILITY,
            summary = "PC と機材を繋ぐ。現場ではケーブル長と電源が問題になる",
            aliases = listOf("usb", "オーディオインターフェース", "usb-c", "type-c"),
            pins = listOf(
                PinAssignment("USB 2.0", "5m まで。それ以上はリピータが要る"),
                PinAssignment("バスパワー", "PC から給電。不安定なら外部電源にする"),
            ),
            cautions = listOf(
                "長い USB ケーブルは動かない。5m を超えるならアクティブリピータか光変換",
                "バスパワーの機材はハブを介すと電力が足りずに落ちる。PC に直挿しする",
                "本番中に PC がスリープすると音が止まる。省電力設定を全部切る",
            ),
            advanced = listOf(
                "USB は経路にグランドが通るので、PC のノイズが音に乗ることがある。" +
                    "USB アイソレータか、出力側で DI を通すと消える",
                "Windows では ASIO ドライバを使う。共有モードのままだと" +
                    "レイテンシが読めず、他アプリの音も混ざる",
            ),
        ),
        Connector(
            name = "マイクスプリッター（パラボックス）",
            category = ConnectorCategory.UTILITY,
            summary = "1本のマイクを FOH・モニター・録音に分ける。トランス式が基本",
            aliases = listOf("スプリッター", "パラ", "split", "トランス", "絶縁"),
            pins = listOf(
                PinAssignment("ダイレクト出力", "ファンタムを送る側。1系統だけがここになる"),
                PinAssignment("トランス出力", "絶縁された側。ファンタムは通らない"),
                PinAssignment("グランドリフト", "トランス側のグランドを切るスイッチ"),
            ),
            cautions = listOf(
                "ファンタムを送れるのは1系統だけ。2系統から送ると電圧が競合して音が歪む。" +
                    "誰が送るかを仕込みの最初に決める",
                "ダイレクト側の卓が電源を落とすと、全系統の音が変わることがある",
                "トランスは低域の位相を少し回す。FOH とモニターで音が違って聞こえる原因になる",
            ),
            advanced = listOf(
                "抵抗式（Y ケーブル）の分岐はインピーダンスが崩れてレベルが落ちる。" +
                    "マイクレベルの分岐にトランス以外を使わないこと",
                "配信案件では録音系をトランス側に置くと、卓側の事故から切り離せる",
            ),
        ),
        Connector(
            name = "グランドリフト・アイソレーショントランス",
            category = ConnectorCategory.UTILITY,
            summary = "ハムノイズを止めるための絶縁。電源の接地は絶対に切らない",
            aliases = listOf("グランドループ", "ハム", "アース", "リフト", "ブーン"),
            pins = listOf(
                PinAssignment("信号側リフト", "XLR の1番を切る。これは安全"),
                PinAssignment("電源側リフト", "接地を切る。やってはいけない"),
            ),
            cautions = listOf(
                "ハムが消えるからといって電源の接地を浮かせない。感電と機材破損につながる",
                "リフトするのは信号のグランドだけ。それでも消えないなら電源の取り方を直す",
                "リフトした側から見るとシールドが片側だけになる。長距離では逆にノイズを拾う",
            ),
            advanced = listOf(
                "ハムの正体はたいてい2点接地でできた輪（グランドループ）。" +
                    "電源を1箇所から取るのが最も確実な対策で、絶縁はその次の手",
                "50Hz / 60Hz とその倍音（100 / 120Hz）に山が出る。" +
                    "アナライザで見ると機材の共振と区別できる",
            ),
        ),
        Connector(
            name = "インターカム（4ピン XLR）",
            category = ConnectorCategory.UTILITY,
            summary = "スタッフ間の連絡回線。音声用の XLR と形が似ていて取り違えが起きる",
            aliases = listOf("インカム", "intercom", "clearcom", "連絡"),
            pins = listOf(
                PinAssignment("1", "グランド"),
                PinAssignment("2", "電源（+30V 前後）"),
                PinAssignment("3", "マイク信号"),
                PinAssignment("4", "受話信号"),
            ),
            cautions = listOf(
                "音声用の3ピン XLR とは別物。電源が乗っているので、繋ぎ間違えると機材が壊れる",
                "ケーブルを音声に流用しない。見た目が同じでも回線が違う",
                "終端の設定を間違えると系統全体がノイジーになる",
            ),
            advanced = listOf(
                "ワイヤレスインカムは無線マイクと帯域が近い機種がある。" +
                    "周波数の割り当ては音声のワイヤレスと合わせて考える",
            ),
        ),
        Connector(
            name = "スマートフォン・PC の再生接続",
            category = ConnectorCategory.UTILITY,
            summary = "BGM と SE の出し元。本番の事故が最も多い経路",
            aliases = listOf("bluetooth", "lightning", "usb-c", "スマホ", "bgm", "再生"),
            pins = listOf(
                PinAssignment("3.5mm", "アンバランス。DI を通してから卓へ"),
                PinAssignment("USB-C / Lightning", "デジタル。専用の変換アダプタが要る"),
                PinAssignment("Bluetooth", "本番では使わない"),
            ),
            cautions = listOf(
                "Bluetooth は本番で使わない。100〜200ms の遅れがあり、" +
                    "接続が切れる・通知音が乗る・圧縮で音が痩せる",
                "端末の通知とアラームを必ず切る。機内モードにして Wi-Fi だけ戻すのが確実",
                "再生アプリの音量と端末の音量が別々に効く。両方を上げてから卓で合わせる",
                "充電しながら再生するとハムが乗ることがある。DI のグランドリフトで切る",
            ),
            advanced = listOf(
                "映像や配信と同期させる場合、端末ごとに出力の遅延が違う。" +
                    "本番と同じ経路で1度は実測すること",
                "音量の自動調整（ラウドネスノーマライズ）とイコライザは必ず切る。" +
                    "曲ごとに音量が変わる原因になる",
            ),
        ),
    )

    fun byCategory(category: ConnectorCategory): List<Connector> =
        ALL.filter { it.category == category }

    /**
     * 部分一致で引く。空文字なら全件（重要度順のまま）。
     *
     * 名前だけでなく注意点と上級者向けの本文も対象にしている。
     * 現場で引きたいのは「ファンタム」のような単語ではなく
     * 「リボン 壊れる」のような症状であることが多いため。
     */
    fun search(query: String): List<Connector> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return ALL
        return ALL.filter { it.searchText.contains(needle) }
    }
}
