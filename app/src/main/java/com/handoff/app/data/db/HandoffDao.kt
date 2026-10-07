package com.handoff.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HandoffDao {

    @Query("SELECT * FROM handoffs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HandoffEntity>>

    @Query("SELECT * FROM handoffs WHERE id = :id")
    suspend fun getById(id: String): HandoffEntity?

    @Query(
        "SELECT * FROM handoffs WHERE title LIKE '%' || :query || '%' " +
            "OR platform LIKE '%' || :query || '%' ORDER BY createdAt DESC",
    )
    suspend fun search(query: String): List<HandoffEntity>

    @Upsert
    suspend fun upsert(entity: HandoffEntity)

    @Query("DELETE FROM handoffs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM handoffs")
    suspend fun clear()
}
