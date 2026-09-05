package com.patoolbox.core.dsp

/**
 * ハウリング検出の厳しさ。
 *
 * 現場によって「まだ鳴っていないが危ない」を見たいときと、
 * 「確実にハウっているものだけ」を見たいときがある。
 * 突出量と継続フレーム数を同時に動かすので、利用者に見せるのは3段階だけにしている。
 *
 * ハウリング検出の画面と本番万能コントローラーのモニターで同じものを使う。
 * 段の数や意味が画面ごとに違うと、「厳しい」で見えなかったものが
 * 別の画面では出る、という説明のつかない差になるため。
 */
enum class FeedbackSensitivity(
    val label: String,
    val detail: String,
    val prominenceDb: Double,
    val sustainFrames: Int,
) {
    /** リハで芽を探すとき。楽器の音も拾うので、一覧は賑やかになる */
    HIGH("敏感", "芽の段階で出す", prominenceDb = 9.0, sustainFrames = 3),

    NORMAL("普通", "迷ったらこれ", prominenceDb = 12.0, sustainFrames = 4),

    /** 本番中。確実に発振しているものだけ出す */
    LOW("厳しい", "確実なものだけ", prominenceDb = 16.0, sustainFrames = 6),
}
