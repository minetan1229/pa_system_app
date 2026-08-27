package com.patoolbox.feature.reference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patoolbox.core.designsystem.component.PaNotice
import com.patoolbox.core.designsystem.component.PaPanel
import com.patoolbox.core.designsystem.component.PaPill
import com.patoolbox.core.designsystem.component.PaSectionHeader
import com.patoolbox.core.designsystem.component.PaTone
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.reference.Connector
import com.patoolbox.core.reference.ConnectorCategory
import com.patoolbox.core.reference.Connectors
import com.patoolbox.core.reference.Glossary
import com.patoolbox.core.reference.GlossaryCategory
import com.patoolbox.core.reference.GlossaryTerm
import com.patoolbox.core.reference.TroubleshootFlow
import com.patoolbox.core.reference.TroubleshootQuestion
import com.patoolbox.core.reference.TroubleshootResolution
import com.patoolbox.core.reference.Troubleshooting

/**
 * コネクタ図鑑。
 *
 * 検索していないときはカテゴリで畳んで出す。中身が増えたので、
 * 全部を縦に並べると目的のものに辿り着けない。
 * 検索したときはカテゴリを跨いで重要度順のまま出す
 * （「ハウリング」のように、どのカテゴリにあるか分からない引き方が多いため）。
 */
@Composable
internal fun ConnectorTab(gutter: Dp) {
    val dimens = LocalPaDimens.current
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { Connectors.search(query) }
    val searching = query.isNotBlank()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.reference_connector_search)) },
            placeholder = { Text(stringResource(R.string.reference_connector_search_hint)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = gutter, vertical = dimens.spaceSm),
        )

        if (searching) {
            Text(
                text = stringResource(R.string.reference_connector_count, results.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = gutter),
            )
        }

        if (searching && results.isEmpty()) {
            Text(
                text = stringResource(R.string.reference_no_result),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(gutter),
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = gutter,
                end = gutter,
                bottom = dimens.spaceXl,
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            if (searching) {
                items(results, key = { it.name }) { connector ->
                    ConnectorCard(connector, showCategory = true)
                }
                return@LazyColumn
            }

            ConnectorCategory.entries.forEach { category ->
                item(key = "cat_${category.name}") {
                    PaSectionHeader(
                        title = category.label,
                        subtitle = category.description,
                        modifier = Modifier.padding(top = dimens.spaceSm),
                    )
                }
                items(Connectors.byCategory(category), key = { it.name }) { connector ->
                    ConnectorCard(connector, showCategory = false)
                }
            }
        }
    }
}

/**
 * コネクタ1枚。
 *
 * 上級者向けの補足は畳んでおく。全部開いていると
 * 「XLR のピン番号を確かめたいだけ」のときに邪魔になるが、
 * 無いと Dante のような込み入ったものに手が届かない。
 */
