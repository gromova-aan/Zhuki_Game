package com.example.zhuki_game

import android.content.Context

class UserRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun register(user: User): Long {
        val id = db.userDao().insert(user)
        setCurrentUserId(id)
        return id
    }

    suspend fun getAllUsers(): List<User> = db.userDao().getAll()

    suspend fun getUser(id: Long): User? = db.userDao().getById(id)

    fun getCurrentUserId(): Long = prefs.getLong(KEY_CURRENT_USER_ID, NO_USER)

    fun setCurrentUserId(id: Long) {
        prefs.edit().putLong(KEY_CURRENT_USER_ID, id).apply()
    }

    suspend fun saveRecord(score: Int, difficulty: Int): Long? {
        val userId = getCurrentUserId()
        if (userId == NO_USER) return null
        return db.scoreRecordDao().insert(
            ScoreRecord(
                userId = userId,
                score = score,
                difficulty = difficulty,
                dateMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun getRecords(): List<RecordWithUser> =
        db.scoreRecordDao().getAllWithUserNames()

    companion object {
        const val NO_USER = -1L
        private const val PREFS_NAME = "current_user"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
    }
}
