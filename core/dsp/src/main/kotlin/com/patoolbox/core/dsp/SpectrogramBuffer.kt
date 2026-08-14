package com.patoolbox.core.dsp

/**
 * スペクトログラムの履歴。固定長のリングバッファ。
 *
 * 解析はオーディオスレッドから毎フレーム来るので、[push] では割り当てをしない。
 * 行を毎回作り直すと 1秒に十数回の GC 圧力になり、録音の取りこぼしに繋がる。
 *
 * 値は dB で持つ。オーディオスレッド側で dB にしてしまう方が、
 * 描画側でフレームごとに log を回すより安い。
 */
class SpectrogramBuffer(
    val columns: Int,
    val historySize: Int,
) {
    init {
        require(columns > 0) { "カラム数が不正: $columns" }
        require(historySize > 0) { "履歴の長さが不正: $historySize" }
    }

    private val rows = Array(historySize) { FloatArray(columns) { FLOOR_DB } }

    /** 次に書き込む位置。ここが最も古い行でもある */
    private var head = 0

    /** 埋まっている行数。起動直後は履歴が足りない */
    var size: Int = 0
        private set

    fun push(columnDb: FloatArray) {
        require(columnDb.size >= columns) { "カラム数が合わない: ${columnDb.size} < $columns" }
        columnDb.copyInto(rows[head], destinationOffset = 0, startIndex = 0, endIndex = columns)
        head = (head + 1) % historySize
        if (size < historySize) size++
    }

    /**
     * 古い順に行を渡す。
     * 描画側は「上が最新」で描くことが多いので、逆順が要るときは [forEachNewestFirst]。
     */
    inline fun forEachOldestFirst(action: (index: Int, row: FloatArray) -> Unit) {
        for (i in 0 until size) {
            action(i, rowAt(i))
        }
    }

    inline fun forEachNewestFirst(action: (index: Int, row: FloatArray) -> Unit) {
        for (i in 0 until size) {
            action(i, rowAt(size - 1 - i))
        }
    }

    /** 古い順で [index] 番目の行。 */
    fun rowAt(index: Int): FloatArray {
        require(index in 0 until size) { "範囲外: $index (size=$size)" }
        // 埋まりきる前は 0 から順に入っているので、head は「最も古い」ではない
        val start = if (size < historySize) 0 else head
        return rows[(start + index) % historySize]
    }

    fun clear() {
        for (row in rows) {
            row.fill(FLOOR_DB)
        }
        head = 0
        size = 0
    }

    companion object {
        /** 初期値。まだ何も入っていない部分は最も暗く描かれる */
        const val FLOOR_DB = -120f
    }
}
