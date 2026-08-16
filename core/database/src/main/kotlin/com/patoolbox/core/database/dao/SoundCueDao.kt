package com.patoolbox.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.patoolbox.core.database.entity.SoundCueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundCueDao {

    @Query("SELECT * FROM sound_cues ORDER BY position ASC, id ASC")
    fun observeAll(): Flow<List<SoundCueEntity>>

    @Query("SELECT * FROM sound_cues ORDER BY position ASC, id ASC")
    suspend fun all(): List<SoundCueEntity>

    /** 無料版のパッド数制限の判定に使う。 */
    @Query("SELECT COUNT(*) FROM sound_cues")
    suspend fun count(): Int

    /** 追加時の並び順を決めるため、末尾の位置を取る。空なら null */
    @Query("SELECT MAX(position) FROM sound_cues")
    suspend fun maxPosition(): Int?

    @Insert
    suspend fun insert(cue: SoundCueEntity): Long

    @Update
    suspend fun update(cue: SoundCueEntity)

    @Query("UPDATE sound_cues SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Query("DELETE FROM sound_cues WHERE id = :id")
    suspend fun deleteById(id: Long)
}
