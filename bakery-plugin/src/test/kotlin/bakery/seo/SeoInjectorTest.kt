package bakery.seo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-SEO-5 — Unit tests for [SeoInjector].
 *
 * Pure domain: replaces or adds a `<!-- SEO: bakery -->` marker block
 * inside `header.thyme` with a rendered SEO fragment.
 *
 * Methodology: DDD/TDD baby steps.
 */
class SeoInjectorTest {
    private val injector = SeoInjector()
    private val fragment = "    <link rel=\"canonical\" href=\"https://example.com/\" />"

    @Nested
    @DisplayName("inject into existing marker block")
    inner class ExistingMarker {
        @Test
        @DisplayName("replaces the SEO marker block with the fragment")
        fun `replaces seo marker block`() {
            val header =
                """
                <head>
                <!-- SEO: bakery -->
                <old-seo />
                <!-- /SEO: bakery -->
                <title>Old</title>
                </head>
                """.trimIndent()

            val result = injector.inject(header, fragment)

            assertThat(result).doesNotContain("<!-- SEO: bakery -->")
            assertThat(result).doesNotContain("<old-seo />")
            assertThat(result).contains(fragment)
            assertThat(result).contains("<title>Old</title>")
        }
    }

    @Nested
    @DisplayName("inject without marker block")
    inner class NoMarker {
        @Test
        @DisplayName("injects fragment right after <head> when no marker present")
        fun `injects after head when no marker`() {
            val header =
                """
                <head>
                <title>Page</title>
                </head>
                """.trimIndent()

            val result = injector.inject(header, fragment)

            assertThat(result).contains(fragment)
            assertThat(result).contains("<title>Page</title>")
        }
    }

    @Nested
    @DisplayName("idempotence")
    inner class Idempotence {
        @Test
        @DisplayName("re-injecting the same fragment yields identical output")
        fun `re injecting yields same output`() {
            val header =
                """
                <head>
                <!-- SEO: bakery -->
                <old />
                <!-- /SEO: bakery -->
                <title>Page</title>
                </head>
                """.trimIndent()

            val first = injector.inject(header, fragment)
            val second = injector.inject(first, fragment)

            assertThat(second).isEqualTo(first)
        }
    }
}