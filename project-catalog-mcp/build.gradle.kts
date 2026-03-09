plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    application
}

group = "dev.skrip"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // MCP Kotlin SDK
    implementation("io.modelcontextprotocol:kotlin-sdk:0.4.0")

    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("ch.qos.logback:logback-classic:1.5.12")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("mcp.ServerKt")
}

// Task to run the server
tasks.register<JavaExec>("runServer") {
    group = "application"
    description = "Run MCP server (stdio)"
    mainClass.set("mcp.ServerKt")
    classpath = sourceSets["main"].runtimeClasspath
    standardInput = System.`in`
}

// Task to run the client that lists tools
tasks.register<JavaExec>("runListTools") {
    group = "application"
    description = "Run client to list available tools"
    mainClass.set("mcp.ClientKt")
    classpath = sourceSets["main"].runtimeClasspath
}

// Create fat JAR for server
tasks.register<Jar>("serverJar") {
    group = "build"
    description = "Create executable JAR for MCP server"
    archiveBaseName.set("mcp-server")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "mcp.ServerKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get())
}
