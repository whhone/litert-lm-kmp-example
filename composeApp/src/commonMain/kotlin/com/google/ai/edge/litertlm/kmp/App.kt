package com.google.ai.edge.litertlm.kmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BackendOption(val label: String) {
  CPU("CPU"),
  GPU("GPU");

  fun toBackend(): Backend = when (this) {
    CPU -> Backend.CPU()
    GPU -> Backend.GPU()
  }
}

@Composable
@Preview
fun App() {
  val platform = getPlatformName()
  println("Running on: $platform")

  val modelPath = when (platform) {
    PlatformName.ANDROID -> "/data/local/tmp/model.litertlm"
    PlatformName.DESKTOP -> "/usr/local/google/home/whhone/.litert-lm/models/gemma3-1b/model.litertlm"
    else -> throw IllegalStateException("Unsupported platform: $platform")
  }

  val cacheDir = getCacheDir()

  var selectedBackend by remember { mutableStateOf(BackendOption.CPU) }
  var engine by remember { mutableStateOf<Engine?>(null) }
  var conversation by remember { mutableStateOf<Conversation?>(null) }
  var initStatus by remember { mutableStateOf("Backend: CPU (initializes on first message)") }
  var isInitialized by remember { mutableStateOf(false) }
  var isInitializing by remember { mutableStateOf(false) }

  DisposableEffect(Unit) {
    onDispose {
      if (conversation?.isAlive == true) conversation?.close()
      if (engine?.isInitialized() == true) engine?.close()
      conversation = null
      engine = null
    }
  }

  val coroutineScope = rememberCoroutineScope()

  MaterialTheme {
    var messages by remember { mutableStateOf(listOf<String>()) }
    var currentResponse by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.primaryContainer)
        .safeContentPadding().fillMaxSize(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "Backend:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
          )
          BackendOption.entries.forEach { option ->
            FilterChip(
              selected = selectedBackend == option,
              onClick = {
                if (selectedBackend != option && !isInitializing && !isGenerating) {
                  val oldConv = conversation
                  val oldEng = engine
                  selectedBackend = option
                  conversation = null
                  engine = null
                  isInitialized = false
                  messages = emptyList()
                  currentResponse = ""
                  initStatus = "Backend: ${option.label} (initializes on first message)"
                  coroutineScope.launch(Dispatchers.IO) {
                    if (oldConv?.isAlive == true) oldConv.close()
                    if (oldEng?.isInitialized() == true) oldEng.close()
                  }
                }
              },
              enabled = !isInitializing && !isGenerating,
              label = { Text(option.label) },
            )
          }
        }

        Text(
          initStatus,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(start = 8.dp),
        )
      }

      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        itemsIndexed(messages) { index, message ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (index % 2 == 0) Arrangement.End else Arrangement.Start
          ) {
            Text(
              message,
              color = Color.White,
              modifier = Modifier
                .background(
                  if (index % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                  shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
            )
          }
        }

        if (currentResponse != "") {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Start
            ) {
              Text(
                currentResponse,
                color = Color.White,
                modifier = Modifier
                  .background(
                    MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(8.dp)
                  )
                  .padding(8.dp)
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = input,
          onValueChange = { input = it },
          modifier = Modifier.weight(1f),
          placeholder = { Text(if (isInitializing) initStatus else "Type a message...") },
          enabled = !isInitializing && !isGenerating,
        )
        Button(
          onClick = {
            val prompt = input.trim()
            if (prompt.isNotBlank() && !isInitializing && !isGenerating) {
              val targetBackend = selectedBackend
              input = ""
              messages = messages + prompt
              isGenerating = true
              currentResponse = ""
              coroutineScope.launch {
                try {
                  var activeConv = conversation
                  if (activeConv == null) {
                    isInitializing = true
                    initStatus = "Initializing model (${targetBackend.label})..."
                    var localEngine: Engine? = null
                    var localConversation: Conversation? = null
                    try {
                      val (eng, conv) = withContext(Dispatchers.IO) {
                        val e = Engine(
                          EngineConfig(
                            modelPath = modelPath,
                            backend = targetBackend.toBackend(),
                            cacheDir = cacheDir,
                          )
                        )
                        localEngine = e
                        e.initialize()
                        val c = e.createConversation(
                          ConversationConfig(systemInstruction = Contents.of("No emoji."))
                        )
                        localConversation = c
                        e to c
                      }
                      engine = eng
                      conversation = conv
                      activeConv = conv
                      isInitialized = true
                      initStatus = "Ready (${targetBackend.label})"
                    } catch (e: CancellationException) {
                      if (localConversation?.isAlive == true) localConversation.close()
                      if (localEngine?.isInitialized() == true) localEngine.close()
                      throw e
                    } catch (e: Exception) {
                      if (localConversation?.isAlive == true) localConversation.close()
                      if (localEngine?.isInitialized() == true) localEngine.close()
                      initStatus = "Model error (${targetBackend.label}): ${e.message}"
                      throw e
                    } finally {
                      isInitializing = false
                    }
                  }

                  activeConv.sendMessageAsync(prompt)
                    .flowOn(Dispatchers.IO)
                    .collect { token ->
                      currentResponse += token.toString()
                    }
                  messages = messages + currentResponse.trim()
                  currentResponse = ""
                } catch (e: CancellationException) {
                  throw e
                } catch (e: Exception) {
                  messages = messages + "Error: ${e.message}"
                  currentResponse = ""
                } finally {
                  isGenerating = false
                }
              }
            }
          },
          enabled = !isInitializing && !isGenerating && input.isNotBlank(),
          modifier = Modifier.padding(start = 8.dp)
        ) {
          Text(if (isInitializing || isGenerating) "..." else "Send")
        }
      }
    }
  }
}
