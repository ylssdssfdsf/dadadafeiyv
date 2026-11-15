package com.dafeiyu.app.data.files

import android.content.Context
import android.net.Uri
import com.dafeiyu.app.data.SecurePrefs
import com.dafeiyu.app.data.db.FishDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileRepository(
    context: Context,
    private val dao: FishDao,
    private val prefs: SecurePrefs
) {
    private val api = DeepSeekFiles(context.applicationContext, prefs)

    suspend fun list(): List<DocumentEntity> = withContext(Dispatchers.IO) {
        dao.documents()
    }

    suspend fun add(uri: Uri, richLayout: Boolean): DocumentEntity =
        withContext(Dispatchers.IO) {
            val entity = api.upload(uri, richLayout)
            val rowId = dao.insertDocument(entity)
            entity.copy(id = rowId)
        }

    suspend fun remove(entity: DocumentEntity) = withContext(Dispatchers.IO) {
        api.delete(entity.fileId)
        dao.deleteDocument(entity.id)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dao.documents().forEach { api.delete(it.fileId) }
        dao.clearDocuments()
    }
}
