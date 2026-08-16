package com.patoolbox.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * ツールカタログの不変条件を守るためのテスト。
 * 35個のテーブルは手で編集するので、うっかり壊したらここで落ちるようにしている。
 */
class ToolIdTest {

    @Test
    fun `カタログは36ツールある`() {
        assertThat(ToolId.entries).hasSize(36)
    }

    @Test
    fun `無料18 Pro18 の線引きを保つ`() {
        val free = ToolId.entries.count { it.access != ToolAccess.PRO }
        val pro = ToolId.entries.count { it.access == ToolAccess.PRO }

        assertThat(free).isEqualTo(18)
        assertThat(pro).isEqualTo(18)
    }

    @Test
    fun `バッジは4文字以内で空でない`() {
        ToolId.entries.forEach { tool ->
            assertThat(tool.badge).isNotEmpty()
            assertThat(tool.badge.length).isAtMost(4)
        }
    }

    @Test
    fun `バッジはカテゴリ内で重複しない`() {
        ToolCategory.entries.forEach { category ->
            val badges = ToolId.entries.filter { it.category == category }.map { it.badge }
            assertThat(badges).containsNoDuplicates()
        }
    }

    @Test
    fun `全カテゴリにツールがある`() {
        ToolCategory.entries.forEach { category ->
            assertThat(ToolId.entries.any { it.category == category }).isTrue()
        }
    }

    @Test
    fun `phase は1から6の範囲`() {
        ToolId.entries.forEach { tool ->
            assertThat(tool.phase).isIn(1..6)
        }
    }

    @Test
    fun `fromIdOrNull は enum 名で引ける`() {
        assertThat(ToolId.fromIdOrNull("SPL_METER")).isEqualTo(ToolId.SPL_METER)
        assertThat(ToolId.fromIdOrNull("NOPE")).isNull()
    }
}
