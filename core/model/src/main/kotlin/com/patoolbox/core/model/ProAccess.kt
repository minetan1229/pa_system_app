package com.patoolbox.core.model

/**
 * 課金による可否判定はこの1箇所に集約する。
 * Free/Pro の線引きは運用しながら変わるので、各画面に散らさない。
 */
fun ProStatus.canOpen(tool: ToolId): Boolean = isPro || !tool.requiresPro

/** 開けるが機能制限がかかる状態か（記録・出力・保存件数の制限を出す判断に使う）。 */
fun ProStatus.isLimited(tool: ToolId): Boolean =
    !isPro && tool.access == ToolAccess.FREE_LIMITED

/** Free で保存できる件数の上限。Pro は無制限。 */
fun ProStatus.saveLimit(): Int? = if (isPro) null else FREE_SAVE_LIMIT

const val FREE_SAVE_LIMIT = 3
