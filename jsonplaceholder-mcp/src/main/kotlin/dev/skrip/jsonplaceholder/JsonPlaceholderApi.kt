package dev.skrip.jsonplaceholder

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object JsonPlaceholderApi {

    private const val BASE_URL = "https://jsonplaceholder.typicode.com"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getPost(id: Int): Post {
        return client.get("$BASE_URL/posts/$id").body()
    }

    suspend fun getUser(id: Int): User {
        return client.get("$BASE_URL/users/$id").body()
    }

    fun close() {
        client.close()
    }
}
