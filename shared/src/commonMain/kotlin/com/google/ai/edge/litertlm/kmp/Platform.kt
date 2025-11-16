package com.google.ai.edge.litertlm.kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform