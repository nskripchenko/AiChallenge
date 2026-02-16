package dev.skrip.aichallenge

actual fun getApiKey(): String? = System.getenv("ANTHROPIC_API_KEY")
