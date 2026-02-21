package dev.skrip.aichallenge

import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getEnvApiKey(): String? = NSProcessInfo.processInfo.environment["ANTHROPIC_API_KEY"] as? String

actual fun copyToClipboard(text: String) {
    platform.UIKit.UIPasteboard.generalPasteboard.string = text
}