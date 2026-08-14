package com.patoolbox.feature.wireless

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.calc.Intermodulation
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.ui.R as CoreUiR

/**
 * ワイヤレス周波数調整。
 *
 * **法令上の可否は判定しない。** 使ってよい周波数は電波法と免許の条件で決まり、
 * TVホワイトスペースは運用地点ごとに違う。ここで扱うのは
 * 「入力した組み合わせで混変調が起きるか」だけ。
 * 検証できない法令データをアプリに持たせると、それを信じた運用が違法になりうる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirelessScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WirelessViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    var input by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wireless_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(CoreUiR.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (!uiState.proStatus.isPro) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(dimens.gutter),
            ) {
                Text(
                    text = stringResource(R.string.wireless_pro),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.gutter),
            verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
        ) {
            LegalNotice()

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(R.string.wireless_add_label)) },
                    placeholder = { Text("470.425") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        viewModel.addFrequency(input)
                        input = ""
                    },
                    modifier = Modifier.heightIn(min = dimens.minTouch),
                ) {
                    Text(stringResource(R.string.wireless_add))
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            SettingsRow(
                uiState = uiState,
                onGuard = viewModel::setGuard,
                onSpacing = viewModel::setMinSpacing,
                onToggleFifth = viewModel::toggleFifthOrder,
            )

            FrequencyList(
                uiState = uiState,
                onRemove = viewModel::removeFrequency,
                onClear = viewModel::clearAll,
            )

            ReportCard(uiState)

            PlanCard(
                uiState = uiState,
                onFrom = viewModel::setPlanFrom,
                onTo = viewModel::setPlanTo,
                onStep = viewModel::setPlanStep,
                onCount = viewModel::setPlanCount,
                onGenerate = viewModel::generatePlan,
                modifier = Modifier.padding(bottom = dimens.gutter),
            )
        }
    }
}

@Composable
private fun LegalNotice() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.wireless_legal_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(R.string.wireless_legal_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun SettingsRow(
    uiState: WirelessUiState,
    onGuard: (Long) -> Unit,
    onSpacing: (Long) -> Unit,
    onToggleFifth: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.wireless_guard),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (guard in GUARD_OPTIONS) {
                FilterChip(
                    selected = uiState.guardKHz == guard,
                    onClick = { onGuard(guard) },
                    label = { Text("±${guard}k") },
                )
            }
        }
        Text(
            text = stringResource(R.string.wireless_spacing),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (spacing in SPACING_OPTIONS) {
                FilterChip(
                    selected = uiState.minSpacingKHz == spacing,
                    onClick = { onSpacing(spacing) },
                    label = { Text("${spacing}k") },
                )
            }
            FilterChip(
                selected = uiState.includeFifthOrder,
                onClick = { onToggleFifth() },
                label = { Text(stringResource(R.string.wireless_fifth)) },
            )
        }
    }
}

@Composable
private fun FrequencyList(
    uiState: WirelessUiState,
    onRemove: (Long) -> Unit,
    onClear: () -> Unit,
) {
    if (uiState.frequenciesKHz.isEmpty()) {
        Text(
            text = stringResource(R.string.wireless_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val troubled = uiState.report.conflicts.map { it.victimKHz }.toSet()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(
                        R.string.wireless_count,
                        uiState.frequenciesKHz.size,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.wireless_clear))
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            for (khz in uiState.frequenciesKHz) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "%.3f MHz".format(Intermodulation.kHzToMhz(khz)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (khz in troubled) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (khz in troubled) {
                        Text(
                            text = stringResource(R.string.wireless_hit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(onClick = { onRemove(khz) }) {
                        Text(stringResource(R.string.wireless_remove))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(uiState: WirelessUiState) {
    if (uiState.frequenciesKHz.size < 2) return
    val report = uiState.report

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (report.isClean) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (report.isClean) {
                Text(
                    text = stringResource(R.string.wireless_clean),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                return@Card
            }

            Text(
                text = stringResource(R.string.wireless_problems, report.conflicts.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

            for ((khz, score) in report.troubleScores().take(TOP_OFFENDERS)) {
                Text(
                    text = stringResource(
                        R.string.wireless_offender,
                        "%.3f".format(Intermodulation.kHzToMhz(khz)),
                        score,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            for (conflict in report.conflicts.take(MAX_LISTED_CONFLICTS)) {
                Text(
                    text = stringResource(
                        R.string.wireless_conflict,
                        conflict.product.order.label,
                        conflict.product.sources.joinToString(" + ") {
                            "%.3f".format(Intermodulation.kHzToMhz(it))
                        },
                        "%.3f".format(Intermodulation.kHzToMhz(conflict.victimKHz)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            if (report.conflicts.size > MAX_LISTED_CONFLICTS) {
                Text(
                    text = stringResource(
                        R.string.wireless_more,
                        report.conflicts.size - MAX_LISTED_CONFLICTS,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            for ((a, b) in report.spacingViolations) {
                Text(
                    text = stringResource(
                        R.string.wireless_spacing_violation,
                        "%.3f".format(Intermodulation.kHzToMhz(a)),
                        "%.3f".format(Intermodulation.kHzToMhz(b)),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    uiState: WirelessUiState,
    onFrom: (String) -> Unit,
    onTo: (String) -> Unit,
    onStep: (String) -> Unit,
    onCount: (String) -> Unit,
    onGenerate: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.wireless_plan_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.wireless_plan_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    value = uiState.planFromMHz,
                    onValueChange = onFrom,
                    label = stringResource(R.string.wireless_plan_from),
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    value = uiState.planToMHz,
                    onValueChange = onTo,
                    label = stringResource(R.string.wireless_plan_to),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    value = uiState.planStepKHz,
                    onValueChange = onStep,
                    label = stringResource(R.string.wireless_plan_step),
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    value = uiState.planCount,
                    onValueChange = onCount,
                    label = stringResource(R.string.wireless_plan_count),
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onGenerate(false) },
                    enabled = !uiState.isPlanning,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.wireless_plan_new))
                }
                OutlinedButton(
                    onClick = { onGenerate(true) },
                    enabled = !uiState.isPlanning && uiState.frequenciesKHz.isNotEmpty(),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.wireless_plan_add))
                }
            }

            uiState.planShortfall?.let { shortfall ->
                if (shortfall > 0) {
                    Text(
                        text = stringResource(R.string.wireless_plan_shortfall, shortfall),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

private val GUARD_OPTIONS = listOf(25L, 50L, 100L)
private val SPACING_OPTIONS = listOf(125L, 300L, 500L)
private const val TOP_OFFENDERS = 3
private const val MAX_LISTED_CONFLICTS = 8
