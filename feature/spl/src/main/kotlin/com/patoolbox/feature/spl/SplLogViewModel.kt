package com.patoolbox.feature.spl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.data.MeasurementRepository
import com.patoolbox.core.data.di.IoDispatcher
import com.patoolbox.core.export.CsvWriter
import com.patoolbox.core.model.Measurement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import javax.inject.Inject

data class SplLogUiState(
    val measurements: List<Measurement> = emptyList(),
)

/** 保存した測定の一覧と書き出し。 */
@HiltViewModel
class SplLogViewModel @Inject constructor(
    private val repository: MeasurementRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    val uiState: StateFlow<SplLogUiState> = repository.observeAll()
        .map { SplLogUiState(measurements = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SplLogUiState(),
        )

    fun delete(measurement: Measurement) {
        viewModelScope.launch { repository.delete(measurement.id) }
    }

    fun suggestedFileName(measurement: Measurement): String =
        "${measurement.title}.csv"

    /**
     * CSV を書き出す。
     *
     * 測定条件と校正状態を注記として先頭に入れる。
     * 数字だけの表を渡されても、どの設定で測ったか分からなければ判断に使えないため。
     */
    fun exportCsv(measurement: Measurement, output: OutputStream) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val samples = repository.samples(measurement.id)
                val comments = buildList {
                    add("測定: ${measurement.title}")
                    add("重み付け: ${measurement.weightingLabel}")
                    add("校正: ${measurement.calibrationMethod.name} / " +
                        "オフセット ${"%.1f".format(measurement.calibrationOffsetDb)} dB")
                    add(
                        "要約: Leq ${"%.1f".format(measurement.leqDb)} / " +
                            "Lmax ${"%.1f".format(measurement.maxDb)} / " +
                            "Lmin ${"%.1f".format(measurement.minDb)}",
                    )
                    if (measurement.isUncalibrated) {
                        add("未校正のため参考値です。計量法上の証明用途には使用できません")
                    }
                    if (measurement.clipped) {
                        add("過大入力（クリップ）を検出した区間があります")
                    }
                }

                val csv = CsvWriter.build(
                    header = listOf("経過秒", "瞬時値(dB)", "Leq(dB)", "マーカー"),
                    rows = samples.map { sample ->
                        listOf(
                            "%.0f".format(sample.offsetMs / 1000.0),
                            "%.1f".format(sample.instantDb),
                            "%.1f".format(sample.leqDb),
                            sample.marker,
                        )
                    },
                    comments = comments,
                )

                output.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
