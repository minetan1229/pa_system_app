package com.patoolbox.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patoolbox.core.designsystem.component.PaCard
import com.patoolbox.core.designsystem.component.contrastingInk
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.R
import com.patoolbox.core.ui.accentColor
import com.patoolbox.core.ui.descriptionRes
import com.patoolbox.core.ui.titleRes

/**
 * ホームのランチャーに並ぶツール1枚。
 *
 * アイコンではなく文字バッジ（SPL / RTA / Ω など）で識別する。
 * 暗いFOHでも屋外でも読めることを優先した結果で、material-icons-extended（約30MB）を
 * 抱え込まずに済むという副作用もある。★も同じ理由でグリフを直接使っている。
 *
 * バッジは色面で塗る。36枚が並ぶ画面では、文字を読む前に色と位置で当たりを
 * 付けられることの方が速さに効く。
 */
@Composable
fun ToolCard(
    tool: ToolId,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    val accent = tool.category.accentColor()

    PaCard(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = dimens.toolCardMinHeight),
        onClick = onClick,
        corner = dimens.cardCorner,
        contentPadding = dimens.spaceMd,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            ToolBadge(text = tool.badge, accent = accent, size = dimens.badgeSize)
            FavoriteStar(
                isFavorite = isFavorite,
                onClick = onToggleFavorite,
                minTouch = dimens.minTouch,
            )
        }

        Text(
            text = stringResource(tool.titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = stringResource(tool.descriptionRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.alpha(if (tool.implemented) 1f else 0.75f),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
            AccessChip(access = tool.access)
            if (!tool.implemented) {
                ComingSoonChip()
            }
        }
    }
}

@Composable
private fun ToolBadge(
    text: String,
    accent: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(dimens.cornerSmall))
            .background(accent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            // 白か黒かは面の明るさで決める。固定にすると、
            // カテゴリ色のうち明るいもの（現場ドキュメントの橙）で読めなくなる
            color = contrastingInk(accent),
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FavoriteStar(
    isFavorite: Boolean,
    onClick: () -> Unit,
    minTouch: Dp,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        if (isFavorite) R.string.favorite_remove else R.string.favorite_add,
    )
    Box(
        modifier = modifier
            .size(minTouch)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isFavorite) "★" else "☆",
            style = MaterialTheme.typography.titleLarge,
            color = if (isFavorite) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        )
    }
}
