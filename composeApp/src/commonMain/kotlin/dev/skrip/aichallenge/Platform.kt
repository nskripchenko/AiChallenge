package dev.skrip.aichallenge

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform