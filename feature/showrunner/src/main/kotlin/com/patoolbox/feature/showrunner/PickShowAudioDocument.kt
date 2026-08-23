package com.patoolbox.feature.showrunner

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

/**
 * 音声ファイルだけを選ばせる取り込み。
 *
 * [com.patoolbox.feature.sfx] にある同名の実装と中身は同じ。
 * feature モジュールをまたいで公開すると、SE パッド画面の内部実装（ここでは
 * 使わない編集シートなどまで）に依存が広がるので、この小さな契約だけ複製している。
 */
class PickShowAudioDocument : ActivityResultContract<Unit, Uri?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(AUDIO_TYPE)
            .putExtra(Intent.EXTRA_MIME_TYPES, AUDIO_MIME_TYPES)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent.takeIf { resultCode == android.app.Activity.RESULT_OK }?.data

    private companion object {
        const val AUDIO_TYPE = "audio/*"
        val AUDIO_MIME_TYPES = arrayOf(
            "audio/*",
            "audio/mpeg",
            "audio/mp4",
            "audio/aac",
            "audio/wav",
            "audio/x-wav",
            "audio/ogg",
            "audio/flac",
        )
    }
}
