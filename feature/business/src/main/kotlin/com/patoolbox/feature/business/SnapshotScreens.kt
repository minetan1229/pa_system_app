package com.patoolbox.feature.business

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ProStatus
import com.patoolbox.core.model.Snapshot
import com.patoolbox.core.model.SnapshotChannel
import com.patoolbox.core.ui.DateTimeText

/**
 * スナップショット（卓の設定控え）の一覧。
 *
 * 卓の内部データは機種ごとに形式が違い、外から読める保証もない。
 * ここで残すのは **人が読める記録** で、翌日の同じ現場や別の卓でも
 * 追い込みの出発点として使える。機種固有のファイルを扱おうとすると、
 * 対応卓の外では何の役にも立たなくなる。
 */
@Composable
fun SnapshotListScreen(
    onOpen: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SnapshotListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    var showCreate by rememberSaveable { mutableStateOf(false) }

    BusinessScaffold(
        title = stringResource(R.string.snapshot_title),
        onBack = onBack,
        proStatus = uiState.proStatus,
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                text = { Text(stringResource(R.string.snapshot_add)) },
                icon = {},
            )
        },
    ) { contentModifier ->
        Column(modifier = contentModifier) {
            Text(
                text = stringResource(R.string.snapshot_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = dimens.gutterSmall),
            )

            if (uiState.snapshots.isEmpty()) {
                Text(
                    text = stringResource(R.string.snapshot_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
                modifier = Modifier.padding(bottom = dimens.gutter),
            ) {
                items(uiState.snapshots, key = { it.id }) { snapshot ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(snapshot.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = snapshot.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = listOfNotNull(
                                        snapshot.consoleName.takeIf { it.isNotBlank() },
                                        DateTimeText.formatDateTime(snapshot.takenAtEpochMs),
                                    ).joinToString(" ／ "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { viewModel.delete(snapshot) }) {
                                Text(
                                    text = stringResource(R.string.business_delete),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateSnapshotDialog(
            onDismiss = { showCreate = false },
            onCreate = { title, console, channels ->
                showCreate = false
                viewModel.create(title, console, channels, onOpen)
            },
        )
    }
}

@Composable
private fun CreateSnapshotDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Int) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var console by rememberSaveable { mutableStateOf("") }
    var channels by rememberSaveable {
        mutableStateOf(SnapshotListViewModel.DEFAULT_CHANNELS.toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.snapshot_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.snapshot_name),
                )
                TextField(
                    value = console,
                    onValueChange = { console = it },
                    label = stringResource(R.string.snapshot_console),
                )
                TextField(
                    value = channels,
                    onValueChange = { channels = it },
                    label = stringResource(R.string.snapshot_channels),
                    numeric = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        title,
                        console,
                        channels.toIntOrNull() ?: SnapshotListViewModel.DEFAULT_CHANNELS,
                    )
                },
            ) {
                Text(stringResource(R.string.business_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.business_cancel))
            }
        },
    )
}

/** スナップショットの中身。チャンネルごとに自由記述で控える。 */
@Composable
fun SnapshotDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SnapshotDetailViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    var expanded by remember { mutableStateOf<Long?>(null) }

    BusinessScaffold(
        title = snapshot?.title ?: stringResource(R.string.snapshot_title),
        onBack = onBack,
        // 一覧で Pro を確認済み。ここで再度弾くと編集途中に閉じてしまう
        proStatus = ProStatus(isPro = true, source = com.patoolbox.core.model.ProSource.NONE),
        modifier = modifier,
    ) { contentModifier ->
        val current = snapshot ?: return@BusinessScaffold

        Column(modifier = contentModifier) {
            SnapshotHeader(current, viewModel::updateDetails)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
                modifier = Modifier.padding(bottom = dimens.gutter),
            ) {
                items(current.channels, key = { it.id }) { channel ->
                    ChannelCard(
                        channel = channel,
                        expanded = expanded == channel.id,
                        onToggle = {
                            expanded = if (expanded == channel.id) null else channel.id
                        },
                        onSave = viewModel::saveChannel,
                    )
                }
            }
        }
    }
}

@Composable
private fun SnapshotHeader(
    snapshot: Snapshot,
    onSave: (String, String, String) -> Unit,
) {
    var note by remember(snapshot.id) { mutableStateOf(snapshot.note) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(
                R.string.snapshot_header,
                snapshot.consoleName.ifBlank { "—" },
                DateTimeText.formatDateTime(snapshot.takenAtEpochMs),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextField(
            value = note,
            onValueChange = {
                note = it
                onSave(snapshot.title, snapshot.consoleName, it)
            },
            label = stringResource(R.string.snapshot_overall),
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChannelCard(
    channel: SnapshotChannel,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSave: (SnapshotChannel) -> Unit,
) {
    var draft by remember(channel.id) { mutableStateOf(channel) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "CH ${channel.channel}　${channel.name}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!expanded) {
                if (!channel.isEmpty) {
                    Text(
                        text = listOfNotNull(
                            channel.gain.takeIf { it.isNotBlank() }?.let { "GAIN $it" },
                            channel.hpf.takeIf { it.isNotBlank() }?.let { "HPF $it" },
                            channel.eq.takeIf { it.isNotBlank() }?.let { "EQ $it" },
                        ).joinToString(" ／ "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                TextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it); onSave(draft) },
                    label = stringResource(R.string.snapshot_ch_name),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = draft.gain,
                        onValueChange = { draft = draft.copy(gain = it); onSave(draft) },
                        label = stringResource(R.string.snapshot_ch_gain),
                        modifier = Modifier.weight(1f),
                    )
                    TextField(
                        value = draft.hpf,
                        onValueChange = { draft = draft.copy(hpf = it); onSave(draft) },
                        label = stringResource(R.string.snapshot_ch_hpf),
                        modifier = Modifier.weight(1f),
                    )
                }
                TextField(
                    value = draft.eq,
                    onValueChange = { draft = draft.copy(eq = it); onSave(draft) },
                    label = stringResource(R.string.snapshot_ch_eq),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = draft.send,
                    onValueChange = { draft = draft.copy(send = it); onSave(draft) },
                    label = stringResource(R.string.snapshot_ch_send),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it); onSave(draft) },
                    label = stringResource(R.string.snapshot_ch_note),
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
