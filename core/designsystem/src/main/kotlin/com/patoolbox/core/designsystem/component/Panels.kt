package com.patoolbox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.designsystem.theme.LocalPaThemeMode
import com.patoolbox.core.designsystem.theme.PaBrand
import com.patoolbox.core.designsystem.theme.isDarkAppearance
import com.patoolbox.core.model.ThemeMode

/**
 * 意味の色。
 *
 * 具体的な色ではなく「何を伝えたいか」で指定させる。
 * 暗所モードでは赤以外の光を出せず、屋外モードでは淡い色が飛ぶので、
 * 呼び出し側が色を直接持つとその2つのテーマで必ず壊れる。
 */
enum class PaTone {
    /** 補足・件数などの中立な札 */
    NEUTRAL,

    /** アプリのブランド色。「いま選んでいる」ものに使う */
    BRAND,

    /** 情報・参考値 */
    INFO,

    /** 正常・完了・安全 */
    SUCCESS,

    /** 注意。壊れてはいないが気を付ける */
    WARNING,

    /** 危険・不可逆・エラー */
    DANGER,
}

/**
 * 淡い色を使えるテーマか。
 *
 * 暗所モードは赤以外の色を出せず、屋外モードは淡い色のコントラストが足りない。
 * どちらも無彩色 + 明度差に落とす。
 */
@Composable
private fun restrictedPalette(): Boolean = when (LocalPaThemeMode.current) {
    ThemeMode.NIGHT_RED, ThemeMode.OUTDOOR -> true
    else -> false
}

@Composable
fun PaTone.container(): Color {
    val scheme = MaterialTheme.colorScheme
    if (restrictedPalette()) {
        return when (this) {
            PaTone.DANGER -> scheme.errorContainer
            PaTone.BRAND -> scheme.primaryContainer
            else -> scheme.surfaceContainerHigh
        }
    }
    val dark = LocalPaThemeMode.current.isDarkAppearance()
    return when (this) {
        PaTone.NEUTRAL -> scheme.surfaceContainerHigh
        PaTone.BRAND -> scheme.primaryContainer
        PaTone.INFO -> scheme.secondaryContainer
        PaTone.SUCCESS -> scheme.tertiaryContainer
        PaTone.WARNING -> if (dark) PaBrand.amberTintDark else PaBrand.amberTintLight
        PaTone.DANGER -> scheme.errorContainer
    }
}

@Composable
fun PaTone.content(): Color {
    val scheme = MaterialTheme.colorScheme
    if (restrictedPalette()) {
        return when (this) {
            PaTone.DANGER -> scheme.onErrorContainer
            PaTone.BRAND -> scheme.onPrimaryContainer
            else -> scheme.onSurface
        }
    }
    val dark = LocalPaThemeMode.current.isDarkAppearance()
    return when (this) {
        PaTone.NEUTRAL -> scheme.onSurfaceVariant
        PaTone.BRAND -> scheme.onPrimaryContainer
        PaTone.INFO -> scheme.onSecondaryContainer
        PaTone.SUCCESS -> scheme.onTertiaryContainer
        PaTone.WARNING -> if (dark) Color(0xFFF0C070) else PaBrand.amber
        PaTone.DANGER -> scheme.onErrorContainer
    }
}

/**
 * 状態を示す小さな札（ピル）。
 *
 * 「Pro」「不可逆」「削る」のように、**1〜4文字で状態を言い切る**ものだけに使う。
 * 説明文を入れると横に伸びて、並べたときに読み飛ばせなくなる。
 */
@Composable
fun PaPill(
    text: String,
    modifier: Modifier = Modifier,
    tone: PaTone = PaTone.NEUTRAL,
    leadingDotColor: Color? = null,
) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(dimens.cornerPill))
            .background(tone.container())
            .padding(horizontal = dimens.spaceSm, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingDotColor != null) {
            Box(
                modifier = Modifier
                    .size(dimens.spaceSm)
                    .background(leadingDotColor, CircleShape),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tone.content(),
            maxLines = 1,
        )
    }
}

