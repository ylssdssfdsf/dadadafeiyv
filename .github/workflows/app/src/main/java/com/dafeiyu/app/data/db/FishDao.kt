package com.dafeiyu.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dafeiyu.app.data.files.DocumentEntity

@Dao
interface FishDao {

    @Query("SELECT * FROM messages ORDER BY timestamp ASC, id ASC")
    suspend fun all(): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC, id DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<MessageEntity>

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun count(): Int

    @Query("DELETE FROM messages")
    suspend fun clear()

    @Query("DELETE FROM facts")
    suspend fun clearFacts()

    @Query("DELETE FROM meta")
    suspend fun clearMeta()

    @Query("DELETE FROM messages WHERE id IN (SELECT id FROM messages ORDER BY timestamp ASC, id ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    @Query("SELECT * FROM facts ORDER BY timestamp DESC")
    suspend fun facts(): List<FactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putFact(fact: FactEntity)

    @Query("SELECT value FROM meta WHERE `key` = :key LIMIT 1")
    suspend fun meta(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMeta(meta: MetaEntity)

    @Query("SELECT * FROM documents ORDER BY addedAt DESC")
    suspend fun documents(): List<DocumentEntity>

    @Insert
    suspend fun insertDocument(document: DocumentEntity): Long

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: Long)

    @Query("DELETE FROM documents")
    suspend fun clearDocuments()
}
