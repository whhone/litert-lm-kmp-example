package com.google.ai.edge.litertlm.kmp

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.google.ai.edge.litertlm.*

@Composable
@Preview
fun App() {
  val platform = getPlatformName()
  println("Running on: $platform")

  val modelPath = when (platform) {
    PlatformName.ANDROID -> "/data/local/tmp/gemma3-1b-it-int4.litertlm"
    PlatformName.DESKTOP -> "/usr/local/google/home/whhone/models/litertlm/gemma3-1b-it-int4.litertlm"
    else -> throw IllegalStateException("Unsupported platform: $platform")
  }

  val cacheDir = getCacheDir()

  val engine = Engine(
    EngineConfig(
      modelPath = modelPath,
      backend = Backend.CPU,
      cacheDir = cacheDir,
    )
  )
  engine.initialize()
  val conversation =
    engine.createConversation(ConversationConfig(systemMessage = Message.of("No emoji.")))

  MaterialTheme {
    var messages by remember { mutableStateOf(mutableListOf<String>()) }
    var currentResponse by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.primaryContainer)
        .safeContentPadding().fillMaxSize(),
    ) {
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
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
          placeholder = { Text("Type a message...") }
        )
        Button(
          onClick = {
            if (input.isNotBlank()) {
              val message = Message.of(input)

              val callback = object : MessageCallback {
                override fun onMessage(message: Message) {
                  currentResponse += message.toString()
                }

                override fun onDone() {
                  messages.add(currentResponse.trim())
                  currentResponse = ""
                }

                override fun onError(throwable: Throwable) {}
              }

              conversation.sendMessageAsync(message, callback)


              messages = (messages + input).toMutableList()
              // messages.add(input)

              input = ""
            }
          },
          modifier = Modifier.padding(start = 8.dp)
        ) {
          Text("Send")
        }
      }
    }
  }
}
