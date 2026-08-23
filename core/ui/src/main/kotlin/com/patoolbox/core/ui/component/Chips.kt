package com.patoolbox.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.patoolbox.core.designsystem.component.PaPill
import com.patoolbox.core.designsystem.component.PaTone
import com.patoolbox.core.model.ToolAccess
import com.patoolbox.core.model.ToolLevel
import com.patoolbox.core.ui.R

/**
 * Pro 専用 / 無料（制限あり）を示す小さなチップ。
 *
 * 状態を示す札は [PaPill] に統一している。ここで独自に色を組まないのは、
 * 4つのテーマぶんの出し分けを1か所（[PaTone]）に集めておくため。
 */
@Composable
fun AccessChip(
    access: ToolAccess,
    modifier: Modifier = Modifier,
) {
    when (access) {
        ToolAccess.PRO -> PaPill(
            text = stringResource(R.string.badge_pro),
            tone = PaTone.BRAND,
            modifier = modifier,
        )

        ToolAccess.FREE_LIMITED -> PaPill(
            text = stringResource(R.string.badge_free_limited),
            tone = PaTone.NEUTRAL,
            modifier = modifier,
        )

        // 無料は既定なのでバッジを出さない（画面のノイズを減らす）
        ToolAccess.FREE -> Unit
    }
}

/**
 * 前提知識の要る道具に付ける札。
 *
 * 初心者の表示のときだけ出す。**押せなくはしない**——
 * 「上級」と書いてあるだけで、開けば同じように使える。
 * 使わせないための札ではなく、数字を読み違えやすいことを先に言うための札。
 */
@Composable
fun LevelChip(
    level: ToolLevel,
    modifier: Modifier = Modifier,
) {
    when (level) {
        // 前提知識が要らないものは既定なので出さない
        ToolLevel.BASIC -> Unit

        ToolLevel.INTERMEDIATE -> PaPill(
            text = stringResource(R.string.badge_level_intermediate),
            tone = PaTone.NEUTRAL,
            modifier = modifier,
        )

        ToolLevel.ADVANCED -> PaPill(
            text = stringResource(R.string.badge_level_advanced),
            tone = PaTone.INFO,
            modifier = modifier,
        )
    }
}

/** 未実装ツールに付ける「準備中」チップ。 */
@Composable
fun ComingSoonChip(modifier: Modifier = Modifier) {
    PaPill(
        text = stringResource(R.string.coming_soon),
        tone = PaTone.WARNING,
        modifier = modifier,
    )
}

@Composable
fun PaChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
) {
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = contentColor,
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
