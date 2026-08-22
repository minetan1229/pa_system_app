package com.patoolbox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import com.patoolbox.core.designsystem.theme.LocalPaDimens

/**
 * この app の面（カード）の基本形。
 *
 * Material の `Card` を直に使わず1枚挟んでいるのは、影ではなく
 * 「わずかな明度差 + 髪の毛一本の枠線」で面を分ける作りを全画面で揃えるため。
 * 影は暗所モードでは見えず、屋外モードでは飛ぶので、4つのテーマすべてで
 * 成立するのは線で分ける方だけになる。
 */
@Composable
fun PaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    corner: Dp = LocalPaDimens.current.cardCorner,
    contentPadding: Dp = LocalPaDimens.current.space,
    verticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(LocalPaDimens.current.spaceSm),
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = LocalPaDimens.current
    val shape = RoundedCornerShape(corner)

    Column(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .border(dimens.hairline, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * 節の見出し。
 *
 * 「見出し + 補足」を必ず対にする。項目名だけ置くと、
 * 初めて開いた人がその節に何が入っているのか読み取れない。
 */
@Composable
fun PaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceXs / 2)) {
            Text(
                // 見出しは大きさではなく太さと余白で作る。
                // 情報量の多い画面で見出しだけ大きいと、目が見出しに引っ張られて
                // 中身が読まれなくなる（Cloudflare のダッシュボードと同じ考え方）
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * 小さな数値の札。「いま何がどうなっているか」を一列に並べるのに使う。
 * 値を大きく、ラベルを小さくして、離れた場所からは値だけが読めるようにしてある。
 */
@Composable
fun PaStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            // 値は等幅。桁が変わるたびに横位置が動くと、
            // 並べた札を上から読み比べられない
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace,
            ),
            color = valueColor,
        )
    }
}
