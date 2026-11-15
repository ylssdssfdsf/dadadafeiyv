package com.dafeiyu.app.data

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.io.buffered
import kotlin.io.useLines

data class ApiMessage(
    val role: String,
    val content: String,
    val fileIds: List<String> = emptyList()
)

private const val DEFAULT_MODEL = "deepseek-flash"

/**
 * DeepSeek 是 OpenAI 兼容格式，这里手写 SSE 解析，不引入额外依赖。
 * 只负责「把 messages 变成一串增量文字」，不关心人设、记忆和界面。
 *
 * 2026 年 7 月起 deepseek-chat 已退役；当前默认模型是 deepseek-flash。
 * 模型名做成可配置，以后官方再换名字不用改代码。
 */
class DeepSeekClient(
    private val apiKeyProvider: () -> String,
    private val modelProvider: () -> String = { DEFAULT_MODEL }
) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun streamChat(messages: List<ApiMessage>): Flow<String> = callbackFlow {
        val key = apiKeyProvider().trim()
        if (key.isEmpty()) {
            close(IllegalStateException("还没填 DeepSeek API Key"))
            return@callbackFlow
        }

        val array = JSONArray()
        messages.forEach { message ->
            val item = JSONObject().put("role", message.role)
            if (message.fileIds.isEmpty()) {
                item.put("content", message.content)
            } else {
                // 带文件时 content 必须是块数组；file 块引用 Files API 返回的 file_id。
                val blocks = JSONArray()
                blocks.put(JSONObject().put("type", "text").put("text", message.content))
                message.fileIds.forEach { id ->
                    blocks.put(JSONObject().put("type", "file").put("file_id", id))
                }
                item.put("content", blocks)
            }
            array.put(item)
        }
        val body = JSONObject()
            .put("model", modelProvider().trim().ifBlank { DEFAULT_MODEL })
            .put("messages", array)
            .put("stream", true)
            .toString()

        val request = Request.Builder()
            .url(ENDPOINT)
            .header("Authorization", "Bearer $key")
            .header("Accept", "text/event-stream")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val call = http.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        val detail = response.body?.string()?.take(300).orEmpty()
                        close(IOException("DeepSeek ${response.code}: $detail"))
                        return
                    }
                    val reader = response.body?.charStream()
                    if (reader == null) {
                        close(IOException("空响应"))
                        return
                    }
                    reader.buffered().useLines { lines ->
                        for (line in lines) {
                            if (!line.startsWith("data:")) continue
                            val payload = line.removePrefix("data:").trim()
                            if (payload == "[DONE]") break
                            val delta = runCatching {
                                JSONObject(payload)
                                    .optJSONArray("choices")
                                    ?.optJSONObject(0)
                                    ?.optJSONObject("delta")
                                    ?.optString("content")
                                    .orEmpty()
                            }.getOrDefault("")
                            if (delta.isNotEmpty()) trySend(delta)
                        }
                    }
                    close()
                } catch (e: Exception) {
                    close(e)
                } finally {
                    response.close()
                }
            }
        })

        awaitClose { call.cancel() }
    }

    companion object {
        private const val ENDPOINT = "https://api.deepseek.com/chat/completions"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
