package dev.skrip.aichallenge.di

import dev.skrip.aichallenge.data.remote.AnthropicRemoteDataSource
import dev.skrip.aichallenge.data.repository.AnthropicChatAgent
import dev.skrip.aichallenge.data.repository.HaikuContextCompressor
import dev.skrip.aichallenge.data.repository.HaikuFactsExtractor
import dev.skrip.aichallenge.data.source.LlmDataSource
import dev.skrip.aichallenge.domain.repository.ChatAgent
import dev.skrip.aichallenge.domain.repository.ContextCompressor
import dev.skrip.aichallenge.domain.repository.FactsExtractor
import dev.skrip.aichallenge.logging.AgentLogger
import dev.skrip.aichallenge.logging.InMemoryAgentLogger
import dev.skrip.aichallenge.ui.viewmodel.ChatViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging) {
                level = LogLevel.NONE
            }
        }
    }
}

val dataModule = module {
    single<AgentLogger> { InMemoryAgentLogger() }
    single<LlmDataSource> { AnthropicRemoteDataSource(get(), get()) }
    single<ChatAgent> { AnthropicChatAgent(get()) }
    single<ContextCompressor> { HaikuContextCompressor(get()) }
    single<FactsExtractor> { HaikuFactsExtractor(get()) }
}

val viewModelModule = module {
    single { ChatViewModel(get(), get(), get(), get(), get()) }
}

val appModules = listOf(networkModule, dataModule, viewModelModule)
