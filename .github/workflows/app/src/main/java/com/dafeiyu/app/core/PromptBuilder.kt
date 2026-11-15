package com.dafeiyu.app.core

import com.dafeiyu.app.data.ApiMessage

/**
 * 上下文分四段拼，不把全部历史硬塞进去：
 *   固定人设 -> 低频事实 -> 高频状态 -> 旧摘要 -> 最近 N 轮
 * 越靠前越稳定，缓存越容易命中。
 */
object PromptBuilder {
    fun build(
        persona: String,
        moodLine: String,
        facts: List<Pair<String, String>>,
        summary: String,
        recent: List<FishMessage>,
        documents: List<FishDocument> = emptyList()
    ): List<ApiMessage> {
        val result = mutableListOf<ApiMessage>()

        result += ApiMessage("system", persona)

        val factText = if (facts.isEmpty()) {
            "（还没记住什么，别硬编）"
        } else {
            facts.joinToString("\n") { "- ${it.first}：${it.second}" }
        }
        result += ApiMessage("system", "【你记得的事】\n$factText")

        // 心情和时间每轮都变，故意放在后面，不破坏最前面那段人设前缀。
        result += ApiMessage("system", "【此刻状态】$moodLine")

        if (summary.isNotBlank()) {
            result += ApiMessage("system", "【更早的聊天摘要】\n$summary")
        }

        if (documents.isNotEmpty()) {
            val names = documents.joinToString("\n") {
                "- ${it.name}" + if (it.attached) "（本轮引用）" else "（暂不引用）"
            }
            result += ApiMessage("system", "【主人给过的文件】\n$names")
        }

        recent.forEach { message ->
            // 文件消息只用于界面展示，历史里跳过；真正内容由 file 块带进去。
            if (message.role == Role.FILE) return@forEach
            result += ApiMessage(
                role = if (message.role == Role.USER) "user" else "assistant",
                content = message.content
            )
        }

        // 最后一条必须是用户消息，否则部分兼容接口会拒绝。
        if (result.last().role != "user") {
            result += ApiMessage("user", "（继续）")
        }

        // 只把当前勾选的文件挂在最后一条 user 消息上：
        // 历史消息保持纯文本，避免同一份文档在多轮里被重复计费、撑爆上下文。
        val attachedIds = documents.filter { it.attached }.map { it.fileId }.take(20)
        if (attachedIds.isNotEmpty()) {
            val lastIndex = result.lastIndex
            result[lastIndex] = result[lastIndex].copy(fileIds = attachedIds)
        }

        return result
    }
}
