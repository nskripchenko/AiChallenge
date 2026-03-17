package com.example.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing user data.
 * Provides methods to fetch, create, update and delete users.
 */
class UserRepository(
    private val apiService: ApiService,
    private val database: UserDatabase
) {
    /**
     * Fetches a user by their ID.
     * First checks the local database, then falls back to API.
     */
    suspend fun getUserById(userId: String): User? {
        return withContext(Dispatchers.IO) {
            // Try local cache first
            database.getUserById(userId) ?: run {
                // Fetch from API if not in cache
                val user = apiService.fetchUser(userId)
                user?.let { database.insertUser(it) }
                user
            }
        }
    }

    /**
     * Creates a new user in the system.
     * Validates the user data before creating.
     */
    suspend fun createUser(name: String, email: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isValidEmail(email)) {
                    return@withContext Result.failure(InvalidEmailException())
                }

                val user = User(
                    id = generateId(),
                    name = name,
                    email = email,
                    createdAt = System.currentTimeMillis()
                )

                apiService.createUser(user)
                database.insertUser(user)
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Updates an existing user's information.
     */
    suspend fun updateUser(user: User): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                apiService.updateUser(user)
                database.updateUser(user)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Deletes a user by their ID.
     */
    suspend fun deleteUser(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deleteUser(userId)
                database.deleteUser(userId)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    private fun generateId(): String {
        return java.util.UUID.randomUUID().toString()
    }
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val createdAt: Long
)

class InvalidEmailException : Exception("Invalid email format")

interface ApiService {
    suspend fun fetchUser(userId: String): User?
    suspend fun createUser(user: User)
    suspend fun updateUser(user: User)
    suspend fun deleteUser(userId: String)
}

interface UserDatabase {
    fun getUserById(userId: String): User?
    fun insertUser(user: User)
    fun updateUser(user: User)
    fun deleteUser(userId: String)
}
