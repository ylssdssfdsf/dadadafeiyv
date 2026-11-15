package com.dafeiyu.app.data

import com.dafeiyu.app.core.FishMessage
import com.dafeiyu.app.core.Persona
import com.dafeiyu.app.core.Role
import com.dafeiyu.app.data.db.FactEntity
import com.dafeiyu.app.data.db.FishDao
import com.dafeiyu.app.data.db.MessageEntity
import com.dafeiyu.app.data.db.MetaEntity

/**
 * 鱼的记忆。分三块：最近 N 轮原文、结构化事实、更早内容的摘要。
 * 事实记忆目前提供 remember() 供手动/后续接 LLM 抽取；MVP 先把骨架留好。
 */
class MemoryRepository(private val dao: FishDao) {

    suspend fun recent(limit: Int = 16): List<FishMessage> =
        dao.latest(limit).reversed().map { it.toDomain() }

    suspend fun save(message: FishMessage) {
        dao.insert(
            MessageEntity(
                role = message.role.name,
                content = message.content,
                timestamp = message.timestamp,
                proactive = message.proactive
            )
        )
    }

    suspend fun facts(): List<Pair<String, String>> =
        dao.facts().map { it.key to it.value }

    suspend fun remember(key: String, value: String) {
        dao.putFact(FactEntity(key, value, System.currentTimeMillis()))
    }

    suspend fun summary(): String = dao.meta("summary").orEmpty()

    suspend fun setSummary(text: String) {
        dao.putMeta(MetaEntity("summary", text))
    }

    /** 人设存在本机，改完下一轮就生效（会改变缓存前缀，属预期）。 */
    suspend fun persona(): String = dao.meta("persona")
        ?.takeIf { it.isNotBlank() } ?: Persona.DEFAULT

    suspend fun setPersona(text: String) {
        dao.putMeta(MetaEntity("persona", text))
    }

    suspend fun resetPersona() {
        dao.putMeta(MetaEntity("persona", Persona.DEFAULT))
    }

    suspend fun count(): Int = dao.count()

    suspend fun clear() {
        dao.clear()
        dao.clearFacts()
        setSummary("")
        // 不动 persona：性格和记忆是两回事，"重新养一条"不该把人设也冲掉。
    }

    /**
     * 超过 60 条就把最老的一段折成摘要，避免上下文无限膨胀。
     * 想更聪明：把 old 文本丢给 DeepSeek 做一次摘要，再 setSummary 即可。
     */
    suspend fun maybeSummarize() {
        val all = dao.all()
        if (all.size <= SUMMARY_THRESHOLD) return
        val old = all.take(all.size - KEEP_RECENT)
        if (old.isEmpty()) return
        val folded = old.joinToString("；") {
            (if (it.role == Role.USER.name) "你说" else "鱼说") + "：" + it.content.take(60)
        }
        setSummary((summary() + " " + folded).take(MAX_SUMMARY))
        dao.deleteOldest(old.size)
    }

    private fun MessageEntity.toDomain() = FishMessage(
        id = id,
        role = when (role) {
            Role.USER.name -> Role.USER
            Role.FILE.name -> Role.FILE
            else -> Role.FISH
        },
        content = content,
        timestamp = timestamp,
        proactive = proactive
    )

    companion object {
        private const val SUMMARY_THRESHOLD = 60
        private const val KEEP_RECENT = 20
        private const val MAX_SUMMARY = 1200
    }
}
