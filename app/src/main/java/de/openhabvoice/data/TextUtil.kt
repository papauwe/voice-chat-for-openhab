package de.openhabvoice.data

/**
 * Entfernt Markdown-Formatierung fuer die Sprachausgabe (TTS),
 * damit keine "Sternchen" oder Syntaxzeichen vorgelesen werden.
 * Fuer die Anzeige wird der Rohtext belassen (Markdown-Renderer).
 */
fun stripMarkdown(text: String): String {
    var t = text

    // Trennzeilen der Tabelle (|---|---|) inkl. Zeilenumbruch entfernen – VOR der
    // Zeilen-Konvertierung, damit keine Leerzeile / kein "---, ---" stehen bleibt
    t = t.replace(Regex("(?m)^\\s*\\|[\\s\\-:|]+\\|\\s*(?:\\r?\\n|$)"), "")
    // Tabellen in Satzbau umwandeln: "| a | b |" -> "a, b" pro Zeile
    t = t.replace(Regex("(?m)^\\s*\\|(.*)\\|\\s*$")) { m ->
        m.groupValues[1].split('|').joinToString(", ") { it.trim() }.removeSuffix(", ")
    }
    // Code-Bloecke: nur Inhalt
    t = t.replace(Regex("```[a-zA-Z]*\\n?([\\s\\S]*?)```")) { it.groupValues[1].trim() }
    // Inline-Code
    t = t.replace(Regex("`([^`]*)`")) { it.groupValues[1] }
    // Links [Text](URL) -> Text
    t = t.replace(Regex("\\[([^\\]]+)\\]\\([^)]*\\)")) { it.groupValues[1] }
    // Ueberschriften-Marker
    t = t.replace(Regex("(?m)^#{1,6}\\s+"), "")
    // Bullet-Marker
    t = t.replace(Regex("(?m)^\\s*[-*+]\\s+"), "")
    // Fett / Kursiv
    t = t.replace(Regex("\\*\\*([^*]+)\\*\\*")) { it.groupValues[1] }
    t = t.replace(Regex("__([^_]+)__")) { it.groupValues[1] }
    t = t.replace(Regex("\\*([^*\\n]+)\\*")) { it.groupValues[1] }
    t = t.replace(Regex("(?<!\\w)_([^_\\n]+)_(?!\\w)")) { it.groupValues[1] }
    // Emoji & Symbole entfernen (TTS liest sonst Dinge wie "weisses Haeckchen" vor):
    // BMP-Symbole (2600-27BF, 2B00-2BFF, 2190-21FF), Variation Selector (FE0F),
    // Zero-Width-Joiner (200D), lone Surrogates (D800-DFFF) und astral Emoji (10000-10FFFF).
    // Wichtig: astral Emoji (z. B. 💡) als Surrogate-Paar nur per \x{10000}-\x{10FFFF} erfassbar –
    // die BMP-Klasse [\uD800-\uDFFF] matcht keine Haelften gueltiger Paare.
    t = t.replace(
        Regex("[\\uD800-\\uDFFF\\u2190-\\u21FF\\u2600-\\u27BF\\u2B00-\\u2BFF\\uFE0F\\u200D\\x{10000}-\\x{10FFFF}]"),
        ""
    )
    // Doppelte Leerzeichen nach Emoji-Entfernung bereinigen
    t = t.replace(Regex("  +"), " ")

    // Doppelte Leerzeilen zusammenfassen
    t = t.replace(Regex("\\n{3,}"), "\n\n")

    return t.trim()
}
