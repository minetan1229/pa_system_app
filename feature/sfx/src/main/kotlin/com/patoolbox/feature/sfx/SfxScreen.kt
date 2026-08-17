package com.patoolbox.feature.sfx

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.PaCard
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.SoundCue
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.PaToolScaffold

/**
 * SE パッド。
 *
 * 押した瞬間に鳴ることだけを優先した画面にしてある。本番中に見るのは
 * 「どれを押すか」だけなので、設定はすべて長押しの奥に隠し、表には
 * パッドと停止しか置いていない。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SfxScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SfxViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current

    // 音声ファイルだけを出す。標準の OpenDocument は type を */* にしてしまい、
    // 端末によっては全ファイルが並ぶ（PickAudioDocument のコメント参照）
    val pickLauncher = rememberLauncherForActivityResult(
        PickAudioDocument(),
    ) { uri -> uri?.let(viewModel::import) }

    // 鳴っている間は画面を消さない。消えた画面を起こしてから止めるのでは間に合わない
    KeepScreenOn(enabled = uiState.isAnyPlaying)

    PaToolScaffold(
        tool = ToolId.SFX_PADS,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.sfx_title),
        actions = {
            if (uiState.isAnyPlaying) {
                TextButton(onClick = viewModel::stopAll) {
                    Text(
                        text = stringResource(R.string.sfx_stop_all),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = dimens.gutter,
                end = dimens.gutter,
                top = dimens.spaceSm,
                bottom = dimens.spaceXl,
            ),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        ) {
            uiState.error?.let { error ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ErrorCard(message = error, onDismiss = viewModel::dismissError)
                }
            }

            if (uiState.cues.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState()
                }
            }

            items(uiState.cues, key = { it.id }) { cue ->
                SoundPad(
                    cue = cue,
                    isPlaying = cue.id in uiState.playingIds,
                    onTrigger = { viewModel.trigger(cue) },
                    onEdit = { viewModel.edit(cue) },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier.padding(top = dimens.spaceSm),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
                ) {
                    Button(
                        onClick = { pickLauncher.launch(Unit) },
                        enabled = uiState.canAddMore && !uiState.importing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = dimens.minTouch),
                    ) {
                        Text(
                            stringResource(
                                if (uiState.importing) R.string.sfx_importing else R.string.sfx_add,
                            ),
                        )
                    }
                    Text(
                        text = if (uiState.canAddMore) {
                            stringResource(R.string.sfx_add_note)
                        } else {
                            stringResource(R.string.sfx_free_limit, SoundCue.FREE_LIMIT)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.canAddMore) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }
        }
    }

    uiState.editing?.let { cue ->
        PadEditorSheet(
            cue = cue,
            onDismiss = viewModel::dismissEdit,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
        )
    }
}

/**
 * パッド1枚。
 *
 * 押している間だけわずかに縮む。触ったのに鳴らなかったのか、
 * 触れていなかったのかを、音を聞く前に手で分かるようにするため。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundPad(
    cue: SoundCue,
    isPlaying: Boolean,
    onTrigger: () -> Unit,
    onEdit: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    val accent = padColor(cue.colorIndex)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "padScale",
    )
    val container by animateColorAsState(
        targetValue = if (isPlaying) accent else MaterialTheme.colorScheme.surfaceContainer,
        label = "padColor",
    )

    // 鳴っているときは色面の上に文字が乗るので、面の明るさから文字色を決める。
    // 固定の白や黒にすると、6色のうち必ずどれかで読めなくなる
    val content = if (isPlaying) {
        if (accent.luminance() > 0.5f) Color.Black else Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val shape = RoundedCornerShape(dimens.cornerLarge)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(shape)
            .background(container)
            .border(
                width = if (isPlaying) 0.dp else dimens.hairline,
                color = if (isPlaying) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTrigger,
                onLongClick = onEdit,
            ),
    ) {
        // 停まっているときの色の手がかり。左上に小さく置く
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .padding(dimens.space)
                    .size(dimens.spaceMd)
                    .clip(CircleShape)
                    .background(accent),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(dimens.space),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs / 2),
        ) {
            Text(
                text = cue.title,
                style = MaterialTheme.typography.titleMedium,
                color = content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(cue.durationLabel)
                    if (cue.loop) append("  ↻")
                },
                style = MaterialTheme.typography.labelMedium,
                color = content.copy(alpha = 0.75f),
            )
        }

        if (isPlaying) {
            Text(
                text = stringResource(R.string.sfx_playing),
                style = MaterialTheme.typography.labelSmall,
                color = content,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(dimens.space),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    val dimens = LocalPaDimens.current
    PaCard(modifier = Modifier.fillMaxWidth(), contentPadding = dimens.spaceLg) {
        Text(
            text = stringResource(R.string.sfx_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.sfx_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    PaCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        borderColor = MaterialTheme.colorScheme.error,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text(
                text = stringResource(R.string.sfx_dismiss),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

/**
 * パッドの設定。長押しで開く。
 *
 * 本番中に触るものではないので、表に出さずここにまとめてある。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PadEditorSheet(
    cue: SoundCue,
    onDismiss: () -> Unit,
    onSave: (SoundCue) -> Unit,
    onDelete: (SoundCue) -> Unit,
) {
    val dimens = LocalPaDimens.current
    val sheetState = rememberModalBottomSheetState()
    var draft by remember(cue.id) { mutableStateOf(cue) }
    var confirmingDelete by remember(cue.id) { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.gutter)
                .padding(bottom = dimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(dimens.space),
        ) {
            Text(
                text = stringResource(R.string.sfx_edit_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = draft.title,
                onValueChange = { draft = draft.copy(title = it) },
                label = { Text(stringResource(R.string.sfx_edit_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                Text(
                    text = stringResource(R.string.sfx_edit_gain, (draft.gain * 100).toInt()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = draft.gain,
                    onValueChange = { draft = draft.copy(gain = it) },
                    valueRange = 0f..1f,
                )
                Text(
                    text = stringResource(R.string.sfx_edit_gain_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sfx_edit_loop),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked = draft.loop,
                    onCheckedChange = { draft = draft.copy(loop = it) },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                Text(
                    text = stringResource(R.string.sfx_edit_color),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd)) {
                    for (index in 0 until SoundCue.COLOR_COUNT) {
                        val color = padColor(index)
                        Box(
                            modifier = Modifier
                                .size(dimens.minTouch)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (draft.colorIndex == index) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .combinedClickable(
                                    onClick = { draft = draft.copy(colorIndex = index) },
                                ),
                        )
                    }
                }
            }

            Button(
                onClick = { onSave(draft) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = dimens.minTouch),
            ) {
                Text(stringResource(R.string.sfx_edit_save))
            }

            // 削除は取り返しがつかない（ファイルごと消える）ので二段階にする
            if (confirmingDelete) {
                Text(
                    text = stringResource(R.string.sfx_delete_confirm, cue.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                    OutlinedButton(
                        onClick = { confirmingDelete = false },
                        modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch),
                    ) {
                        Text(stringResource(R.string.sfx_delete_cancel))
                    }
                    Button(
                        onClick = { onDelete(cue) },
                        modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch),
                    ) {
                        Text(stringResource(R.string.sfx_delete_do))
                    }
                }
            } else {
                TextButton(
                    onClick = { confirmingDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.sfx_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

