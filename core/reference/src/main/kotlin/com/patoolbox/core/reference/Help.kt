package com.patoolbox.core.reference

import com.patoolbox.core.model.ToolId

/**
 * 解説の1節。
 *
 * 見出しを必ず付けるのは、現場で全部読む人がいないため。
 * 「いま知りたいのはどれか」を見出しだけで選べる粒度に割ってある。
 */
data class HelpSection(val heading: String, val body: String)

/**
 * 1画面ぶんの解説。
 *
 * [summary] は画面上部にそのまま1行で出す前提の短文。
 * [sections] はボタンを押して開いたときに読む本文。
 *
 * @param tool 紐づくツール。null は画面に属さない読み物（単位の話など）
 * @param keywords 検索に引っかけたい語。本文に無い言い回し（略称・英語）を足す
 */
data class HelpTopic(
    val id: String,
    val title: String,
    val summary: String,
    val sections: List<HelpSection>,
    val tool: ToolId? = null,
    val keywords: List<String> = emptyList(),
) {
    /**
     * 検索用に本文をまとめて小文字にしたもの。
     *
     * 公開しているのは、ホームのツール検索がこれを索引に混ぜているため。
     * 「Dante」や「ハウリング」のように、ツール名にも説明文にも出てこないが
     * 解説の本文には書いてある語で目的の画面へ着けるようにしている。
     */
    val searchText: String = buildString {
        append(title).append(' ')
        append(summary).append(' ')
        sections.forEach { append(it.heading).append(' ').append(it.body).append(' ') }
        keywords.forEach { append(it).append(' ') }
    }.lowercase()
}

/**
 * アプリ内の解説すべて。
 *
 * 文言を strings.xml ではなく Kotlin で持っているのは [Connectors] などと同じ理由で、
 * これが「UI の文言」ではなく「アプリの中身」だから。量が多く、構造があり、
 * 節ごとに順番の意味がある。
 */
object HelpTopics {

    val ALL: List<HelpTopic> = buildList {
        addAll(MeasureHelp.TOPICS)
        addAll(CalcHelp.TOPICS)
        addAll(DocumentHelp.TOPICS)
        addAll(BusinessHelp.TOPICS)
        addAll(ConceptHelp.TOPICS)
    }

    private val byTool: Map<ToolId, HelpTopic> =
        ALL.mapNotNull { topic -> topic.tool?.let { it to topic } }.toMap()

    fun forTool(tool: ToolId): HelpTopic? = byTool[tool]

    fun byId(id: String): HelpTopic? = ALL.firstOrNull { it.id == id }

    /** 空文字なら全件。順番は登録順（＝重要な順）を保つ */
    fun search(query: String): List<HelpTopic> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return ALL
        return ALL.filter { it.searchText.contains(needle) }
    }
}
