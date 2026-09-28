package com.example.zhuki_game

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

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

    @Update
    suspend fun update(record: ScoreRecord)

    @Query(
        """SELECT records.id, records.userId, records.score,
                  records.difficulty, records.dateMillis
           FROM records INNER JOIN users ON users.id = records.userId
           WHERE users.fullName = :fullName
             AND records.difficulty = :difficulty
           LIMIT 1"""
    )
    suspend fun findByFullNameAndDifficulty(
        fullName: String,
        difficulty: Int
    ): ScoreRecord?

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
