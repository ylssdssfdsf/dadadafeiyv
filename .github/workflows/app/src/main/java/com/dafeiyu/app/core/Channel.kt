package com.dafeiyu.app.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class Role { USER, FISH, FILE }

data class FishMessage(
    val id: Long = 0L,
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val proactive: Boolean = false
)

data class FishDocument(
    val id: Long,
    val fileId: String,
    val name: String,
    val mime: String,
    val size: Long,
    val purpose: String,
    val addedAt: Long,
    val attached: Boolean = true
)

data class RenderState(
    val messages: List<FishMessage> = emptyList(),
    val streaming: String? = null,
    val error: String? = null,
    val isGenerating: Boolean = false,
    val moodLabel: String = "躺平中",
    val documents: List<FishDocument> = emptyList(),
    val isUploading: Boolean = false,
    val uploadError: String? = null
)

/**
 * 一条「嘴」。
 *
 * 内核只认这个接口：incoming 是有人对鱼说了话，render 是鱼把话显示出去。
 * 以后想加 QQ Bot / 微信 / 通知桥，只要再写一个实现，AgentKernel 一行都不用改。
 */
interface Channel {
    val incoming: Flow<FishMessage>
    val state: StateFlow<RenderState>

    fun submit(text: String)

    suspend fun render(state: RenderState)
}
