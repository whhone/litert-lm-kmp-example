package com.google.ai.edge.litertlm.kmp

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual fun getPlatformName(): PlatformName = PlatformName.ANDROID

actual fun getCacheDir(): String? {
    return "/data/data/com.google.ai.edge.litertlm.kmp/cache/"
}
