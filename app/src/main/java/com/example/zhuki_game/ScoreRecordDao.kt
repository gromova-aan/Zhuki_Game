package com.example.zhuki_game

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

data class RecordWithUser(
    val userName: String,
    val score: Int,
    val difficulty: Int,
    val dateMillis: Long
)

@Dao
interface ScoreRecordDao {

    @Insert
    suspend fun insert(record: ScoreRecord): Long

    @Query(
        """SELECT users.fullName AS userName, records.score,
                  records.difficulty, records.dateMillis
           FROM records INNER JOIN users ON users.id = records.userId
           ORDER BY records.score DESC"""
    )
    suspend fun getAllWithUserNames(): List<RecordWithUser>

    @Query("DELETE FROM records")
    suspend fun clearAll()
}
