package dev.skrip.aichallenge

actual fun getApiKey(): String? = BuildConfig.ANTHROPIC_API_KEY.ifBlank { null }
