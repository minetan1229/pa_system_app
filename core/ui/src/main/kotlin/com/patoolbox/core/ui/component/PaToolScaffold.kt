package com.patoolbox.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.reference.HelpTopics
import com.patoolbox.core.ui.R
import com.patoolbox.core.ui.emitsSound
import com.patoolbox.core.ui.identityColor
import com.patoolbox.core.ui.titleRes

/**
 * ツール画面の共通の枠。
 *
 * 上から「戻る・画面名・解説」の帯、その下に識別帯（色・バッジ・1行の役割）が入る。
 * 計測画面はどれも「図があってボタンがある」構成になるため、
 * 中身だけでは見分けがつかない。開いた瞬間に色とバッジと1行で
 * 「いま何の道具を持っているか」が分かるようにするのがこの枠の仕事。
 *
 * @param tool 画面に対応するツール。色・バッジ・解説はここから引く
 * @param actions 画面固有の操作。解説ボタンの左に並ぶ
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaToolScaffold(
    tool: ToolId,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(tool.titleRes),
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val accent = tool.identityColor()
    val topic = HelpTopics.forTool(tool)
    val roleLine = subtitle ?: topic?.summary

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Text(stringResource(R.string.back))
                        }
                    },
                    actions = {
                        actions()
                        HelpAction(topic = topic)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
                ToolIdentityBar(tool = tool, accent = accent, roleLine = roleLine)
            }
        },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

/**
 * 識別帯。
 *
 * 左端の色帯とバッジで「どの道具か」、右の1行で「何をする道具か」を出す。
 * 音が出る画面には赤い札を必ず付ける。PA に繋いだまま
 * 不用意に測定を始めるのを、押す前に止めるための表示。
 */
@Composable
fun ToolIdentityBar(
    tool: ToolId,
    accent: Color,
    roleLine: String?,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = dimens.gutter, vertical = dimens.spaceSm),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 色の面に文字を置くので、面の明るさから文字色を決める。
            // 白か黒に固定すると、13色のうち必ずどれかで読めなくなる
            Box(
                modifier = Modifier
                    .size(dimens.badgeSize)
                    .clip(RoundedCornerShape(dimens.cornerSmall))
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tool.badge,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (accent.luminance() > 0.5f) Color.Black else Color.White,
                    maxLines = 1,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                if (roleLine != null) {
                    Text(
                        text = roleLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (tool.emitsSound) {
                    Text(
                        text = stringResource(R.string.tool_emits_sound),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // 色帯は画面の一番上ではなく識別帯の下端に置く。
        // 上に置くとステータスバーと混ざって、どの画面でも同じに見える
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(accent),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * 図の上に置く小さな凡例。
 *
 * 「この線は何か」を図の中に書き込むと図が汚れるので、すぐ上に1行で出す。
 */
@Composable
fun ChartLegend(
    entries: List<Pair<String, Color>>,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        entries.forEach { (label, color) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(dimens.spaceMd)
                        .height(dimens.spaceXs / 2 + 2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(color),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
