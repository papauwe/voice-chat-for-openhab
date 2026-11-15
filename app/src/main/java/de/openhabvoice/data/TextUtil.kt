package de.openhabvoice.data

/**
 * Strips Markdown formatting for text-to-speech (TTS) so that no
 * "asterisks" or syntax characters are read aloud.
 * For display the raw text is kept (Markdown renderer).
 */
fun stripMarkdown(text: String): String {
    var t = text

    // Remove table separator rows (|---|---|) including the line break - BEFORE
    // the line conversion, so no empty line or "---, ---" is left behind
    t = t.replace(Regex("(?m)^\\s*\\|[\\s\\-:|]+\\|\\s*(?:\\r?\\n|$)"), "")
    // Convert table rows into sentences: "| a | b |" -> "a, b" per row
    t = t.replace(Regex("(?m)^\\s*\\|(.*)\\|\\s*$")) { m ->
        m.groupValues[1].split('|').joinToString(", ") { it.trim() }.removeSuffix(", ")
    }
    // Code blocks: keep the content only
    t = t.replace(Regex("```[a-zA-Z]*\\n?([\\s\\S]*?)```")) { it.groupValues[1].trim() }
    // Inline code
    t = t.replace(Regex("`([^`]*)`")) { it.groupValues[1] }
    // Links [text](URL) -> text
    t = t.replace(Regex("\\[([^\\]]+)\\]\\([^)]*\\)")) { it.groupValues[1] }
    // Heading markers
    t = t.replace(Regex("(?m)^#{1,6}\\s+"), "")
    // Bullet markers
    t = t.replace(Regex("(?m)^\\s*[-*+]\\s+"), "")
    // Bold / italic
    t = t.replace(Regex("\\*\\*([^*]+)\\*\\*")) { it.groupValues[1] }
    t = t.replace(Regex("__([^_]+)__")) { it.groupValues[1] }
    t = t.replace(Regex("\\*([^*\\n]+)\\*")) { it.groupValues[1] }
    t = t.replace(Regex("(?<!\\w)_([^_\\n]+)_(?!\\w)")) { it.groupValues[1] }
    // Remove emojis & symbols (TTS would otherwise read out things like "white check mark"):
    // BMP symbols (2600-27BF, 2B00-2BFF, 2190-21FF), variation selector (FE0F),
    // zero-width joiner (200D), lone surrogates (D800-DFFF) and astral emoji (10000-10FFFF).
    // Important: astral emoji (e.g. 💡) as a surrogate pair can only be matched via \x{10000}-\x{10FFFF} -
    // the BMP class [\uD800-\uDFFF] does not match the halves of valid pairs.
    t = t.replace(
        Regex("[\\uD800-\\uDFFF\\u2190-\\u21FF\\u2600-\\u27BF\\u2B00-\\u2BFF\\uFE0F\\u200D\\x{10000}-\\x{10FFFF}]"),
        ""
    )
    // Clean up double spaces after emoji removal
    t = t.replace(Regex("  +"), " ")

    // Collapse duplicate blank lines
    t = t.replace(Regex("\\n{3,}"), "\n\n")

    return t.trim()
}
