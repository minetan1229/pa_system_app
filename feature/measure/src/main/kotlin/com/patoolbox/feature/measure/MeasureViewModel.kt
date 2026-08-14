package com.patoolbox.feature.measure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.audio.SweepMeasurementEngine
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.calc.SpeedOfSound
import com.patoolbox.core.dsp.RoomAnalysis
import com.patoolbox.core.model.ProStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 測定条件のうち、現場で変える必要があるもの。 */
enum class SweepLength(val seconds: Double, val label: String) {
    SHORT(1.0, "1秒"),
    NORMAL(2.0, "2秒"),
    LONG(5.0, "5秒"),
}

enum class SweepLevel(val dbFs: Double, val label: String) {
    LOW(-20.0, "-20dB"),
    MID(-12.0, "-12dB"),
    HIGH(-6.0, "-6dB"),
}

/**
 * 遅延の読み。
 *
 * [rawSamples] は録音の先頭からの位置、[alignedSamples] は出力した瞬間からの経過。
 * 端末のタイムスタンプが取れれば後者が出るが、**それでもアナログ段の遅れは残る**ので
 * 絶対値として信用してはいけない。基準測定との差だけが校正なしで意味を持つ。
 */
data class DelayReading(
    val rawSamples: Double,
    val alignedSamples: Double?,
    val sampleRate: Int,
    val confidenceDb: Double,
    val isReliable: Boolean,
    val method: RoomAnalysis.DelayMethod,
) {
    val rawMs: Double get() = rawSamples * 1000.0 / sampleRate

    fun differenceMsFrom(reference: DelayReading): Double =
        (rawSamples - reference.rawSamples) * 1000.0 / sampleRate

    fun differenceMetersFrom(reference: DelayReading, speedOfSound: Double): Double =
        differenceMsFrom(reference) / 1000.0 * speedOfSound
}

sealed interface MeasureState {
    data object Idle : MeasureState
    data object Measuring : MeasureState
    data class Done(val result: RoomAnalysis.Result, val reading: DelayReading) : MeasureState
    data class Failed(val message: String) : MeasureState
}

data class MeasureUiState(
    val state: MeasureState = MeasureState.Idle,
    val proStatus: ProStatus = ProStatus.Free,
    val sweepLength: SweepLength = SweepLength.NORMAL,
    val sweepLevel: SweepLevel = SweepLevel.MID,
    val temperatureCelsius: Double = 20.0,
    /** ディレイ実測で「基準にする」を押したときの読み */
    val reference: DelayReading? = null,
) {
    val isMeasuring: Boolean get() = state is MeasureState.Measuring
    val result: RoomAnalysis.Result? get() = (state as? MeasureState.Done)?.result
    val reading: DelayReading? get() = (state as? MeasureState.Done)?.reading
    val speedOfSound: Double get() = SpeedOfSound.forConditions(temperatureCelsius)
}

/**
 * スイープを1回鳴らして、遅延・極性・残響をまとめて出す。
 *
 * ディレイ実測 / 極性チェック / 残響測定 の3画面が共有する。
 * 現場では別の道具でも、やっていることは「スイープを鳴らして録って解析する」1回なので、
 * 測定と解析を1箇所にまとめてある。
 */
@HiltViewModel
class MeasureViewModel @Inject constructor(
    private val engine: SweepMeasurementEngine,
    proGate: ProGate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeasureUiState())
    val uiState: StateFlow<MeasureUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            proGate.proStatus.collect { status ->
                _uiState.update { it.copy(proStatus = status) }
            }
        }
    }

    fun setSweepLength(length: SweepLength) {
        _uiState.update { it.copy(sweepLength = length) }
    }

    fun setSweepLevel(level: SweepLevel) {
        _uiState.update { it.copy(sweepLevel = level) }
    }

    fun setTemperature(celsius: Double) {
        _uiState.update {
            it.copy(
                temperatureCelsius = celsius
                    .coerceIn(SpeedOfSound.MIN_CELSIUS, SpeedOfSound.MAX_CELSIUS),
            )
        }
    }

    /** 今の測定を基準にする。以降はここからの差が出る。 */
    fun markAsReference() {
        val reading = _uiState.value.reading ?: return
        _uiState.update { it.copy(reference = reading) }
    }

    fun clearReference() {
        _uiState.update { it.copy(reference = null) }
    }

    fun measure() {
        val current = _uiState.value
        if (current.isMeasuring) return
        if (!current.proStatus.isPro) return
        if (!engine.hasPermission()) {
            _uiState.update { it.copy(state = MeasureState.Failed("マイクの許可がありません")) }
            return
        }

        _uiState.update { it.copy(state = MeasureState.Measuring) }

        viewModelScope.launch {
            runCatching {
                val capture = engine.measure(
                    SweepMeasurementEngine.Config(
                        sweepSeconds = current.sweepLength.seconds,
                        levelDbFs = current.sweepLevel.dbFs,
                    ),
                )
                if (capture.silent) error("音が返ってきていません。出力とマイクを確認してください")

                val result = RoomAnalysis.analyze(
                    reference = capture.reference,
                    recorded = capture.recorded,
                    sampleRate = capture.sampleRate,
                )
                result to DelayReading(
                    rawSamples = result.delay.delaySamples,
                    alignedSamples = capture.alignmentOffsetSamples?.let {
                        result.delay.delaySamples - it
                    },
                    sampleRate = capture.sampleRate,
                    confidenceDb = result.delay.confidenceDb,
                    isReliable = result.delay.isReliable,
                    method = result.delayMethod,
                )
            }.onSuccess { (result, reading) ->
                _uiState.update { it.copy(state = MeasureState.Done(result, reading)) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        state = MeasureState.Failed(
                            throwable.message ?: "測定に失敗しました",
                        ),
                    )
                }
            }
        }
    }
}
