package dev.skrip.jsonplaceholder

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)

@Serializable
data class Company(
    val name: String,
    val catchPhrase: String,
    val bs: String
)

@Serializable
data class User(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    val company: Company
)
