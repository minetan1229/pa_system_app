package com.patoolbox.feature.business

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.calc.TaxMode
import com.patoolbox.core.calc.TaxRate
import com.patoolbox.core.calc.TaxRounding
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.Invoice
import com.patoolbox.core.model.InvoiceLineItem
import com.patoolbox.core.model.ProSource
import com.patoolbox.core.model.ProStatus
import com.patoolbox.core.ui.DateTimeText

@Composable
fun InvoiceListScreen(
    onOpen: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvoiceListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    var showCreate by rememberSaveable { mutableStateOf(false) }

    BusinessScaffold(
        title = stringResource(R.string.invoice_title),
        onBack = onBack,
        proStatus = uiState.proStatus,
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                text = { Text(stringResource(R.string.invoice_add)) },
                icon = {},
            )
        },
    ) { contentModifier ->
        Column(modifier = contentModifier) {
            if (uiState.invoices.isEmpty()) {
                Text(
                    text = stringResource(R.string.invoice_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = dimens.gutter),
                )
                return@Column
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
                modifier = Modifier.padding(vertical = dimens.gutterSmall),
            ) {
                items(uiState.invoices, key = { it.id }) { invoice ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(invoice.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${invoice.documentLabel}　${invoice.number}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = listOfNotNull(
                                        invoice.clientName.takeIf { it.isNotBlank() },
                                        DateTimeText.formatDate(invoice.issueDateEpochMs),
                                    ).joinToString(" ／ "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { viewModel.delete(invoice) }) {
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
        var client by remember { mutableStateOf("") }
        var isEstimate by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.invoice_add)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isEstimate,
                            onClick = { isEstimate = false },
                            label = { Text(stringResource(R.string.invoice_kind_invoice)) },
                        )
                        FilterChip(
                            selected = isEstimate,
                            onClick = { isEstimate = true },
                            label = { Text(stringResource(R.string.invoice_kind_estimate)) },
                        )
                    }
                    TextField(
                        value = client,
                        onValueChange = { client = it },
                        label = stringResource(R.string.invoice_client),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCreate = false
                        viewModel.create(isEstimate, client, onOpen)
                    },
                ) {
                    Text(stringResource(R.string.business_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) {
                    Text(stringResource(R.string.business_cancel))
                }
            },
        )
    }
}

/**
 * 請求書・見積書の編集。
 *
 * 合計欄は税率ごとの内訳を必ず出す。適格請求書では
 * 「税率ごとに区分した合計額と、それに対する消費税額」の記載が要る。
 */
