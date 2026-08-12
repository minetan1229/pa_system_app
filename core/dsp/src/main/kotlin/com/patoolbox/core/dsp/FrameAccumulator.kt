package com.patoolbox.core.dsp

/**
 * 録音ブロック（例: 1024サンプル）を FFT のフレーム（例: 8192サンプル）に組み直す。
 *
 * オーディオスレッドから呼ぶので割り当てをしない。[add] は inline なので
 * コールバックのラムダもヒープに乗らない。
 *
 * @param hopSize フレームを1つ出したあと、何サンプル進めるか。
 *   frameSize の半分にすると50%オーバーラップになり、時間分解能が上がる。
 */
class FrameAccumulator(
    val frameSize: Int,
    val hopSize: Int = frameSize / 2,
) {
    init {
        require(frameSize > 0) { "frameSize は正の値" }
        require(hopSize in 1..frameSize) { "hopSize は 1..frameSize" }
    }

    private val frame = FloatArray(frameSize)
    private var filled = 0

    /** 溜まっているサンプル数。 */
    val pending: Int get() = filled

    /**
     * ブロックを足す。フレームが揃うたびに [onFrame] が呼ばれる。
     * 渡される配列は内部バッファなので、保持せずその場で解析すること。
     */
    inline fun add(block: FloatArray, length: Int, onFrame: (FloatArray) -> Unit) {
        var offset = 0
        while (offset < length) {
            val copied = copyIn(block, offset, length)
            offset += copied

            if (isFrameReady()) {
                onFrame(frameBuffer())
                advance()
            }
        }
    }

    fun reset() {
        filled = 0
    }

    // --- inline から呼ぶため internal で公開している ---

    @PublishedApi
    internal fun copyIn(block: FloatArray, offset: Int, length: Int): Int {
        val copied = minOf(frameSize - filled, length - offset)
        block.copyInto(frame, filled, offset, offset + copied)
        filled += copied
        return copied
    }

    @PublishedApi
    internal fun isFrameReady(): Boolean = filled == frameSize

    @PublishedApi
    internal fun frameBuffer(): FloatArray = frame

    @PublishedApi
    internal fun advance() {
        if (hopSize >= frameSize) {
            filled = 0
        } else {
            frame.copyInto(frame, 0, hopSize, frameSize)
            filled = frameSize - hopSize
        }
    }
}
