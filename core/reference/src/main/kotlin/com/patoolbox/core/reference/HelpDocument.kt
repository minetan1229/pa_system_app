package com.patoolbox.core.reference

import com.patoolbox.core.model.ToolId

/** 現場ドキュメント系の解説。 */
internal object DocumentHelp {

    val TOPICS: List<HelpTopic> = listOf(
        HelpTopic(
            id = "show_timer",
            tool = ToolId.SHOW_TIMER,
            title = "本番タイマー",
            summary = "持ち時間を大きく出す。同じ画面で音のレベルとハウリングも見る",
            keywords = listOf("タイマー", "カウントダウン", "押し", "本番モード", "ハウリング"),
            sections = listOf(
                HelpSection(
                    heading = "押し（超過）の表示",
                    body = """
                        カウントダウンが 0 を割ると、数字が赤に変わって
                        超過した時間を数え始める。色が切り替わること自体が合図なので、
                        数字を読まなくても遠目で分かる。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "同じ画面にモニタを載せている理由",
                    body = """
                        本番中は卓から離れられない。時間を見るために画面を切り替えると
                        音が見えなくなり、その往復が面倒でどちらも見なくなる。

                        だからレベルとスペクトラム、ハウリング検出をこの画面に置いている。
                        細かい設定はしない代わりに、見れば分かる状態だけを出す。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "ハウリング表示",
                    body = """
                        モニタ中、鳴り続けている突出した成分を見つけると
                        その周波数・音名・1/3オクターブ帯域を出す。

                        「周りより大きく、かつ続いている」ものだけを拾うので、
                        楽器の音では基本的に出ない。出たらまずゲインを下げ、
                        それでも続くなら出た周波数を狭く削る。

                        検出は目安。表示が出ないからハウっていない、という保証ではない。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "本番モード",
                    body = """
                        通知・着信・画面消灯をまとめて止める。何を止めるかは個別に選べる。
                        アラームで転換を管理している現場もあるので、一括にしていない。

                        通知を止めるには端末の「おやすみモード」を触る許可が要る。
                        許可が無いときは設定への導線を出す。

                        画面を閉じると自動で元に戻す。端末を黙らせたまま
                        戻し忘れるのが、この機能でいちばん困る失敗なので。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "sfx_pads",
            tool = ToolId.SFX_PADS,
            title = "SE パッド",
            summary = "取り込んだ音を押した瞬間に鳴らす。オフラインで動く",
            keywords = listOf("SE", "効果音", "パッド", "出囃子", "ジングル"),
            sections = listOf(
                HelpSection(
                    heading = "取り込めるファイル",
                    body = """
                        音声ファイルだけを選ぶようにしてある（mp3 / m4a / wav / aac / ogg / flac）。
                        取り込んだ時点でアプリの中にコピーするので、
                        元のファイルを消しても、圏外でも鳴る。

                        現場でネットワークに依存するものを使わない、というのが前提。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "パッドの操作",
                    body = """
                        タップで再生。長押しで設定（名前・色・音量・ループ・削除）。
                        本番中に触るのはタップだけになるよう、設定は全部奥に隠してある。

                        色は6色。暗い FOH で見分けるためのものなので、
                        隣り合うパッドは違う色にしておくとよい。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "音量について",
                    body = """
                        パッドごとの音量は、素材どうしのばらつきを揃えるためのもの。
                        全体の音量は卓側で取ること。ここで上げ切ってしまうと、
                        本番中に下げる手段が卓しか無くなる。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "気をつけること",
                    body = """
                        他のアプリが音を出していると、取り合いになって止まることがある。
                        本番前に、実際に使う経路で一度鳴らして確かめること。

                        削除するとファイルごと消える。取り消せない。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "patch_sheet",
            tool = ToolId.PATCH_SHEET,
            title = "パッチ表・インプットリスト",
            summary = "どのチャンネルに何が入るかの表。現場で最初に配る紙",
            keywords = listOf("パッチ", "インプットリスト", "ch", "マルチ"),
            sections = listOf(
                HelpSection(
                    heading = "何を書くか",
                    body = """
                        ch番号・音源・マイクやDI・スタンド・ファンタムの要否・マルチの番号。
                        この6つが揃っていれば、仕込みは他人に任せられる。

                        マイクの機種を書いておくと、代替品を出すときの判断が早い。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "ファンタムの列を必ず埋める",
                    body = """
                        コンデンサマイクとアクティブDIには48Vが要る。
                        リボンマイクには掛けてはいけない。

                        表に書いていないと、仕込みの人がまとめて全部オンにする。
                        その一手で機材が壊れることがある。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "PDF にして配る",
                    body = """
                        作った表は PDF にして共有できる。
                        紙で配る、事前にメールで送る、どちらにも使える。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "run_sheet",
            tool = ToolId.RUN_SHEET,
            title = "進行表・タイムテーブル",
            summary = "時間割を作る。押したり巻いたりしたぶんを自動で後ろに反映する",
            keywords = listOf("進行表", "タイムテーブル", "香盤", "押し", "巻き"),
            sections = listOf(
                HelpSection(
                    heading = "押し巻きの自動計算",
                    body = """
                        1つの項目が延びると、それ以降の時刻がまとめてずれる。
                        手で書き直すと必ずどこかが古いままになるので、自動で計算している。

                        「この時点で何分押している」が常に見えるので、
                        どこで巻くかの相談が数字でできる。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "書いておくとよいこと",
                    body = """
                        転換の時間を必ず項目として立てること。
                        演奏時間だけ並べた進行表は、必ず現実とずれる。

                        搬入・サウンドチェック・開場・終演・撤収まで入れておくと、
                        当日の全体が1枚で見える。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "stage_plot",
            tool = ToolId.STAGE_PLOT,
            title = "ステージプロット",
            summary = "誰がどこに立つかの平面図。モニターと電源の位置も入れる",
            keywords = listOf("ステージプロット", "図面", "配置図", "モニター"),
            sections = listOf(
                HelpSection(
                    heading = "何を描くか",
                    body = """
                        演者の位置、アンプとドラムの位置、モニターの番号と向き、電源の位置。

                        モニターの番号を図に入れておくのが要点。
                        「上手のギター」ではなく「モニター3」で会話できるようになる。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "PDF で渡す",
                    body = """
                        図面はそのまま PDF にできる。
                        パッチ表と一緒に渡すと、初めての会場でも仕込みが通じる。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "job_manager",
            tool = ToolId.JOB_MANAGER,
            title = "案件管理",
            summary = "会場・日時・搬入・連絡先を案件単位でまとめる",
            keywords = listOf("案件", "現場", "スケジュール", "連絡先"),
            sections = listOf(
                HelpSection(
                    heading = "案件が中心にある理由",
                    body = """
                        パッチ表も進行表も見積も、結局は1つの現場に紐づく。
                        案件を先に作っておくと、後から「あの現場の資料」でまとめて引ける。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "写真を残す",
                    body = """
                        分電盤の位置、搬入口、卓の設置場所、天井のフックまで撮っておくと、
                        次に同じ会場に入るときの下見が要らなくなる。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "snapshot",
            tool = ToolId.SNAPSHOT,
            title = "セッティング再現",
            summary = "前回の卓設定を写真と数値で残す。同じ現場を早く立ち上げる",
            keywords = listOf("スナップショット", "再現", "セッティング", "設定記録"),
            sections = listOf(
                HelpSection(
                    heading = "何を残すか",
                    body = """
                        卓の画面写真、EQ とディレイの数値、ゲインの位置。
                        アナログ卓なら全体を1枚撮っておくだけでも効く。

                        数値だけでなく写真を一緒に残すのは、
                        書き写す途中で必ず抜けが出るため。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "gear_inventory",
            tool = ToolId.GEAR_INVENTORY,
            title = "機材台帳・貸出管理",
            summary = "何を持っているか、いまどこにあるかを管理する",
            keywords = listOf("台帳", "在庫", "貸出", "点検", "機材"),
            sections = listOf(
                HelpSection(
                    heading = "使い方",
                    body = """
                        機材を登録して、貸出と返却を記録する。
                        現場から戻ってきていないものが一覧で分かる。

                        点検の記録を残しておくと、
                        「前回いつ直したか」が次の故障のときに効いてくる。
                    """.trimIndent(),
                ),
            ),
        ),
        HelpTopic(
            id = "pdf_export",
            tool = ToolId.PDF_EXPORT,
            title = "PDF出力・共有",
            summary = "パッチ表・進行表・図面を PDF にして渡す",
            keywords = listOf("PDF", "出力", "共有", "印刷"),
            sections = listOf(
                HelpSection(
                    heading = "なぜ PDF なのか",
                    body = """
                        相手の環境に左右されずに、こちらが見せたい形のまま届く。
                        印刷しても崩れない。現場で紙にする前提の書類には向いている。
                    """.trimIndent(),
                ),
                HelpSection(
                    heading = "渡す前に確かめること",
                    body = """
                        ページが切れていないか、文字が枠から出ていないか。
                        自動で改ページしているので、項目が多いと分かれる位置が変わる。
                    """.trimIndent(),
                ),
            ),
        ),
    )
}
