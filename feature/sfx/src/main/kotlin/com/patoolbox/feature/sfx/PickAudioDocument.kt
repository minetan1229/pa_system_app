package com.patoolbox.feature.sfx

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

/**
 * 音声ファイルだけを選ばせる取り込み。
 *
 * 標準の [ActivityResultContracts.OpenDocument] は使えない。
 * あちらは Intent の type を必ず「すべての MIME」にしたうえで
 * `EXTRA_MIME_TYPES` を添えるだけなので、多くの端末のファイル選択画面が
 * 「全ファイル」を出してしまう（EXTRA_MIME_TYPES を尊重しない実装が多い）。
 *
 * ここでは type そのものを音声（[AUDIO_TYPE]）にして、
 * 補助として個別の MIME も添える。両方を出しておくと、
 * type しか見ない実装と EXTRA_MIME_TYPES を見る実装のどちらでも音声だけになる。
 *
 * （Kotlin のブロックコメントは入れ子になるので、
 * ここに MIME のワイルドカード表記をそのまま書くとコメントが閉じない）
 */
class PickAudioDocument : ActivityResultContract<Unit, Uri?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(AUDIO_TYPE)
            .putExtra(Intent.EXTRA_MIME_TYPES, AUDIO_MIME_TYPES)
            // 取り込み後もアプリ内にコピーして使うので、恒久的な権限までは要らない。
            // ただし読み出しの間だけは確実に必要
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent.takeIf { resultCode == android.app.Activity.RESULT_OK }?.data

    private companion object {
        const val AUDIO_TYPE = "audio/*"

        /**
         * 個別に並べる MIME。
         *
         * 端末によっては拡張子から MIME を引けず `application/octet-stream` になる
         * ファイルがあるが、それを通すと結局すべてのファイルが並ぶので入れていない。
         * 選べない素材があったときは、いったん端末側で mp3 か wav に変換してもらう。
         */
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
