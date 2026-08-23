package com.patoolbox.feature.stageplot

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.patoolbox.core.export.StageGeometry
import com.patoolbox.core.export.StagePlotRenderer
import com.patoolbox.core.model.StagePlot

/**
 * 配置図の編集キャンバス。
 *
 * 描画は [StagePlotRenderer] に任せる。PDF と同じコードなので、
 * ここで見えている図と書き出した図が食い違わない。
 */
@Composable
fun StagePlotCanvas(
    plot: StagePlot,
    selectedItemId: Long?,
    onSelect: (Long?) -> Unit,
    onMove: (itemId: Long, x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renderer = remember { StagePlotRenderer() }
    val itemPalette = stageItemPalette()
    val itemTextPalette = stageItemTextPalette()
    val colors = StagePlotRenderer.Colors(
        stageOutline = MaterialTheme.colorScheme.outline.toArgb(),
        stageFill = MaterialTheme.colorScheme.surfaceContainerLowest.toArgb(),
        itemPalette = itemPalette.map { it.toArgb() },
        itemTextPalette = itemTextPalette.map { it.toArgb() },
        itemOutline = MaterialTheme.colorScheme.outline.toArgb(),
        label = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
        selectedOutline = MaterialTheme.colorScheme.primary.toArgb(),
    )

    // ジェスチャは pointerInput の中で完結するので、最新の plot を参照できるようにしておく。
    // キーに plot を渡すと記号を1つ動かすたびにジェスチャ検出が作り直されて、
    // ドラッグが途中で切れる
    val currentPlot by rememberUpdatedState(plot)
    val currentSelect by rememberUpdatedState(onSelect)
    val currentMove by rememberUpdatedState(onMove)
    var draggingId by remember { mutableStateOf<Long?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val stage = stageRect(currentPlot, size.width, size.height)
                    val hit = StageGeometry.itemAt(currentPlot, stage, offset.x, offset.y)
                    currentSelect(hit?.id)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val stage = stageRect(currentPlot, size.width, size.height)
                        val hit = StageGeometry.itemAt(currentPlot, stage, offset.x, offset.y)
                        draggingId = hit?.id
                        if (hit != null) currentSelect(hit.id)
                    },
                    onDragEnd = { draggingId = null },
                    onDragCancel = { draggingId = null },
                    onDrag = { change, _ ->
                        val id = draggingId ?: return@detectDragGestures
                        change.consume()
                        val stage = stageRect(currentPlot, size.width, size.height)
                        if (stage.width() <= 0f || stage.height() <= 0f) return@detectDragGestures
                        // 指の位置をそのまま記号の中心にする。差分を積むと、
                        // 端で止まったあと指を戻したときに位置がずれていく
                        val x = (change.position.x - stage.left) / stage.width()
                        val y = (change.position.y - stage.top) / stage.height()
                        currentMove(id, x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
                    },
                )
            },
    ) {
        drawIntoCanvas { canvas ->
            renderer.draw(
                canvas = canvas.nativeCanvas,
                plot = plot,
                bounds = RectF(0f, CAPTION_ROOM, size.width, size.height - CAPTION_ROOM),
                colors = colors,
                selectedItemId = selectedItemId,
            )
        }
    }
}

/** 当たり判定に使うステージ矩形。描画で使う bounds と必ず同じ式にすること。 */
private fun stageRect(plot: StagePlot, width: Int, height: Int): RectF =
    StageGeometry.fitStage(
        plot,
        RectF(0f, CAPTION_ROOM, width.toFloat(), height - CAPTION_ROOM),
    )

/** 図の上下に「客席側」「上手/下手」を出すぶんの余白（px）。 */
private const val CAPTION_ROOM = 40f
