package com.patoolbox.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.patoolbox.core.designsystem.component.PaCard
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.reference.HelpTopic
import com.patoolbox.core.reference.HelpTopics
import com.patoolbox.core.ui.R

/**
 * 画面の解説を開くボタン。
 *
 * どの画面でも同じ場所（右上）に、同じ言葉で置く。
 * 「困ったらここ」が画面ごとに動くと、結局どこにも無いのと同じになる。
 */
@Composable
fun HelpAction(
    topic: HelpTopic?,
    modifier: Modifier = Modifier,
) {
    if (topic == null) return
    var open by rememberSaveable { mutableStateOf(false) }

    TextButton(onClick = { open = true }, modifier = modifier) {
        Text(stringResource(R.string.help_open))
    }

    if (open) {
        HelpSheet(topic = topic, onDismiss = { open = false })
    }
}

/**
 * 解説のボトムシート。
 *
 * 本文は節ごとに分けて、見出しだけ拾い読みできるようにしてある。
 * 下端に検索を置いていて、その画面以外の解説へも移れる
 * （「dB とは」のように、いま開いている画面とは別の話を知りたくなることが多いため）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSheet(
    topic: HelpTopic,
    onDismiss: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // シートの中で別の解説へ移れる。戻る先は最初に開いた画面の解説
    var shown by remember(topic.id) { mutableStateOf(topic) }
    var query by rememberSaveable(topic.id) { mutableStateOf("") }

    val results = remember(query) {
        if (query.isBlank()) emptyList() else HelpTopics.search(query)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = dimens.gutter,
                end = dimens.gutter,
                bottom = dimens.spaceXl,
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.space),
        ) {
            item(key = "head") {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
                    Text(
                        text = shown.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = shown.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(shown.sections, key = { "${shown.id}_${it.heading}" }) { section ->
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
                    Text(
                        text = section.heading,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = section.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            item(key = "search") {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.help_search_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.help_search_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (query.isNotBlank() && results.isEmpty()) {
                        Text(
                            text = stringResource(R.string.help_no_result),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(results, key = { "hit_${it.id}" }) { hit ->
                PaCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        shown = hit
                        query = ""
                    },
                ) {
                    Text(
                        text = hit.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = hit.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item(key = "close") {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimens.spaceSm),
                ) {
                    Text(stringResource(R.string.help_close))
                }
            }
        }
    }
}