@Composable
private fun ConnectorCard(connector: Connector, showCategory: Boolean) {
    val dimens = LocalPaDimens.current
    var expanded by rememberSaveable(connector.name) { mutableStateOf(false) }

    PaPanel(
        title = connector.name,
        subtitle = connector.summary,
        trailing = if (showCategory) {
            { PaPill(text = connector.category.label) }
        } else {
            null
        },
    ) {
        connector.pins.forEach { pin ->
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                Text(
                    text = pin.pin,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.width(PIN_COLUMN_WIDTH),
                )
                Text(
                    text = pin.signal,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (connector.cautions.isNotEmpty()) {
            PaNotice(
                title = stringResource(R.string.reference_caution),
                body = connector.cautions.joinToString("\n") { "・$it" },
                tone = PaTone.DANGER,
            )
        }

        if (connector.advanced.isEmpty()) return@PaPanel

        TextButton(onClick = { expanded = !expanded }) {
            Text(
                stringResource(
                    if (expanded) {
                        R.string.reference_advanced_hide
                    } else {
                        R.string.reference_advanced_show
                    },
                ),
            )
        }
        if (expanded) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            connector.advanced.forEach { note ->
                Text(
                    text = "・$note",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * 切り分けの分岐。
 * 履歴を持って1つ戻れるようにしている。押し間違えて最初からやり直すのは現場で辛い。
 */
@Composable
internal fun TroubleshootTab(gutter: Dp, minTouch: Dp) {
    val dimens = LocalPaDimens.current
    var flow by remember { mutableStateOf<TroubleshootFlow?>(null) }
    val history = remember { mutableStateListOf<String>() }

    val current = flow
    if (current == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(gutter),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            item {
                PaSectionHeader(title = stringResource(R.string.reference_select_flow))
            }
            items(Troubleshooting.ALL, key = { it.title }) { candidate ->
                PaPanel(
                    title = candidate.title,
                    subtitle = candidate.summary,
                    onClick = {
                        flow = candidate
                        history.clear()
                        history += candidate.startId
                    },
                ) {
                    Button(
                        onClick = {
                            flow = candidate
                            history.clear()
                            history += candidate.startId
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = minTouch),
                    ) { Text(candidate.title) }
                }
            }
        }
        return
    }

    val stepId = history.lastOrNull() ?: current.startId
    val step = current.step(stepId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(gutter),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        Text(
            text = current.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (step) {
            is TroubleshootQuestion -> {
                Text(
                    text = step.text,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                    Button(
                        onClick = { history += step.yesId },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = minTouch * 1.4f),
                    ) { Text(stringResource(R.string.reference_yes)) }
                    OutlinedButton(
                        onClick = { history += step.noId },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = minTouch * 1.4f),
                    ) { Text(stringResource(R.string.reference_no)) }
                }
            }

            is TroubleshootResolution -> {
                PaPanel(
                    title = step.cause,
                    subtitle = stringResource(R.string.reference_cause),
                    trailing = {
                        PaPill(
                            text = stringResource(R.string.reference_actions),
                            tone = PaTone.SUCCESS,
                        )
                    },
                ) {
                    step.actions.forEach { action ->
                        Text(
                            text = "・$action",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            null -> Unit
        }

        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            TextButton(
                onClick = { if (history.size > 1) history.removeAt(history.lastIndex) },
                enabled = history.size > 1,
            ) { Text(stringResource(R.string.reference_undo)) }
            TextButton(
                onClick = {
                    flow = null
                    history.clear()
                },
            ) { Text(stringResource(R.string.reference_restart)) }
        }
    }
}

/**
 * PA用語辞典。
 *
 * 検索していないときはカテゴリで畳んで出す（[ConnectorTab] と同じ構成）。
 * 検索したときはカテゴリを跨いで一覧のまま出す——「ハウリング」のように、
 * どのカテゴリにあるか分からない引き方が多いため。
 */
@Composable
internal fun GlossaryTab(gutter: Dp) {
    val dimens = LocalPaDimens.current
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { Glossary.search(query) }
    val searching = query.isNotBlank()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.reference_search)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = gutter, vertical = dimens.spaceSm),
        )

        if (searching) {
            Text(
                text = stringResource(R.string.reference_term_count, results.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = gutter),
            )
        }

        if (searching && results.isEmpty()) {
            Text(
                text = stringResource(R.string.reference_no_result),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(gutter),
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = gutter,
                end = gutter,
                bottom = dimens.spaceXl,
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            if (searching) {
                items(results, key = { it.term }) { term ->
                    GlossaryTermCard(term, showCategory = true)
                }
                return@LazyColumn
            }

            GlossaryCategory.entries.forEach { category ->
                item(key = "cat_${category.name}") {
                    PaSectionHeader(
                        title = category.label,
                        modifier = Modifier.padding(top = dimens.spaceSm),
                    )
                }
                items(Glossary.byCategory(category), key = { it.term }) { term ->
                    GlossaryTermCard(term, showCategory = false)
                }
            }
        }
    }
}

@Composable
private fun GlossaryTermCard(term: GlossaryTerm, showCategory: Boolean) {
    PaPanel(
        title = term.term,
        subtitle = term.english,
        trailing = if (showCategory) {
            { PaPill(text = term.category.label) }
        } else {
            null
        },
    ) {
        Text(
            text = term.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** XLR の「1」から Dante の「RJ45 / etherCON」まで収まる幅 */
private val PIN_COLUMN_WIDTH = 104.dp
