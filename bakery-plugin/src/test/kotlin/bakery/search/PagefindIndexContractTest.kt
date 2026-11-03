package bakery.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PagefindIndexContractTest {
    private fun language(
        code: String,
        pageCount: Int = 10,
    ) = SearchLanguage(code = code, hash = "${code}_hash", wasm = code, pageCount = pageCount)

    private fun index(vararg languages: SearchLanguage) = SearchIndex(version = "1.6.0", languages = languages.toList())

    @Test
    fun `a bundle carrying every staged language is valid`() {
        val violations =
            PagefindIndexContract.verify(
                index(language("fr", 464), language("en", 120)),
                expectedLanguages = setOf("fr", "en"),
            )

        assertThat(violations).isEmpty()
    }

    @Test
    fun `a language with no index entry is reported missing`() {
        val violations =
            PagefindIndexContract.verify(
                index(language("fr")),
                expectedLanguages = setOf("fr", "en"),
            )

        assertThat(violations)
            .containsExactly(SearchIndexViolation(PagefindIndexContract.MISSING_LANGUAGE, "en"))
    }

    @Test
    fun `an index that indexed zero pages is reported empty`() {
        val violations =
            PagefindIndexContract.verify(
                index(language("fr", 0)),
                expectedLanguages = setOf("fr"),
            )

        assertThat(violations)
            .containsExactly(SearchIndexViolation(PagefindIndexContract.EMPTY_LANGUAGE, "fr=0"))
    }

    @Test
    fun `a language missing is not also reported empty`() {
        val violations =
            PagefindIndexContract.verify(
                index(),
                expectedLanguages = setOf("fr"),
            )

        assertThat(violations)
            .containsExactly(SearchIndexViolation(PagefindIndexContract.MISSING_LANGUAGE, "fr"))
    }

    @Test
    fun `an extra or stale language is never a publication blocker`() {
        val violations =
            PagefindIndexContract.verify(
                index(language("fr"), language("xx", 999)),
                expectedLanguages = setOf("fr"),
            )

        assertThat(violations).isEmpty()
    }

    @Test
    fun `violations are sorted by language for a deterministic report`() {
        val violations =
            PagefindIndexContract.verify(
                index(),
                expectedLanguages = setOf("ja", "fr", "en"),
            )

        assertThat(violations.map { it.detail }).containsExactly("en", "fr", "ja")
    }

    @Test
    fun `the minimum page count is configurable`() {
        val strict =
            PagefindIndexContract.verify(
                index(language("fr", 10)),
                expectedLanguages = setOf("fr"),
                minimumPages = 11,
            )

        assertThat(strict).hasSize(1)
    }
}
