package com.patoolbox.core.reference

enum class GlossaryCategory(val label: String) {
    CONSOLE("卓・信号系"),
    SIGNAL("音の性質"),
    GEAR("機材"),
    SITE("現場・進行"),
}

data class GlossaryTerm(
    val term: String,
    /** 英語表記や正式名称。略語の元を知りたい場面が多い */
    val english: String,
    val category: GlossaryCategory,
    val description: String,
)

/**
 * PA 用語辞典。
 *
 * 「調べれば分かる正確な定義」より「現場でその言葉が飛んできたとき何をすればいいか」
 * を書くようにしている。新人が本番中に引く前提。
 */
object Glossary {

    val ALL: List<GlossaryTerm> = listOf(
        // --- 卓・信号系 ---
        GlossaryTerm("FOH", "Front of House", GlossaryCategory.CONSOLE, "客席側のメイン卓、またはその位置。客に聞こえる音を作る"),
        GlossaryTerm("モニター", "Monitor / Foldback", GlossaryCategory.CONSOLE, "演者に返す音。ステージ上のスピーカーやイヤモニで返す"),
        GlossaryTerm("AUX", "Auxiliary", GlossaryCategory.CONSOLE, "メインとは別に作る送り。モニターやエフェクトに使う"),
        GlossaryTerm("プリ / ポスト", "Pre / Post fader", GlossaryCategory.CONSOLE, "AUXの送りをフェーダーの前から取るか後から取るか。モニターはプリが基本"),
        GlossaryTerm("DCA / VCA", "Digitally / Voltage Controlled Amplifier", GlossaryCategory.CONSOLE, "複数チャンネルの音量をまとめて動かすフェーダー。音はグループに送られない"),
        GlossaryTerm("グループ / バス", "Group / Bus", GlossaryCategory.CONSOLE, "複数チャンネルをまとめて processing する経路。まとめてEQやコンプを掛けられる"),
        GlossaryTerm("マトリクス", "Matrix", GlossaryCategory.CONSOLE, "メインやグループからさらに別の出力を作る。ロビー送りや録音送りに使う"),
        GlossaryTerm("ゲイン", "Gain / Trim", GlossaryCategory.CONSOLE, "入力段の増幅量。ここで適正レベルにしないと後段で無理が出る"),
        GlossaryTerm("ゲインストラクチャー", "Gain structure", GlossaryCategory.CONSOLE, "入口から出口までのレベル配分。どこかで無理をすると歪むかノイズが増える"),
        GlossaryTerm("パッド", "Pad", GlossaryCategory.CONSOLE, "入力を10〜20dB下げるスイッチ。入力が大きすぎるときに入れる"),
        GlossaryTerm("ハイパス / ローカット", "HPF / High Pass Filter", GlossaryCategory.CONSOLE, "低域を切るフィルタ。かぶりと吹かれが減るので、キック以外は基本入れる"),
        GlossaryTerm("インサート", "Insert", GlossaryCategory.CONSOLE, "そのチャンネルの信号を外部機器に出して戻す接続"),
        GlossaryTerm("パッチ", "Patch", GlossaryCategory.CONSOLE, "どの入力をどのチャンネルに割り当てるか。表にして共有する"),
        GlossaryTerm("ミュート / カット", "Mute", GlossaryCategory.CONSOLE, "そのチャンネルを黙らせる。MC中の楽器マイクは落とすのが基本"),
        GlossaryTerm("ファンタム", "Phantom power (+48V)", GlossaryCategory.CONSOLE, "コンデンサマイクとアクティブDIに送る電源。リボンマイクには掛けない"),

        GlossaryTerm("シーン / スナップショット", "Scene / Snapshot", GlossaryCategory.CONSOLE, "卓の設定をまとめて保存・呼び出しする機能。曲やコーナーごとに切り替える"),
        GlossaryTerm("レイヤー", "Layer", GlossaryCategory.CONSOLE, "デジタル卓でフェーダーに割り当てるチャンネルの組。切り替えを間違えると別の音を触る"),
        GlossaryTerm("ソロ / PFL / AFL", "Solo / Pre-Fader Listen / After-Fader Listen", GlossaryCategory.CONSOLE, "1chだけ聞く機能。PFLはフェーダー前、AFLは後。客席の音には出ない"),
        GlossaryTerm("センド・オン・フェーダー", "Sends on fader", GlossaryCategory.CONSOLE, "AUXの送り量をフェーダーで操作するモード。モニターを作るときに速い"),
        GlossaryTerm("ユニティ / 0dB", "Unity gain", GlossaryCategory.CONSOLE, "入ってきた信号をそのまま通す位置。フェーダーはまずここに置いてゲインで合わせる"),
        GlossaryTerm("サイドチェイン", "Side chain", GlossaryCategory.CONSOLE, "別の信号でコンプを動かす仕組み。MCが被ったらBGMを下げる、といった使い方"),
        GlossaryTerm("オートミキサー", "Automatic mixer", GlossaryCategory.CONSOLE, "話していないマイクを自動で下げる機能。会議や講演でハウリングを大きく減らせる"),
        GlossaryTerm("パン", "Pan / Panpot", GlossaryCategory.CONSOLE, "左右の定位。客席の大半はどちらかのスピーカーしか聞こえないので、極端に振らない"),

        // --- 音の性質 ---
        GlossaryTerm("ハウリング", "Feedback", GlossaryCategory.SIGNAL, "マイクとスピーカーが回って発振する現象。まず配置、次にゲイン、最後にEQ"),
        GlossaryTerm("かぶり", "Bleed / Spill", GlossaryCategory.SIGNAL, "狙っていない音が他のマイクに入ること。ドラムで特に問題になる"),
        GlossaryTerm("近接効果", "Proximity effect", GlossaryCategory.SIGNAL, "指向性マイクに近づくと低域が持ち上がる現象。歌い手の距離で音が変わる"),
        GlossaryTerm("位相", "Phase", GlossaryCategory.SIGNAL, "波形のタイミングのずれ。2本のマイクで打ち消し合うと痩せた音になる"),
        GlossaryTerm("極性", "Polarity", GlossaryCategory.SIGNAL, "＋−の向き。反転すると低域が消える。位相とは別物"),
        GlossaryTerm("dBu / dBV", "dBu / dBV", GlossaryCategory.SIGNAL, "電圧レベルの単位。0dBu=0.775V、0dBV=1V。約2.2dB違う"),
        GlossaryTerm("dBFS", "dB Full Scale", GlossaryCategory.SIGNAL, "デジタルの最大値を0とした目盛り。超えるとクリップする"),
        GlossaryTerm("Leq", "Equivalent continuous sound level", GlossaryCategory.SIGNAL, "測定時間中のエネルギー平均。騒音規制の基準に使われる"),
        GlossaryTerm("A特性", "A-weighting", GlossaryCategory.SIGNAL, "人の聴感に合わせた重み付け。騒音レベルの基本"),
        GlossaryTerm("クレストファクタ", "Crest factor", GlossaryCategory.SIGNAL, "ピークと平均の比。大きいほど瞬間的な余裕が要る"),
        GlossaryTerm("ヘッドルーム", "Headroom", GlossaryCategory.SIGNAL, "歪むまでの余裕。無くなると音が潰れる"),
        GlossaryTerm("RT60", "Reverberation time", GlossaryCategory.SIGNAL, "音が60dB減衰するまでの時間。会場の響きの長さ"),

        GlossaryTerm("ピンクノイズ", "Pink noise", GlossaryCategory.SIGNAL, "1オクターブあたりのエネルギーが等しいノイズ（-3dB/oct）。RTAで平らになるのが基準。測定の標準"),
        GlossaryTerm("ホワイトノイズ", "White noise", GlossaryCategory.SIGNAL, "1Hzあたりのエネルギーが等しいノイズ。オクターブで見ると+3dB/octの右上がり。スピーカーに入れるとツイータを焼く"),
        GlossaryTerm("ブラウンノイズ", "Brown / Red noise", GlossaryCategory.SIGNAL, "-6dB/oct のノイズ。低域に偏る。サブの確認と低域の共振探しに使う"),
        GlossaryTerm("スイープ", "Sine sweep", GlossaryCategory.SIGNAL, "周波数を動かしながら鳴らす信号。インパルス応答・ディレイ時間・極性の測定に使う"),
        GlossaryTerm("GBF（ハウリングまでの余裕）", "Gain Before Feedback", GlossaryCategory.SIGNAL, "ハウリングする手前までにどれだけゲインを上げられるか。マイクの本数を倍にすると3dB減る"),
        GlossaryTerm("ノッチ", "Notch filter", GlossaryCategory.SIGNAL, "1点だけを狭く深く削るフィルタ。ハウリング対策の基本。Q8以上・-6dB程度で使う"),
        GlossaryTerm("Q（キュー）", "Q factor", GlossaryCategory.SIGNAL, "EQで触る幅。大きいほど狭い。Q1は約1.4オクターブ、Q8は約0.18オクターブ"),
        GlossaryTerm("コムフィルタ", "Comb filtering", GlossaryCategory.SIGNAL, "時間差のある同じ音が混ざると、櫛状に打ち消しが並ぶ現象。EQでは直せない"),
        GlossaryTerm("3:1 の原則", "3-to-1 rule", GlossaryCategory.SIGNAL, "マイク同士の距離を、マイクから音源までの距離の3倍以上にする。かぶりの打ち消しを抑える"),
        GlossaryTerm("定在波", "Standing wave / Room mode", GlossaryCategory.SIGNAL, "部屋の寸法で決まる低域の偏り。場所によって同じ音が大きくも小さくも聞こえる"),
        GlossaryTerm("逆二乗の法則", "Inverse square law", GlossaryCategory.SIGNAL, "距離が倍になると音圧は6dB下がる（点音源の場合）。ラインアレイでは3dBに近づく"),
        GlossaryTerm("音速", "Speed of sound", GlossaryCategory.SIGNAL, "気温15℃で約340m/s。1ms=34cm。気温が1℃上がると0.6m/s速くなる"),
        GlossaryTerm("空気吸収", "Air absorption", GlossaryCategory.SIGNAL, "距離が伸びると高域から先に減る。100mで8kHzが数dB落ちる。湿度でも変わる"),
        GlossaryTerm("直間比", "Direct-to-reverberant ratio", GlossaryCategory.SIGNAL, "直接音と反射音の比。これが小さいと、音量を上げても言葉が聞き取れない"),
        GlossaryTerm("マスキング", "Masking", GlossaryCategory.SIGNAL, "大きい音が近い帯域の小さい音を隠す現象。EQで場所を空ける理由がこれ"),
        GlossaryTerm("等ラウドネス曲線", "Equal-loudness contour", GlossaryCategory.SIGNAL, "音量によって聞こえ方の周波数バランスが変わることを示す曲線。小音量では低域と高域が痩せて聞こえる"),
        GlossaryTerm("レイテンシ", "Latency", GlossaryCategory.SIGNAL, "信号が通り抜けるまでの遅れ。デジタル卓で1〜3ms、無線やBluetoothはさらに大きい"),
        GlossaryTerm("サンプリング周波数", "Sample rate", GlossaryCategory.SIGNAL, "1秒間に何回サンプルを取るか。48kHzが基本。機材間で揃っていないと音が途切れる"),
        GlossaryTerm("ビット深度", "Bit depth", GlossaryCategory.SIGNAL, "1サンプルの分解能。24bitで理論上144dBのダイナミックレンジ"),
        GlossaryTerm("THD", "Total Harmonic Distortion", GlossaryCategory.SIGNAL, "元の信号に無い倍音がどれだけ出たか。歪みの量を示す"),
        GlossaryTerm("LUFS", "Loudness Units relative to Full Scale", GlossaryCategory.SIGNAL, "配信・放送で使う音量の単位。人の聴感に近い測り方をする"),

        // --- 機材 ---
        GlossaryTerm("DI", "Direct Injection box", GlossaryCategory.GEAR, "アンバランスをバランスに変換する箱。楽器を長距離送るのに使う"),
        GlossaryTerm("スネーク / マルチ", "Snake / Multicore", GlossaryCategory.GEAR, "多回線をまとめたケーブル。ステージと卓を繋ぐ"),
        GlossaryTerm("ステージボックス", "Stage box", GlossaryCategory.GEAR, "ステージ側の入出力箱。デジタル卓では変換器も兼ねる"),
        GlossaryTerm("パワードスピーカー", "Powered / Active speaker", GlossaryCategory.GEAR, "アンプ内蔵のスピーカー。電源が要る"),
        GlossaryTerm("パッシブスピーカー", "Passive speaker", GlossaryCategory.GEAR, "外部アンプで鳴らすスピーカー。インピーダンスに注意"),
        GlossaryTerm("バイアンプ", "Bi-amp", GlossaryCategory.GEAR, "低域と高域を別のアンプで鳴らす方式。結線を間違えると高域が飛ぶ"),
        GlossaryTerm("サブ / SW", "Subwoofer", GlossaryCategory.GEAR, "低域専用スピーカー。位置と位相で効き方が大きく変わる"),
        GlossaryTerm("ディレイタワー", "Delay tower / Delay stack", GlossaryCategory.GEAR, "後方席用の追加スピーカー。距離ぶん遅らせて出す"),
        GlossaryTerm("フィル", "Fill (front / side)", GlossaryCategory.GEAR, "メインが届かない範囲を埋める小型スピーカー"),
        GlossaryTerm("ワイヤレス", "Wireless microphone system", GlossaryCategory.GEAR, "無線マイク。周波数の調整と電池管理が必須"),
        GlossaryTerm("イヤモニ / IEM", "In-Ear Monitor", GlossaryCategory.GEAR, "耳に入れるモニター。ハウリングしないが返しの作り方が変わる"),
        GlossaryTerm("コンプレッサー", "Compressor", GlossaryCategory.GEAR, "大きい音を抑えて音量差を減らす。掛けすぎると平坦になる"),
        GlossaryTerm("ゲート", "Noise gate", GlossaryCategory.GEAR, "小さい音を遮断する。ドラムのかぶり対策に使う"),
        GlossaryTerm("ディエッサー", "De-esser", GlossaryCategory.GEAR, "サ行のきつさを抑える。ボーカルに使う"),

        GlossaryTerm("グライコ", "GEQ / Graphic Equalizer", GlossaryCategory.GEAR, "1/3オクターブごとにスライダーが並ぶEQ。モニターのハウリング対策に使う"),
        GlossaryTerm("パライコ", "PEQ / Parametric Equalizer", GlossaryCategory.GEAR, "周波数・Q・量を自由に決めるEQ。1点を狭く削るならこちら"),
        GlossaryTerm("ハウリングサプレッサ", "Feedback suppressor", GlossaryCategory.GEAR, "発振を検出して自動でノッチを入れる機材。頼りすぎると音がやせる"),
        GlossaryTerm("クロスオーバー", "Crossover", GlossaryCategory.GEAR, "帯域を分けてスピーカーごとに送る処理。分ける点と傾きで音が変わる"),
        GlossaryTerm("リミッター", "Limiter", GlossaryCategory.GEAR, "設定した値を超えさせない処理。スピーカーを守る最後の砦"),
        GlossaryTerm("ラインアレイ", "Line array", GlossaryCategory.GEAR, "スピーカーを縦に並べて指向性を作る方式。距離による減衰が小さい"),
        GlossaryTerm("カーディオイドサブ", "Cardioid subwoofer", GlossaryCategory.GEAR, "サブを前後に配置して後ろへの低域を打ち消す組み方。ステージ上の低域が減る"),
        GlossaryTerm("測定用マイク", "Measurement microphone", GlossaryCategory.GEAR, "無指向性で特性が平坦なマイク。RTAや残響測定に使う"),
        GlossaryTerm("バウンダリーマイク", "Boundary / PZM microphone", GlossaryCategory.GEAR, "床や壁に置くマイク。反射と直接音が揃うのでコムフィルタが出にくい"),
        GlossaryTerm("リボンマイク", "Ribbon microphone", GlossaryCategory.GEAR, "薄い金属箔で拾うマイク。ファンタムを掛けると壊れる機種がある"),
        GlossaryTerm("ショックマウント", "Shock mount", GlossaryCategory.GEAR, "床や台の振動をマイクに伝えないためのサスペンション"),
        GlossaryTerm("ポップガード / ウインドスクリーン", "Pop filter / Windscreen", GlossaryCategory.GEAR, "破裂音と風を防ぐ。屋外では必須で、高域が少し落ちる"),
        GlossaryTerm("スイッチングハブ", "Network switch", GlossaryCategory.GEAR, "音声ネットワークの中心。省電力機能とQoSの設定次第で音が出ない"),

        // --- 現場・進行 ---
        GlossaryTerm("仕込み", "Setup / Load-in", GlossaryCategory.SITE, "本番前の設営作業。搬入から結線まで"),
        GlossaryTerm("バラシ", "Load-out / Strike", GlossaryCategory.SITE, "終演後の撤収作業"),
        GlossaryTerm("サウンドチェック", "Sound check", GlossaryCategory.SITE, "本番前の音の確認。楽器ごとに音を出して調整する"),
        GlossaryTerm("ゲネプロ", "General rehearsal", GlossaryCategory.SITE, "本番と同じ進行で通す最終リハーサル"),
        GlossaryTerm("押し / 巻き", "Running late / Speeding up", GlossaryCategory.SITE, "進行が予定より遅れること／早めること"),
        GlossaryTerm("香盤", "Running order", GlossaryCategory.SITE, "出演順と時間を書いた表"),
        GlossaryTerm("転換", "Changeover", GlossaryCategory.SITE, "出演者が入れ替わる間の機材入れ替え"),
        GlossaryTerm("板付き", "On stage before curtain", GlossaryCategory.SITE, "開演前から演者がステージ上にいる状態"),
        GlossaryTerm("きっかけ / キュー", "Cue", GlossaryCategory.SITE, "音出しや操作のタイミング指示"),
        GlossaryTerm("トークバック", "Talkback", GlossaryCategory.SITE, "卓からステージへ話しかけるマイク回線"),
        GlossaryTerm("場当たり", "Blocking / Technical rehearsal", GlossaryCategory.SITE, "立ち位置ときっかけだけを確認する稽古。音は通しで出さないことが多い"),
        GlossaryTerm("尺", "Duration", GlossaryCategory.SITE, "その項目にかかる時間。進行表の基本単位"),
        GlossaryTerm("客入れ / 客出し", "House open / House close", GlossaryCategory.SITE, "開場中と終演後のBGM。音量は会話ができる程度（客席で60〜70dB(A)）"),
        GlossaryTerm("明転 / 暗転", "Lights up / Blackout", GlossaryCategory.SITE, "照明の切り替え。音のきっかけと合わせることが多い"),
        GlossaryTerm("マイクチェック", "Line check", GlossaryCategory.SITE, "全回線に音が来ているかを1本ずつ確かめる作業。音作りの前に行う"),
        GlossaryTerm("回し", "Passing the mic", GlossaryCategory.SITE, "1本のマイクを複数人で使い回すこと。ハウリングと音量差が起きやすい"),
        GlossaryTerm("返し", "Foldback / Wedge", GlossaryCategory.SITE, "演者に返すモニターのこと。「返しをください」は音量を上げてほしいという意味"),
    )

    fun byCategory(category: GlossaryCategory): List<GlossaryTerm> =
        ALL.filter { it.category == category }

    /** 用語・英語表記・説明のどれかに一致するものを返す。 */
    fun search(query: String): List<GlossaryTerm> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return ALL
        return ALL.filter {
            it.term.lowercase().contains(normalized) ||
                it.english.lowercase().contains(normalized) ||
                it.description.lowercase().contains(normalized)
        }
    }
}
