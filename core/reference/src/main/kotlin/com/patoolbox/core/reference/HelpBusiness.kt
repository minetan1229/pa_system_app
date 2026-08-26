package com.patoolbox.core.reference

import com.patoolbox.core.model.ToolId

/** 見積・稼働・バックアップの解説。 */
internal object BusinessHelp {

    val TOPICS: List<HelpTopic> = listOf(
        HelpTopic(
            id = "invoice",
            tool = ToolId.INVOICE,
            title = "見積書・請求書",
            summary = "案件から機材費と人件費で作る。消費税とインボイス番号に対応",
            keywords = listOf("見積", "請求", "消費税", "インボイス", "適格請求書"),
            sections = listOf(
                HelpSection(
                    heading = "作り方",
                    body = """
                        案件を選んで、機材と人員を積み上げる。
                        台帳に登録してある機材はそのまま呼び出せる。

                        単価を毎回入れ直さずに済むよう、よく使う項目は残しておくとよい。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "インボイス番号",
                    body = """
                        適格請求書には登録番号の記載が要る。
                        設定に入れておけば、出力する書類に自動で載る。

                        税率と税額の書き方には決まりがあるので、
                        最初の1枚は税理士か取引先に確認してから使うこと。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "work_log",
            tool = ToolId.WORK_LOG,
            title = "稼働記録",
            summary = "現場ごとの時間・移動費・収支をためて集計する",
            keywords = listOf("稼働", "収支", "経費", "移動費"),
            sections = listOf(
                HelpSection(
                    heading = "何のために付けるか",
                    body = """
                        1本あたりの実質単価が見えるようになる。
                        「安いが近くて短い現場」と「高いが遠くて長い現場」を
                        感覚ではなく数字で比べられる。

                        確定申告のときの材料にもなる。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "移動時間も入れる",
                    body = """
                        拘束時間は現場にいる時間だけではない。
                        移動と積み込みを入れないと、実際の時給がまるで違って見える。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "cloud_backup",
            tool = ToolId.CLOUD_BACKUP,
            title = "バックアップ",
            summary = "端末が壊れたときのために、データを書き出して持っておく",
            keywords = listOf("バックアップ", "復元", "データ", "移行"),
            sections = listOf(
                HelpSection(
                    heading = "復元は破壊的な操作",
                    body = """
                        復元すると、いま端末に入っているデータは
                        バックアップの中身に置き換わる。戻せない。

                        機種変更のときだけ使うものと考えて、
                        普段は書き出しだけしておくのが安全。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "書き出す頻度",
                    body = """
                        案件を作ったとき、パッチ表を仕上げたときなど、
                        失うと作り直しになるものを入れた直後に取るとよい。
                    """.trimIndent(),
                ),
            ),
        ),
    )
}

/**
 * 画面に紐づかない読み物。
 *
 * 「dB とは何か」のように複数の画面から参照される話は、
 * 各画面の解説で繰り返すのではなくここに置いて検索から引けるようにする。
 */
internal object ConceptHelp {

    val TOPICS: List<HelpTopic> = listOf(
        HelpTopic(
            id = "index",
            title = "解説の見取り図",
            summary = "どの画面にも右上に「解説」があります。ここからは全部を検索できます",
            keywords = listOf("ヘルプ", "使い方", "help", "index", "目次"),
            sections = listOf(
                HelpSection(
                    heading = "各画面の解説",
                    body = """
                        どのツールを開いても、右上に「解説」が出ています。
                        その画面が何をする道具か、数字をどう読むか、
                        なぜその単位なのか、何に気をつけるかが書いてあります。

                        解説の中には検索窓があるので、
                        開いている画面とは別の話（「dB とは」など）もそこから引けます。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "単位や仕組みの話",
                    body = """
                        画面に属さない読み物も入れてあります。

                        ・dB（デシベル）の読み方
                        ・校正（キャリブレーション）
                        ・ゲインストラクチャー
                        ・ハウリングの仕組みと止め方
                        ・ファンタム電源（48V）

                        検索窓に「dB」「校正」「ゲイン」などと打つと出ます。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "ホームの検索",
                    body = """
                        ホームの検索窓は、ツール名だけでなく解説の本文も見ています。
                        「Dante」「ハウリング」「70V」のように、
                        困っていることを打てば該当するツールが出ます。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "音が出る画面",
                    body = """
                        シグナルジェネレータ、ディレイ実測、極性チェック、室内特性測定、
                        メトロノーム、SE パッドは**スピーカーから音を出します**。
                        画面の上に赤い注意が出るので、PA に繋いだ状態では
                        必ずフェーダーを下げてから触ってください。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "concept_db",
            title = "dB（デシベル）の読み方",
            summary = "dB は単位ではなく比。後ろに付く基準で意味が変わる",
            keywords = listOf("dB", "デシベル", "対数", "dBu", "dBV", "dBFS", "dBSPL"),
            sections = listOf(
                HelpSection(
                    heading = "なぜ対数なのか",
                    body = """
                        音の世界は扱う範囲が広すぎる。囁き声と耳が痛い音では
                        圧力にして100万倍の開きがある。そのまま並べると桁が邪魔で読めない。

                        対数にすると、この100万倍が 0〜120 に収まる。
                        しかも人の耳は「倍になった」を「一定量増えた」と感じるので、
                        対数の目盛りと聞こえ方がだいたい一致する。二重に都合がよい。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "覚えておく数字",
                    body = """
                        ・+3dB … 力が2倍。スピーカーをもう1本足したとき
                        ・+6dB … 電圧が2倍。距離が半分になったとき
                        ・+10dB … 力が10倍。人には「だいたい2倍の大きさ」に聞こえる
                        ・−6dB … 距離が2倍になったときの落ち方（点音源）
                        ・1dB … ぎりぎり気づく差

                        棒の高さが電圧比（下の数字は倍率）。+10dBで3.16倍、+20dBで
                        ようやく10倍になる感覚をつかんでおくと、卓のフェーダーを
                        大きく動かした「つもり」と実際の変化量のズレが減る。
                    """.trimIndent(),
                    diagram = HelpDiagram.BarSeries(
                        unit = "倍",
                        bars = listOf(
                            HelpDiagram.BarSeries.Bar("+3dB", 1.41f, "電力2倍"),
                            HelpDiagram.BarSeries.Bar("+6dB", 2.00f, "電圧2倍"),
                            HelpDiagram.BarSeries.Bar("+10dB", 3.16f, "体感2倍"),
                            HelpDiagram.BarSeries.Bar("+20dB", 10.00f, "電力100倍"),
                        ),
                    ),
                ),
                HelpSection(
                    heading = "基準が違えば別の目盛り",
                    body = """
                        dBu は 0.775V、dBV は 1V、dBFS はデジタルの上限、
                        dB SPL は 20µPa が 0 の位置。

                        「-18dB」と言われたら、必ず何の -18 かを確かめること。
                        dBFS の -18 は健全なレベル、dB SPL の -18 はありえない値。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "concept_calibration",
            title = "校正（キャリブレーション）",
            summary = "端末のマイクの癖を測って引く作業。これをしないと絶対値は出せない",
            keywords = listOf("校正", "キャリブレーション", "オフセット", "騒音計"),
            sections = listOf(
                HelpSection(
                    heading = "なぜ必要か",
                    body = """
                        端末のマイクは通話用に作られていて、感度が機種ごとに10dB以上違う。
                        さらに、多くの機種で自動ゲイン調整や雑音抑制が入っている。

                        校正していない状態の数字は「0dBFS を 120dB SPL とみなした」仮の値で、
                        絶対値としては当てにならない。増えた減ったを見る道具になる。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "やり方",
                    body = """
                        いちばん確かなのは校正器（94dB や 114dB を出す筒）を当てること。

                        持っていなければ、信用できる騒音計と並べて置き、
                        安定した音を鳴らして差を入力する。これでも実用の精度になる。
                        測るときと同じ場所・同じ向きで合わせること。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "外部マイクを使う場合",
                    body = """
                        USB の測定用マイクを繋げば、端末のマイクより素直な特性で測れる。
                        入力ごとに校正値を別々に保存しているので、
                        繋ぎ替えても取り違えない。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "concept_gain_structure",
            title = "ゲインストラクチャー",
            summary = "どこでどれだけ増幅するかの設計。ノイズと歪みの両方を避ける",
            keywords = listOf("ゲイン", "ゲインステージ", "ヘッドルーム", "S/N", "歪み"),
            sections = listOf(
                HelpSection(
                    heading = "考え方",
                    body = """
                        信号は入り口で必要なだけ持ち上げ、そこから先は素通しにするのが基本。
                        入り口で足りないぶんを後段で稼ぐと、ノイズも一緒に大きくなる。

                        逆に入り口で上げすぎると、後段に余裕が無くなって歪む。
                        「ノイズに埋もれない」と「歪まない」の間を取る作業になる。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "実際の手順",
                    body = """
                        1. 卓のフェーダーを 0（ユニティ）に置く
                        2. いちばん大きい音を出してもらう
                        3. ヘッドアンプでメーターが -18dBFS 前後、ピークで -6dBFS を超えない位置に
                        4. そこから先はフェーダーで音量を作る

                        後で音量が足りないと感じても、まずはフェーダーで。
                        ヘッドアンプに戻るのは、明らかにレベルが低いときだけ。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "concept_feedback",
            title = "ハウリングの仕組みと止め方",
            summary = "スピーカーの音がマイクに戻る輪で発振する。輪のどこかを切る",
            keywords = listOf("ハウリング", "フィードバック", "発振", "ゲインビフォーフィードバック"),
            sections = listOf(
                HelpSection(
                    heading = "何が起きているか",
                    body = """
                        スピーカーから出た音がマイクに入り、また増幅されてスピーカーから出る。
                        この一周で音が小さくならなければ、際限なく大きくなる。これがハウリング。

                        必ず特定の周波数で起きるのは、一周したときの遅れと
                        部屋やマイクの癖が重なって、その周波数だけ増えやすくなるから。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "止める手（効く順）",
                    body = """
                        1. ゲインを下げる … 確実。ただし音量も下がる
                        2. マイクとスピーカーの位置を変える … 根本的。距離と向きが効く
                        3. マイクを口に近づける … 相対的にゲインを下げるのと同じ効果
                        4. 指向性を使う … 単一指向性マイクの後ろにスピーカーを置かない
                        5. EQ で削る … 出た周波数を狭く。広く削ると音が痩せる

                        EQ で追いかけるのは最後。3本も削る状況なら、
                        1〜4 のどれかが間違っている。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "リンギング",
                    body = """
                        まだ発振していないが、ぎりぎりで音が伸びて聞こえる状態。
                        ここまで来ていると、演者が動いた瞬間にハウる。

                        本番前にゆっくりゲインを上げて、ハウる直前から
                        6dB 下げた位置を上限にしておくとよい。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "マイク本数とハウリングマージン（NOM）",
                    body = """
                        同時に開いているマイクの本数（NOM: Number of Open Mics）が
                        倍になるごとに、ハウリングまでの余裕（GBF: Gain Before Feedback）は
                        およそ3dBずつ減る。会議や合唱のように複数マイクが常に開いている
                        現場ほど、1本あたりのゲインを絞る必要が出てくる理由がこれ。

                        対策は「使っていないマイクを閉じる」か、
                        話していないマイクを自動で下げるゲインシェアリング
                        （オートミキサー）を入れること。
                    """.trimIndent(),
                    diagram = HelpDiagram.BarSeries(
                        unit = "dB",
                        bars = listOf(
                            HelpDiagram.BarSeries.Bar("1本", 0f),
                            HelpDiagram.BarSeries.Bar("2本", -3f),
                            HelpDiagram.BarSeries.Bar("4本", -6f),
                            HelpDiagram.BarSeries.Bar("8本", -9f),
                        ),
                    ),
                ),
            ),
        ),
        HelpTopic(
            id = "concept_phantom",
            title = "ファンタム電源（48V）",
            summary = "マイクケーブルで電源を送る仕組み。掛けてよいものと悪いものがある",
            keywords = listOf("ファンタム", "48V", "コンデンサマイク", "リボンマイク"),
            sections = listOf(
                HelpSection(
                    heading = "仕組み",
                    body = """
                        XLR の2番と3番に、同じ電圧（+48V）を同時に掛ける。
                        信号は2番と3番の「差」で送られるので、
                        同じ電圧を両方に乗せても信号には影響しない。
                        1番（グランド）との間に電圧がかかり、そこから電源を取る。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "要るもの・危ないもの",
                    body = """
                        要る … コンデンサマイク、アクティブDI、一部のワイヤレス受信機
                        不要 … ダイナミックマイク（掛かっていても基本は無害）
                        危ない … リボンマイク。結線ミスや断線があると壊れることがある

                        怪しいケーブルを使うくらいなら、ファンタムを切ること。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "抜き差しの順番",
                    body = """
                        必ずファンタムを切ってから抜き差しする。
                        入れたまま抜くと、突入電流でスピーカーから大きなノイズが出る。
                        アンプとスピーカーを傷めることがある。

                        面倒でも、フェーダーを下げる → ファンタムを切る → 抜く、の順で。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "concept_compressor",
            title = "コンプレッサーの動き方",
            summary = "スレッショルドを境に、超えた分だけレシオで圧縮する。曲線で見ると仕組みが分かる",
            keywords = listOf(
                "コンプレッサー", "スレッショルド", "レシオ", "アタック", "リリース",
                "ニー", "ゲインリダクション", "圧縮比", "compressor",
            ),
            sections = listOf(
                HelpSection(
                    heading = "入力と出力の関係を線で見る",
                    body = """
                        コンプは「入力レベル→出力レベル」の変換をしている装置、
                        と考えると分かりやすい。何もしなければ入力と出力は
                        45度の直線（点線）で一致するが、コンプはスレッショルドを
                        超えたところから傾きを寝かせる。

                        下の図はスレッショルド -20dBFS・レシオ4:1の例。
                        入力が 0dBFS まで振れても、出力は -15dBFS までしか
                        上がらない（5dB ぶん圧縮された）のが線の折れ方で見える。
                        レシオを上げるほど、この折れたあとの線はより水平に近づく。
                    """.trimIndent(),
                    diagram = HelpDiagram.LineCurve(
                        xLabel = "入力(dBFS)",
                        yLabel = "出力(dBFS)",
                        series = listOf(
                            HelpDiagram.LineCurve.Series(
                                label = "無圧縮(1:1)",
                                points = listOf(-40f to -40f, 0f to 0f),
                            ),
                            HelpDiagram.LineCurve.Series(
                                label = "Thr-20dB Ratio4:1",
                                points = listOf(-40f to -40f, -20f to -20f, 0f to -15f),
                            ),
                        ),
                    ),
                ),
                HelpSection(
                    heading = "アタック・リリースは「いつ」の話",
                    body = """
                        上の曲線は「どれだけ」圧縮するかの話で、
                        アタックとリリースは「いつ」圧縮が効くかの話になる。

                        アタックが遅ければ、折れ線に乗るまでに一瞬の間があり、
                        アタック音（キックの打撃、歌の子音）はそのまま通り抜ける。
                        リリースが遅ければ、音が小さくなったあとも
                        しばらく圧縮が残り続ける。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "ニーとメイクアップゲイン",
                    body = """
                        ハードニーは図の折れ目が角ばったまま、
                        ソフトニーは折れ目の手前から曲線でなだらかに移行する。
                        同じスレッショルド・レシオでも、ソフトニーの方が
                        かかり始めが自然に聞こえやすい。

                        圧縮すると出力全体が下がるので、メイクアップゲインで
                        持ち上げて元の音量感に戻す。上げすぎるとハウリング
                        マージンを削っていることに気づきにくいので、
                        必要最小限にとどめる。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "concept_polar_pattern",
            title = "マイクの指向性（ポーラーパターン）",
            summary = "正面と背面をどれだけ拾うかの形。ハウリングとかぶりの対策はここから決まる",
            keywords = listOf(
                "指向性", "ポーラーパターン", "カーディオイド", "スーパーカーディオイド",
                "ハイパーカーディオイド", "無指向性", "オムニ", "双指向性", "figure8", "polar",
            ),
            sections = listOf(
                HelpSection(
                    heading = "カーディオイド",
                    body = """
                        正面をよく拾い、背面をほとんど拾わない単一指向性。
                        SM58 をはじめライブの標準パターンで、
                        真後ろにモニタースピーカーを置くのがハウリング対策の基本形になる。
                    """.trimIndent(),
                    diagram = HelpDiagram.PolarPattern(HelpDiagram.PolarPattern.Pattern.CARDIOID),
                ),
                HelpSection(
                    heading = "スーパーカーディオイド / ハイパーカーディオイド",
                    body = """
                        カーディオイドより正面の指向性が鋭い代わりに、
                        真後ろではなく後方斜め約125°の方向に小さな感度の山ができる
                        （図の背面側にできている小さなふくらみがそれ）。

                        モニターを真後ろに置くと、この山にちょうど当たって
                        ハウリングしやすくなる。真後ろを避けてやや斜めに置くとよい。
                    """.trimIndent(),
                    diagram = HelpDiagram.PolarPattern(HelpDiagram.PolarPattern.Pattern.SUPERCARDIOID),
                ),
                HelpSection(
                    heading = "無指向性（オムニ）",
                    body = """
                        全方向をほぼ均等に拾う。図が真円に近いほど無指向性に近い。
                        自然な音でハウリングには弱く、ラベリアマイクに多く使われる。
                    """.trimIndent(),
                    diagram = HelpDiagram.PolarPattern(HelpDiagram.PolarPattern.Pattern.OMNI),
                ),
                HelpSection(
                    heading = "双指向性（フィギュア8）",
                    body = """
                        正面と背面を同じだけ拾い、側面をほとんど拾わない。
                        リボンマイクや MS 方式のステレオ収音で使う。
                        図が8の字（両側にふくらみ、左右がくびれた形）になる。
                    """.trimIndent(),
                    diagram = HelpDiagram.PolarPattern(HelpDiagram.PolarPattern.Pattern.FIGURE_8),
                ),
            ),
        ),
    )
}
