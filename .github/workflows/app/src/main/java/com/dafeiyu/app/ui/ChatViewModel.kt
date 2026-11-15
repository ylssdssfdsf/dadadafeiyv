package com.dafeiyu.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dafeiyu.app.channel.InAppChannel
import com.dafeiyu.app.core.AgentKernel
import com.dafeiyu.app.core.FishMessage
import com.dafeiyu.app.core.MoodEngine
import com.dafeiyu.app.core.Persona
import com.dafeiyu.app.core.RenderState
import com.dafeiyu.app.core.Role
import com.dafeiyu.app.data.DeepSeekClient
import com.dafeiyu.app.data.MemoryRepository
import com.dafeiyu.app.data.SecurePrefs
import com.dafeiyu.app.data.db.AppDatabase
import com.dafeiyu.app.data.files.FileRepository
import com.dafeiyu.app.proactive.ProactiveWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = SecurePrefs(app)
    private val dao = AppDatabase.get(app).fishDao()
    private val memory = MemoryRepository(dao)
    private val files = FileRepository(app, dao, prefs)
    private val client = DeepSeekClient(
        apiKeyProvider = { prefs.apiKey() },
        modelProvider = { prefs.model() }
    )
    private val channel = InAppChannel()
    private val kernel = AgentKernel(client, memory, channel, MoodEngine())

    // 本轮不引用的文件 id。只存内存，不落库。
    private val detachedIds = mutableSetOf<Long>()

    val state = channel.state

    private val _persona = MutableStateFlow(Persona.DEFAULT)
    val persona: StateFlow<String> = _persona

    init {
        viewModelScope.launch {
            val history: List<FishMessage> = runCatching { memory.recent(60) }
                .getOrDefault(emptyList())
            val documents = runCatching { files.list() }.getOrDefault(emptyList())
            _persona.value = memory.persona()
            channel.render(
                RenderState(
                    messages = history,
                    documents = documents.map { it.toCore() }
                )
            )
            kernel.observeIncoming()
        }
    }

    fun send(text: String) {
        if (state.value.isGenerating) return
        if (!prefs.hasApiKey()) {
            viewModelScope.launch {
                channel.render(state.value.copy(error = "先去右上角「设置」填 DeepSeek API Key"))
            }
            return
        }
        channel.submit(text)
    }

    fun dismissError() {
        viewModelScope.launch { channel.render(state.value.copy(error = null)) }
    }

    fun dismissUploadError() {
        viewModelScope.launch { channel.render(state.value.copy(uploadError = null)) }
    }

    /** 系统分享进来或文件选择器选中的文件，都走这里。 */
    fun ingestFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (!prefs.hasApiKey()) {
            viewModelScope.launch {
                channel.render(state.value.copy(error = "要先填 API Key 才能上传文件"))
            }
            return
        }
        viewModelScope.launch {
            channel.render(state.value.copy(isUploading = true, uploadError = null))
            val failures = mutableListOf<String>()
            for (uri in uris) {
                runCatching { files.add(uri, prefs.richLayout()) }
                    .onSuccess { entity ->
                        val notice = FishMessage(
                            role = Role.FILE,
                            content = "收到文件：${entity.name}（${formatSize(entity.size)}）"
                        )
                        memory.save(notice)
                        channel.render(
                            state.value.copy(
                                messages = state.value.messages + notice,
                                documents = state.value.documents + entity.toCore()
                            )
                        )
                    }
                    .onFailure { error ->
                        failures += error.message?.take(80) ?: "有个文件没传上去"
                    }
            }
            channel.render(
                state.value.copy(
                    isUploading = false,
                    uploadError = failures.joinToString("；").takeIf { it.isNotEmpty() }
                )
            )
        }
    }

    fun toggleAttached(id: Long) {
        if (!detachedIds.remove(id)) detachedIds.add(id)
        viewModelScope.launch {
            channel.render(
                state.value.copy(
                    documents = state.value.documents.map {
                        it.copy(attached = it.id !in detachedIds)
                    }
                )
            )
        }
    }

    fun removeDocument(id: Long) {
        viewModelScope.launch {
            runCatching {
                files.list().firstOrNull { it.id == id }?.let { files.remove(it) }
            }
            detachedIds.remove(id)
            channel.render(
                state.value.copy(documents = state.value.documents.filter { it.id != id })
            )
        }
    }

    fun clearAllFiles() {
        viewModelScope.launch {
            runCatching { files.clear() }
            detachedIds.clear()
            channel.render(state.value.copy(documents = emptyList()))
        }
    }

    fun apiKey(): String = prefs.apiKey()

    fun saveApiKey(key: String) {
        prefs.saveApiKey(key)
    }

    fun model(): String = prefs.model()

    fun saveModel(value: String) {
        prefs.setModel(value)
    }

    fun richLayout(): Boolean = prefs.richLayout()

    fun setRichLayout(enabled: Boolean) {
        prefs.setRichLayout(enabled)
    }

    fun savePersona(text: String) {
        viewModelScope.launch {
            memory.setPersona(text)
            _persona.value = text
        }
    }

    fun resetPersona() {
        viewModelScope.launch {
            memory.resetPersona()
            _persona.value = Persona.DEFAULT
        }
    }

    fun proactiveEnabled(): Boolean = prefs.proactiveEnabled()

    fun setProactive(enabled: Boolean) {
        prefs.setProactiveEnabled(enabled)
        if (enabled) {
            ProactiveWorker.schedule(getApplication<Application>())
        } else {
            ProactiveWorker.cancel(getApplication<Application>())
        }
    }

    fun clearMemory() {
        viewModelScope.launch {
            memory.clear()
            channel.render(RenderState(documents = state.value.documents))
        }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 0 -> "大小未知"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
