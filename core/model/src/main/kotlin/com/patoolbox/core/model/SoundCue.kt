package com.patoolbox.core.model

/**
 * SE パッド1枚。
 *
 * 音そのものは端末に取り込んだファイルとして持つ。元の場所（クラウド、SDカード、
 * 共有ストレージ）を参照し続ける作りにしないのは、**本番で鳴らないと意味がない**から。
 * 元ファイルを消された・端末をオフラインにした・共有元の権限が切れた、
 * のどれが起きても本番中には対処できない。
 *
 * @param fileName アプリ内部ストレージ上のファイル名
 * @param position パッドの並び順。0 が左上
 * @param gain このパッド固有の音量（0.0〜1.0）。素材ごとの録音レベル差を吸収する
 * @param colorIndex パッドの色。暗所で位置ではなく色で覚えられるようにする
 */
data class SoundCue(
    val id: Long = 0,
    val title: String,
    val fileName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val position: Int,
    val loop: Boolean = false,
    val gain: Float = 1f,
    val colorIndex: Int = 0,
    val importedAtEpochMs: Long,
) {
    /** "1:05" 形式。1時間を超える SE は想定していない */
    val durationLabel: String
        get() {
            val totalSeconds = durationMs / 1000
            return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
        }

    companion object {
        /** パッドに割り当てられる色の数。[colorIndex] はこれで割った余りで使う */
        const val COLOR_COUNT = 6

        /** 無料版で持てるパッドの数 */
        const val FREE_LIMIT = 4
    }
}
