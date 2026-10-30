package bakery.seo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * BKY-OUT-1 (i18n_review S-263) — the head of a *variant* page declares its own
 * locale.
 *
 * The published site measured on `/en/`, `/ar/`, `/ja/` (review S-263): the
 * `canonical` and `og:url` point to the French page (no `{lang}/` prefix),
 * `og:locale` is `fr_FR` everywhere and `og:image` reads `og-images/fr/…`. So a
 * search engine is told that every translated page is the French original.
 *
 * [LocalizedSeoHead] rewrites those four fields deterministically from the
 * variant language — no LLM, no re-translation, idempotent.
 */
class LocalizedSeoHeadTest {

    @Test
    fun `the og locale catalog maps every workspace language`() {
        assertThat(LocalizedSeoHead.localeOf("fr")).isEqualTo("fr_FR")
        assertThat(LocalizedSeoHead.localeOf("en")).isEqualTo("en_US")
        assertThat(LocalizedSeoHead.localeOf("de")).isEqualTo("de_DE")
        assertThat(LocalizedSeoHead.localeOf("ar")).isEqualTo("ar_AR")
        assertThat(LocalizedSeoHead.localeOf("zh")).isEqualTo("zh_CN")
        assertThat(LocalizedSeoHead.localeOf("fa")).isEqualTo("fa_IR")
    }

    @Test
    fun `an unknown language falls back to its own code`() {
        assertThat(LocalizedSeoHead.localeOf("xx")).isEqualTo("xx")
    }

    @Test
    fun `a blank language is rejected`() {
        assertThrows<IllegalArgumentException> { LocalizedSeoHead.localeOf(" ") }
    }

    @Test
    fun `the english variant declares an english canonical`() {
        val rewritten = LocalizedSeoHead.rewrite(HEADER, "en", "fr", ogImageLanguage = "en")

        assertThat(rewritten).contains("""th:href="${'$'}{config.site_host} + 'en/' + ${'$'}{canonUri}"""")
        assertThat(rewritten).doesNotContain("""th:href="${'$'}{config.site_host} + ${'$'}{canonUri}"""")
    }

    @Test
    fun `the english variant declares an english og url`() {
        val rewritten = LocalizedSeoHead.rewrite(HEADER, "en", "fr", ogImageLanguage = "en")

        assertThat(rewritten).contains("""property="og:url" th:content="${'$'}{config.site_host} + 'en/' + ${'$'}{canonUri}"""")
    }

    @Test
    fun `the english variant declares its own og locale`() {
        val rewritten = LocalizedSeoHead.rewrite(HEADER, "en", "fr", ogImageLanguage = "en")

        assertThat(rewritten).contains("""<meta property="og:locale" content="en_US"/>""")
        assertThat(rewritten).doesNotContain("""content="fr_FR"""")
    }

    @Test
    fun `the og image reads the language directory`() {
        val rewritten = LocalizedSeoHead.rewrite(HEADER, "en", "fr", ogImageLanguage = "en")

        assertThat(rewritten).contains("og-images/en/")
        assertThat(rewritten).doesNotContain("og-images/fr/")
    }

    @Test
    fun `a language without its own og images falls back to the given fallback`() {
        val rewritten = LocalizedSeoHead.rewrite(HEADER, "de", "fr", ogImageLanguage = "en")

        assertThat(rewritten).contains("og-images/en/")
        assertThat(rewritten).contains("""content="de_DE"""")
        assertThat(rewritten).contains("""+ 'de/' + ${'$'}{canonUri}""")
    }

    @Test
    fun `the reference language keeps a prefixless canonical`() {
        val rewritten = LocalizedSeoHead.rewrite(HEADER, "fr", "fr", ogImageLanguage = "fr")

        assertThat(rewritten).contains("""th:href="${'$'}{config.site_host} + ${'$'}{canonUri}"""")
        assertThat(rewritten).contains("""content="fr_FR"""")
        assertThat(rewritten).contains("og-images/fr/")
    }

    @Test
    fun `the rewrite is idempotent`() {
        val once = LocalizedSeoHead.rewrite(HEADER, "en", "fr", ogImageLanguage = "en")
        val twice = LocalizedSeoHead.rewrite(once, "en", "fr", ogImageLanguage = "en")

        assertThat(twice).isEqualTo(once)
    }

    @Test
    fun `an already prefixed canonical is never prefixed twice`() {
        val once = LocalizedSeoHead.rewrite(HEADER, "en", "fr", ogImageLanguage = "en")

        assertThat(once).doesNotContain("'en/' + 'en/'")
    }

    @Test
    fun `a head without any seo field is left byte-identical`() {
        val plain = "<html xmlns:th=\"http://www.thymeleaf.org\">\n<head>\n<title>T</title>\n</head>\n</html>\n"

        assertThat(LocalizedSeoHead.rewrite(plain, "en", "fr", ogImageLanguage = "en")).isEqualTo(plain)
    }

    @Test
    fun `the hreflang alternates are never rewritten`() {
        val rewritten = LocalizedSeoHead.rewrite(HEADER, "en", "fr", ogImageLanguage = "en")

        assertThat(rewritten).contains("""hreflang="en" th:unless=""")
    }

    private companion object {
        /** The real cheroliv.com head shape (S-263 measurement). */
        val HEADER =
            """
            <html xmlns:th="http://www.thymeleaf.org" lang="en">
            <head>
                <th:block th:with="canonUri=(${'$'}{canonOverride != null} ? ${'$'}{canonOverride} : ''),
                                   ogImage=${'$'}{'og-images/fr/' + #strings.replace(content.uri, '.html', '.png')}">
                    <link rel="canonical" th:href="${'$'}{config.site_host} + ${'$'}{canonUri}"/>
                    <link rel="alternate" hreflang="en" th:unless="${'$'}{false}" th:href="${'$'}{config.site_host} + 'en/' + ${'$'}{hreflangPage}"/>
                    <meta property="og:url" th:content="${'$'}{config.site_host} + ${'$'}{canonUri}"/>
                    <meta property="og:locale" content="fr_FR"/>
                    <meta property="og:image" th:content="${'$'}{config.site_host} + ${'$'}{ogImage}"/>
                </th:block>
            </head>
            </html>
            """.trimIndent()
    }
}
