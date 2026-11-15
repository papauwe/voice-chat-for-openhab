package de.openhabvoice.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor

/**
 * Renders Markdown (GFM: bold, italic, lists, tables, code) using the
 * mikepenz multiplatform-markdown-renderer with Material 3 styling.
 * Colors are adapted to the bubble colors.
 */
@Composable
fun MarkdownMessage(text: String, contentColor: Color, modifier: Modifier = Modifier) {
    Markdown(
        content = text,
        colors = markdownColor(
            text = contentColor,
            codeBackground = contentColor.copy(alpha = 0.08f),
            inlineCodeBackground = contentColor.copy(alpha = 0.08f),
            tableBackground = contentColor.copy(alpha = 0.05f),
            dividerColor = contentColor.copy(alpha = 0.25f)
        ),
        modifier = modifier
    )
}
