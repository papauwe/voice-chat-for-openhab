package de.openhabvoice.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit-Tests fuer stripMarkdown: Markdown wird zu TTS-freundlichem Klartext.
 */
class TextUtilTest {

    @Test
    fun `Klartext bleibt unveraendert`() {
        assertEquals("Hallo Welt", stripMarkdown("Hallo Welt"))
    }

    @Test
    fun `Tabelle wird zu Kommazeilen und Trennzeile entfernt`() {
        val input = """
            | Name | Zustand |
            |------|---------|
            | WZ   | an      |
        """.trimIndent()
        assertEquals("Name, Zustand\nWZ, an", stripMarkdown(input))
    }

    @Test
    fun `Codeblock wird zu Inhalt ohne Backticks`() {
        assertEquals("val x = 1", stripMarkdown("```kotlin\nval x = 1\n```"))
        assertEquals("val x = 1", stripMarkdown("```\nval x = 1\n```"))
    }

    @Test
    fun `Inline-Code Backticks werden entfernt`() {
        assertEquals("Der Wert ist 42 Grad", stripMarkdown("Der Wert ist `42` Grad"))
    }

    @Test
    fun `Link wird zu Linktext`() {
        assertEquals("openHAB ist gut", stripMarkdown("[openHAB](https://openhab.org) ist gut"))
    }

    @Test
    fun `Ueberschriften-Marker werden entfernt`() {
        assertEquals("Temperatur", stripMarkdown("## Temperatur"))
        assertEquals("Alles", stripMarkdown("### Alles"))
    }

    @Test
    fun `Bullet-Marker werden entfernt`() {
        assertEquals("Licht an\nRadio aus", stripMarkdown("- Licht an\n* Radio aus"))
    }

    @Test
    fun `Fett und Kursiv werden entfernt`() {
        assertEquals("ist wichtig", stripMarkdown("ist **wichtig**"))
        assertEquals("ist wichtig", stripMarkdown("ist __wichtig__"))
    }

    @Test
    fun `fett markierte Zahl bleibt vollstaendig erhalten - Grenzfall aus openHAB`() {
        // Regression: openHAB-Antwort "Heute wurden **0,6 kWh** erzeugt" – die Zahl
        // wurde von der TTS nicht vorgelesen, wenn das Markup ueberlebt.
        assertEquals("Heute wurden 0,6 kWh erzeugt", stripMarkdown("Heute wurden **0,6 kWh** erzeugt"))
        assertEquals("0,6 kWh", stripMarkdown("**0,6 kWh**"))
        assertEquals("Heute wurden 0,6 kWh erzeugt", stripMarkdown("Heute wurden *0,6 kWh* erzeugt"))
        assertEquals("Heute wurden 0,6 kWh erzeugt", stripMarkdown("Heute wurden __0,6 kWh__ erzeugt"))
        assertEquals("Heute wurden 0,6 kWh erzeugt", stripMarkdown("Heute wurden _0,6 kWh_ erzeugt"))
    }

    @Test
    fun `Original-Antwort vom echten openHAB (Balkonkraftwerk) wird ohne Markup gestrippt`() {
        // Exakter Rohstring aus /rest/voice/conversations/<id> des echten Servers:
        // "Heute wurden mit dem Balkonkraftwerk **0,631 kWh** Strom erzeugt."
        assertEquals(
            "Heute wurden mit dem Balkonkraftwerk 0,631 kWh Strom erzeugt.",
            stripMarkdown("Heute wurden mit dem Balkonkraftwerk **0,631 kWh** Strom erzeugt.")
        )
        // gleiche Antwort mit Emoji am Ende (in der App hängt der TTS-Sprachtext hinterher)
        assertEquals(
            "Heute wurden mit dem Balkonkraftwerk 0,631 kWh Strom erzeugt.",
            stripMarkdown("Heute wurden mit dem Balkonkraftwerk **0,631 kWh** Strom erzeugt. ⚡")
        )
    }

    @Test
    fun `fett markierte Zahl am Zeilenanfang und in Liste bleibt erhalten`() {
        assertEquals("0,6 kWh wurden erzeugt", stripMarkdown("**0,6 kWh** wurden erzeugt"))
        assertEquals(
            "Heute: 0,6 kWh\nGestern: 1,2 kWh",
            stripMarkdown("- Heute: **0,6 kWh**\n- Gestern: **1,2 kWh**")
        )
    }

    @Test
    fun `fett markierte Zahl in Tabelle bleibt erhalten`() {
        val input = """
            | Zeitraum | Erzeugung |
            |----------|-----------|
            | Heute    | **0,6 kWh** |
        """.trimIndent()
        assertEquals("Zeitraum, Erzeugung\nHeute, 0,6 kWh", stripMarkdown(input))
    }

    @Test
    fun `Kursiv mit Wortgrenze wird entfernt`() {
        assertEquals("leise wichtig", stripMarkdown("*leise* _wichtig_"))
    }

    @Test
    fun `Emoji und Symbole werden entfernt`() {
        assertEquals("Licht an", stripMarkdown("Licht an ✅💡"))
        assertEquals("Morgen", stripMarkdown("☀️Morgen"))
    }

    @Test
    fun `Mehrfache Leerzeilen werden zusammengefasst`() {
        assertEquals("a\n\nb", stripMarkdown("a\n\n\n\nb"))
    }

    @Test
    fun `gemischter Chat-Text wird aufgeraeumt`() {
        val input = """
            ## Status
            - WZ-Lampe: **an**
            - Temp: `21.5` Grad ✅
        """.trimIndent()
        assertEquals("Status\nWZ-Lampe: an\nTemp: 21.5 Grad", stripMarkdown(input))
    }
}
