package com.google.ai.edge.litertlm.kmp

enum class PlatformName {
    ANDROID, IOS, DESKTOP, JS, WASMJS
}

expect fun getPlatformName(): PlatformName

expect fun getCacheDir(): String?