package bakery.search

/**
 * BKY-SEARCH-1 — does the published Pagefind bundle carry every language the
 * deployment claims to ship?
 *
 * The contract is value-level: the caller hands in the languages that were
 * actually staged (a partial corpus is a legitimate state, D8 CHE-I18N-22) and
 * the minimum number of pages expected per language. A missing entry and an
 * empty index are distinct defects — a language whose index was built but
 * indexed zero pages silently returns no result instead of 404ing.
 *
 * Extra languages are never reported: a superset of the staged tree (a stale
 * index left by a previous build) is a hygiene concern for the caller, not a
 * publication blocker.
 *
 * Pure domain: no I/O, no Gradle, no LLM.
 */
object PagefindIndexContract {
    const val MISSING_LANGUAGE: String = "missing-language"
    const val EMPTY_LANGUAGE: String = "empty-language"

    fun verify(
        index: SearchIndex,
        expectedLanguages: Collection<String>,
        minimumPages: Int = 1,
    ): List<SearchIndexViolation> {
        require(minimumPages >= 0) { "minimumPages must not be negative" }

        val expected = expectedLanguages.filter { it.isNotBlank() }.toSortedSet()
        val actual = index.languagesByCode

        val missing =
            expected
                .filterNot { it in actual }
                .map { SearchIndexViolation(MISSING_LANGUAGE, it) }

        val empty =
            expected.mapNotNull { code ->
                actual[code]
                    ?.takeIf { it.pageCount < minimumPages }
                    ?.let { SearchIndexViolation(EMPTY_LANGUAGE, "$code=${it.pageCount}") }
            }

        return missing + empty
    }
}

data class SearchIndexViolation(
    val code: String,
    val detail: String,
) {
    override fun toString(): String = "$code: $detail"
}