@Composable
fun InvoiceDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvoiceDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    val context = LocalContext.current
    var editingLine by remember { mutableStateOf<InvoiceLineItem?>(null) }
    var showHeader by remember { mutableStateOf(false) }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(PDF_MIME_TYPE),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { viewModel.exportPdf(it) }
        }
    }

    BusinessScaffold(
        title = uiState.invoice?.documentLabel ?: stringResource(R.string.invoice_title),
        onBack = onBack,
        proStatus = ProStatus(isPro = true, source = ProSource.NONE),
        modifier = modifier,
        actions = {
            TextButton(onClick = { showHeader = true }) {
                Text(stringResource(R.string.invoice_header))
            }
            TextButton(onClick = { pdfLauncher.launch(viewModel.suggestedFileName()) }) {
                Text(stringResource(R.string.invoice_pdf))
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::addLine,
                text = { Text(stringResource(R.string.invoice_add_line)) },
                icon = {},
            )
        },
    ) { contentModifier ->
        val invoice = uiState.invoice ?: return@BusinessScaffold

        Column(modifier = contentModifier) {
            if (uiState.registrationLooksWrong) {
                Text(
                    text = stringResource(R.string.invoice_registration_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
                modifier = Modifier.weight(1f).padding(vertical = dimens.gutterSmall),
            ) {
                items(invoice.lines, key = { it.id }) { line ->
                    val index = invoice.lines.indexOf(line)
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { editingLine = line },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = line.description.ifBlank {
                                    stringResource(R.string.invoice_untitled_line)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = formatYen(
                                    uiState.totals.lineAmounts.getOrElse(index) { 0 },
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                }
            }

            TotalsCard(uiState)
        }
    }

    editingLine?.let { line ->
        LineDialog(
            line = line,
            onDismiss = { editingLine = null },
            onSave = {
                viewModel.saveLine(it)
                editingLine = null
            },
            onDelete = {
                viewModel.deleteLine(line)
                editingLine = null
            },
        )
    }

    if (showHeader) {
        uiState.invoice?.let { invoice ->
            HeaderDialog(
                invoice = invoice,
                onDismiss = { showHeader = false },
                onSave = {
                    viewModel.save(it)
                    showHeader = false
                },
            )
        }
    }
}

@Composable
private fun TotalsCard(uiState: InvoiceDetailUiState) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 72.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            for (breakdown in uiState.totals.breakdowns) {
                TotalRow(
                    label = stringResource(R.string.invoice_rate_net, breakdown.rate.label),
                    value = formatYen(breakdown.netAmount),
                )
                TotalRow(
                    label = stringResource(R.string.invoice_rate_tax, breakdown.rate.label),
                    value = formatYen(breakdown.taxAmount),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            TotalRow(
                label = stringResource(R.string.invoice_total),
                value = formatYen(uiState.totals.grossTotal),
                emphasis = true,
            )
            Text(
                text = stringResource(
                    R.string.invoice_tax_note,
                    uiState.taxMode.label,
                    uiState.rounding.label,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String, emphasis: Boolean = false) {
    val style = if (emphasis) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.bodyMedium
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun LineDialog(
    line: InvoiceLineItem,
    onDismiss: () -> Unit,
    onSave: (InvoiceLineItem) -> Unit,
    onDelete: () -> Unit,
) {
    var draft by remember(line.id) { mutableStateOf(line) }
    var quantity by remember(line.id) { mutableStateOf(line.quantity.toString()) }
    var price by remember(line.id) { mutableStateOf(line.unitPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.invoice_add_line)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = draft.description,
                    onValueChange = { draft = draft.copy(description = it) },
                    label = stringResource(R.string.invoice_line_name),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = stringResource(R.string.invoice_line_qty),
                        numeric = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextField(
                        value = draft.unit,
                        onValueChange = { draft = draft.copy(unit = it) },
                        label = stringResource(R.string.invoice_line_unit),
                        modifier = Modifier.weight(1f),
                    )
                }
                TextField(
                    value = price,
                    onValueChange = { price = it },
                    label = stringResource(R.string.invoice_line_price),
                    numeric = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (rate in TaxRate.entries) {
                        FilterChip(
                            selected = draft.taxRateName == rate.name,
                            onClick = { draft = draft.copy(taxRateName = rate.name) },
                            label = { Text(rate.label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        draft.copy(
                            quantity = quantity.toDoubleOrNull() ?: 1.0,
                            unitPrice = price.toLongOrNull() ?: 0L,
                        ),
                    )
                },
            ) {
                Text(stringResource(R.string.business_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text(
                        text = stringResource(R.string.business_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.business_cancel))
                }
            }
        },
    )
}

@Composable
private fun HeaderDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onSave: (Invoice) -> Unit,
) {
    var draft by remember(invoice.id) { mutableStateOf(invoice) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.invoice_header)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                TextField(
                    value = draft.number,
                    onValueChange = { draft = draft.copy(number = it) },
                    label = stringResource(R.string.invoice_number),
                )
                TextField(
                    value = draft.clientName,
                    onValueChange = { draft = draft.copy(clientName = it) },
                    label = stringResource(R.string.invoice_client),
                )
                TextField(
                    value = draft.subject,
                    onValueChange = { draft = draft.copy(subject = it) },
                    label = stringResource(R.string.invoice_subject),
                )
                TextField(
                    value = draft.issuerName,
                    onValueChange = { draft = draft.copy(issuerName = it) },
                    label = stringResource(R.string.invoice_issuer),
                )
                TextField(
                    value = draft.registrationNumber,
                    onValueChange = { draft = draft.copy(registrationNumber = it) },
                    label = stringResource(R.string.invoice_registration),
                )

                Text(
                    text = stringResource(R.string.invoice_tax_mode),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (mode in TaxMode.entries) {
                        FilterChip(
                            selected = draft.taxModeName == mode.name,
                            onClick = { draft = draft.copy(taxModeName = mode.name) },
                            label = { Text(mode.label) },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.invoice_rounding),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (rounding in TaxRounding.entries) {
                        FilterChip(
                            selected = draft.taxRoundingName == rounding.name,
                            onClick = { draft = draft.copy(taxRoundingName = rounding.name) },
                            label = { Text(rounding.label) },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.invoice_rounding_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                TextField(
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) },
                    label = stringResource(R.string.invoice_note),
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text(stringResource(R.string.business_save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.business_cancel))
            }
        },
    )
}

private const val PDF_MIME_TYPE = "application/pdf"
