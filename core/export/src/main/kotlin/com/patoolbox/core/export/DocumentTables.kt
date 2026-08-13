package com.patoolbox.core.export

import com.patoolbox.core.model.Job
import com.patoolbox.core.model.PatchSheet
import com.patoolbox.core.model.TimelineEntry

/**
 * ドメインのモデルを PDF の表に変換する。
 * 描画（[PdfTableWriter]）とは分けているので、列構成の変更はここだけで済む。
 */
object DocumentTables {

    /** パッチ表。現場で配る想定なので、印刷して読める列だけに絞る。 */
    fun patchSheet(
        sheet: PatchSheet,
        jobName: String,
        generatedAt: String,
    ): PdfTable = PdfTable(
        title = sheet.name,
        subtitle = listOfNotNull(
            jobName.takeIf { it.isNotBlank() },
            "出力 $generatedAt",
        ).joinToString(" / "),
        columns = listOf(
            PdfColumn("CH", weight = 0.6f, alignEnd = true),
            PdfColumn("音源・楽器", weight = 3f),
            PdfColumn("マイク・DI", weight = 2.6f),
            PdfColumn("スタンド", weight = 1.8f),
            PdfColumn("48V", weight = 0.8f),
            PdfColumn("マルチ", weight = 1.2f),
            PdfColumn("備考", weight = 2.6f),
        ),
        rows = sheet.rows.map { row ->
            listOf(
                row.channel.toString(),
                row.source,
                row.micModel,
                row.standType,
                if (row.phantom) "✓" else "",
                row.multiNumber,
                row.notes,
            )
        },
        footer = "48V 使用 ${sheet.rows.count { it.phantom }} ch / 全 ${sheet.rows.size} ch",
    )

    /** 進行表。時刻は計算済みのものを受け取る。 */
    fun schedule(
        job: Job,
        entries: List<TimelineEntry>,
        formatTime: (Long) -> String,
        totalMinutes: Int,
    ): PdfTable = PdfTable(
        title = "${job.name} 進行表",
        subtitle = listOfNotNull(
            job.venueName.takeIf { it.isNotBlank() },
            job.clientName.takeIf { it.isNotBlank() },
        ).joinToString(" / "),
        columns = listOf(
            PdfColumn("開始", weight = 1f),
            PdfColumn("終了", weight = 1f),
            PdfColumn("項目", weight = 4f),
            PdfColumn("長さ", weight = 1f, alignEnd = true),
            PdfColumn("担当", weight = 2f),
        ),
        rows = entries.map { entry ->
            listOf(
                formatTime(entry.startAtEpochMs) + if (entry.isAnchor) " ★" else "",
                formatTime(entry.endAtEpochMs),
                entry.item.title,
                "${entry.item.durationMinutes}分",
                entry.item.owner,
            )
        },
        footer = "合計 ${totalMinutes / 60} 時間 ${totalMinutes % 60} 分 / ★ は固定時刻",
    )
}
