package bakery.i18n

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * BKY-OUT-2 — the document direction of a variant page.
 *
 * The review measured `lang="ar"` with no `dir="rtl"`: three languages were
 * published LTR with RTL glyphs. These cases pin the direction per language and
 * the idempotence of the rewrite.
 */
class DocumentDirectionTest {

    @Test
    fun `arabic persian and urdu are right to left`() {
        assertThat(DocumentDirection.RTL_LANGUAGES).containsExactlyInAnyOrder("ar", "fa", "ur")
        assertThat(DocumentDirection.isRtl("ar")).isTrue()
        assertThat(DocumentDirection.isRtl("fa")).isTrue()
        assertThat(DocumentDirection.isRtl("ur")).isTrue()
        assertThat(DocumentDirection.isRtl("fr")).isFalse()
        assertThat(DocumentDirection.isRtl("he")).isFalse()
    }

    @Test
    fun `a blank language is rejected`() {
        assertThrows<IllegalArgumentException> { DocumentDirection.isRtl(" ") }
    }

    @Test
    fun `an rtl language gains dir on the html tag`() {
        val rewritten = DocumentDirection.apply("<html lang=\"ar\">\n<head></head>\n</html>\n", "ar")

        assertThat(rewritten).startsWith("<html lang=\"ar\" dir=\"rtl\">")
    }

    @Test
    fun `an ltr language keeps the html tag untouched`() {
        val header = "<html lang=\"en\">\n<head></head>\n</html>\n"

        assertThat(DocumentDirection.apply(header, "en")).isEqualTo(header)
    }

    @Test
    fun `a stale rtl is dropped for a ltr language`() {
        val rewritten = DocumentDirection.apply("<html lang=\"de\" dir=\"rtl\">\n</html>\n", "de")

        assertThat(rewritten).isEqualTo("<html lang=\"de\">\n</html>\n")
    }

    @Test
    fun `an existing dir is replaced, never duplicated`() {
        val once = DocumentDirection.apply("<html lang=\"ar\">\n</html>\n", "ar")
        val twice = DocumentDirection.apply(once, "ar")

        assertThat(twice).isEqualTo(once)
        assertThat(once).containsOnlyOnce("dir=")
    }

    @Test
    fun `a head without an html tag is unchanged`() {
        val fragment = "<head>\n<title>T</title>\n</head>\n"

        assertThat(DocumentDirection.apply(fragment, "ar")).isEqualTo(fragment)
    }

    @Test
    fun `a self-closing html tag keeps its form`() {
        val rewritten = DocumentDirection.apply("<html lang=\"fa\"/>", "fa")

        assertThat(rewritten).isEqualTo("<html lang=\"fa\" dir=\"rtl\"/>")
    }
}
