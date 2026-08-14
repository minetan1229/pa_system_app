package com.patoolbox.core.data

import android.content.Context
import com.patoolbox.core.database.dao.RecordingDao
import com.patoolbox.core.database.entity.RecordingEntity
import com.patoolbox.core.model.Recording
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 録音。行は Room、音声はアプリ内部のファイル。
 *
 * 2つに分かれている以上、片方だけが残る状態が必ず起こりうる
 * （書き込み中に電池が切れる、削除の途中で落ちる）。
 * 行の無いファイルは [pruneOrphans] で掃除する。
 * 逆にファイルの無い行は一覧に出したうえで「ファイルがありません」と表示する
 * ——黙って消すと、利用者は録れていたはずの音がどうなったか分からない。
 */
interface RecordingRepository {

    fun observeAll(): Flow<List<Recording>>

    suspend fun count(): Int

    /** これから録る音を書き込むファイル。まだ DB には登録しない */
    fun newFile(startedAtEpochMs: Long): File

    fun fileOf(recording: Recording): File

    suspend fun save(recording: Recording): Long

    suspend fun updateDetails(recording: Recording, title: String, note: String)

    suspend fun delete(recording: Recording)

    /** DB に行の無いファイルを消す。録音の中断で取り残されたものが対象 */
    suspend fun pruneOrphans(): Int
}

@Singleton
class LocalRecordingRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: RecordingDao,
) : RecordingRepository {

    private val directory: File
        get() = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    override fun observeAll(): Flow<List<Recording>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun count(): Int = dao.count()

    override fun newFile(startedAtEpochMs: Long): File =
        File(directory, "rec_$startedAtEpochMs.wav")

    override fun fileOf(recording: Recording): File = File(directory, recording.fileName)

    override suspend fun save(recording: Recording): Long = dao.insert(recording.toEntity())

    override suspend fun updateDetails(recording: Recording, title: String, note: String) {
        dao.updateDetails(recording.id, title, note)
    }

    override suspend fun delete(recording: Recording) {
        // ファイルを先に消す。行だけ残ってもファイルが無ければ一覧で分かるが、
        // 逆（ファイルだけ残る）は利用者からは見えないまま容量を食い続ける
        fileOf(recording).delete()
        dao.deleteById(recording.id)
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

    private companion object {
        const val DIRECTORY = "recordings"
    }
}

private fun RecordingEntity.toModel() = Recording(
    id = id,
    jobId = jobId,
    title = title,
    fileName = fileName,
    startedAtEpochMs = startedAtEpochMs,
    durationSeconds = durationSeconds,
    sampleRate = sampleRate,
    sizeBytes = sizeBytes,
    peakAmplitude = peakAmplitude,
    note = note,
)

private fun Recording.toEntity() = RecordingEntity(
    id = id,
    jobId = jobId,
    title = title,
    fileName = fileName,
    startedAtEpochMs = startedAtEpochMs,
    durationSeconds = durationSeconds,
    sampleRate = sampleRate,
    sizeBytes = sizeBytes,
    peakAmplitude = peakAmplitude,
    note = note,
)
