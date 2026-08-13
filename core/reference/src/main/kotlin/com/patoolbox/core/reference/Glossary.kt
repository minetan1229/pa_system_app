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
