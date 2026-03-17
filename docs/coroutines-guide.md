# Kotlin Coroutines Guide

Coroutines are a powerful feature in Kotlin for handling asynchronous programming. They provide a way to write asynchronous code that looks sequential.

## What are Coroutines?

A coroutine is a lightweight thread. Unlike regular threads, coroutines are very cheap to create. You can have thousands of coroutines without any performance issues.

## Basic Usage

To start a coroutine, you use a coroutine builder like `launch` or `async`.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    launch {
        delay(1000L)
        println("World!")
    }
    println("Hello,")
}
```

## Suspend Functions

A suspend function is a function that can be paused and resumed. It can only be called from a coroutine or another suspend function.

```kotlin
suspend fun fetchData(): String {
    delay(1000L)
    return "Data loaded"
}
```

## Structured Concurrency

Kotlin coroutines follow structured concurrency principles. This means that new coroutines can only be launched in a specific CoroutineScope.

## Dispatchers

Dispatchers determine what thread the coroutine runs on:
- Dispatchers.Main - main thread for UI
- Dispatchers.IO - for I/O operations
- Dispatchers.Default - for CPU-intensive work

## Error Handling

Exceptions in coroutines are handled using try-catch or supervisorScope for independent child failures.
