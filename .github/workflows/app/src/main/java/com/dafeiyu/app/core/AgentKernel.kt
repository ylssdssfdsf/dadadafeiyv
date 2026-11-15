package com.dafeiyu.app.core

import com.dafeiyu.app.data.DeepSeekClient
import com.dafeiyu.app.data.MemoryRepository
import kotlinx.coroutines.flow.collect

/**
 * 鱼本体。跟平台完全无关：拿到一句话，拼上下文，流式生成，写回记忆和通道。
 */
class AgentKernel(
    private val client: DeepSeekClient,
    private val memory: MemoryRepository,
    private val channel: Channel,
    private val mood: MoodEngine = MoodEngine()
) {
    suspend fun observeIncoming() {
        channel.incoming.collect { message -> respondTo(message) }
    }

    private suspend fun respondTo(userMessage: FishMessage) {
        val before = channel.state.value
        memory.save(userMessage)
        channel.render(
            before.copy(
                messages = before.messages + userMessage,
                streaming = "",
                isGenerating = true,
                error = null,
                moodLabel = mood.snapshot().label
            )
        )

        val prompt = PromptBuilder.build(
            persona = memory.persona(),
            moodLine = mood.statusLine(),
            facts = memory.facts(),
            summary = memory.summary(),
            recent = memory.recent(RECENT_LIMIT),
            documents = channel.state.value.documents
        )

        val buffer = StringBuilder()
        try {
            client.streamChat(prompt).collect { chunk ->
                buffer.append(chunk)
                channel.render(channel.state.value.copy(streaming = buffer.toString()))
            }

            val reply = FishMessage(
                role = Role.FISH,
                content = buffer.toString().ifBlank { "……（鱼卡壳了）" }
            )
            memory.save(reply)
            mood.drift()
            memory.maybeSummarize()

            channel.render(
                channel.state.value.copy(
                    messages = channel.state.value.messages + reply,
                    streaming = null,
                    isGenerating = false
                )
            )
        } catch (e: Exception) {
            channel.render(
                channel.state.value.copy(
                    streaming = null,
                    isGenerating = false,
                    error = e.message ?: "网络出问题了"
                )
            )
        }
    }

    companion object {
        private const val RECENT_LIMIT = 16
    }
}
