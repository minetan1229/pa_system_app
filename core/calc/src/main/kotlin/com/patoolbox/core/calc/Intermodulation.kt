package com.patoolbox.core.calc

import kotlin.math.abs

/**
 * ワイヤレスマイクの混変調（インターモジュレーション）計算。
 *
 * 送信機を複数立てると、受信機やアンプの非線形性で元の周波数の和や差にあたる
 * 成分が生まれる。それが別のマイクの受信周波数に重なると、そのマイクだけが
 * ノイズを拾ったり音が途切れたりする。**本番中に、しかも特定の1本だけ、
 * 他のマイクを開けたときにだけ起きる**ので、原因の切り分けが極めて難しい。
 * 事前に組み合わせを選んでおくのが唯一の対処になる。
 *
 * ### 周波数は kHz の整数で扱う
 *
 * 実機のチャンネルは 25kHz などの整数グリッドに乗っている。
 * Double で持つと 2f1-f2 の計算で下位の桁が揺れ、「ぴったり一致」の判定が
 * 環境依存になる。整数なら結果が完全に再現する。
 *
 * ### このクラスは法令上の可否を一切判定しない
 *
 * 使ってよい周波数は電波法と免許の条件で決まり、TVホワイトスペースに至っては
 * 運用地点ごとに違う。ここで扱うのは「その組み合わせで干渉が起きるか」だけ。
 * 使用可能な周波数は機材の取扱説明書と免許の条件で確認すること。
 */
object Intermodulation {

    /**
     * 混変調積の次数。
     *
     * 実務でまず問題になるのは3次。5次は3次より20dB以上低いのが普通だが、
     * 送信機が近接していると効いてくる。2次は通常は帯域外に落ちるので、
     * 広い範囲にまたがる運用でだけ見る。
     */
    enum class Order(val label: String, val description: String) {
        /** f1 ± f2。通常は帯域外に落ちる */
        SECOND("2次", "f1 ± f2"),

        /** 2f1 − f2。最も強く、最初に潰すべき */
        THIRD_TWO_TONE("3次(2波)", "2f1 − f2"),

        /** f1 + f2 − f3。本数が増えると急激に増える */
        THIRD_THREE_TONE("3次(3波)", "f1 + f2 − f3"),

        /** 3f1 − 2f2。送信機が近いときに効く */
        FIFTH_TWO_TONE("5次(2波)", "3f1 − 2f2"),
        ;
    }

    /** 既定で見る次数。3次だけを見るのが実務の標準。 */
    val DEFAULT_ORDERS: Set<Order> = setOf(Order.THIRD_TWO_TONE, Order.THIRD_THREE_TONE)

    data class Product(
        val frequencyKHz: Long,
        val order: Order,
        /** この積を作った周波数。表示して「どれを動かせばよいか」を示す */
        val sources: List<Long>,
    )

    data class Conflict(
        /** 妨害を受ける側の周波数 */
        val victimKHz: Long,
        val product: Product,
    ) {
        /** 積が受信周波数からどれだけずれているか。0 なら直撃 */
        val offsetKHz: Long get() = product.frequencyKHz - victimKHz
    }

    data class Report(
        val conflicts: List<Conflict>,
        val spacingViolations: List<Pair<Long, Long>>,
    ) {
        val isClean: Boolean get() = conflicts.isEmpty() && spacingViolations.isEmpty()

        /**
         * 周波数ごとの問題の件数。多い順。
         * どれを動かせば一番効くかがこれで分かる。
         */
        fun troubleScores(): List<Pair<Long, Int>> {
            val counts = mutableMapOf<Long, Int>()
            for (conflict in conflicts) {
                counts[conflict.victimKHz] = (counts[conflict.victimKHz] ?: 0) + 1
                for (source in conflict.product.sources.distinct()) {
                    counts[source] = (counts[source] ?: 0) + 1
                }
            }
            for ((a, b) in spacingViolations) {
                counts[a] = (counts[a] ?: 0) + 1
                counts[b] = (counts[b] ?: 0) + 1
            }
            return counts.entries
                .sortedWith(compareByDescending<Map.Entry<Long, Int>> { it.value }.thenBy { it.key })
                .map { it.key to it.value }
        }
    }

    /**
     * [frequenciesKHz] から生まれる混変調積を列挙する。
     *
     * 負や0になる組み合わせは捨てる（物理的に存在しない）。
     */
    fun products(
        frequenciesKHz: List<Long>,
        orders: Set<Order> = DEFAULT_ORDERS,
    ): List<Product> {
        val sorted = frequenciesKHz.distinct().sorted()
        val result = mutableListOf<Product>()

        for (i in sorted.indices) {
            for (j in sorted.indices) {
                if (i == j) continue
                val f1 = sorted[i]
                val f2 = sorted[j]

                if (Order.SECOND in orders && i < j) {
                    add(result, f1 + f2, Order.SECOND, listOf(f1, f2))
                    add(result, abs(f1 - f2), Order.SECOND, listOf(f1, f2))
                }
                if (Order.THIRD_TWO_TONE in orders) {
                    add(result, 2 * f1 - f2, Order.THIRD_TWO_TONE, listOf(f1, f2))
                }
                if (Order.FIFTH_TWO_TONE in orders) {
                    add(result, 3 * f1 - 2 * f2, Order.FIFTH_TWO_TONE, listOf(f1, f2))
                }
            }
        }

        if (Order.THIRD_THREE_TONE in orders) {
            // f1 + f2 − f3。f1 と f2 は入れ替えても同じなので i<j に絞る
            for (i in sorted.indices) {
                for (j in i + 1 until sorted.size) {
                    for (k in sorted.indices) {
                        if (k == i || k == j) continue
                        add(
                            result,
                            sorted[i] + sorted[j] - sorted[k],
                            Order.THIRD_THREE_TONE,
                            listOf(sorted[i], sorted[j], sorted[k]),
                        )
                    }
                }
            }
        }
        return result
    }

