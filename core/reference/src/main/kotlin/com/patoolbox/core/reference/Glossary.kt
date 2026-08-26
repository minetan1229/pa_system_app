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
        GlossaryTerm(
            "波長",
            "Wavelength",
            GlossaryCategory.SIGNAL,
            "λ=音速÷周波数。100Hzで約3.4m、1kHzで約34cm、10kHzで約3.4cm。" +
                "低域は波長が部屋や人の体のサイズに近いぶん回り込み・定在波・コムフィルタが起きやすく、" +
                "高域は波長が短いぶん直進して吸音材でも止めやすい",
        ),
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

        // --- 追加分（現場でよく飛ぶが載っていなかった言葉） ---
        GlossaryTerm(
            "こもり",
            "Muddiness / Boxiness",
            GlossaryCategory.SIGNAL,
            "200〜500Hzあたりが盛り上がって、言葉や音の輪郭がぼやける聞こえ方。" +
                "近接効果・部屋の定在波・残響のどれかが原因。RTAでその帯域の盛り上がりを確認できる",
        ),
        GlossaryTerm(
            "抜け",
            "Clarity / Presence",
            GlossaryCategory.SIGNAL,
            "音が奥に埋もれず前に出てくる感じ。2〜5kHz付近の量とこもりの少なさで決まることが多い",
        ),
        GlossaryTerm(
            "音の芯",
            "Fundamental presence",
            GlossaryCategory.SIGNAL,
            "その音の一番大事な帯域（ボーカルなら基音〜倍音の低いところ）。ここを削ると迫力が消える",
        ),
        GlossaryTerm(
            "ドンシャリ",
            "V-shaped EQ / Smiley curve",
            GlossaryCategory.SIGNAL,
            "低域と高域を持ち上げ、中域を下げたEQの形。派手に聞こえるが長時間だと疲れやすい",
        ),
        GlossaryTerm(
            "タイムアライメント",
            "Time alignment",
            GlossaryCategory.SIGNAL,
            "複数のスピーカー（メインとサブ、メインとディレイタワーなど）から届く音のタイミングを揃える調整。ずれるとコムフィルタが起きる",
        ),
        GlossaryTerm(
            "プリディレイ",
            "Pre-delay",
            GlossaryCategory.SIGNAL,
            "リバーブがかかり始めるまでの遅れ。直接音と反射音を分離して、こもらせずに奥行きを出す",
        ),
        GlossaryTerm(
            "スラップバック",
            "Slapback echo",
            GlossaryCategory.SIGNAL,
            "80〜150ms程度の短いディレイを1回だけ返すエフェクト。ロカビリー的なボーカルの効果でよく使う",
        ),
        GlossaryTerm(
            "ダッキング",
            "Ducking",
            GlossaryCategory.CONSOLE,
            "ある音が鳴っているあいだ別の音を自動で下げる処理。サイドチェインの代表的な使い方",
        ),
        GlossaryTerm(
            "音圧",
            "Perceived loudness / Density",
            GlossaryCategory.SIGNAL,
            "現場で「音圧が足りない」と言うときは、音量そのものよりコンプ等で詰まった密度感を指すことが多い。dBの値とは別軸の感覚語",
        ),
        GlossaryTerm(
            "セルフノイズ",
            "Self noise",
            GlossaryCategory.GEAR,
            "マイクやプリアンプ自体が発する暗騒音。数値が低いほど静かな環境での収録に強い",
        ),
        GlossaryTerm(
            "ファントムセンター",
            "Phantom center",
            GlossaryCategory.SIGNAL,
            "左右のスピーカーから同じ音を出すことで、中央にあるように聞こえる定位。座る位置がずれると崩れる",
        ),
        GlossaryTerm(
            "リダンダンシー",
            "Redundancy",
            GlossaryCategory.GEAR,
            "回線や機材を二重化しておくこと。無線マイクの予備機やネットワークの冗長化など、本番中の1点故障で止めないための備え",
        ),
        GlossaryTerm(
            "頭出し",
            "Cueing up",
            GlossaryCategory.SITE,
            "音源や映像の再生位置を、きっかけの直前に合わせておくこと",
        ),
        GlossaryTerm(
            "地明かり",
            "General wash (lighting term, often paired with sound cues)",
            GlossaryCategory.SITE,
            "舞台全体を均一に照らす明かり。演出のきっかけを照明部と共有するときに出てくる言葉",
        ),
        GlossaryTerm(
            "ゲネ落ち",
            "Failure during general rehearsal",
            GlossaryCategory.SITE,
            "ゲネプロ中に機材やオペレーションが止まること。本番前に洗い出すための工程なので、ここで起きるのはむしろ収穫",
        ),

        // --- 追加分2（卓操作・機材・現場言葉をさらに広く） ---
        GlossaryTerm(
            "リコール",
            "Recall",
            GlossaryCategory.CONSOLE,
            "保存したシーンやスナップショットを卓に呼び戻すこと。一部だけ呼ぶ「セーフ」設定もある",
        ),
        GlossaryTerm(
            "セーフ",
            "Recall safe",
            GlossaryCategory.CONSOLE,
            "シーンをリコールしても変えたくないチャンネル・パラメータを除外しておく機能。フェーダーだけセーフ、というように使う",
        ),
        GlossaryTerm(
            "リンク",
            "Stereo link",
            GlossaryCategory.CONSOLE,
            "2chをステレオペアとして連動させる設定。片方を触るともう片方も一緒に動く",
        ),
        GlossaryTerm(
            "ダイレクトアウト",
            "Direct out",
            GlossaryCategory.CONSOLE,
            "チャンネルの信号をAUXやバスを通さず単独で送る出力。マルチトラック録音でよく使う",
        ),
        GlossaryTerm(
            "ゲインシェアリング",
            "Gain sharing / automixing",
            GlossaryCategory.CONSOLE,
            "複数マイクが同時に開いたとき、全体のゲインを自動で配分してハウリングと暗騒音の増加を抑える方式",
        ),
        GlossaryTerm(
            "ワードクロック",
            "Word clock",
            GlossaryCategory.GEAR,
            "デジタル機器同士のサンプリングタイミングを揃える基準信号。揃っていないとプチノイズや音切れが出る",
        ),
        GlossaryTerm(
            "レイテンシコンペンセーション",
            "Latency compensation",
            GlossaryCategory.GEAR,
            "デジタル処理で生じる遅延を、他のチャンネルの遅延に合わせて自動で揃える機能",
        ),
        GlossaryTerm(
            "アイソレーショントランス",
            "Isolation transformer / DI transformer",
            GlossaryCategory.GEAR,
            "電気的に回路を絶縁しながら信号を伝える部品。グラウンドループ由来のハムを断つのに使う",
        ),
        GlossaryTerm(
            "グラウンドループ",
            "Ground loop",
            GlossaryCategory.SIGNAL,
            "複数の機材が別の経路でアースにつながり、電位差でループ電流が流れる現象。「ブーン」というハムの主な原因",
        ),
        GlossaryTerm(
            "ハムバランス",
            "Hum balance / ground lift",
            GlossaryCategory.GEAR,
            "アンバランス機材のハムを軽減するための調整、またはグラウンドを持ち上げるスイッチ（グラウンドリフト）",
        ),
        GlossaryTerm(
            "リハーサルマーク",
            "Rehearsal mark",
            GlossaryCategory.SITE,
            "進行表や譜面で、練習や場当たりの開始位置に付ける目印",
        ),
        GlossaryTerm(
            "香盤合わせ",
            "Schedule sync meeting",
            GlossaryCategory.SITE,
            "各セクション（音響・照明・演出）が香盤の内容とタイミングをすり合わせる打ち合わせ",
        ),
        GlossaryTerm(
            "本番尺",
            "Actual show duration",
            GlossaryCategory.SITE,
            "リハーサルではなく本番当日に実際にかかる時間。MCやアンコールの分だけ台本の尺より延びることが多い",
        ),

        // --- 追加分3（ダイナミクスのパラメータ・マイク指向性・DI・RF・システムチューニング） ---
        GlossaryTerm(
            "スレッショルド",
            "Threshold",
            GlossaryCategory.CONSOLE,
            "コンプ・ゲートが動き始める音量のライン。コンプはこれを超えた分だけ圧縮し、ゲートはこれを下回ると閉じる",
        ),
        GlossaryTerm(
            "レシオ（圧縮比）",
            "Ratio",
            GlossaryCategory.CONSOLE,
            "コンプの圧縮の強さ。4:1ならスレッショルドを超えた分が1/4に圧縮される。10:1以上はほぼリミッターと同じ働きになる",
        ),
        GlossaryTerm(
            "アタックタイム",
            "Attack time",
            GlossaryCategory.CONSOLE,
            "スレッショルドを超えてから圧縮が効き始めるまでの時間。速い（1ms以下）とアタック音まで潰れ、遅い（20ms以上）と立ち上がりを残して胴鳴りだけ潰せる",
        ),
        GlossaryTerm(
            "リリースタイム",
            "Release time",
            GlossaryCategory.CONSOLE,
            "信号がスレッショルドを下回ってから圧縮が解除されるまでの時間。曲のテンポに近い長さにすると呼吸のように自然に聞こえる",
        ),
        GlossaryTerm(
            "ニー",
            "Knee (hard / soft)",
            GlossaryCategory.CONSOLE,
            "スレッショルド付近で圧縮がどれだけ滑らかに立ち上がるか。ハードニーは境目で急に効き、ソフトニーは手前からじわっと効き始める",
        ),
        GlossaryTerm(
            "メイクアップゲイン",
            "Make-up gain",
            GlossaryCategory.CONSOLE,
            "圧縮で下がった音量を持ち上げて補う調整。ここを上げすぎるとハウリングマージンを削っていることに気づきにくい",
        ),
        GlossaryTerm(
            "ゲインリダクション（GR）",
            "Gain reduction",
            GlossaryCategory.CONSOLE,
            "コンプがいまどれだけ音量を下げているかを示すメーター表示。ボーカルなら常時3〜6dB程度動いているのが一般的な目安",
        ),
        GlossaryTerm(
            "エキスパンダー",
            "Expander",
            GlossaryCategory.GEAR,
            "スレッショルド以下の信号を緩やかな比率で下げる処理。ゲートより自然に暗騒音を抑えられる。講演マイクの環境ノイズ低減などに使う",
        ),
        GlossaryTerm(
            "ゲインコンペンセーション",
            "Gain compensation",
            GlossaryCategory.CONSOLE,
            "1本のヘッドアンプをFOHとモニターで共有するとき、片方がゲインを変えてももう片方の卓では音量が変わらないよう自動で打ち消す機能",
        ),
        GlossaryTerm(
            "カーディオイド",
            "Cardioid",
            GlossaryCategory.GEAR,
            "正面の音を最もよく拾い、背面をほとんど拾わない単一指向性。SM58をはじめライブの標準的な指向性",
        ),
        GlossaryTerm(
            "スーパーカーディオイド / ハイパーカーディオイド",
            "Supercardioid / Hypercardioid",
            GlossaryCategory.GEAR,
            "カーディオイドより正面の指向性が鋭いパターン。ただし真後ろではなく後方斜め約125°方向に感度の山（死角ではない点）ができるため、ウェッジの置き場所には注意が要る",
        ),
        GlossaryTerm(
            "無指向性 / オムニ",
            "Omnidirectional",
            GlossaryCategory.GEAR,
            "全方向をほぼ均等に拾う指向性。自然な音だがハウリングには弱い。ラベリアマイクに多く採用される",
        ),
        GlossaryTerm(
            "双指向性",
            "Figure-8 / Bidirectional",
            GlossaryCategory.GEAR,
            "正面と背面を拾い、側面をほとんど拾わない指向性。リボンマイクに多く、MS方式のステレオ収音でも使う",
        ),
        GlossaryTerm(
            "パッシブDI",
            "Passive DI",
            GlossaryCategory.GEAR,
            "トランスで変換するDI。電源不要で頑丈、過大入力に強い。ベースやキーボードなど出力の大きい音源に向く",
        ),
        GlossaryTerm(
            "アクティブDI",
            "Active DI",
            GlossaryCategory.GEAR,
            "電子回路（ファンタムまたは電池駆動）で変換するDI。入力インピーダンスが高く、パッシブピックアップのアコギやビンテージベースの音痩せを防ぐ",
        ),
        GlossaryTerm(
            "スルーアウト",
            "Thru out",
            GlossaryCategory.GEAR,
            "DIに入った楽器の信号をステージ用アンプへ分岐して返す端子。「DIで卓へ、アンプで本人モニター」という定番の使い方を作る",
        ),
        GlossaryTerm(
            "相互変調（インターモジュレーション）",
            "Intermodulation distortion (IMD)",
            GlossaryCategory.SIGNAL,
            "複数の送信波が受信機内で混ざり合って生じる不要な成分。2f1−f2などの位置に現れ、既存波や自分の波に重なると混信の原因になる",
        ),
        GlossaryTerm(
            "ダイバーシティ受信",
            "Diversity reception",
            GlossaryCategory.GEAR,
            "2本のアンテナで受信し、状態の良い方を自動で選ぶ方式。人や物の陰による瞬断（ドロップアウト）を減らす",
        ),
        GlossaryTerm(
            "スプレイ角",
            "Splay angle",
            GlossaryCategory.GEAR,
            "ラインアレイの素子（キャビネット）どうしの角度。上下のカバレッジをどこまで広げるかを決める",
        ),
        GlossaryTerm(
            "トリム角 / サイトアングル",
            "Trim angle / Sight angle",
            GlossaryCategory.GEAR,
            "ラインアレイ全体を吊る角度。客席のどこからどこまでを狙うかの基準線になる",
        ),
        GlossaryTerm(
            "近距離場 / 遠距離場",
            "Near field / Far field",
            GlossaryCategory.SIGNAL,
            "ラインアレイに近い範囲は線音源的にふるまい距離2倍で-3dB、十分離れると点音源に近づき-6dBになる。境目の距離はアレイの長さと周波数で変わる",
        ),
        GlossaryTerm(
            "伝達関数",
            "Transfer function",
            GlossaryCategory.SIGNAL,
            "卓から出した信号とマイクで拾った信号を比較して得る、システム全体の周波数特性・位相特性。デュアルFFT測定の基本量",
        ),
        GlossaryTerm(
            "コヒーレンス",
            "Coherence",
            GlossaryCategory.SIGNAL,
            "測定の信頼度を示す値。反射や騒音で汚れた帯域は下がり、その帯域の測定結果はそのまま信用できない",
        ),
        GlossaryTerm(
            "トゥルーピーク",
            "True peak",
            GlossaryCategory.SIGNAL,
            "サンプルとサンプルの間で実際に生じるピークまで考慮した値。配信・放送のマスターはこれを基準にリミッティングする（目安-1dBTP）",
        ),
        GlossaryTerm(
            "ラウドネスクリープ",
            "Loudness creep",
            GlossaryCategory.SITE,
            "開演から終演にかけて、じわじわと音量が上がっていく現象。耳が大音量に慣れて麻痺するために起きる。終盤に上げる余白を残しておく設計が要る",
        ),
        GlossaryTerm(
            "STI（音声明瞭度指標）",
            "Speech Transmission Index",
            GlossaryCategory.SIGNAL,
            "言葉の聞き取りやすさを表す指標。直接音と残響・騒音の比で決まり、EQでは改善できない",
        ),
        GlossaryTerm(
            "NC値",
            "Noise Criteria",
            GlossaryCategory.SIGNAL,
            "空調など常時鳴っている暗騒音のレベルを評価する指標。会議室やホールの設備音響で使われる",
        ),
        GlossaryTerm(
            "AEC（エコーキャンセラー）",
            "Acoustic Echo Canceller",
            GlossaryCategory.GEAR,
            "スピーカーから出た音がマイクに回り込むのを打ち消す処理。遠隔会議やハイブリッド開催の音響で使う",
        ),
        GlossaryTerm(
            "パワーコンプレッション",
            "Power compression",
            GlossaryCategory.SIGNAL,
            "スピーカーのボイスコイルが発熱すると能率が落ち、入力を上げても音圧が思ったほど伸びなくなる現象。長時間の連続運用ほど効いてくる",
        ),
        GlossaryTerm(
            "RMSリミッター / ピークリミッター",
            "RMS limiter / Peak limiter",
            GlossaryCategory.GEAR,
            "RMSリミッターは平均的な過大入力からドライバーの発熱を守り、ピークリミッターは瞬間的な過大電圧からコーンの変位を守る。役割が違うので通常は両方を設定する",
        ),
        GlossaryTerm(
            "アンビエンスマイク",
            "Ambience microphone",
            GlossaryCategory.GEAR,
            "客席の拍手・歓声を拾うマイク。配信ミックスに会場の空気感を足したり、IEM使用者に無観客感を感じさせない目的で混ぜる",
        ),
        GlossaryTerm(
            "バーチャルサウンドチェック",
            "Virtual soundcheck",
            GlossaryCategory.SITE,
            "リハで録ったマルチトラックを卓に再生してミックスを作り込む手法。演者を拘束せずに時間をかけて仕込める",
        ),
        GlossaryTerm(
            "インプットリスト",
            "Input list / Channel list",
            GlossaryCategory.SITE,
            "どのチャンネルに何の音源が立ち上がるかの一覧表。仕込み前に必要なマイク・スタンド・ケーブルの数量を割り出す元になる",
        ),
        GlossaryTerm(
            "ステージプロット",
            "Stage plot",
            GlossaryCategory.SITE,
            "ステージ上の楽器・マイク・モニターの配置図。インプットリストとあわせて仕込み前に読み込む",
        ),
        GlossaryTerm(
            "テクニカルライダー",
            "Technical rider",
            GlossaryCategory.SITE,
            "アーティスト側が要求する技術仕様書。マイク・モニター本数、電源容量、吊り点などの希望がまとめられている",
        ),
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
