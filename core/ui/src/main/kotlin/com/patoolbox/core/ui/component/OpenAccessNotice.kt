package com.patoolbox.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.patoolbox.core.designsystem.component.PaCard
import com.patoolbox.core.designsystem.component.PaIllustration
import com.patoolbox.core.designsystem.component.PaScene
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.ui.R

/**
 * 「札は目印であってロックではない」ことを言う帯。
 *
 * ツールカードには「上級」「中級」「PRO」の札が付く。**どれも押せなくはしていない**が、
 * 札が付いているだけで「自分にはまだ開けないもの」だと読まれてしまう。
 * 開いた錠前の絵をいちばん左に置いているのは、文章を読む前に
 * 「閉じていない」ことだけ先に伝わるようにするため。
 *
 * @param proUnlocked PRO の道具もいま開けるか。先行版のあいだは true。
 *   ここを状態から渡しているのは、Phase 5 で課金が入ったあとに
 *   「PRO も開きます」と書いたままの帯が残らないようにするため
 */
@Composable
fun OpenAccessNotice(
    proUnlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current

    PaCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = dimens.spaceMd,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PaIllustration(
                scene = PaScene.OPEN_ACCESS,
                contentDescription = stringResource(R.string.open_access_image),
                modifier = Modifier.size(width = 64.dp, height = 44.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
            ) {
                Text(
                    text = stringResource(R.string.open_access_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.open_access_body_level),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (proUnlocked) {
                    Text(
                        text = stringResource(R.string.open_access_body_pro),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
