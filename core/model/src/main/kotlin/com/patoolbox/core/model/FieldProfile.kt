package com.patoolbox.core.model

/**
 * 使う人の現場プロフィール。
 *
 * 「全部入り」のアプリは、道具が増えるほど初めての人には使えなくなる。
 * かといって機能を隠すと、慣れた人には遠回りな道具になる。
 * そこで**隠すのではなく並べ方と説明の量を変える**ことにした。
 * ここはその判断材料をまとめて持つ場所。
 *
 * 増やすときの原則: この data class に軸を足し、
 * 「その軸で何が変わるか」を1か所（[com.patoolbox.core.model.ExperienceLevel] のように）
 * 明文化してから使う。設定に項目だけ増えて何も変わらない、を作らないため。
 */
data class FieldProfile(
    val level: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val console: ConsoleType = ConsoleType.UNSET,
) {
    companion object {
        val Default = FieldProfile()
    }
}

/**
 * 慣れの度合い。既定は中級。
 *
 * 初めて開いた人を初心者と決めつけない。ここを既定にすると、
 * 経験者が最初に見る画面が説明だらけになる。
 *
 * この設定で変わるのは次の3つだけ。**機能そのものは消さない。**
 *
 * | | ホームの中身 | 説明文 | 道具一覧 |
 * |---|---|---|---|
 * | [BEGINNER] | 入門の道具＋校正の案内を大きく | 全部出す | 上級の道具に札を付ける |
 * | [INTERMEDIATE] | よく使う4つ＋分類の入口 | 短く出す | そのまま |
 * | [ADVANCED] | 38個をそのまま並べる | 省く | そのまま |
 */
enum class ExperienceLevel {
    /** 初心者。用語と手順の説明を優先し、上級の道具には注意の札を付ける */
    BEGINNER,

    /** 中級者。既定 */
    INTERMEDIATE,

    /** 上級者。説明を省き、密度を優先する */
    ADVANCED,
}

/**
 * よく使う卓の種類。
 *
 * いま効くのは「ホームに最初に出す4つ」の入れ替えだけ。
 * アナログ卓なら帯域チャートとインピーダンス計算、デジタル卓ならパッチ表とスナップショット、
 * という並びに寄せる。
 *
 * 解説やトラブルシュートの中身を卓別に出し分けるのは、
 * 文章を書き足してからにすること（項目だけ増やして何も変わらない状態にしない）。
 */
enum class ConsoleType {
    /** 未設定。卓に依らない並びにする */
    UNSET,

    /** アナログ卓 */
    ANALOG,

    /** デジタル卓 */
    DIGITAL,
}
