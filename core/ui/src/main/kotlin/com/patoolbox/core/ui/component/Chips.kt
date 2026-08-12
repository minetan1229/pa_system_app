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
import com.patoolbox.core.model.ToolAccess
import com.patoolbox.core.ui.R

/** Pro 専用 / 無料（制限あり）を示す小さなチップ。 */
@Composable
fun AccessChip(
    access: ToolAccess,
    modifier: Modifier = Modifier,
) {
    when (access) {
        ToolAccess.PRO -> PaChip(
            text = stringResource(R.string.badge_pro),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = modifier,
        )

        ToolAccess.FREE_LIMITED -> PaChip(
            text = stringResource(R.string.badge_free_limited),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            modifier = modifier,
        )

        // 無料は既定なのでバッジを出さない（画面のノイズを減らす）
        ToolAccess.FREE -> Unit
    }
}

/** 未実装ツールに付ける「準備中」チップ。 */
@Composable
fun ComingSoonChip(modifier: Modifier = Modifier) {
    PaChip(
        text = stringResource(R.string.coming_soon),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
