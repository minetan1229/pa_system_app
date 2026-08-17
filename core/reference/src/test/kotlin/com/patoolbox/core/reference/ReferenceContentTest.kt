package com.patoolbox.core.reference

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * リファレンスは中身がそのまま製品価値になるので、
 * 「壊れていないこと」を機械的に確認できるところは全部テストしておく。
 * 内容の正しさは人が見るしかないが、構造の破綻はここで防げる。
 */
class ReferenceContentTest {

    // --- コネクタ ---

    @Test
    fun `コネクタは全カテゴリに存在する`() {
        ConnectorCategory.entries.forEach { category ->
            assertWithMessage(category.label)
                .that(Connectors.byCategory(category))
                .isNotEmpty()
        }
    }

    @Test
    fun `コネクタ名は重複しない`() {
        assertThat(Connectors.ALL.map { it.name }).containsNoDuplicates()
    }

    @Test
    fun `コネクタにはピン配列か注意点がある`() {
        Connectors.ALL.forEach { connector ->
            assertWithMessage(connector.name)
                .that(connector.pins.isNotEmpty() || connector.cautions.isNotEmpty())
                .isTrue()
            assertWithMessage(connector.name).that(connector.summary).isNotEmpty()
        }
    }

    @Test
    fun `XLRのピン配列が規格どおり`() {
        val xlr = Connectors.ALL.first { it.name.startsWith("XLR") }

        assertThat(xlr.pins.map { it.pin }).containsExactly("1", "2", "3").inOrder()
        assertThat(xlr.pins[1].signal).contains("ホット")
        assertThat(xlr.pins[2].signal).contains("コールド")
    }

    @Test
    fun `コネクタ検索が名前と注意点と別名に効く`() {
        assertThat(Connectors.search("Dante").map { it.name }).contains("Dante")
        assertThat(Connectors.search("ダンテ").map { it.name }).contains("Dante")
        // 症状から引く。「リボン」は注意点の本文にしか出てこない
        assertThat(Connectors.search("リボン").map { it.name })
            .contains("ファンタム電源（48V）")
    }

    @Test
    fun `コネクタ検索は空なら全件を重要度順で返す`() {
        assertThat(Connectors.search("")).containsExactlyElementsIn(Connectors.ALL).inOrder()
        assertThat(Connectors.search("  ")).hasSize(Connectors.ALL.size)
    }

    @Test
    fun `一致しないコネクタ検索は空`() {
        assertThat(Connectors.search("該当しない語句xyz")).isEmpty()
    }

    @Test
    fun `デジタルとネットワークのコネクタが載っている`() {
        // 「浅い図鑑」に戻っていないことの見張り。
        // AES/EBU と Dante が無い状態は、この app では不足として扱う
        val names = Connectors.ALL.map { it.name }
        assertThat(names).contains("AES/EBU（AES3）")
        assertThat(names).contains("MADI（AES10）")
        assertThat(names).contains("Dante")
    }

    @Test
    fun `カテゴリごとに並びが崩れていない`() {
        // byCategory は ALL の順（重要度順）を保つこと
        ConnectorCategory.entries.forEach { category ->
            val fromAll = Connectors.ALL.filter { it.category == category }
            assertWithMessage(category.label)
                .that(Connectors.byCategory(category))
                .containsExactlyElementsIn(fromAll)
                .inOrder()
        }
    }

    // --- 解説 ---

    @Test
    fun `実装済みの全ツールに解説がある`() {
        com.patoolbox.core.model.ToolId.entries
            .filter { it.implemented }
            .forEach { tool ->
                assertWithMessage(tool.name)
                    .that(HelpTopics.forTool(tool))
                    .isNotNull()
            }
    }

