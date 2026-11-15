package com.dafeiyu.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dafeiyu.app.MainActivity
import com.dafeiyu.app.core.FishMessage
import com.dafeiyu.app.core.Role
import com.dafeiyu.app.ui.theme.FishColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel, onOpenSettings: () -> Unit) {
    val state by vm.state.collectAsState()
    val activity = LocalContext.current as MainActivity
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, (state.streaming?.length ?: 0) / 20) {
        val lastIndex = state.messages.lastIndex + if (state.streaming != null) 1 else 0
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FishAvatar()
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("大肥鱼", style = MaterialTheme.typography.titleMedium)
                            Text(
                                state.moodLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onOpenSettings) { Text("设置") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            state.error?.let { message ->
                NoticeBar(message) { vm.dismissError() }
            }
            state.uploadError?.let { message ->
                NoticeBar(message) { vm.dismissUploadError() }
            }

            if (state.documents.isNotEmpty() || state.isUploading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "上传解析中…",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    state.documents.forEach { document ->
                        FilterChip(
                            selected = document.attached,
                            onClick = { vm.toggleAttached(document.id) },
                            label = { Text(document.name, maxLines = 1) },
                            trailingIcon = {
                                Text(
                                    "×",
                                    modifier = Modifier.clickable {
                                        vm.removeDocument(document.id)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                itemsIndexed(state.messages) { _, message ->
                    MessageBubble(message)
                }
                if (state.streaming != null) {
                    item {
                        MessageBubble(
                            message = FishMessage(
                                role = Role.FISH,
                                content = state.streaming!!.ifEmpty { "……" }
                            ),
                            isStreaming = true
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { activity.openFilePicker() },
                    enabled = !state.isUploading
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(6.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("跟鱼说点什么…") },
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val text = input
                        input = ""
                        vm.send(text)
                    },
                    enabled = input.isNotBlank() &&
                        !state.isGenerating &&
                        !state.isUploading
                ) {
                    Text(if (state.isGenerating) "…" else "发送")
                }
            }
        }
    }
}

@Composable
private fun NoticeBar(message: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    }
}

@Composable
private fun MessageBubble(message: FishMessage, isStreaming: Boolean = false) {
    if (message.role == Role.FILE) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        return
    }

    val isUser = message.role == Role.USER
    val dark = isSystemInDarkTheme()
    val background = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer
        dark -> FishColors.bubbleDark
        else -> FishColors.bubbleLight
    }
    val foreground = when {
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        dark -> Color(0xFFDCE9FF)
        else -> Color(0xFF17335F)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = background,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(if (isUser) Alignment.CenterEnd else Alignment.CenterStart)
                .widthIn(max = 300.dp)
        ) {
            Text(
                text = message.content + if (isStreaming) " ▍" else "",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = foreground
            )
        }
    }
}
