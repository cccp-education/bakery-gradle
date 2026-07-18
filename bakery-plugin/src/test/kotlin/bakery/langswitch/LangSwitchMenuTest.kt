package bakery.langswitch

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows

class LangSwitchMenuTest {

    @Test
    fun `generateLinks for 2 languages FR default from FR root`() {
        val menu = LangSwitchMenu(
            supportedLanguages = listOf("fr", "en"),
            defaultLanguage = "fr",
            currentLanguage = "fr",
            currentPath = ""
        )
        val links = menu.generateLinks()
        assertEquals(2, links.size)

        val frLink = links.find { it.targetLanguage == "fr" }!!
        assertEquals("index.html", frLink.resolve())
        assertTrue(frLink.isSelfLink())

        val enLink = links.find { it.targetLanguage == "en" }!!
        assertEquals("en/index.html", enLink.resolve())
    }

    @Test
    fun `generateLinks for 2 languages FR default from EN subdir`() {
        val menu = LangSwitchMenu(
            supportedLanguages = listOf("fr", "en"),
            defaultLanguage = "fr",
            currentLanguage = "en",
            currentPath = "en/"
        )
        val links = menu.generateLinks()
        assertEquals(2, links.size)

        val frLink = links.find { it.targetLanguage == "fr" }!!
        assertEquals("../index.html", frLink.resolve())

        val enLink = links.find { it.targetLanguage == "en" }!!
        assertEquals("index.html", enLink.resolve())
        assertTrue(enLink.isSelfLink())
    }

    @Test
    fun `generateLinks for 10 languages from EN subdir`() {
        val langs = listOf("fr", "en", "ar", "bn", "es", "hi", "pt", "ru", "ur", "zh")
        val menu = LangSwitchMenu(
            supportedLanguages = langs,
            defaultLanguage = "fr",
            currentLanguage = "en",
            currentPath = "en/"
        )
        val links = menu.generateLinks()
        assertEquals(10, links.size)

        val frLink = links.find { it.targetLanguage == "fr" }!!
        assertEquals("../index.html", frLink.resolve())

        val arLink = links.find { it.targetLanguage == "ar" }!!
        assertEquals("../ar/index.html", arLink.resolve())

        val enLink = links.find { it.targetLanguage == "en" }!!
        assertEquals("index.html", enLink.resolve())
    }

    @Test
    fun `active language is marked as self-link`() {
        val menu = LangSwitchMenu(
            supportedLanguages = listOf("fr", "en"),
            defaultLanguage = "fr",
            currentLanguage = "en",
            currentPath = "en/"
        )
        val links = menu.generateLinks()
        val activeLink = links.find { it.isSelfLink() }
        assertEquals("en", activeLink?.targetLanguage)
    }

    @Test
    fun `default language link from non-default subdir goes up`() {
        val menu = LangSwitchMenu(
            supportedLanguages = listOf("fr", "en", "ar"),
            defaultLanguage = "fr",
            currentLanguage = "ar",
            currentPath = "ar/"
        )
        val links = menu.generateLinks()

        val frLink = links.find { it.targetLanguage == "fr" }!!
        assertEquals("../index.html", frLink.resolve())

        val enLink = links.find { it.targetLanguage == "en" }!!
        assertEquals("../en/index.html", enLink.resolve())

        val arLink = links.find { it.targetLanguage == "ar" }!!
        assertEquals("index.html", arLink.resolve())
    }

    @Test
    fun `empty supported languages throws`() {
        assertThrows<IllegalArgumentException> {
            LangSwitchMenu(
                supportedLanguages = emptyList(),
                defaultLanguage = "fr",
                currentLanguage = "fr",
                currentPath = ""
            )
        }
    }

    @Test
    fun `default language not in supported languages throws`() {
        assertThrows<IllegalArgumentException> {
            LangSwitchMenu(
                supportedLanguages = listOf("en", "ar"),
                defaultLanguage = "fr",
                currentLanguage = "en",
                currentPath = "en/"
            )
        }
    }

    @Test
    fun `current language not in supported languages throws`() {
        assertThrows<IllegalArgumentException> {
            LangSwitchMenu(
                supportedLanguages = listOf("fr", "en"),
                defaultLanguage = "fr",
                currentLanguage = "zh",
                currentPath = ""
            )
        }
    }
}