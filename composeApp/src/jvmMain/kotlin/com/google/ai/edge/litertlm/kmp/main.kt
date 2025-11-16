package com.google.ai.edge.litertlm.kmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "LiteRtLmKmp",
    ) {
        App()
    }
}