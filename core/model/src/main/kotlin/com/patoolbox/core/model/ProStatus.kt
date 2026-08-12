package com.patoolbox.core.model

/**
 * Pro の有効状態。
 *
 * 現場は圏外のことが多いので、購入情報はローカルにキャッシュして
 * [ProSource.OFFLINE_GRACE] の猶予期間中はオフラインでも Pro を維持する。
 * この判断がないと「電波が無いと有料機能が死ぬ」アプリになる。
 */
data class ProStatus(
    val isPro: Boolean,
    val source: ProSource,
) {
    companion object {
        val Free = ProStatus(isPro = false, source = ProSource.NONE)
    }
}

enum class ProSource {
    /** 未購入 */
    NONE,

    /** 月額/年額サブスクが有効 */
    SUBSCRIPTION,

    /** 買い切り（ライフタイム） */
    LIFETIME,

    /** オフラインで再検証できないが、猶予期間内なので有効扱い */
    OFFLINE_GRACE,

    /** 開発用の強制 ON（デバッグビルドのみ） */
    DEBUG_OVERRIDE,
}
