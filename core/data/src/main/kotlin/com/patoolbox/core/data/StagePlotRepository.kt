package com.patoolbox.core.data

import com.patoolbox.core.database.dao.StagePlotDao
import com.patoolbox.core.database.entity.StageItemEntity
import com.patoolbox.core.database.entity.StagePlotEntity
import com.patoolbox.core.model.StageItem
import com.patoolbox.core.model.StageItemColor
import com.patoolbox.core.model.StagePlot
import com.patoolbox.core.model.StageSymbol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** ステージプロット（配置図）。 */
interface StagePlotRepository {

    /** 一覧。記号は含まない（一覧では使わない） */
    fun observeAll(): Flow<List<StagePlot>>

    fun observeForJob(jobId: Long): Flow<List<StagePlot>>

    fun observeWithItems(plotId: Long): Flow<StagePlot?>

    suspend fun count(): Int

    suspend fun create(name: String, jobId: Long? = null): Long

    suspend fun updateDetails(plot: StagePlot)

    suspend fun delete(plotId: Long)

    /** @return 追加した記号のID */
    suspend fun addItem(plotId: Long, item: StageItem): Long

    /** ドラッグ中に呼ばれる。座標だけを書き、他の列には触らない */
    suspend fun moveItem(plotId: Long, itemId: Long, x: Float, y: Float)

    suspend fun renameItem(plotId: Long, item: StageItem, label: String)

    /** 色だけを書く。[moveItem] と同じ理由で他の列には触らない */
    suspend fun recolorItem(plotId: Long, itemId: Long, colorIndex: Int)

    suspend fun deleteItem(plotId: Long, itemId: Long)
}

@Singleton
class RoomStagePlotRepository @Inject constructor(
    private val dao: StagePlotDao,
) : StagePlotRepository {

    override fun observeAll(): Flow<List<StagePlot>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeForJob(jobId: Long): Flow<List<StagePlot>> =
        dao.observeForJob(jobId).map { list -> list.map { it.toModel() } }

    override fun observeWithItems(plotId: Long): Flow<StagePlot?> =
        combine(dao.observeById(plotId), dao.observeItems(plotId)) { plot, items ->
            plot?.toModel()?.copy(items = items.map { it.toModel() })
        }

    override suspend fun count(): Int = dao.count()

    override suspend fun create(name: String, jobId: Long?): Long {
        val now = System.currentTimeMillis()
        return dao.insertPlot(
            StagePlotEntity(
                jobId = jobId,
                name = name,
                stageWidthMeters = StagePlot.DEFAULT_WIDTH_METERS,
                stageDepthMeters = StagePlot.DEFAULT_DEPTH_METERS,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
    }

    override suspend fun updateDetails(plot: StagePlot) {
        dao.updatePlot(
            id = plot.id,
            name = plot.name,
            width = plot.stageWidthMeters.coerceIn(StagePlot.MIN_METERS, StagePlot.MAX_METERS),
            depth = plot.stageDepthMeters.coerceIn(StagePlot.MIN_METERS, StagePlot.MAX_METERS),
            notes = plot.notes,
            now = System.currentTimeMillis(),
        )
    }

    override suspend fun delete(plotId: Long) {
        dao.deletePlotById(plotId)
    }

    override suspend fun addItem(plotId: Long, item: StageItem): Long {
        val id = dao.upsertItem(
            StageItemEntity(
                stagePlotId = plotId,
                symbol = item.symbol.name,
                label = item.label,
                x = item.x.coerceIn(0f, 1f),
                y = item.y.coerceIn(0f, 1f),
                colorIndex = StageItemColor.coerce(item.colorIndex),
            ),
        )
        dao.touchPlot(plotId, System.currentTimeMillis())
        return id
    }

    override suspend fun moveItem(plotId: Long, itemId: Long, x: Float, y: Float) {
        dao.moveItem(itemId, x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
        dao.touchPlot(plotId, System.currentTimeMillis())
    }

    override suspend fun recolorItem(plotId: Long, itemId: Long, colorIndex: Int) {
        dao.recolorItem(itemId, StageItemColor.coerce(colorIndex))
        dao.touchPlot(plotId, System.currentTimeMillis())
    }

    override suspend fun renameItem(plotId: Long, item: StageItem, label: String) {
        dao.upsertItem(
            StageItemEntity(
                id = item.id,
                stagePlotId = plotId,
                symbol = item.symbol.name,
                label = label,
                x = item.x,
                y = item.y,
                colorIndex = item.colorIndex,
            ),
        )
        dao.touchPlot(plotId, System.currentTimeMillis())
    }

    override suspend fun deleteItem(plotId: Long, itemId: Long) {
        dao.deleteItemById(itemId)
        dao.touchPlot(plotId, System.currentTimeMillis())
    }
}

private fun StagePlotEntity.toModel() = StagePlot(
    id = id,
    jobId = jobId,
    name = name,
    stageWidthMeters = stageWidthMeters,
    stageDepthMeters = stageDepthMeters,
    notes = notes,
    updatedAtEpochMs = updatedAtEpochMs,
)

private fun StageItemEntity.toModel() = StageItem(
    id = id,
    // 知らない名前が入っていたら落とさずマイクとして扱う。
    // 図が1つ開けなくなるより、記号が1つ違う方がはるかにまし
    symbol = runCatching { StageSymbol.valueOf(symbol) }.getOrDefault(StageSymbol.MIC),
    label = label,
    x = x,
    y = y,
    colorIndex = StageItemColor.coerce(colorIndex),
)
