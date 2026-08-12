package com.patoolbox.core.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder

/**
 * 使用するマイク入力ソース。
 *
 * **MediaRecorder.AudioSource.MIC は使わない。**
 * MIC には端末の音声処理（AGC・ノイズ抑制・帯域制限）が入るため、
 * レベルも周波数特性も測定には使えない値になる。
 */
enum class MicInputSource(
    val androidSource: Int,
    val label: String,
    /** 測定に使える入力か（音声処理が入らないか） */
    val isMeasurementGrade: Boolean,
) {
    /** 端末が対応していれば最優先。音声処理が一切入らない */
    UNPROCESSED(
        androidSource = MediaRecorder.AudioSource.UNPROCESSED,
        label = "UNPROCESSED",
        isMeasurementGrade = true,
    ),

    /** UNPROCESSED 非対応端末のフォールバック。通常 AGC/NS が掛からない */
    VOICE_RECOGNITION(
        androidSource = MediaRecorder.AudioSource.VOICE_RECOGNITION,
        label = "VOICE_RECOGNITION",
        isMeasurementGrade = false,
    ),
    ;

    companion object {
        /**
         * 端末が UNPROCESSED に対応しているかを見て、使うソースを決める。
         *
         * 対応を申告していても実装が怪しい端末はあるので、これで「測定グレード」が
         * 保証されるわけではない。だからこそ校正が必須になる。
         */
        fun resolve(context: Context): MicInputSource {
            val audioManager = context.getSystemService(AudioManager::class.java)
            val supported = audioManager
                ?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)
                ?.toBooleanStrictOrNull() ?: false

            return if (supported) UNPROCESSED else VOICE_RECOGNITION
        }
    }
}
