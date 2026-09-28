package com.example.zhuki_game

import android.content.Context

sealed class RecordSaveResult {
    object NoUser : RecordSaveResult()
    data class Created(val score: Int) : RecordSaveResult()
    data class Beaten(val score: Int) : RecordSaveResult()
    data class Kept(val bestScore: Int) : RecordSaveResult()
}

class UserRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun register(user: User): Long {
        val id = if (user.id == 0L) {
            db.userDao().insert(user)
        } else {
            db.userDao().update(user)
            user.id
        }
        setCurrentUserId(id)
        return id
    }

    suspend fun getAllUsers(): List<User> = db.userDao().getAll()

    suspend fun getUser(id: Long): User? = db.userDao().getById(id)

    fun getCurrentUserId(): Long = prefs.getLong(KEY_CURRENT_USER_ID, NO_USER)

    fun setCurrentUserId(id: Long) {
        prefs.edit().putLong(KEY_CURRENT_USER_ID, id).apply()
    }

    suspend fun saveRecord(score: Int): RecordSaveResult {
        val userId = getCurrentUserId()
        if (userId == NO_USER) return RecordSaveResult.NoUser

        val user = db.userDao().getById(userId) ?: return RecordSaveResult.NoUser
        val dao = db.scoreRecordDao()
        val now = System.currentTimeMillis()

        val existing = dao.findByFullNameAndDifficulty(user.fullName, user.difficulty)
        return if (existing == null) {
            dao.insert(
                ScoreRecord(
                    userId = userId,
                    score = score,
                    difficulty = user.difficulty,
                    dateMillis = now
                )
            )
            RecordSaveResult.Created(score)
        } else if (score > existing.score) {
            dao.update(existing.copy(score = score, dateMillis = now))
            RecordSaveResult.Beaten(score)
        } else {
            RecordSaveResult.Kept(existing.score)
        }
    }

    suspend fun getRecords(): List<RecordWithUser> =
        db.scoreRecordDao().getAllWithUserNames()

    companion object {
        const val NO_USER = -1L
        private const val PREFS_NAME = "current_user"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
    }
}
