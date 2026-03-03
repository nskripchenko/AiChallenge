package dev.skrip.aichallenge.di

import dev.skrip.aichallenge.data.storage.JsonChatHistoryStorage
import dev.skrip.aichallenge.data.storage.JsonMemoryStorage
import dev.skrip.aichallenge.domain.repository.ChatHistoryStorage
import dev.skrip.aichallenge.domain.repository.MemoryStorage
import org.koin.dsl.module

val jvmModule = module {
    single<ChatHistoryStorage> { JsonChatHistoryStorage() }
    single<MemoryStorage> { JsonMemoryStorage() }
}
