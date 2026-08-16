package com.patoolbox.core.model

/**
 * 本番モードで何を止めるか。
 *
 * 現場ごとに事情が違う（アラームで転換を管理している、BGM は別アプリで出している、
 * 別の卓からの連絡を通知で受けている）ので、一括で決め打ちにせず個別に選べるようにしている。
 *
 * @param silenceNotifications おやすみモードを入れて通知を止める。
 *   OS の設定なので [ShowModeCapability.NOTIFICATION_POLICY] の許可が要る
 * @param allowAlarms おやすみモード中もアラームは鳴らす。
 *   [silenceNotifications] が false のときは意味を持たない
 * @param keepScreenOn 本番中は画面を消さない
 * @param allowOtherAppAudio 他アプリの音を止めない（音声フォーカスを奪わない）
 */
data class ShowModeSettings(
    val silenceNotifications: Boolean = true,
    val allowAlarms: Boolean = false,
    val keepScreenOn: Boolean = true,
    val allowOtherAppAudio: Boolean = true,
) {
    /** OS の許可が必要な項目を1つでも使うか */
    val needsNotificationPolicy: Boolean get() = silenceNotifications

    companion object {
        val Default = ShowModeSettings()
    }
}

/** 本番モードが OS 側に要求するもの。UI の案内文を出し分けるために名前を付けている。 */
enum class ShowModeCapability {
    /**
     * 通知ポリシーへのアクセス（おやすみモードの切り替え）。
     * アプリからは要求できず、利用者が設定画面で手で許可する必要がある。
     */
    NOTIFICATION_POLICY,
}
