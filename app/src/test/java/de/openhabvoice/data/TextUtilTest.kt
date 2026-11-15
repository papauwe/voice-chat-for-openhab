package de.openhabvoice.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for stripMarkdown: Markdown is converted to TTS-friendly plain text.
 */
class TextUtilTest {

    @Test
    fun `plain text stays unchanged`() {
        assertEquals("Hallo Welt", stripMarkdown("Hallo Welt"))
    }

    @Test
    fun `table becomes comma rows and separator row is removed`() {
        val input = """
            | Name | Zustand |
            |------|---------|
            | WZ   | an      |
        """.trimIndent()
        assertEquals("Name, Zustand\nWZ, an", stripMarkdown(input))
    }

    @Test
    fun `code block becomes content without backticks`() {
        assertEquals("val x = 1", stripMarkdown("```kotlin\nval x = 1\n```"))
        assertEquals("val x = 1", stripMarkdown("```\nval x = 1\n```"))
    }

    @Test
    fun `inline code backticks are removed`() {
        assertEquals("Der Wert ist 42 Grad", stripMarkdown("Der Wert ist `42` Grad"))
    }

    @Test
    fun `link becomes link text`() {
        assertEquals("openHAB ist gut", stripMarkdown("[openHAB](https://openhab.org) ist gut"))
    }

    @Test
    fun `heading markers are removed`() {
        assertEquals("Temperatur", stripMarkdown("## Temperatur"))
        assertEquals("Alles", stripMarkdown("### Alles"))
    }

    @Test
    fun `bullet markers are removed`() {
        assertEquals("Licht an\nRadio aus", stripMarkdown("- Licht an\n* Radio aus"))
    }

    @Test
    fun `bold and italic are removed`() {
        assertEquals("ist wichtig", stripMarkdown("ist **wichtig**"))
        assertEquals("ist wichtig", stripMarkdown("ist __wichtig__"))
    }

    @Test
    fun `bold marked number stays fully intact - edge case from openHAB`() {
        // Regression: openHAB answer "Heute wurden **0,6 kWh** erzeugt" - the number
        // was not read aloud by TTS when the markup survived.
        assertEquals("Heute wurden 0,6 kWh erzeugt", stripMarkdown("Heute wurden **0,6 kWh** erzeugt"))
        assertEquals("0,6 kWh", stripMarkdown("**0,6 kWh**"))
        assertEquals("Heute wurden 0,6 kWh erzeugt", stripMarkdown("Heute wurden *0,6 kWh* erzeugt"))
        assertEquals("Heute wurden 0,6 kWh erzeugt", stripMarkdown("Heute wurden __0,6 kWh__ erzeugt"))
        assertEquals("Heute wurden 0,6 kWh erzeugt", stripMarkdown("Heute wurden _0,6 kWh_ erzeugt"))
    }

    @Test
    fun `original answer from real openHAB (balcony solar plant) is stripped without markup`() {
        // Exact raw string from /rest/voice/conversations/<id> of the real server:
        // "Heute wurden mit dem Balkonkraftwerk **0,631 kWh** Strom erzeugt."
        assertEquals(
            "Heute wurden mit dem Balkonkraftwerk 0,631 kWh Strom erzeugt.",
            stripMarkdown("Heute wurden mit dem Balkonkraftwerk **0,631 kWh** Strom erzeugt.")
        )
        // same answer with an emoji at the end (in the app the TTS utterance lags behind)
        assertEquals(
            "Heute wurden mit dem Balkonkraftwerk 0,631 kWh Strom erzeugt.",
            stripMarkdown("Heute wurden mit dem Balkonkraftwerk **0,631 kWh** Strom erzeugt. ⚡")
        )
    }

    @Test
    fun `bold number at line start and in list stays intact`() {
        assertEquals("0,6 kWh wurden erzeugt", stripMarkdown("**0,6 kWh** wurden erzeugt"))
        assertEquals(
            "Heute: 0,6 kWh\nGestern: 1,2 kWh",
            stripMarkdown("- Heute: **0,6 kWh**\n- Gestern: **1,2 kWh**")
        )
    }

    @Test
    fun `bold number in table stays intact`() {
        val input = """
            | Zeitraum | Erzeugung |
            |----------|-----------|
            | Heute    | **0,6 kWh** |
        """.trimIndent()
        assertEquals("Zeitraum, Erzeugung\nHeute, 0,6 kWh", stripMarkdown(input))
    }

    @Test
    fun `italic with word boundary is removed`() {
        assertEquals("leise wichtig", stripMarkdown("*leise* _wichtig_"))
    }

    @Test
    fun `emojis and symbols are removed`() {
        assertEquals("Licht an", stripMarkdown("Licht an ✅💡"))
        assertEquals("Morgen", stripMarkdown("☀️Morgen"))
    }

    @Test
    fun `multiple blank lines are collapsed`() {
        assertEquals("a\n\nb", stripMarkdown("a\n\n\n\nb"))
    }

    @Test
    fun `mixed chat text is cleaned up`() {
        val input = """
            ## Status
            - WZ-Lampe: **an**
            - Temp: `21.5` Grad ✅
        """.trimIndent()
        assertEquals("Status\nWZ-Lampe: an\nTemp: 21.5 Grad", stripMarkdown(input))
    }
}
