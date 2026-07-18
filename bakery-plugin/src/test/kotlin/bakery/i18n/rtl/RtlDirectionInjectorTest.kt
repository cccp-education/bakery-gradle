package bakery.i18n.rtl

import bakery.pivot.PivotFrontmatter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RtlDirectionInjectorTest {

    private val injector = RtlDirectionInjector()

    private fun baseFm(asciidoc: Map<String, String> = emptyMap()): PivotFrontmatter =
        PivotFrontmatter(
            title = "Test",
            date = "2026-07-18",
            type = "post",
            status = "published",
            asciidocAttributes = asciidoc
        )

    @Nested
    inner class RtlInjection {

        @Test
        fun `inject arabic sets jbake-lang and rtl directive`() {
            val result = injector.inject(baseFm(), "ar")
            assertThat(result.jbakeAttributes["lang"]).isEqualTo("ar")
            assertThat(result.asciidocAttributes["lang"]).isEqualTo("rtl")
        }

        @Test
        fun `inject urdu sets jbake-lang and rtl directive`() {
            val result = injector.inject(baseFm(), "ur")
            assertThat(result.jbakeAttributes["lang"]).isEqualTo("ur")
            assertThat(result.asciidocAttributes["lang"]).isEqualTo("rtl")
        }

        @Test
        fun `inject french sets jbake-lang without rtl directive`() {
            val result = injector.inject(baseFm(), "fr")
            assertThat(result.jbakeAttributes["lang"]).isEqualTo("fr")
            assertThat(result.asciidocAttributes).doesNotContainKey("lang")
        }

        @Test
        fun `inject english sets jbake-lang without rtl directive`() {
            val result = injector.inject(baseFm(), "en")
            assertThat(result.jbakeAttributes["lang"]).isEqualTo("en")
            assertThat(result.asciidocAttributes).doesNotContainKey("lang")
        }

        @Test
        fun `inject chinese sets jbake-lang without rtl directive`() {
            val result = injector.inject(baseFm(), "zh")
            assertThat(result.jbakeAttributes["lang"]).isEqualTo("zh")
            assertThat(result.asciidocAttributes).doesNotContainKey("lang")
        }
    }

    @Nested
    inner class UnknownLanguage {

        @Test
        fun `unknown language is a no-op`() {
            val fm = baseFm()
            val result = injector.inject(fm, "xx")
            assertThat(result).isEqualTo(fm)
        }

        @Test
        fun `unknown language returns equal frontmatter for empty attributes`() {
            val fm = baseFm(asciidoc = mapOf("summary" to "x"))
            val result = injector.inject(fm, "xx")
            assertThat(result).isEqualTo(fm)
        }
    }

    @Nested
    inner class Idempotence {

        @Test
        fun `injecting arabic twice yields same result`() {
            val once = injector.inject(baseFm(), "ar")
            val twice = injector.inject(once, "ar")
            assertThat(twice).isEqualTo(once)
        }

        @Test
        fun `ltr injection removes pre-existing rtl marker`() {
            val rtlSource = baseFm(asciidoc = mapOf("lang" to "rtl"))
            val result = injector.inject(rtlSource, "fr")
            assertThat(result.jbakeAttributes["lang"]).isEqualTo("fr")
            assertThat(result.asciidocAttributes).doesNotContainKey("lang")
        }

        @Test
        fun `re-injecting overwrites previous jbake-lang value`() {
            val source = PivotFrontmatter(
                title = "Test",
                date = "2026-07-18",
                type = "post",
                status = "published",
                jbakeAttributes = mapOf("lang" to "fr")
            )
            val result = injector.inject(source, "ar")
            assertThat(result.jbakeAttributes["lang"]).isEqualTo("ar")
            assertThat(result.asciidocAttributes["lang"]).isEqualTo("rtl")
        }

        @Test
        fun `round-trip arabic then french drops rtl`() {
            val arabic = injector.inject(baseFm(), "ar")
            val french = injector.inject(arabic, "fr")
            assertThat(french.jbakeAttributes["lang"]).isEqualTo("fr")
            assertThat(french.asciidocAttributes).doesNotContainKey("lang")
        }
    }

    @Nested
    inner class DirectiveModel {

        @Test
        fun `rtl directive equality is structural`() {
            assertThat(RtlDirective("ar", true)).isEqualTo(RtlDirective("ar", true))
            assertThat(RtlDirective("ar", true)).isNotEqualTo(RtlDirective("ur", true))
        }
    }
}