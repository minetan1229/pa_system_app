package com.patoolbox.core.ui.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 横画面固定＋没入表示（システムバーを隠す）の切り替え。
 *
 * 卓の脇にスマホを立てて図だけを見る使い方を想定している。端末の自動回転任せだと、
 * 回転ロックの設定や置き方次第で横にならない。[active] が true の間だけ横に固定し、
 * 画面を出た・[active] を false に戻したときは元の向きとバー表示に戻す。
 *
 * MainActivity 側で configChanges に orientation/screenSize を含めていないと、
 * 向きを変えるたびに Activity が作り直されて計測が止まる。
 */
@Composable
fun FullscreenLandscapeEffect(active: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(activity, active) {
        val window = activity?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, view) }
        val originalOrientation = activity?.requestedOrientation

        if (active) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController?.let {
                it.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                it.hide(WindowInsetsCompat.Type.systemBars())
            }
        }

        onDispose {
            if (active) {
                activity?.requestedOrientation =
                    originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                insetsController?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
