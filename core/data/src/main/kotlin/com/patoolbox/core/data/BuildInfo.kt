package com.patoolbox.core.data

import android.content.Context
import android.content.pm.ApplicationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ビルド種別の判定。
 * BuildConfig を各モジュールで生成させたくないので、実行時のフラグから見る。
 * デバッグ用の Pro 強制 ON がリリースビルドで効かないようにするために使う。
 */
@Singleton
class BuildInfo @Inject constructor(
    @ApplicationContext context: Context,
) {
    val isDebuggable: Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
