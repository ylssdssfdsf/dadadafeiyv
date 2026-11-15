package com.dafeiyu.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dafeiyu.app.core.Persona

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: ChatViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val savedPersona by vm.persona.collectAsState()

    var key by rememberSaveable { mutableStateOf(vm.apiKey()) }
    var keySaved by rememberSaveable { mutableStateOf(false) }
    var model by rememberSaveable { mutableStateOf(vm.model()) }
    var modelSaved by rememberSaveable { mutableStateOf(false) }
    var proactive by rememberSaveable { mutableStateOf(vm.proactiveEnabled()) }
    var richLayout by rememberSaveable { mutableStateOf(vm.richLayout()) }
    var draft by rememberSaveable { mutableStateOf(savedPersona) }
    var personaSaved by rememberSaveable { mutableStateOf(false) }
    var cleared by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(savedPersona) { draft = savedPersona }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("鱼的设置") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("接入", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = key,
                onValueChange = { key = it; keySaved = false },
                label = { Text("DeepSeek API Key") },
                placeholder = { Text("sk-...") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { vm.saveApiKey(key); keySaved = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (keySaved) "已保存到本机" else "保存 Key") }

            OutlinedTextField(
                value = model,
                onValueChange = { model = it; modelSaved = false },
                label = { Text("模型名") },
                supportingText = {
                    Text("当前官方默认是 deepseek-flash；旧名 deepseek-chat 已退役")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { vm.saveModel(model); modelSaved = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (modelSaved) "模型已保存" else "保存模型名") }

            HorizontalDivider()

            Text("性格", style = MaterialTheme.typography.titleMedium)
            Text(
                "直接改下面的文字，下一轮对话生效。改人设会改变提示词前缀，属正常现象。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Persona.PRESETS.forEach { (label, text) ->
                    OutlinedButton(onClick = { draft = text; personaSaved = false }) {
                        Text(label)
                    }
                }
                OutlinedButton(onClick = { vm.resetPersona(); personaSaved = false }) {
                    Text("恢复默认")
                }
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it; personaSaved = false },
                label = { Text("人设") },
                minLines = 8,
                maxLines = 18,
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
            )
            Button(
                onClick = { vm.savePersona(draft); personaSaved = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (personaSaved) "性格已保存" else "保存性格") }

            HorizontalDivider()

            Text("文件", style = MaterialTheme.typography.titleMedium)
            Text(
                "支持 PDF / Word / PPT / Excel / 电子书 / 纯文本 / 图片。可分享到本 App，或点聊天页的 + 选择。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "注意：文件会上传到 DeepSeek 云端解析，默认永久保存在你的账号下，同一账号的所有 Key 都能引用。删除按钮会同时删云端。敏感文件请勿上传。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("图文混排解析", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "关：PDF/Word/PPT 只给模型文字；开：连图表一起给（更慢更贵）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = richLayout,
                    onCheckedChange = { richLayout = it; vm.setRichLayout(it) }
                )
            }
            if (state.documents.isEmpty()) {
                Text(
                    "（还没有文件）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.documents.forEach { document ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(document.name, maxLines = 1)
                            Text(
                                purposeLabel(document.purpose),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { vm.removeDocument(document.id) }) {
                            Text("删除")
                        }
                    }
                }
                OutlinedButton(
                    onClick = { vm.clearAllFiles() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("删除全部文件（含云端）") }
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("主动找你聊", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "每 2 小时按概率开口，不调用模型",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = proactive,
                    onCheckedChange = { proactive = it; vm.setProactive(it) }
                )
            }

            HorizontalDivider()

            Text("重新养一条", style = MaterialTheme.typography.titleMedium)
            Text(
                "清空聊天记录、事实记忆和旧摘要；人设与已上传文件不受影响。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { vm.clearMemory(); cleared = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (cleared) "记忆已清空" else "清空记忆") }
        }
    }
}

private fun purposeLabel(purpose: String): String = when (purpose) {
    "text_recognition" -> "仅解析文字"
    "rich_layout_recognition" -> "图文混排"
    else -> "原文件 / 图片"
}
