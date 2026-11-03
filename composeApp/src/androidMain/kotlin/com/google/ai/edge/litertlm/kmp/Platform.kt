package com.google.ai.edge.litertlm.kmp

actual fun getPlatformName(): PlatformName = PlatformName.ANDROID

actual fun getCacheDir(): String? {
    return "/data/data/com.google.ai.edge.litertlm.kmp/cache/"
}
