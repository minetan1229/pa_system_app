package com.patoolbox.feature.business

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens

/**
 * バックアップと復元。
 *
 * 自動のクラウド同期は入れていない。書き出したファイルを利用者が
 * 自分のクラウドに置く形にすることで、「音声を含むデータを端末外に送らない」
 * という性質を保ったまま、端末故障への備えという目的を満たせる。
 *
 * 復元は**現在のデータを完全に置き換える**ので、確認を挟む。
 */
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    val context = LocalContext.current
    var confirmRestore by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { viewModel.export(it) }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> confirmRestore = uri }

    BusinessScaffold(
        title = stringResource(R.string.backup_title),
        onBack = onBack,
        proStatus = uiState.proStatus,
        modifier = modifier,
    ) { contentModifier ->
        Column(
            modifier = contentModifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
        ) {
            Text(
                text = stringResource(R.string.backup_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.backup_excluded_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    for (item in uiState.excluded) {
                        Text(
                            text = "・$item",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Button(
                onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
                enabled = !uiState.isWorking,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
            ) {
                Text(stringResource(R.string.backup_export))
            }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                enabled = !uiState.isWorking,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
            ) {
                Text(stringResource(R.string.backup_restore))
            }

            Text(
                text = stringResource(R.string.backup_restore_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            uiState.message?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = dimens.gutter),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.restoreSucceeded) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ),
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }

    confirmRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            title = { Text(stringResource(R.string.backup_confirm_title)) },
            text = { Text(stringResource(R.string.backup_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRestore = null
                        context.contentResolver.openInputStream(uri)
                            ?.let { viewModel.restore(it) }
                    },
                ) {
                    Text(
                        text = stringResource(R.string.backup_confirm_ok),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = null }) {
                    Text(stringResource(R.string.business_cancel))
                }
            },
        )
    }
}

private const val BACKUP_MIME_TYPE = "application/octet-stream"