    @Test
    fun `解説のIDは重複しない`() {
        assertThat(HelpTopics.ALL.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun `解説には要約と節がある`() {
        HelpTopics.ALL.forEach { topic ->
            assertWithMessage(topic.id).that(topic.summary).isNotEmpty()
            assertWithMessage(topic.id).that(topic.sections).isNotEmpty()
            topic.sections.forEach { section ->
                assertWithMessage("${topic.id} / ${section.heading}")
                    .that(section.body)
                    .isNotEmpty()
            }
        }
    }

    @Test
    fun `解説の検索が本文にも効く`() {
        assertThat(HelpTopics.search("スイープ").map { it.id })
            .contains("delay_finder")
        assertThat(HelpTopics.search("デシベル").map { it.id })
            .contains("concept_db")
        assertThat(HelpTopics.search("")).hasSize(HelpTopics.ALL.size)
        assertThat(HelpTopics.search("該当しない語句xyz")).isEmpty()
    }

    // --- 帯域チャート ---

    @Test
    fun `帯域の範囲が逆転していない`() {
        FrequencyChart.ALL.forEach { instrument ->
            assertWithMessage(instrument.instrument)
                .that(instrument.fundamentalFromHz)
                .isLessThan(instrument.fundamentalToHz)

            instrument.tips.forEach { tip ->
                assertWithMessage("${instrument.instrument} / ${tip.label}")
                    .that(tip.fromHz)
                    .isLessThan(tip.toHz)
            }
        }
    }

    @Test
    fun `帯域が可聴域に収まっている`() {
        FrequencyChart.ALL.forEach { instrument ->
            instrument.tips.forEach { tip ->
                assertWithMessage("${instrument.instrument} / ${tip.label}")
                    .that(tip.fromHz)
                    .isAtLeast(20.0)
                assertWithMessage("${instrument.instrument} / ${tip.label}")
                    .that(tip.toHz)
                    .isAtMost(20_000.0)
            }
        }
    }

    @Test
    fun `楽器名は重複しない`() {
        assertThat(FrequencyChart.ALL.map { it.instrument }).containsNoDuplicates()
    }

    @Test
    fun `周波数から該当する帯域を引ける`() {
        // 250Hz は複数の楽器で「こもり」に関わる帯域
        val hits = FrequencyChart.tipsAt(250.0)

        assertThat(hits).isNotEmpty()
        hits.forEach { (_, tip) ->
            assertThat(250.0).isAtLeast(tip.fromHz)
            assertThat(250.0).isAtMost(tip.toHz)
        }
    }

    @Test
    fun `範囲外の周波数では何も返らない`() {
        assertThat(FrequencyChart.tipsAt(19_999.0)).isEmpty()
    }

    // --- トラブルシュート ---

    @Test
    fun `分岐先のIDが必ず存在する`() {
        Troubleshooting.ALL.forEach { flow ->
            val ids = flow.steps.map { it.id }.toSet()
            assertWithMessage(flow.title).that(ids).contains(flow.startId)

            flow.steps.filterIsInstance<TroubleshootQuestion>().forEach { question ->
                assertWithMessage("${flow.title} / ${question.id} の yes")
                    .that(ids).contains(question.yesId)
                assertWithMessage("${flow.title} / ${question.id} の no")
                    .that(ids).contains(question.noId)
            }
        }
    }

    @Test
    fun `全ステップが開始地点から辿り着ける`() {
        // 到達できないステップは書いたつもりで使われていない＝バグ
        Troubleshooting.ALL.forEach { flow ->
            val reachable = mutableSetOf<String>()
            val queue = ArrayDeque(listOf(flow.startId))

            while (queue.isNotEmpty()) {
                val id = queue.removeFirst()
                if (!reachable.add(id)) continue
                val step = flow.step(id)
                if (step is TroubleshootQuestion) {
                    queue += step.yesId
                    queue += step.noId
                }
            }

            assertWithMessage(flow.title)
                .that(reachable)
                .containsExactlyElementsIn(flow.steps.map { it.id })
        }
    }

    @Test
    fun `どの分岐も必ず対処に行き着く`() {
        Troubleshooting.ALL.forEach { flow ->
            flow.steps.filterIsInstance<TroubleshootQuestion>().forEach { question ->
                listOf(question.yesId, question.noId).forEach { nextId ->
                    assertWithMessage("${flow.title} / $nextId")
                        .that(terminates(flow, nextId, depth = 0))
                        .isTrue()
                }
            }
        }
    }

    @Test
    fun `対処には具体的な行動が書いてある`() {
        Troubleshooting.ALL.forEach { flow ->
            flow.steps.filterIsInstance<TroubleshootResolution>().forEach { resolution ->
                assertWithMessage("${flow.title} / ${resolution.id}")
                    .that(resolution.actions)
                    .isNotEmpty()
            }
        }
    }

    @Test
    fun `トラブルシュートは3種類ある`() {
        assertThat(Troubleshooting.ALL).hasSize(3)
        assertThat(Troubleshooting.ALL.map { it.title })
            .containsExactly("音が出ない", "ハウリングする", "ハムノイズが出る")
    }

    // --- 用語辞典 ---

    @Test
    fun `用語は重複しない`() {
        assertThat(Glossary.ALL.map { it.term }).containsNoDuplicates()
    }

    @Test
    fun `用語には説明と英語表記がある`() {
        Glossary.ALL.forEach { term ->
            assertWithMessage(term.term).that(term.description).isNotEmpty()
            assertWithMessage(term.term).that(term.english).isNotEmpty()
        }
    }

    @Test
    fun `用語は全カテゴリに存在する`() {
        GlossaryCategory.entries.forEach { category ->
            assertWithMessage(category.label)
                .that(Glossary.byCategory(category))
                .isNotEmpty()
        }
    }

    @Test
    fun `検索が用語と英語と説明に効く`() {
        assertThat(Glossary.search("ハウリング").map { it.term }).contains("ハウリング")
        assertThat(Glossary.search("feedback").map { it.term }).contains("ハウリング")
        assertThat(Glossary.search("48V").map { it.term }).contains("ファンタム")
    }

    @Test
    fun `空の検索は全件返す`() {
        assertThat(Glossary.search("")).hasSize(Glossary.ALL.size)
        assertThat(Glossary.search("   ")).hasSize(Glossary.ALL.size)
    }

    @Test
    fun `一致しない検索は空`() {
        assertThat(Glossary.search("該当しない語句xyz")).isEmpty()
    }

    private fun terminates(flow: TroubleshootFlow, id: String, depth: Int): Boolean {
        if (depth > MAX_DEPTH) return false
        return when (val step = flow.step(id)) {
            is TroubleshootResolution -> true
            is TroubleshootQuestion ->
                terminates(flow, step.yesId, depth + 1) &&
                    terminates(flow, step.noId, depth + 1)
            null -> false
        }
    }

    private companion object {
        const val MAX_DEPTH = 20
    }
}
