package com.patoolbox.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SoundCueTest {

    private fun cue(durationMs: Long) = SoundCue(
        title = "SE",
        fileName = "cue_1.wav",
        durationMs = durationMs,
        sizeBytes = 1000,
        position = 0,
        importedAtEpochMs = 0,
    )

    @Test
    fun `長さは分秒で出る`() {
        assertThat(cue(65_000).durationLabel).isEqualTo("1:05")
        assertThat(cue(3_000).durationLabel).isEqualTo("0:03")
    }

    @Test
    fun `長さが取れなかった場合も表示は壊れない`() {
        // MediaMetadataRetriever が長さを返さない形式では 0 が入る
        assertThat(cue(0).durationLabel).isEqualTo("0:00")
    }

    @Test
    fun `秒は切り捨てる`() {
        // 1.9秒を 2 秒と出すと、繋ぎのタイミングを計るときに1秒ずれる
        assertThat(cue(1_900).durationLabel).isEqualTo("0:01")
    }

    @Test
    fun `色は既定で6色`() {
        assertThat(SoundCue.COLOR_COUNT).isEqualTo(6)
    }
}

class ShowModeSettingsTest {

    @Test
    fun `既定は通知を止めて画面を点けたままにする`() {
        val settings = ShowModeSettings.Default

        assertThat(settings.silenceNotifications).isTrue()
        assertThat(settings.keepScreenOn).isTrue()
    }

    @Test
    fun `既定では他アプリの音を止めない`() {
        // BGM を別アプリで出しながら SE を重ねる使い方があるので、
        // 勝手に相手を黙らせる方を既定にしない
        assertThat(ShowModeSettings.Default.allowOtherAppAudio).isTrue()
    }

    @Test
    fun `通知を止める設定のときだけ OS の許可が要る`() {
        assertThat(ShowModeSettings(silenceNotifications = true).needsNotificationPolicy).isTrue()
        assertThat(ShowModeSettings(silenceNotifications = false).needsNotificationPolicy).isFalse()
    }

    @Test
    fun `画面だけ点けたい場合は許可なしで成立する`() {
        val settings = ShowModeSettings(
            silenceNotifications = false,
            keepScreenOn = true,
        )

        assertThat(settings.needsNotificationPolicy).isFalse()
    }
}
