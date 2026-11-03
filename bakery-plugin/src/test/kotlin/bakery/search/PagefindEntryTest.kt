package bakery.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PagefindEntryTest {
    private val entry =
        """
        {
          "version": "1.6.0",
          "languages": {
            "fr": {"hash": "fr_a99f8d72fa", "wasm": "fr", "page_count": 464},
            "en": {"hash": "en_1234abcd", "wasm": "en", "page_count": 120}
          },
          "include_characters": ["_"]
        }
        """.trimIndent()

    @Nested
    inner class Parse {
        @Test
        fun `reads one independent index per language`() {
            val index = PagefindEntry.parse(entry).getOrNull()

            assertThat(index).isNotNull
            assertThat(index!!.version).isEqualTo("1.6.0")
            assertThat(index.codes).containsExactlyInAnyOrder("fr", "en")
            assertThat(index.languagesByCode["fr"]!!.pageCount).isEqualTo(464)
            assertThat(index.languagesByCode["en"]!!.wasm).isEqualTo("en")
        }

        @Test
        fun `sorts languages for a deterministic report`() {
            val index = PagefindEntry.parse(entry).getOrNull()

            assertThat(index!!.languages.map { it.code }).containsExactly("en", "fr")
        }

        @Test
        fun `a missing page count defaults to zero so the contract can flag it`() {
            val json = """{"version":"1.6.0","languages":{"fr":{"hash":"h","wasm":"fr"}}}"""

            val index = PagefindEntry.parse(json).getOrNull()

            assertThat(index!!.languagesByCode["fr"]!!.pageCount).isZero()
        }

        @Test
        fun `malformed JSON is returned as a domain error, never thrown`() {
            val result = PagefindEntry.parse("{not json")

            assertThat(result.isLeft()).isTrue
            assertThat(result.leftOrNull()).isInstanceOf(SearchIndexError.Malformed::class.java)
        }

        @Test
        fun `a missing version is malformed`() {
            val json = """{"languages":{"fr":{"hash":"h","wasm":"fr","page_count":1}}}"""

            assertThat(PagefindEntry.parse(json).isLeft()).isTrue
        }

        @Test
        fun `a negative page count is malformed`() {
            val json = """{"version":"1.6.0","languages":{"fr":{"hash":"h","wasm":"fr","page_count":-1}}}"""

            assertThat(PagefindEntry.parse(json).isLeft()).isTrue
        }
    }
}
