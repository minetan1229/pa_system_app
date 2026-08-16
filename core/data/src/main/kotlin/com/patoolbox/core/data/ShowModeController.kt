package com.patoolbox.core.data

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.patoolbox.core.model.ShowModeSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本番モードの適用。いまのところ中身は「おやすみモード（DND）の切り替え」だけ。
 *
 * ここは **OS の設定を触る** 数少ない場所なので、扱いを厳しくしている。
 *
 * - 許可（通知ポリシーへのアクセス）は **アプリからは要求できない**。
 *   実行時パーミッションのダイアログが無く、利用者が設定アプリで手で有効にする方式。
 *   [notificationPolicySettingsIntent] で画面まで案内するところまでが限界
 * - 本番モードを切ったら **必ず元に戻す**。アプリが勝手に端末を黙らせたまま
 *   放置されると、その日の終わりに気づけない
 * - 入れる前の状態を覚えておいて、そこへ戻す。無条件に「オフ」へ倒すと、
 *   もともとおやすみモードで運用していた人の設定を壊す
 */
@Singleton
class ShowModeController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val notificationManager: NotificationManager?
        get() = context.getSystemService(NotificationManager::class.java)

    /** 本番モードに入る前の割り込みフィルタ。戻すときに使う */
    private var previousFilter: Int? = null

    /** おやすみモードを切り替える許可があるか。 */
    fun hasNotificationPolicyAccess(): Boolean =
        notificationManager?.isNotificationPolicyAccessGranted == true

    /**
     * 許可を取るための設定画面。
     * アプリ内から許可を与える手段が無いので、ここへ送るしかない。
     */
    fun notificationPolicySettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * 本番モードに入る。
     * @return 実際に通知を止められたか。許可が無ければ false（他の項目は画面側が扱う）
     */
    fun enter(settings: ShowModeSettings): Boolean {
        if (!settings.silenceNotifications) return true

        val manager = notificationManager ?: return false
        if (!manager.isNotificationPolicyAccessGranted) return false

        return runCatching {
            if (previousFilter == null) previousFilter = manager.currentInterruptionFilter
            manager.setInterruptionFilter(
                if (settings.allowAlarms) {
                    NotificationManager.INTERRUPTION_FILTER_ALARMS
                } else {
                    NotificationManager.INTERRUPTION_FILTER_NONE
                },
            )
            true
        }.getOrDefault(false)
    }

    /**
     * 本番モードを出る。入る前の状態に戻す。
     *
     * 戻す先を覚えていない（アプリが再起動した後など）場合は
     * ALL（すべて通す）に戻す。黙ったままにするより、鳴りすぎる方がまだ気づける。
     */
    fun exit() {
        val manager = notificationManager ?: return
        if (!manager.isNotificationPolicyAccessGranted) return

        runCatching {
            manager.setInterruptionFilter(
                previousFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL,
            )
        }
        previousFilter = null
    }
}
