package com.dafeiyu.app.channel

import com.dafeiyu.app.core.Channel
import com.dafeiyu.app.core.FishMessage
import com.dafeiyu.app.core.RenderState
import com.dafeiyu.app.core.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/** 鱼住在 App 里的那张嘴。换平台时照着这个再写一个 Channel 实现就行。 */
class InAppChannel : Channel {

    private val _incoming = MutableSharedFlow<FishMessage>(extraBufferCapacity = 16)
    override val incoming: Flow<FishMessage> = _incoming.asSharedFlow()

    private val _state = MutableStateFlow(RenderState())
    override val state: StateFlow<RenderState> = _state

    override fun submit(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            _incoming.tryEmit(FishMessage(role = Role.USER, content = trimmed))
        }
    }

    override suspend fun render(state: RenderState) {
        _state.value = state
    }
}
