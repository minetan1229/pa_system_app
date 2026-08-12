package com.patoolbox.core.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.patoolbox.core.model.AudioInputType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class AudioInputDevice(
    val id: Int,
    val name: String,
    val type: AudioInputType,
    val isBuiltIn: Boolean,
) {
    /** 校正値を紐づけるキー。USB マイクは製品名で区別する */
    val calibrationKey: String
        get() = if (isBuiltIn) BUILTIN_KEY else "${type.name}:$name"

    companion object {
        const val BUILTIN_KEY = "builtin"
    }
}

/** 入力デバイスの一覧。USB 測定マイクを選べるようにするために使う。 */
@Singleton
class AudioDeviceCatalog @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun inputDevices(): List<AudioInputDevice> {
        val audioManager = context.getSystemService(AudioManager::class.java)
            ?: return emptyList()

        return audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            .map { it.toInputDevice() }
            .filter { it.type.isMeasurementCapable }
            .distinctBy { it.id }
    }

    fun findById(id: Int): AudioInputDevice? = inputDevices().firstOrNull { it.id == id }
}

internal fun AudioDeviceInfo.toInputDevice(): AudioInputDevice = AudioInputDevice(
    id = id,
    name = productName?.toString().orEmpty().ifBlank { "入力 $id" },
    type = toInputType(),
    isBuiltIn = type == AudioDeviceInfo.TYPE_BUILTIN_MIC,
)

internal fun AudioDeviceInfo.toInputType(): AudioInputType = when (type) {
    AudioDeviceInfo.TYPE_BUILTIN_MIC -> AudioInputType.BUILTIN_MIC
    AudioDeviceInfo.TYPE_WIRED_HEADSET -> AudioInputType.WIRED_HEADSET
    AudioDeviceInfo.TYPE_USB_DEVICE,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_USB_ACCESSORY,
    -> AudioInputType.USB
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    -> AudioInputType.BLUETOOTH
    else -> AudioInputType.UNKNOWN
}
