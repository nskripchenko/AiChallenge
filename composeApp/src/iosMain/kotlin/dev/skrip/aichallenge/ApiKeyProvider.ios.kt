package dev.skrip.aichallenge

import platform.Foundation.NSProcessInfo

actual fun getApiKey(): String? {
    return NSProcessInfo.processInfo.environment["ANTHROPIC_API_KEY"] as? String
}
