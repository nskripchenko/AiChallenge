package dev.skrip.aichallenge

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getEnvApiKey(): String?

expect fun copyToClipboard(text: String)