/**
 * この app のパネル。Cloudflare のダッシュボードのカードに相当する。
 *
 * 見出し行 → 区切り線 → 中身、という並びを固定している。
 * [PaCard] と違って**見出しを省略できない**のは、
 * パネルが増えたときに「何のかたまりか」を必ず読めるようにするため。
 *
 * @param rail 左端に出す識別帯の色。null なら出さない
 * @param trailing 見出し行の右端。件数のピルや操作を置く
 * @param content null なら見出しだけのパネル（一覧の行として使える）
 */
@Composable
fun PaPanel(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    rail: Color? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    contentPadding: Dp = LocalPaDimens.current.space,
    verticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(LocalPaDimens.current.spaceSm),
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val dimens = LocalPaDimens.current
    val shape = RoundedCornerShape(dimens.cardCorner)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(dimens.hairline, MaterialTheme.colorScheme.outlineVariant, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        if (rail != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(dimens.railWidth)
                    .background(rail),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = contentPadding,
                        end = contentPadding,
                        top = dimens.spaceMd,
                        bottom = dimens.spaceMd,
                    ),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
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

            if (content != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(contentPadding),
                    verticalArrangement = verticalArrangement,
                    content = content,
                )
            }
        }
    }
}

/**
 * 「項目名 : 値」の1行。
 *
 * 表の代わりに使う。列幅を固定するので、縦に並べたときに値の位置が揃う。
 * 揃っていないと、10行を上から読み比べることができない。
 */
@Composable
fun PaKeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelWidth: Dp = 88.dp,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable () -> Unit)? = null,
) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(labelWidth),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/**
 * お知らせ・注意の帯。
 *
 * 左端に色帯を出し、地色は淡く敷く。文字色だけで警告を示さないのは、
 * 暗所モードでは色の差が出ないため——帯の位置と見出しで伝える。
 */
@Composable
fun PaNotice(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    tone: PaTone = PaTone.INFO,
) {
    val dimens = LocalPaDimens.current
    val shape = RoundedCornerShape(dimens.cornerSmall)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(tone.container()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(dimens.railWidth)
                .background(tone.content()),
        )
        Column(
            modifier = Modifier.padding(dimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = tone.content(),
            )
            if (body != null) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * 下線タブ。
 *
 * Material の `PrimaryTabRow` を使わないのは、タブが5つ以上になると
 * 均等割りで文字が折り返して読めなくなるため。
 * 横スクロールにして、選択中を下線と文字の太さで示す。
 */
@Composable
fun PaUnderlineTabs(
    titles: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val dimens = LocalPaDimens.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = dimens.spaceSm),
        ) {
            titles.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .clickable { onSelect(index) }
                        .heightIn(min = dimens.minTouch),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        text = title,
                        style = if (selected) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            horizontal = dimens.spaceMd,
                            vertical = dimens.spaceMd,
                        ),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (selected) dimens.railWidth else 0.dp)
                            .background(if (selected) accent else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * 絞り込みの札。
 *
 * 選択中を面で塗り、それ以外は枠線だけにする。
 * Material の `FilterChip` より詰めて置けるので、分類が8個あっても2行で収まる。
 */
@Composable
fun PaFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val dimens = LocalPaDimens.current
    val shape = RoundedCornerShape(dimens.cornerSmall)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) accent else MaterialTheme.colorScheme.surfaceContainer,
            )
            .border(
                dimens.hairline,
                if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = dimens.spaceMd, vertical = dimens.spaceSm),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                contrastingInk(accent)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

/**
 * 色面に乗せる文字色。
 *
 * **明度のしきい値ではなくコントラスト比で選ぶ。**
 * 「明るければ黒、暗ければ白」で分けると、ブランドの橙（#F6821F）のように
 * 相対輝度が 0.5 を下回るのに白では読めない色（コントラスト比 2.6）を取りこぼす。
 * 白と黒それぞれの比を出して大きい方を採ると、橙では黒（8.1）が選ばれる。
 */
fun contrastingInk(background: Color): Color {
    val luminance = background.luminance()
    val againstWhite = 1.05f / (luminance + 0.05f)
    val againstBlack = (luminance + 0.05f) / 0.05f
    return if (againstBlack >= againstWhite) Color.Black else Color.White
}
