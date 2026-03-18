package dev.skrip.aichallenge.evaluation

import dev.skrip.aichallenge.model.EvaluationQuestion

/**
 * 10 контрольных вопросов для сравнения Plain vs RAG режимов.
 * Вопросы основаны на документах в папке docs/
 */
object EvaluationQuestions {

    val questions = listOf(
        // Вопросы по coroutines-guide.md
        EvaluationQuestion(
            id = 1,
            question = "What is a coroutine in Kotlin?",
            expectation = "Should explain that coroutine is a lightweight thread, cheap to create, thousands can run without issues",
            expectedSources = listOf("coroutines-guide.md", "What are Coroutines?")
        ),
        EvaluationQuestion(
            id = 2,
            question = "What are the main Kotlin coroutine dispatchers?",
            expectation = "Should mention Dispatchers.Main, Dispatchers.IO, Dispatchers.Default with their purposes",
            expectedSources = listOf("coroutines-guide.md", "Dispatchers")
        ),
        EvaluationQuestion(
            id = 3,
            question = "What is a suspend function?",
            expectation = "Should explain it's a function that can be paused and resumed, called from coroutine or another suspend function",
            expectedSources = listOf("coroutines-guide.md", "Suspend Functions")
        ),

        // Вопросы по kotlin-basics.md
        EvaluationQuestion(
            id = 4,
            question = "What is the difference between val and var in Kotlin?",
            expectation = "val is immutable (read-only), var is mutable",
            expectedSources = listOf("kotlin-basics.md", "Variables")
        ),
        EvaluationQuestion(
            id = 5,
            question = "How does Kotlin handle null safety?",
            expectation = "Should mention nullable types with ?, non-null by default, compile-time null checks",
            expectedSources = listOf("kotlin-basics.md", "Null Safety")
        ),
        EvaluationQuestion(
            id = 6,
            question = "What is a data class in Kotlin?",
            expectation = "Class for holding data, auto-generates equals(), hashCode(), toString(), copy()",
            expectedSources = listOf("kotlin-basics.md", "Data Classes")
        ),

        // Вопросы по UserRepository.kt
        EvaluationQuestion(
            id = 7,
            question = "How does UserRepository fetch a user?",
            expectation = "First checks local database cache, then falls back to API if not found",
            expectedSources = listOf("UserRepository.kt", "class:UserRepository")
        ),
        EvaluationQuestion(
            id = 8,
            question = "What validation does createUser perform?",
            expectation = "Validates email format (checks for @ and .)",
            expectedSources = listOf("UserRepository.kt", "class:UserRepository")
        ),

        // Вопросы по architecture.txt
        EvaluationQuestion(
            id = 9,
            question = "What are the three main layers in the application architecture?",
            expectation = "Presentation Layer, Domain Layer, Data Layer",
            expectedSources = listOf("architecture.txt")
        ),
        EvaluationQuestion(
            id = 10,
            question = "What dependency injection framework is used?",
            expectation = "Koin is used for dependency injection",
            expectedSources = listOf("architecture.txt")
        )
    )

    fun getById(id: Int): EvaluationQuestion? = questions.find { it.id == id }
}