    private fun add(into: MutableList<Product>, value: Long, order: Order, sources: List<Long>) {
        if (value > 0) into += Product(value, order, sources)
    }

    /**
     * 使用中の周波数に重なる混変調積を探す。
     *
     * @param guardKHz 受信周波数からこの範囲に入った積を問題とみなす。
     *   受信機の帯域幅より少し広く取るのが安全側
     */
    fun analyze(
        frequenciesKHz: List<Long>,
        guardKHz: Long = DEFAULT_GUARD_KHZ,
        minSpacingKHz: Long = DEFAULT_MIN_SPACING_KHZ,
        orders: Set<Order> = DEFAULT_ORDERS,
    ): Report {
        val sorted = frequenciesKHz.distinct().sorted()
        val conflicts = mutableListOf<Conflict>()

        for (product in products(sorted, orders)) {
            for (victim in sorted) {
                // 積を作った本人に落ちるのは、間隔を守っていれば起きない。
                // 起きた場合は本物の問題なので除外しない
                if (abs(product.frequencyKHz - victim) <= guardKHz) {
                    conflicts += Conflict(victim, product)
                }
            }
        }

        val spacing = mutableListOf<Pair<Long, Long>>()
        for (i in 0 until sorted.size - 1) {
            if (sorted[i + 1] - sorted[i] < minSpacingKHz) {
                spacing += sorted[i] to sorted[i + 1]
            }
        }

        return Report(conflicts = conflicts, spacingViolations = spacing)
    }

    data class PlanRequest(
        val fromKHz: Long,
        val toKHz: Long,
        /** チャンネルの刻み。機材が対応する値を入れる */
        val stepKHz: Long,
        val count: Int,
        val guardKHz: Long = DEFAULT_GUARD_KHZ,
        val minSpacingKHz: Long = DEFAULT_MIN_SPACING_KHZ,
        val orders: Set<Order> = DEFAULT_ORDERS,
        /** すでに使っていて動かせない周波数（他社の運用、放送など） */
        val fixedKHz: List<Long> = emptyList(),
    )

    data class PlanResult(
        val frequenciesKHz: List<Long>,
        val requestedCount: Int,
    ) {
        val isComplete: Boolean get() = frequenciesKHz.size >= requestedCount
        val shortfall: Int get() = (requestedCount - frequenciesKHz.size).coerceAtLeast(0)
    }

    /**
     * 干渉しない組み合わせを探す。
     *
     * 候補を低い方から順に見て、既に選んだものと衝突しなければ採用する貪欲法。
     * 最適解ではないが、結果が毎回同じで、なぜその周波数になったかを説明できる。
     * 乱択で詰め込む方式は本数が1本増えることはあっても、
     * 現場で「昨日と違う周波数が出た」となる方が困る。
     *
     * 希望の本数に届かない場合は、取れたところまでを返す（[PlanResult.shortfall]）。
     * 足りないまま黙って返さないこと。
     */
    fun plan(request: PlanRequest): PlanResult {
        require(request.stepKHz > 0) { "刻みが不正: ${request.stepKHz}" }
        require(request.toKHz >= request.fromKHz) { "範囲が不正" }

        val fixed = request.fixedKHz.distinct().sorted()
        val chosen = fixed.toMutableList()
        val picked = mutableListOf<Long>()
        // 固定分が既に抱えている問題は候補のせいではない。基準として持っておき、
        // 候補を足したときの増分だけを見る
        var baseline = conflictCount(chosen, request)

        var candidate = request.fromKHz
        while (candidate <= request.toKHz && picked.size < request.count) {
            if (hasRoom(chosen, candidate, request.minSpacingKHz)) {
                val trial = (chosen + candidate).sorted()
                val trialCount = conflictCount(trial, request)
                if (trialCount == baseline) {
                    chosen.add(candidate)
                    chosen.sort()
                    picked += candidate
                    baseline = trialCount
                }
            }
            candidate += request.stepKHz
        }

        return PlanResult(frequenciesKHz = picked, requestedCount = request.count)
    }

    private fun hasRoom(chosen: List<Long>, candidate: Long, minSpacingKHz: Long): Boolean =
        chosen.none { abs(it - candidate) < minSpacingKHz }

    private fun conflictCount(frequencies: List<Long>, request: PlanRequest): Int = analyze(
        frequencies,
        guardKHz = request.guardKHz,
        minSpacingKHz = request.minSpacingKHz,
        orders = request.orders,
    ).conflicts.size

    /**
     * 受信機の帯域より少し広く取った既定値。
     * デジタル方式は占有帯域が広いので、機材に合わせて広げること。
     */
    const val DEFAULT_GUARD_KHZ = 50L

    /** 隣り合うチャンネルの最小間隔。多くの機種が 300kHz 以上を推奨している。 */
    const val DEFAULT_MIN_SPACING_KHZ = 300L

    /** kHz ↔ MHz。表示は MHz、計算は kHz で行う。 */
    fun mhzToKHz(mhz: Double): Long = Math.round(mhz * 1000.0)

    fun kHzToMhz(khz: Long): Double = khz / 1000.0
}
