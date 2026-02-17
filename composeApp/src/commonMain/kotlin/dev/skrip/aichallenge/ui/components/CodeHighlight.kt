package dev.skrip.aichallenge.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Простой парсер и подсветка кода в markdown-стиле
 */
object CodeHighlighter {

    private val codeBlockRegex = Regex("""```(\w+)?\n(.*?)```""", RegexOption.DOT_MATCHES_ALL)
    private val inlineCodeRegex = Regex("""`([^`]+)`""")

    // Цветовая схема для подсветки синтаксиса (темная тема)
    private val keywordColor = Color(0xFF569CD6)      // Синий - ключевые слова
    private val stringColor = Color(0xFFCE9178)       // Оранжевый - строки
    private val commentColor = Color(0xFF6A9955)      // Зеленый - комментарии
    private val numberColor = Color(0xFFB5CEA8)       // Светло-зеленый - числа
    private val functionColor = Color(0xFFDCDCAA)     // Желтый - функции
    private val typeColor = Color(0xFF4EC9B0)         // Бирюзовый - типы
    private val codeBackground = Color(0xFF1E1E1E)    // Фон кода

    // Ключевые слова по языкам
    private val kotlinKeywords = setOf(
        "fun", "val", "var", "class", "interface", "object", "data", "sealed", "enum",
        "if", "else", "when", "for", "while", "do", "return", "break", "continue",
        "try", "catch", "finally", "throw", "import", "package", "private", "public",
        "protected", "internal", "open", "override", "abstract", "final", "suspend",
        "coroutine", "inline", "crossinline", "noinline", "reified", "companion",
        "init", "constructor", "this", "super", "null", "true", "false", "is", "as",
        "in", "out", "typealias", "by", "where", "annotation", "lateinit", "lazy"
    )

    private val javaKeywords = setOf(
        "public", "private", "protected", "class", "interface", "extends", "implements",
        "static", "final", "void", "int", "long", "double", "float", "boolean", "char",
        "byte", "short", "if", "else", "for", "while", "do", "switch", "case", "default",
        "break", "continue", "return", "try", "catch", "finally", "throw", "throws",
        "new", "this", "super", "null", "true", "false", "import", "package", "instanceof"
    )

    private val pythonKeywords = setOf(
        "def", "class", "if", "elif", "else", "for", "while", "try", "except", "finally",
        "with", "as", "import", "from", "return", "yield", "break", "continue", "pass",
        "raise", "lambda", "and", "or", "not", "in", "is", "True", "False", "None",
        "global", "nonlocal", "assert", "del", "async", "await"
    )

    private val jsKeywords = setOf(
        "function", "const", "let", "var", "if", "else", "for", "while", "do", "switch",
        "case", "default", "break", "continue", "return", "try", "catch", "finally",
        "throw", "new", "this", "class", "extends", "import", "export", "from", "async",
        "await", "null", "undefined", "true", "false", "typeof", "instanceof", "of", "in"
    )

    fun parseAndHighlight(text: String): AnnotatedString {
        return buildAnnotatedString {
            var lastIndex = 0
            val codeBlocks = codeBlockRegex.findAll(text)

            for (match in codeBlocks) {
                // Текст до блока кода
                val beforeCode = text.substring(lastIndex, match.range.first)
                append(highlightInlineCode(beforeCode))

                // Блок кода
                val language = match.groupValues[1].lowercase().ifEmpty { "text" }
                val code = match.groupValues[2].trimEnd()

                // Заголовок языка
                withStyle(SpanStyle(color = Color.Gray, fontFamily = FontFamily.Monospace)) {
                    append("─── $language ───\n")
                }

                // Код с подсветкой
                append(highlightCode(code, language))

                withStyle(SpanStyle(color = Color.Gray, fontFamily = FontFamily.Monospace)) {
                    append("\n───────────\n")
                }

                lastIndex = match.range.last + 1
            }

            // Оставшийся текст
            if (lastIndex < text.length) {
                append(highlightInlineCode(text.substring(lastIndex)))
            }
        }
    }

    private fun highlightInlineCode(text: String): AnnotatedString {
        return buildAnnotatedString {
            var lastIndex = 0

            for (match in inlineCodeRegex.findAll(text)) {
                append(text.substring(lastIndex, match.range.first))

                withStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color(0xFF2D2D2D),
                    color = Color(0xFFE06C75)
                )) {
                    append(match.groupValues[1])
                }

                lastIndex = match.range.last + 1
            }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }

    private fun highlightCode(code: String, language: String): AnnotatedString {
        val keywords = when (language) {
            "kotlin", "kt" -> kotlinKeywords
            "java" -> javaKeywords
            "python", "py" -> pythonKeywords
            "javascript", "js", "typescript", "ts" -> jsKeywords
            else -> emptySet()
        }

        return buildAnnotatedString {
            val lines = code.lines()
            lines.forEachIndexed { index, line ->
                append(highlightLine(line, keywords, language))
                if (index < lines.size - 1) append("\n")
            }
        }
    }

    private fun highlightLine(line: String, keywords: Set<String>, language: String): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            val codeStyle = SpanStyle(fontFamily = FontFamily.Monospace)

            while (i < line.length) {
                when {
                    // Однострочные комментарии
                    (language in listOf("kotlin", "kt", "java", "javascript", "js", "typescript", "ts") &&
                            line.substring(i).startsWith("//")) ||
                            (language in listOf("python", "py") && line.substring(i).startsWith("#")) -> {
                        withStyle(codeStyle.merge(SpanStyle(color = commentColor))) {
                            append(line.substring(i))
                        }
                        i = line.length
                    }

                    // Строки в двойных кавычках
                    line[i] == '"' -> {
                        val endIndex = findClosingQuote(line, i, '"')
                        withStyle(codeStyle.merge(SpanStyle(color = stringColor))) {
                            append(line.substring(i, endIndex + 1))
                        }
                        i = endIndex + 1
                    }

                    // Строки в одинарных кавычках
                    line[i] == '\'' -> {
                        val endIndex = findClosingQuote(line, i, '\'')
                        withStyle(codeStyle.merge(SpanStyle(color = stringColor))) {
                            append(line.substring(i, endIndex + 1))
                        }
                        i = endIndex + 1
                    }

                    // Числа
                    line[i].isDigit() -> {
                        val start = i
                        while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i] == 'f' || line[i] == 'L')) {
                            i++
                        }
                        withStyle(codeStyle.merge(SpanStyle(color = numberColor))) {
                            append(line.substring(start, i))
                        }
                    }

                    // Идентификаторы и ключевые слова
                    line[i].isLetter() || line[i] == '_' -> {
                        val start = i
                        while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) {
                            i++
                        }
                        val word = line.substring(start, i)

                        val color = when {
                            word in keywords -> keywordColor
                            word.first().isUpperCase() -> typeColor
                            i < line.length && line[i] == '(' -> functionColor
                            else -> Color.White
                        }

                        withStyle(codeStyle.merge(SpanStyle(
                            color = color,
                            fontWeight = if (word in keywords) FontWeight.Bold else FontWeight.Normal
                        ))) {
                            append(word)
                        }
                    }

                    // Прочие символы
                    else -> {
                        withStyle(codeStyle.merge(SpanStyle(color = Color.White))) {
                            append(line[i])
                        }
                        i++
                    }
                }
            }
        }
    }

    private fun findClosingQuote(line: String, start: Int, quote: Char): Int {
        var i = start + 1
        while (i < line.length) {
            if (line[i] == quote && (i == start + 1 || line[i - 1] != '\\')) {
                return i
            }
            i++
        }
        return line.length - 1
    }
}
