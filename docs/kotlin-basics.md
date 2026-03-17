# Kotlin Basics

Kotlin is a modern programming language that makes developers happier. It's concise, safe, and fully interoperable with Java.

## Variables

In Kotlin, you can declare variables using `val` for read-only (immutable) variables and `var` for mutable variables.

```kotlin
val name = "John"  // Immutable
var age = 25       // Mutable
```

## Null Safety

Kotlin's type system is designed to eliminate null pointer exceptions. By default, variables cannot hold null values.

```kotlin
var name: String = "John"
name = null  // Compilation error

var nullableName: String? = "John"
nullableName = null  // OK
```

## Functions

Functions in Kotlin are declared using the `fun` keyword. They can have default parameter values and named arguments.

```kotlin
fun greet(name: String, greeting: String = "Hello"): String {
    return "$greeting, $name!"
}
```

## Data Classes

Data classes are used to hold data. The compiler automatically generates equals(), hashCode(), toString(), and copy() methods.

```kotlin
data class User(val name: String, val age: Int)
```

## Extension Functions

You can extend a class with new functionality without inheriting from it using extension functions.

```kotlin
fun String.addExclamation(): String = this + "!"
```
