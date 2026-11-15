package com.dafeiyu.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val role: String,
    val content: String,
    val timestamp: Long,
    val proactive: Boolean = false
)

@Entity(tableName = "facts")
data class FactEntity(
    @PrimaryKey val key: String,
    val value: String,
    val timestamp: Long
)

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String
)
