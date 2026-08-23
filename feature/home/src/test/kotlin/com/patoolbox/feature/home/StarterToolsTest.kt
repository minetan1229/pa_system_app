package com.patoolbox.feature.home

import com.google.common.truth.Truth.assertThat
import com.patoolbox.core.model.ConsoleType
import com.patoolbox.core.model.ExperienceLevel
import com.patoolbox.core.model.FieldProfile
import com.patoolbox.core.model.ToolAccess
import com.patoolbox.core.model.ToolId
import org.junit.Test

class StarterToolsTest {

    @Test
    fun `どの段でも4つ出る`() {
        ExperienceLevel.entries.forEach { level ->
            ConsoleType.entries.forEach { console ->
                val tools = starterTools(FieldProfile(level = level, console = console))
                assertThat(tools).hasSize(4)
                assertThat(tools).containsNoDuplicates()
            }
        }
    }

    /**
     * 最初の画面から「Pro を買え」に着地させない、という決めごとの番人。
     * ここが破れると、初めて開いた人が4回続けて課金の壁に当たる。
     *
     * 上級者は対象外。その段の人が最初に開くのは Pro の測定道具なので、
     * 無料の道具に差し替えると「自分向けではない画面」になる。
     */
    @Test
    fun `初心者と中級者の4つに Pro 専用は入れない`() {
        listOf(ExperienceLevel.BEGINNER, ExperienceLevel.INTERMEDIATE).forEach { level ->
            ConsoleType.entries.forEach { console ->
                val tools = starterTools(FieldProfile(level = level, console = console))
                assertThat(tools.map { it.access }).doesNotContain(ToolAccess.PRO)
            }
        }
    }

    @Test
    fun `段ごとに中身が変わる`() {
        val beginner = starterTools(FieldProfile(level = ExperienceLevel.BEGINNER))
        val intermediate = starterTools(FieldProfile(level = ExperienceLevel.INTERMEDIATE))
        val advanced = starterTools(FieldProfile(level = ExperienceLevel.ADVANCED))

        assertThat(beginner).isNotEqualTo(intermediate)
        assertThat(intermediate).isNotEqualTo(advanced)

        // 「まず音を測る」だけはどの段でも先頭に置く
        listOf(beginner, intermediate, advanced).forEach {
            assertThat(it.first()).isEqualTo(ToolId.SPL_METER)
        }
    }

    @Test
    fun `アナログ卓では帯域チャートが入る`() {
        val tools = starterTools(
            FieldProfile(level = ExperienceLevel.INTERMEDIATE, console = ConsoleType.ANALOG),
        )
        assertThat(tools).contains(ToolId.FREQ_CHART)
    }

    @Test
    fun `デジタル卓ではパッチ表が入る`() {
        val tools = starterTools(
            FieldProfile(level = ExperienceLevel.INTERMEDIATE, console = ConsoleType.DIGITAL),
        )
        assertThat(tools).contains(ToolId.PATCH_SHEET)
    }

    @Test
    fun `初心者はもともとパッチ表が入っているので デジタル卓でも並びが変わらない`() {
        val unset = starterTools(FieldProfile(level = ExperienceLevel.BEGINNER))
        val digital = starterTools(
            FieldProfile(level = ExperienceLevel.BEGINNER, console = ConsoleType.DIGITAL),
        )
        assertThat(digital).isEqualTo(unset)
    }

    /** 上級者は★で自分の4つを決めるので、卓の設定で勝手に入れ替えない。 */
    @Test
    fun `上級者は卓の種類で変わらない`() {
        val base = starterTools(FieldProfile(level = ExperienceLevel.ADVANCED))
        ConsoleType.entries.forEach { console ->
            val tools = starterTools(
                FieldProfile(level = ExperienceLevel.ADVANCED, console = console),
            )
            assertThat(tools).isEqualTo(base)
        }
    }
}
