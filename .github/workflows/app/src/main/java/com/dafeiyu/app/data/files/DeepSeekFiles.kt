package com.dafeiyu.app.data.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.dafeiyu.app.data.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class FileMeta(
    val name: String,
    val mime: String,
    val size: Long,
    val purpose: String
)

/**
 * DeepSeek 官方 Files API。
 *
 * 上传时 purpose 必须排在 file 字段之前（PDF/Word/PPT 用 user_data 会返回 400）。
 * 文档解析在上传过程中完成，可能超过 120 秒，所以这个 client 的读写超时单独放宽。
 */
class DeepSeekFiles(
    private val context: Context,
    private val prefs: SecurePrefs
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    suspend fun upload(uri: Uri, richLayout: Boolean): DocumentEntity =
        withContext(Dispatchers.IO) {
            val meta = resolveMeta(uri, richLayout)
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("purpose", meta.purpose)
                .addFormDataPart("file", meta.name, UriRequestBody(context, uri, meta))
                .build()

            val request = Request.Builder()
                .url(FILES_URL)
                .header("Authorization", "Bearer ${prefs.apiKey()}")
                .post(multipart)
                .build()

            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("上传失败 ${response.code}：${text.take(200)}")
                }
                val fileId = JSONObject(text).optString("id")
                if (fileId.isBlank()) throw IllegalStateException("没拿到 file_id")
                DocumentEntity(
                    fileId = fileId,
                    name = meta.name,
                    mime = meta.mime,
                    size = meta.size,
                    purpose = meta.purpose,
                    addedAt = System.currentTimeMillis()
                )
            }
        }

    suspend fun delete(fileId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$FILES_URL/$fileId")
            .header("Authorization", "Bearer ${prefs.apiKey()}")
            .delete()
            .build()
        runCatching { http.newCall(request).execute().use { } }
    }

    private fun resolveMeta(uri: Uri, richLayout: Boolean): FileMeta {
        val resolver = context.applicationContext.contentResolver
        var name = "未命名文件"
        var size = -1L

        runCatching {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) cursor.getString(nameIndex)?.let { name = it }
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        }

        if (size < 0) {
            size = runCatching {
                resolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                    }
                    total
                } ?: -1L
            }.getOrDefault(-1L)
        }

        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val extension = name.substringAfterLast('.', "").lowercase()
        val isParsedDocument = extension in PARSED_EXTENSIONS || mime in PARSED_MIMES
        val purpose = when {
            isParsedDocument && richLayout -> "rich_layout_recognition"
            isParsedDocument -> "text_recognition"
            else -> "user_data"
        }
        return FileMeta(name, mime, size, purpose)
    }

    private class UriRequestBody(
        private val context: Context,
        private val uri: Uri,
        private val meta: FileMeta
    ) : RequestBody() {
        override fun contentType() = meta.mime.toMediaTypeOrNull()
        override fun contentLength(): Long = if (meta.size >= 0) meta.size else -1L
        override fun writeTo(sink: BufferedSink) {
            val stream = context.applicationContext.contentResolver.openInputStream(uri)
                ?: throw java.io.IOException("打不开这个文件")
            stream.source().use { source -> sink.writeAll(source) }
        }
    }

    companion object {
        private const val FILES_URL = "https://api.deepseek.com/files"
        private val PARSED_EXTENSIONS = setOf("pdf", "doc", "docx", "ppt", "pptx")
        private val PARSED_MIMES = setOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        )
    }
}
