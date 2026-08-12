package com.patoolbox.feature.job

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.Job
import com.patoolbox.core.ui.DateTimeText
import com.patoolbox.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobListScreen(
    onOpenJob: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JobListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.job_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(CoreUiR.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                text = { Text(stringResource(R.string.job_add)) },
                icon = {},
                expanded = true,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = dimens.gutter),
        ) {
            val limit = uiState.saveLimit
            if (limit != null && !uiState.canCreate) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimens.gutterSmall),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.job_limit_reached, limit),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (uiState.jobs.isEmpty()) {
                Text(
                    text = stringResource(R.string.job_empty),
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
                items(uiState.jobs, key = { it.id }) { job ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenJob(job.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = job.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.job_summary,
                                        job.venueName.ifBlank { "—" },
                                        DateTimeText.formatTime(job.loadInAtEpochMs)
                                            .ifBlank { "—" },
                                        DateTimeText.formatTime(job.showAtEpochMs)
                                            .ifBlank { "—" },
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { pendingDelete = job.id }) {
                                Text(stringResource(R.string.job_delete))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.job_add)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.job_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCreate = false
                        viewModel.create(name) { id -> onOpenJob(id) }
                    },
                ) { Text(stringResource(R.string.job_create)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) {
                    Text(stringResource(R.string.job_cancel))
                }
            },
        )
    }

    pendingDelete?.let { id ->
        val job = uiState.jobs.firstOrNull { it.id == id }
        if (job == null) {
            pendingDelete = null
        } else {
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text(stringResource(R.string.job_delete)) },
                text = { Text(stringResource(R.string.job_delete_confirm, job.name)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingDelete = null
                            viewModel.delete(job)
                        },
                    ) { Text(stringResource(R.string.job_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text(stringResource(R.string.job_cancel))
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    onOpenSchedule: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JobDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    val job = uiState.job

    // DB から読めたら1回だけ編集用の初期値に流し込む
    var name by remember(job?.id) { mutableStateOf(job?.name.orEmpty()) }
    var venue by remember(job?.id) { mutableStateOf(job?.venueName.orEmpty()) }
    var date by remember(job?.id) {
        mutableStateOf(DateTimeText.formatDate(job?.eventDateEpochMs))
    }
    var loadIn by remember(job?.id) {
        mutableStateOf(DateTimeText.formatTime(job?.loadInAtEpochMs))
    }
    var show by remember(job?.id) {
        mutableStateOf(DateTimeText.formatTime(job?.showAtEpochMs))
    }
    var client by remember(job?.id) { mutableStateOf(job?.clientName.orEmpty()) }
    var contact by remember(job?.id) { mutableStateOf(job?.contact.orEmpty()) }
    var notes by remember(job?.id) { mutableStateOf(job?.notes.orEmpty()) }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) viewModel.clearSaved()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(job?.name ?: stringResource(R.string.job_detail_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(CoreUiR.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.gutter),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            JobField(name, { name = it }, stringResource(R.string.job_name))
            JobField(venue, { venue = it }, stringResource(R.string.job_venue))
            JobField(date, { date = it }, stringResource(R.string.job_date))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JobField(
                    loadIn,
                    { loadIn = it },
                    stringResource(R.string.job_load_in),
                    Modifier.weight(1f),
                )
                JobField(
                    show,
                    { show = it },
                    stringResource(R.string.job_show),
                    Modifier.weight(1f),
                )
            }
            JobField(client, { client = it }, stringResource(R.string.job_client))
            JobField(contact, { contact = it }, stringResource(R.string.job_contact))
            JobField(notes, { notes = it }, stringResource(R.string.job_notes))

            OutlinedButton(
                onClick = {
                    val current = job ?: return@OutlinedButton
                    val eventDate = DateTimeText.parseDate(date)
                    viewModel.save(
                        current.copy(
                            name = name,
                            venueName = venue,
                            eventDateEpochMs = eventDate?.let { DateTimeText.dateToEpochMs(it) },
                            loadInAtEpochMs = DateTimeText.parseTime(loadIn)
                                ?.let { DateTimeText.toEpochMs(eventDate, it) },
                            showAtEpochMs = DateTimeText.parseTime(show)
                                ?.let { DateTimeText.toEpochMs(eventDate, it) },
                            clientName = client,
                            contact = contact,
                            notes = notes,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.job_save)) }

            OutlinedButton(
                onClick = { onOpenSchedule(viewModel.jobIdValue) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimens.gutter),
            ) { Text(stringResource(R.string.job_open_schedule)) }
        }
    }
}

@Composable
private fun JobField(
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
        modifier = modifier.fillMaxWidth(),
    )
}
