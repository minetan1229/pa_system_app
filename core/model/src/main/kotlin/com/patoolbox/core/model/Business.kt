package com.patoolbox.core.model

/** 機材の種別。台帳の絞り込みに使う。 */
enum class GearCategory(val label: String) {
    SPEAKER("スピーカー"),
    AMP("アンプ"),
    MIXER("卓"),
    MIC("マイク"),
    DI("DI"),
    WIRELESS("ワイヤレス"),
    OUTBOARD("アウトボード"),
    CABLE("ケーブル"),
    STAND("スタンド"),
    POWER("電源"),
    OTHER("その他"),
}

/** 機材の状態。現場に出せるかどうかが一目で分かることが目的。 */
enum class GearStatus(val label: String) {
    AVAILABLE("使用可"),
    IN_USE("持ち出し中"),
    REPAIR("修理中"),
    RETIRED("引退"),
    ;

    val isUsable: Boolean get() = this == AVAILABLE
}

/**
 * 機材台帳の1件。
 *
 * 数量を持たせているのは、ケーブルやスタンドを1本ずつ登録するのが現実的でないため。
 * 同じ型番をまとめて数える運用にしている。
 */
data class GearItem(
    val id: Long = 0,
    val category: GearCategory = GearCategory.OTHER,
    val name: String,
    val maker: String = "",
    val modelName: String = "",
    val serial: String = "",
    val quantity: Int = 1,
    val status: GearStatus = GearStatus.AVAILABLE,
    val note: String = "",
    val updatedAtEpochMs: Long = 0,
) {
    val displayName: String
        get() = listOf(maker, modelName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { name }
}

/**
 * 卓の設定を控えたもの。
 *
 * 卓の内部データは機種ごとに形式が違い、外から読める保証もない。
 * ここで残すのは **人が読める記録** で、翌日の同じ現場や別の卓でも
 * 追い込みの出発点として使えることを狙っている。
 * 機種固有のファイルを扱おうとすると、対応卓の外では何の役にも立たなくなる。
 */
data class Snapshot(
    val id: Long = 0,
    val jobId: Long? = null,
    val title: String,
    val consoleName: String = "",
    val takenAtEpochMs: Long = 0,
    val note: String = "",
    val channels: List<SnapshotChannel> = emptyList(),
)

data class SnapshotChannel(
    val id: Long = 0,
    val channel: Int,
    val name: String = "",
    val gain: String = "",
    val hpf: String = "",
    val eq: String = "",
    val send: String = "",
    val note: String = "",
) {
    val isEmpty: Boolean
        get() = name.isBlank() && gain.isBlank() && hpf.isBlank() &&
            eq.isBlank() && send.isBlank() && note.isBlank()
}

/**
 * 請求書・見積書。
 *
 * 税の扱い（税抜/税込）と端数処理の方法は書類ごとに持つ。
 * 事業者ごとに決まっているのが普通だが、途中で変えたときに
 * 過去の書類の金額が変わってしまうと帳簿が合わなくなる。
 */
data class Invoice(
    val id: Long = 0,
    val jobId: Long? = null,
    /** 請求書番号。連番の付け方は事業者に任せる */
    val number: String = "",
    val issueDateEpochMs: Long = 0,
    val clientName: String = "",
    val subject: String = "",
    val issuerName: String = "",
    /** 適格請求書発行事業者の登録番号（T+13桁） */
    val registrationNumber: String = "",
    val taxModeName: String = "EXCLUSIVE",
    val taxRoundingName: String = "DOWN",
    val note: String = "",
    val isEstimate: Boolean = false,
    val lines: List<InvoiceLineItem> = emptyList(),
    val updatedAtEpochMs: Long = 0,
) {
    val documentLabel: String get() = if (isEstimate) "見積書" else "請求書"
}

data class InvoiceLineItem(
    val id: Long = 0,
    val description: String = "",
    val quantity: Double = 1.0,
    val unit: String = "式",
    val unitPrice: Long = 0,
    val taxRateName: String = "STANDARD",
    val sortOrder: Int = 0,
)

/** 1日の稼働。月の集計と請求の根拠になる。 */
data class WorkLogEntry(
    val id: Long = 0,
    val jobId: Long? = null,
    val title: String = "",
    val dateEpochMs: Long = 0,
    val startMinutesOfDay: Int = 9 * 60,
    val endMinutesOfDay: Int = 18 * 60,
    val breakMinutes: Int = 0,
    val rateTypeName: String = "DAILY",
    val rate: Long = 0,
    val multiplier: Double = 1.0,
    val transportFee: Long = 0,
    val note: String = "",
)
