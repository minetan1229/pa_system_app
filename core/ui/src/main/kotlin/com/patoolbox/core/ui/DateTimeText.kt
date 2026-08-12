package com.patoolbox.core.ui

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 進行表・案件で使う日付と時刻の入出力。
 *
 * 現場の進行表は「日付＋時刻」ではなく時刻（9:00, 19:00）で書くので、
 * UI では HH:mm だけを入力させ、案件の日付と組み合わせて epoch に直す。
 * 日付ピッカーを出すより、文字で打てるほうが搬入前の慌ただしい状況では速い。
 */
object DateTimeText {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun formatTime(epochMs: Long?): String = epochMs
        ?.let { timeFormatter.format(localDateTime(it)) }
        .orEmpty()

    fun formatDate(epochMs: Long?): String = epochMs
        ?.let { dateFormatter.format(localDateTime(it)) }
        .orEmpty()

    fun formatDateTime(epochMs: Long?): String = epochMs
        ?.let { "${formatDate(it)} ${formatTime(it)}" }
        .orEmpty()

    /** "yyyy-MM-dd" を読む。読めなければ null。 */
    fun parseDate(text: String): LocalDate? = runCatching {
        LocalDate.parse(text.trim(), dateFormatter)
    }.getOrNull()

    /** "H:mm" / "HH:mm" / "1900" を読む。読めなければ null。 */
    fun parseTime(text: String): LocalTime? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        // 区切りなしの4桁（1900）も受ける。現場では数字だけ打つ人が多い
        val normalized = if (!trimmed.contains(':') && trimmed.length in 3..4) {
            val cut = trimmed.length - 2
            "${trimmed.substring(0, cut)}:${trimmed.substring(cut)}"
        } else {
            trimmed
        }

        return try {
            val parts = normalized.split(':')
            if (parts.size != 2) return null
            LocalTime.of(parts[0].toInt(), parts[1].toInt())
        } catch (_: NumberFormatException) {
            null
        } catch (_: DateTimeParseException) {
            null
        } catch (_: java.time.DateTimeException) {
            null
        }
    }

    /** 日付と時刻から epoch ミリ秒。日付が無ければ今日として扱う。 */
    fun toEpochMs(date: LocalDate?, time: LocalTime): Long =
        (date ?: LocalDate.now())
            .atTime(time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    fun dateToEpochMs(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun today(): LocalDate = LocalDate.now()

    private fun localDateTime(epochMs: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault())
}
