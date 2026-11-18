package de.openhabvoice.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests for the language-to-model mapping of [VoskModels]. */
class VoskModelsTest {

    @Test
    fun mapsGermanLanguageTagToGermanModel() {
        assertEquals("vosk-model-small-de-0.15", VoskModels.smallModelFor("de-DE"))
    }

    @Test
    fun mapsEnglishLanguageTagToEnglishModel() {
        assertEquals("vosk-model-small-en-us-0.15", VoskModels.smallModelFor("en-US"))
    }

    @Test
    fun isCaseInsensitiveOnTheLanguagePart() {
        assertEquals("vosk-model-small-de-0.15", VoskModels.smallModelFor("DE-de"))
    }

    @Test
    fun worksWithLanguageOnlyTags() {
        assertEquals("vosk-model-small-fr-0.22", VoskModels.smallModelFor("fr"))
    }

    @Test
    fun returnsNullForLanguageWithoutSmallModel() {
        assertNull(VoskModels.smallModelFor("xx-XX"))
    }

    @Test
    fun buildsDownloadUrlFromModelName() {
        assertEquals(
            "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip",
            VoskModels.downloadUrl("vosk-model-small-de-0.15")
        )
    }
}
