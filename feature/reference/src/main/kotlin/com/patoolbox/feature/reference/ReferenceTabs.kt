package com.patoolbox.feature.reference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.patoolbox.core.reference.Connector
import com.patoolbox.core.reference.ConnectorCategory
import com.patoolbox.core.reference.Connectors
import com.patoolbox.core.reference.FrequencyChart
import com.patoolbox.core.reference.Glossary
import com.patoolbox.core.reference.InstrumentBands
import com.patoolbox.core.reference.TroubleshootFlow
import com.patoolbox.core.reference.TroubleshootQuestion
import com.patoolbox.core.reference.TroubleshootResolution
import com.patoolbox.core.reference.Troubleshooting

@Composable
internal fun ConnectorTab(gutter: Dp) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(gutter),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ConnectorCategory.entries.forEach { category ->
            item(key = "cat_${category.name}") {
                SectionHeader(category.label)
            }
            items(Connectors.byCategory(category), key = { it.name }) { connector ->
                ConnectorCard(connector)
            }
        }
    }
}

@Composable
private fun ConnectorCard(connector: Connector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = connector.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = connector.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            connector.pins.forEach { pin ->
                Row {
                    Text(
                        text = pin.pin,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(72.dp),
                    )
                    Text(
                        text = pin.signal,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            if (connector.cautions.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                Text(
                    text = stringResource(R.string.reference_caution),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                connector.cautions.forEach { caution ->
                    Text(
                        text = "・$caution",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun FrequencyTab(gutter: Dp) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(gutter),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(FrequencyChart.ALL, key = { it.instrument }) { instrument ->
            InstrumentCard(instrument)
        }
    }
}

@Composable
private fun InstrumentCard(instrument: InstrumentBands) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = instrument.instrument,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.reference_fundamental,
                    formatHz(instrument.fundamentalFromHz),
                    formatHz(instrument.fundamentalToHz),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            instrument.tips.forEach { tip ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(
                            R.string.reference_band_range,
                            formatHz(tip.fromHz),
                            formatHz(tip.toHz),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(112.dp),
                    )
                    Column {
                        Text(
                            text = tip.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = tip.effect,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
    var flow by remember { mutableStateOf<TroubleshootFlow?>(null) }
    val history = remember { mutableStateListOf<String>() }

    val current = flow
    if (current == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(gutter),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionHeader(stringResource(R.string.reference_select_flow)) }
            items(Troubleshooting.ALL, key = { it.title }) { candidate ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = candidate.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = candidate.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = {
                                flow = candidate
                                history.clear()
                                history += candidate.startId
                            },
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .heightIn(min = minTouch),
                        ) { Text(candidate.title) }
                    }
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Text(
                    text = stringResource(R.string.reference_cause),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = step.cause,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.reference_actions),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                step.actions.forEach { action ->
                    Text(
                        text = "・$action",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            null -> Unit
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
internal fun GlossaryTab(gutter: Dp) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { Glossary.search(query) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.reference_search)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = gutter, vertical = 8.dp),
        )
        Text(
            text = stringResource(R.string.reference_term_count, results.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = gutter),
        )

        if (results.isEmpty()) {
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = gutter,
                end = gutter,
                bottom = gutter,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(results, key = { it.term }) { term ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = term.term,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = term.english,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = term.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

private fun formatHz(hz: Double): String = when {
    hz >= 1000 -> "%.1fk".format(hz / 1000)
    else -> "%.0f".format(hz)
}
