package dev.skrip.aichallenge

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getEnvApiKey(): String? = System.getenv("ANTHROPIC_API_KEY")

actual fun copyToClipboard(text: String) {
    // На Android нужен Context, пока заглушка
}