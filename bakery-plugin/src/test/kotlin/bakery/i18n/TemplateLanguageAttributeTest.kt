package bakery.i18n

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * CHE-I18N-22 US-7d — the `<html lang>` attribute of a translated template must
 * declare the target language, never the French source.
 *
 * The translator only rewrites visible text (attributes are preserved by
 * design), so a German page kept announcing itself as French — a WCAG 3.1.1
 * "Language of Page" violation. This object closes the gap without touching any
 * other attribute or the `hreflang` alternates.
 */
class TemplateLanguageAttributeTest {

    @Test
    fun `the html lang attribute is rewritten to the target language`() {
        val template = """<html lang="fr" data-bs-theme="light">"""

        val result = TemplateLanguageAttribute.ensureLanguage(template, "de")

        assertEquals("""<html lang="de" data-bs-theme="light">""", result)
    }

    @Test
    fun `hreflang alternates are never rewritten`() {
        val template =
            """<link rel="alternate" hreflang="fr" th:href="${'$'}{config.site_host} + 'x'"/>"""

        val result = TemplateLanguageAttribute.ensureLanguage(template, "de")

        assertEquals(template, result)
    }

    @Test
    fun `the lang switcher options are never rewritten`() {
        val template = """<option value="fr" data-lang="fr">Français</option>"""

        val result = TemplateLanguageAttribute.ensureLanguage(template, "de")

        assertEquals(template, result)
    }

    @Test
    fun `a template without html lang is left untouched`() {
        val template = """<head th:fragment="head"><title>x</title></head>"""

        val result = TemplateLanguageAttribute.ensureLanguage(template, "de")

        assertEquals(template, result)
    }

    @Test
    fun `an RTL target sets the html lang and keeps the rtl attribute intact`() {
        val template = """<html lang="fr" data-bs-theme="light">"""

        val result = TemplateLanguageAttribute.ensureLanguage(template, "fa")

        assertEquals("""<html lang="fa" data-bs-theme="light">""", result)
    }

    @Test
    fun `re-running with the same language is a strict no-op`() {
        val template = """<html lang="de" data-bs-theme="light">"""

        val result = TemplateLanguageAttribute.ensureLanguage(template, "de")

        assertEquals(template, result)
    }

    @Test
    fun `the source language is never applied`() {
        val template = """<html lang="fr" data-bs-theme="light">"""

        val result = TemplateLanguageAttribute.ensureLanguage(template, "fr")

        assertEquals(template, result)
    }
}
