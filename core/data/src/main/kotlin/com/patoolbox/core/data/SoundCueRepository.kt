package com.patoolbox.core.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.patoolbox.core.database.dao.SoundCueDao
import com.patoolbox.core.database.entity.SoundCueEntity
import com.patoolbox.core.model.SoundCue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SE パッド。行は Room、音声はアプリ内部のファイル。
 *
 * **取り込み時に必ずアプリ内へコピーする**。選ばれた URI を持ち続ける作りにしないのは、
 * 本番で鳴らないと意味がないから。元ファイルが消される、クラウド上にしか無い、
 * 端末が圏外、共有元の永続権限が失効した——どれも本番中には対処できない。
 * コピーしてしまえば、以後は完全にオフラインで鳴る。
 */
interface SoundCueRepository {

    fun observeAll(): Flow<List<SoundCue>>

    suspend fun count(): Int

    fun fileOf(cue: SoundCue): File

    /**
     * [uri] の音声をアプリ内へ取り込む。
     * @return 登録した行。読めなかった場合は null
     */
    suspend fun import(uri: Uri): SoundCue?

    suspend fun update(cue: SoundCue)

    /** 並べ替え。渡された順に position を振り直す */
    suspend fun reorder(cues: List<SoundCue>)

    suspend fun delete(cue: SoundCue)

    /** DB に行の無いファイルを消す。取り込みの中断で取り残されたものが対象 */
    suspend fun pruneOrphans(): Int
}

@Singleton
class LocalSoundCueRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: SoundCueDao,
) : SoundCueRepository {

    private val directory: File
        get() = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    override fun observeAll(): Flow<List<SoundCue>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun count(): Int = dao.count()

    override fun fileOf(cue: SoundCue): File = File(directory, cue.fileName)

    override suspend fun import(uri: Uri): SoundCue? {
        val displayName = queryDisplayName(uri)
        val importedAt = System.currentTimeMillis()
        // 元の拡張子を残す。MediaPlayer は拡張子を見ないが、書き出したときに
        // 何のファイルか分からなくなるのを避ける
        val extension = displayName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
        val fileName = buildString {
            append("cue_")
            append(importedAt)
            if (extension != null) {
                append('.')
                append(extension.lowercase())
            }
        }
        val target = File(directory, fileName)

        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            target.length()
        }.getOrNull()

        if (copied == null || copied == 0L) {
            target.delete()
            return null
        }

        val cue = SoundCue(
            title = displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                ?: DEFAULT_TITLE,
            fileName = fileName,
            durationMs = durationOf(target),
            sizeBytes = copied,
            position = (dao.maxPosition() ?: -1) + 1,
            colorIndex = ((dao.maxPosition() ?: -1) + 1) % SoundCue.COLOR_COUNT,
            importedAtEpochMs = importedAt,
        )
        return cue.copy(id = dao.insert(cue.toEntity()))
    }

    override suspend fun update(cue: SoundCue) = dao.update(cue.toEntity())

    override suspend fun reorder(cues: List<SoundCue>) {
        cues.forEachIndexed { index, cue -> dao.updatePosition(cue.id, index) }
    }

    override suspend fun delete(cue: SoundCue) {
        // ファイルを先に消す。行だけ残ってもファイルが無ければ一覧で分かるが、
        // 逆（ファイルだけ残る）は利用者からは見えないまま容量を食い続ける
        fileOf(cue).delete()
        dao.deleteById(cue.id)
    }

    override suspend fun pruneOrphans(): Int {
        val known = dao.all().map { it.fileName }.toSet()
        val files = directory.listFiles() ?: return 0
        var removed = 0
        for (file in files) {
            if (file.name !in known && file.delete()) removed++
        }
        return removed
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
    }.getOrNull()

    /**
     * 長さが取れない形式もある（壊れたファイル、対応外のコーデック）。その場合は 0。
     *
     * MediaMetadataRetriever が AutoCloseable になったのは API 29 で、
     * minSdk は 26 なので use{} は使えない。finally で明示的に解放する。
     */
    private fun durationOf(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private companion object {
        const val DIRECTORY = "sound_cues"
        const val DEFAULT_TITLE = "SE"
    }
}

private fun SoundCueEntity.toModel() = SoundCue(
    id = id,
    title = title,
    fileName = fileName,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    position = position,
    loop = loop,
    gain = gain,
    colorIndex = colorIndex,
    importedAtEpochMs = importedAtEpochMs,
)

private fun SoundCue.toEntity() = SoundCueEntity(
    id = id,
    title = title,
    fileName = fileName,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    position = position,
    loop = loop,
    gain = gain,
    colorIndex = colorIndex,
    importedAtEpochMs = importedAtEpochMs,
)
