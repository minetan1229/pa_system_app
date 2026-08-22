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
    fun `可聴域の外では何も返らない`() {
        // 可聴域の上端（20kHz）は「切って良い帯域」として登録があるので、
        // 範囲外の判定はその外側で確かめる
        assertThat(FrequencyChart.tipsAt(25_000.0)).isEmpty()
        assertThat(FrequencyChart.tipsAt(10.0)).isEmpty()
    }

    @Test
    fun `帯域には必ず数字入りのワンアドバイスが付いている`() {
        // 「ここが効く」だけでは卓の前で手が止まる。
        // 中心周波数か量のどちらかは必ず数字で書く決まりにしている
        FrequencyChart.ALL.forEach { instrument ->
            instrument.tips.forEach { tip ->
                assertWithMessage("${instrument.instrument} / ${tip.label}")
                    .that(tip.advice)
                    .isNotEmpty()
                assertWithMessage("${instrument.instrument} / ${tip.label} に数字が無い")
                    .that(tip.advice.any { it.isDigit() })
                    .isTrue()
            }
        }
    }

    @Test
    fun `楽器ごとに作り方が埋まっている`() {
        FrequencyChart.ALL.forEach { instrument ->
            assertWithMessage(instrument.instrument).that(instrument.role).isNotEmpty()
            assertWithMessage(instrument.instrument).that(instrument.micTip).isNotEmpty()
            assertWithMessage(instrument.instrument).that(instrument.dynamicsTip).isNotEmpty()
            assertWithMessage(instrument.instrument).that(instrument.conflicts).isNotEmpty()
            assertWithMessage(instrument.instrument).that(instrument.pitfalls).isNotEmpty()
            assertWithMessage(instrument.instrument).that(instrument.tips).isNotEmpty()
        }
    }

    @Test
    fun `ハイパスは基音の上端より下に置かれている`() {
        // 基音の下端に踏み込むハイパスは正しい（エレキギターの 100Hz、
        // スピーチの 120Hz は、基音の最低音を捨てても成立するので現場の定番）。
        // 一方で基音の範囲を丸ごと超えるハイパスは、その楽器を消す指示になる
        FrequencyChart.ALL.forEach { instrument ->
            val highPass = instrument.highPassHz ?: return@forEach
            assertWithMessage("${instrument.instrument} のハイパスが基音を全部削っている")
                .that(highPass.toDouble())
                .isLessThan(instrument.fundamentalToHz)
            assertWithMessage("${instrument.instrument} のハイパスが現実的な範囲を外れている")
                .that(highPass)
                .isIn(20..500)
        }
    }

    @Test
    fun `タムは3点が別々に載っている`() {
        // 「タム」1枚だと、3点で同じ EQ を使い回す原因になる
        val names = FrequencyChart.ALL.map { it.instrument }
        assertThat(names).contains("ハイタム")
        assertThat(names).contains("ロータム")
        assertThat(names).contains("フロアタム")
    }

    @Test
    fun `タムの重心はハイタムからフロアタムへ下がっていく`() {
        val high = FrequencyChart.ALL.first { it.instrument == "ハイタム" }
        val low = FrequencyChart.ALL.first { it.instrument == "ロータム" }
        val floor = FrequencyChart.ALL.first { it.instrument == "フロアタム" }

        assertThat(high.fundamentalFromHz).isGreaterThan(low.fundamentalFromHz)
        assertThat(low.fundamentalFromHz).isGreaterThan(floor.fundamentalFromHz)
    }

    @Test
    fun `全分類に楽器がある`() {
        InstrumentGroup.entries.forEach { group ->
            assertWithMessage(group.label).that(FrequencyChart.byGroup(group)).isNotEmpty()
        }
    }

    @Test
    fun `帯域チャートは症状の言葉でも引ける`() {
        // 現場で打つのは楽器名ではなく症状のことが多い
        assertThat(FrequencyChart.search("ピエゾ").map { it.instrument })
            .contains("アコースティックギター（ピエゾ）")
        assertThat(FrequencyChart.search("こもり")).isNotEmpty()
        assertThat(FrequencyChart.search("")).hasSize(FrequencyChart.ALL.size)
        assertThat(FrequencyChart.search("該当しない語句xyz")).isEmpty()
    }

    // --- 帯域辞書 ---

    @Test
    fun `帯域辞書は可聴域を隙間なく覆っている`() {
        // 隙間があると「その周波数だけ引けない」ことになる。
        // ハウリング検出から引かれる表なので、抜けは実害になる
        val sorted = BandDictionary.ALL.sortedBy { it.fromHz }

        assertThat(sorted.first().fromHz).isEqualTo(20.0)
        assertThat(sorted.last().toHz).isEqualTo(20_000.0)
        sorted.zipWithNext().forEach { (lower, upper) ->
            assertWithMessage("${lower.label} と ${upper.label} の間に隙間がある")
                .that(lower.toHz)
                .isEqualTo(upper.fromHz)
        }
    }

    @Test
    fun `どの周波数からも帯域を引ける`() {
        listOf(25.0, 60.0, 100.0, 250.0, 500.0, 1_000.0, 2_000.0, 3_150.0, 6_300.0, 12_000.0)
            .forEach { hz ->
                assertWithMessage("$hz Hz が引けない")
                    .that(BandDictionary.at(hz))
                    .isNotNull()
            }
        // 可聴域の外は引けない
        assertThat(BandDictionary.at(10.0)).isNull()
        assertThat(BandDictionary.at(25_000.0)).isNull()
    }

    @Test
    fun `帯域辞書は数字で検索できる`() {
        // アナライザやハウリング検出が出すのは数字なので、そのまま打てること
        val hit = BandDictionary.search("250").single()

        assertThat(hit.contains(250.0)).isTrue()
        assertThat(BandDictionary.search("こもり")).isNotEmpty()
        assertThat(BandDictionary.search("")).hasSize(BandDictionary.ALL.size)
        assertThat(BandDictionary.search("該当しない語句xyz")).isEmpty()
    }

    @Test
    fun `帯域辞書の上げ下げには数字が入っている`() {
        // 「上げると太くなる」だけでは卓の前で手が止まる
        BandDictionary.ALL.forEach { band ->
            assertWithMessage("${band.label} の boost に数字が無い")
                .that(band.boost.any { it.isDigit() })
                .isTrue()
            assertWithMessage("${band.label} の cut に数字が無い")
                .that(band.cut.any { it.isDigit() })
                .isTrue()
            assertWithMessage(band.label).that(band.lives).isNotEmpty()
            assertWithMessage(band.label).that(band.problems).isNotEmpty()
            assertWithMessage(band.label).that(band.feedbackNote).isNotEmpty()
        }
    }

    @Test
    fun `ハウリングしやすい帯域が中域に集まっている`() {
        // 実際に回るのは 300Hz〜5kHz。ここが空になる編集は事故
        val high = BandDictionary.byRisk(FeedbackRisk.HIGH)

        assertThat(high).isNotEmpty()
        high.forEach { band ->
            assertWithMessage("${band.label} が回りやすい扱いになっている")
                .that(band.centerHz)
                .isIn(com.google.common.collect.Range.closed(200.0, 8_000.0))
        }
    }

    // --- テスト信号 ---

    @Test
    fun `テスト信号は全種類に項目がある`() {
        TestSignalKind.entries.forEach { kind ->
            assertWithMessage(kind.label).that(TestSignalGuide.byKind(kind)).isNotEmpty()
        }
        assertThat(TestSignalGuide.ALL.map { it.name }).containsNoDuplicates()
    }

    @Test
    fun `ノイズの傾きが定義どおり`() {
        // ここが狂うと図がそのまま嘘になる
        fun slopeOf(name: String) = TestSignalGuide.ALL.first { it.name.startsWith(name) }
            .slopeDbPerOctave

        assertThat(slopeOf("ピンクノイズ")).isEqualTo(-3.0)
        assertThat(slopeOf("ホワイトノイズ")).isEqualTo(0.0)
        assertThat(slopeOf("ブラウンノイズ")).isEqualTo(-6.0)
    }

    @Test
    fun `テスト信号には用途とレベルの目安がある`() {
        TestSignalGuide.ALL.forEach { signal ->
            assertWithMessage(signal.name).that(signal.soundsLike).isNotEmpty()
            assertWithMessage(signal.name).that(signal.whatItIs).isNotEmpty()
            assertWithMessage(signal.name).that(signal.useFor).isNotEmpty()
            assertWithMessage(signal.name).that(signal.levelTip).isNotEmpty()
        }
    }

    @Test
    fun `機材を壊す信号には注意が書いてある`() {
        // ホワイトノイズ・方形波・連続サイン波はツイータを焼く。
        // 注意が消えた状態で配るわけにはいかない
        listOf("ホワイトノイズ", "サイン波", "方形波").forEach { name ->
            val signal = TestSignalGuide.ALL.first { it.name.startsWith(name) }
            assertWithMessage(signal.name).that(signal.cautions).isNotEmpty()
        }
    }

    @Test
    fun `テスト信号は日本語でも英語でも引ける`() {
        assertThat(TestSignalGuide.search("ピンク").map { it.name }).contains("ピンクノイズ")
        assertThat(TestSignalGuide.search("pink").map { it.name }).contains("ピンクノイズ")
        assertThat(TestSignalGuide.search("sweep")).isNotEmpty()
        assertThat(TestSignalGuide.search("")).hasSize(TestSignalGuide.ALL.size)
        assertThat(TestSignalGuide.search("該当しない語句xyz")).isEmpty()
    }

    // --- 音質劣化 ---

    @Test
    fun `劣化の項目名は重複しない`() {
        assertThat(SignalDegradation.ALL.map { it.title }).containsNoDuplicates()
    }

    @Test
    fun `劣化はすべての項目が症状と対処を持つ`() {
        SignalDegradation.ALL.forEach { item ->
            assertWithMessage(item.title).that(item.symptom).isNotEmpty()
            assertWithMessage(item.title).that(item.mechanism).isNotEmpty()
            assertWithMessage(item.title).that(item.amount).isNotEmpty()
            assertWithMessage(item.title).that(item.fixes).isNotEmpty()
        }
    }

    @Test
    fun `劣化の量には数字が入っている`() {
        // 「劣化する」だけでは対処する価値があるか判断できない
        SignalDegradation.ALL.forEach { item ->
            assertWithMessage("${item.title} の量に数字が無い")
                .that(item.amount.any { it.isDigit() })
                .isTrue()
        }
    }

    @Test
    fun `劣化は信号経路の全段に項目がある`() {
        DegradationStage.entries.forEach { stage ->
            assertWithMessage(stage.label).that(SignalDegradation.byStage(stage)).isNotEmpty()
        }
    }

    @Test
    fun `劣化は上流から下流の順に並んでいる`() {
        // 並び順そのものが「上から確かめれば上流から確かめたことになる」という
        // この画面の作りを支えているので、順番が崩れたら落とす
        val ordinals = SignalDegradation.ALL.map { it.stage.ordinal }
        assertThat(ordinals).isInOrder()
    }

    @Test
    fun `不可逆な劣化が漏れていない`() {
        // 現場で最も損失が大きいのは「後から直せない」種類。
        // ここが空になる編集は事故なので見張る
        val fatal = SignalDegradation.bySeverity(DegradationSeverity.FATAL).map { it.title }
        assertThat(fatal).isNotEmpty()
        assertThat(fatal).contains("入力段のクリップ（歪み）")
    }

    @Test
    fun `劣化は症状の言葉で引ける`() {
        assertThat(SignalDegradation.search("ブーン")).isNotEmpty()
        assertThat(SignalDegradation.search("bluetooth").map { it.title })
            .contains("Bluetooth 経由の再エンコード")
        assertThat(SignalDegradation.search("")).hasSize(SignalDegradation.ALL.size)
        assertThat(SignalDegradation.search("該当しない語句xyz")).isEmpty()
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
