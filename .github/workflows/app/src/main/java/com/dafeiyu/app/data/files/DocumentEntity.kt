package com.dafeiyu.app.data.files

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dafeiyu.app.core.FishDocument

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val fileId: String,
    val name: String,
    val mime: String,
    val size: Long,
    val purpose: String,
    val addedAt: Long
) {
    fun toCore(attached: Boolean = true) = FishDocument(
        id = id,
        fileId = fileId,
        name = name,
        mime = mime,
        size = size,
        purpose = purpose,
        addedAt = addedAt,
        attached = attached
    )
}
