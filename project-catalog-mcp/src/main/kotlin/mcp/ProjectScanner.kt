package mcp

import java.io.File

/**
 * Scans the parent project directory to extract information about structure and components.
 */
object ProjectScanner {

    // Project root is parent of project-catalog-mcp
    private val projectRoot: File by lazy {
        File(System.getProperty("user.dir")).parentFile ?: File("..").canonicalFile
    }

    private val excludedDirs = setOf(
        "build", ".gradle", ".idea", ".git", ".kotlin",
        "node_modules", "target", "out", ".claude"
    )

    /**
     * Get basic project information.
     */
    fun getProjectInfo(): String = buildString {
        appendLine("=== Project Information ===")
        appendLine()
        appendLine("Name: ${projectRoot.name}")
        appendLine("Path: ${projectRoot.absolutePath}")
        appendLine()

        // Detect project type
        val projectType = detectProjectType()
        appendLine("Type: $projectType")
        appendLine()

        // Main directories
        appendLine("Main directories:")
        projectRoot.listFiles()
            ?.filter { it.isDirectory && it.name !in excludedDirs && !it.name.startsWith(".") }
            ?.sortedBy { it.name }
            ?.forEach { appendLine("  - ${it.name}/") }
        appendLine()

        // Key files
        appendLine("Key files:")
        listOf(
            "settings.gradle.kts", "settings.gradle",
            "build.gradle.kts", "build.gradle",
            "gradle.properties", "README.md"
        ).forEach { fileName ->
            if (File(projectRoot, fileName).exists()) {
                appendLine("  - $fileName")
            }
        }
    }

    /**
     * List all modules in the project.
     */
    fun listModules(): String = buildString {
        appendLine("=== Project Modules ===")
        appendLine()

        // Try to parse settings.gradle.kts
        val settingsFile = File(projectRoot, "settings.gradle.kts")
            .takeIf { it.exists() }
            ?: File(projectRoot, "settings.gradle").takeIf { it.exists() }

        if (settingsFile != null) {
            appendLine("From ${settingsFile.name}:")
            val includePattern = Regex("""include\s*\(\s*["':]+([^"']+)["']\s*\)""")
            settingsFile.readText().lines().forEach { line ->
                includePattern.find(line)?.let { match ->
                    appendLine("  - ${match.groupValues[1]}")
                }
            }
            appendLine()
        }

        appendLine("Directory structure:")
        projectRoot.listFiles()
            ?.filter { it.isDirectory && it.name !in excludedDirs && !it.name.startsWith(".") }
            ?.sortedBy { it.name }
            ?.forEach { dir ->
                val hasGradle = File(dir, "build.gradle.kts").exists() ||
                        File(dir, "build.gradle").exists()
                val marker = if (hasGradle) "[module]" else "[dir]"
                appendLine("  $marker ${dir.name}/")

                // Show submodules if any
                dir.listFiles()
                    ?.filter { it.isDirectory && it.name !in excludedDirs }
                    ?.filter { sub ->
                        File(sub, "build.gradle.kts").exists() ||
                                File(sub, "build.gradle").exists()
                    }
                    ?.forEach { sub ->
                        appendLine("    [submodule] ${sub.name}/")
                    }
            }
    }

    /**
     * Find Compose screens in the project.
     */
    fun listScreens(): String = buildString {
        appendLine("=== Compose Screens ===")
        appendLine()

        val screens = mutableListOf<ScreenInfo>()
        val screenPattern = Regex("""@Composable\s+(?:fun|private\s+fun|internal\s+fun)\s+(\w+Screen)\s*\(""")

        findKotlinFiles(projectRoot).forEach { file ->
            val relativePath = file.relativeTo(projectRoot).path
            val content = file.readText()
            val lines = content.lines()

            lines.forEachIndexed { index, line ->
                if (line.contains("@Composable")) {
                    // Check next few lines for Screen function
                    val searchRange = lines.drop(index).take(3).joinToString("\n")
                    screenPattern.find(searchRange)?.let { match ->
                        screens.add(ScreenInfo(
                            name = match.groupValues[1],
                            file = relativePath,
                            line = index + 1
                        ))
                    }
                }
            }
        }

        if (screens.isEmpty()) {
            appendLine("No Compose screens found.")
            appendLine()
            appendLine("Hint: Screens are detected by pattern: @Composable fun *Screen(...)")
        } else {
            appendLine("Found ${screens.size} screen(s):")
            appendLine()
            screens.sortedBy { it.name }.forEach { screen ->
                appendLine("${screen.name}")
                appendLine("  File: ${screen.file}")
                appendLine("  Line: ${screen.line}")
                appendLine()
            }
        }
    }

    private fun detectProjectType(): String {
        val indicators = mutableListOf<String>()

        // Check for KMP
        val buildFiles = listOf(
            File(projectRoot, "build.gradle.kts"),
            File(projectRoot, "composeApp/build.gradle.kts")
        )

        buildFiles.filter { it.exists() }.forEach { file ->
            val content = file.readText()
            when {
                content.contains("kotlin(\"multiplatform\")") ||
                        content.contains("multiplatform") -> indicators.add("Kotlin Multiplatform")
                content.contains("compose") -> indicators.add("Compose")
            }
        }

        // Check for Android
        if (File(projectRoot, "composeApp/src/androidMain").exists() ||
            File(projectRoot, "app/src/main/AndroidManifest.xml").exists()) {
            indicators.add("Android")
        }

        // Check for iOS
        if (File(projectRoot, "iosApp").exists() ||
            File(projectRoot, "composeApp/src/iosMain").exists()) {
            indicators.add("iOS")
        }

        // Check for Desktop/JVM
        if (File(projectRoot, "composeApp/src/jvmMain").exists() ||
            File(projectRoot, "composeApp/src/desktopMain").exists()) {
            indicators.add("Desktop (JVM)")
        }

        return if (indicators.isNotEmpty()) {
            indicators.joinToString(" + ")
        } else {
            "Kotlin/JVM"
        }
    }

    private fun findKotlinFiles(dir: File): Sequence<File> = sequence {
        dir.listFiles()?.forEach { file ->
            when {
                file.isDirectory && file.name !in excludedDirs && !file.name.startsWith(".") -> {
                    yieldAll(findKotlinFiles(file))
                }
                file.isFile && file.extension == "kt" && !file.path.contains("/build/") -> {
                    yield(file)
                }
            }
        }
    }

    private data class ScreenInfo(
        val name: String,
        val file: String,
        val line: Int
    )
}
