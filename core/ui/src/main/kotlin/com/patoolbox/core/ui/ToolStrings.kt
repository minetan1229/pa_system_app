package com.patoolbox.core.ui

import androidx.annotation.StringRes
import com.patoolbox.core.model.ToolCategory
import com.patoolbox.core.model.ToolId

/**
 * ToolId → 表示文字列の対応表。
 * `when` を網羅させているので、ToolId を足したらコンパイルエラーで気づける。
 */
@get:StringRes
val ToolId.titleRes: Int
    get() = when (this) {
        ToolId.SPL_METER -> R.string.tool_spl_meter_title
        ToolId.SPL_LOGGER -> R.string.tool_spl_logger_title
        ToolId.RTA -> R.string.tool_rta_title
        ToolId.FFT -> R.string.tool_fft_title
        ToolId.SPECTROGRAM -> R.string.tool_spectrogram_title
        ToolId.SIGNAL_GENERATOR -> R.string.tool_signal_generator_title
        ToolId.FEEDBACK_FINDER -> R.string.tool_feedback_finder_title
        ToolId.DELAY_FINDER -> R.string.tool_delay_finder_title
        ToolId.POLARITY_CHECK -> R.string.tool_polarity_check_title
        ToolId.ROOM_MEASURE -> R.string.tool_room_measure_title
        ToolId.TUNER -> R.string.tool_tuner_title
        ToolId.METRONOME -> R.string.tool_metronome_title
        ToolId.RECORDER -> R.string.tool_recorder_title
        ToolId.DELAY_CALC -> R.string.tool_delay_calc_title
        ToolId.BPM_CALC -> R.string.tool_bpm_calc_title
        ToolId.DB_CALC -> R.string.tool_db_calc_title
        ToolId.IMPEDANCE_CALC -> R.string.tool_impedance_calc_title
        ToolId.POWER_CALC -> R.string.tool_power_calc_title
        ToolId.COVERAGE_CALC -> R.string.tool_coverage_calc_title
        ToolId.CONNECTOR_REF -> R.string.tool_connector_ref_title
        ToolId.FREQ_CHART -> R.string.tool_freq_chart_title
        ToolId.TROUBLESHOOT -> R.string.tool_troubleshoot_title
        ToolId.GLOSSARY -> R.string.tool_glossary_title
        ToolId.WIRELESS_COORD -> R.string.tool_wireless_coord_title
        ToolId.PATCH_SHEET -> R.string.tool_patch_sheet_title
        ToolId.STAGE_PLOT -> R.string.tool_stage_plot_title
        ToolId.PDF_EXPORT -> R.string.tool_pdf_export_title
        ToolId.RUN_SHEET -> R.string.tool_run_sheet_title
        ToolId.SHOW_TIMER -> R.string.tool_show_timer_title
        ToolId.JOB_MANAGER -> R.string.tool_job_manager_title
        ToolId.SNAPSHOT -> R.string.tool_snapshot_title
        ToolId.GEAR_INVENTORY -> R.string.tool_gear_inventory_title
        ToolId.INVOICE -> R.string.tool_invoice_title
        ToolId.WORK_LOG -> R.string.tool_work_log_title
        ToolId.CLOUD_BACKUP -> R.string.tool_cloud_backup_title
    }

@get:StringRes
val ToolId.descriptionRes: Int
    get() = when (this) {
        ToolId.SPL_METER -> R.string.tool_spl_meter_desc
        ToolId.SPL_LOGGER -> R.string.tool_spl_logger_desc
        ToolId.RTA -> R.string.tool_rta_desc
        ToolId.FFT -> R.string.tool_fft_desc
        ToolId.SPECTROGRAM -> R.string.tool_spectrogram_desc
        ToolId.SIGNAL_GENERATOR -> R.string.tool_signal_generator_desc
        ToolId.FEEDBACK_FINDER -> R.string.tool_feedback_finder_desc
        ToolId.DELAY_FINDER -> R.string.tool_delay_finder_desc
        ToolId.POLARITY_CHECK -> R.string.tool_polarity_check_desc
        ToolId.ROOM_MEASURE -> R.string.tool_room_measure_desc
        ToolId.TUNER -> R.string.tool_tuner_desc
        ToolId.METRONOME -> R.string.tool_metronome_desc
        ToolId.RECORDER -> R.string.tool_recorder_desc
        ToolId.DELAY_CALC -> R.string.tool_delay_calc_desc
        ToolId.BPM_CALC -> R.string.tool_bpm_calc_desc
        ToolId.DB_CALC -> R.string.tool_db_calc_desc
        ToolId.IMPEDANCE_CALC -> R.string.tool_impedance_calc_desc
        ToolId.POWER_CALC -> R.string.tool_power_calc_desc
        ToolId.COVERAGE_CALC -> R.string.tool_coverage_calc_desc
        ToolId.CONNECTOR_REF -> R.string.tool_connector_ref_desc
        ToolId.FREQ_CHART -> R.string.tool_freq_chart_desc
        ToolId.TROUBLESHOOT -> R.string.tool_troubleshoot_desc
        ToolId.GLOSSARY -> R.string.tool_glossary_desc
        ToolId.WIRELESS_COORD -> R.string.tool_wireless_coord_desc
        ToolId.PATCH_SHEET -> R.string.tool_patch_sheet_desc
        ToolId.STAGE_PLOT -> R.string.tool_stage_plot_desc
        ToolId.PDF_EXPORT -> R.string.tool_pdf_export_desc
        ToolId.RUN_SHEET -> R.string.tool_run_sheet_desc
        ToolId.SHOW_TIMER -> R.string.tool_show_timer_desc
        ToolId.JOB_MANAGER -> R.string.tool_job_manager_desc
        ToolId.SNAPSHOT -> R.string.tool_snapshot_desc
        ToolId.GEAR_INVENTORY -> R.string.tool_gear_inventory_desc
        ToolId.INVOICE -> R.string.tool_invoice_desc
        ToolId.WORK_LOG -> R.string.tool_work_log_desc
        ToolId.CLOUD_BACKUP -> R.string.tool_cloud_backup_desc
    }

@get:StringRes
val ToolCategory.titleRes: Int
    get() = when (this) {
        ToolCategory.MEASURE -> R.string.category_measure
        ToolCategory.CALC -> R.string.category_calc
        ToolCategory.DOCUMENT -> R.string.category_document
        ToolCategory.BUSINESS -> R.string.category_business
    }
