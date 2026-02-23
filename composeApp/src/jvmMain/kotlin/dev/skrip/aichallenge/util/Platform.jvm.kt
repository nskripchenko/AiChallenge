package dev.skrip.aichallenge.util

import java.util.UUID

actual fun generateId(): String = UUID.randomUUID().toString()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun getEnvVariable(name: String): String? = System.getenv(name)
