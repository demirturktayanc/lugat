package com.lugat.kelime.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import com.lugat.kelime.domain.WordProgress
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val wordId: String,
    val box: Int,
    val dueAt: Long,
    val correct: Int,
    val wrong: Int,
    val lastSeen: Long,
    val favorite: Boolean,
) {
    fun toDomain() = WordProgress(wordId, box, dueAt, correct, wrong, lastSeen, favorite)

    companion object {
        fun from(p: WordProgress) = ProgressEntity(p.wordId, p.box, p.dueAt, p.correct, p.wrong, p.lastSeen, p.favorite)
    }
}

/** Günlük etkinlik: seri, günlük hedef ve haftalık grafik için. day = LocalDate.toEpochDay() */
@Entity(tableName = "activity")
data class ActivityEntity(
    @PrimaryKey val day: Long,
    val answers: Int,
    val correct: Int,
    val learned: Int,
)

@Dao
interface LugatDao {
    @Query("SELECT * FROM progress")
    fun observeProgress(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE wordId = :id")
    suspend fun progress(id: String): ProgressEntity?

    @Upsert
    suspend fun upsertProgress(item: ProgressEntity)

    @Query("SELECT * FROM activity ORDER BY day")
    fun observeActivity(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activity WHERE day = :day")
    suspend fun activity(day: Long): ActivityEntity?

    @Upsert
    suspend fun upsertActivity(item: ActivityEntity)

    @Query("DELETE FROM progress")
    suspend fun clearProgress()

    @Query("DELETE FROM activity")
    suspend fun clearActivity()
}

@Database(entities = [ProgressEntity::class, ActivityEntity::class], version = 1, exportSchema = true)
abstract class LugatDatabase : RoomDatabase() {
    abstract fun dao(): LugatDao
}
