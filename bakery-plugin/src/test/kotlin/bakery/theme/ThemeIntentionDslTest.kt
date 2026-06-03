package bakery.theme

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance

class ThemeIntentionDslTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToIntention {

        @Test
        fun `converts DSL to intention with defaults`() {
            val dsl = ThemeIntentionDsl()
            dsl.description = "Blog tech Kotlin"
            val intention = dsl.toIntention()
            assertThat(intention.description).isEqualTo("Blog tech Kotlin")
            assertThat(intention.variant).isEqualTo(ThemeVariant.MINIMAL)
            assertThat(intention.overrides.primaryColor).isNull()
        }

        @Test
        fun `converts DSL with variant override`() {
            val dsl = ThemeIntentionDsl()
            dsl.description = "Documentation site"
            dsl.variant = "documentation"
            val intention = dsl.toIntention()
            assertThat(intention.variant).isEqualTo(ThemeVariant.DOCUMENTATION)
        }

        @Test
        fun `converts DSL with color overrides`() {
            val dsl = ThemeIntentionDsl()
            dsl.description = "Custom theme"
            dsl.variant = "magazine"
            dsl.primaryColor = "#ff6600"
            dsl.secondaryColor = "#2ecc71"
            dsl.fontFamily = "Inter"
            val intention = dsl.toIntention()
            assertThat(intention.overrides.primaryColor).isEqualTo("#ff6600")
            assertThat(intention.overrides.secondaryColor).isEqualTo("#2ecc71")
            assertThat(intention.overrides.fontFamily).isEqualTo("Inter")
        }

        @Test
        fun `variant fallback on unknown name returns MINIMAL`() {
            val dsl = ThemeIntentionDsl()
            dsl.description = "Test"
            dsl.variant = "unknown_variant"
            val intention = dsl.toIntention()
            assertThat(intention.variant).isEqualTo(ThemeVariant.MINIMAL)
        }

        @Test
        fun `null color overrides preserved`() {
            val dsl = ThemeIntentionDsl()
            dsl.description = "Test"
            // null is the default for nullable overrides
            val intention = dsl.toIntention()
            assertThat(intention.overrides.primaryColor).isNull()
            assertThat(intention.overrides.secondaryColor).isNull()
            assertThat(intention.overrides.accentColor).isNull()
        }

        @Test
        fun `blank description throws like ThemeIntention`() {
            val dsl = ThemeIntentionDsl()
            dsl.description = ""
            try {
                dsl.toIntention()
                assert(false) { "Should have thrown for blank description" }
            } catch (e: IllegalArgumentException) {
                assertThat(e.message).contains("description est obligatoire")
            }
        }

        @Test
        fun `all overrides map correctly`() {
            val dsl = ThemeIntentionDsl()
            dsl.description = "Full overrides"
            dsl.variant = "portfolio"
            dsl.primaryColor = "#111"
            dsl.secondaryColor = "#222"
            dsl.accentColor = "#333"
            dsl.backgroundColor = "#444"
            dsl.textColor = "#555"
            dsl.fontFamily = "Font"
            dsl.headingFont = "Heading"
            dsl.logoUrl = "/logo.png"
            dsl.faviconUrl = "/favicon.ico"
            val intention = dsl.toIntention()
            assertThat(intention.overrides).isEqualTo(ThemeOverrides(
                primaryColor = "#111",
                secondaryColor = "#222",
                accentColor = "#333",
                backgroundColor = "#444",
                textColor = "#555",
                fontFamily = "Font",
                headingFont = "Heading",
                logoUrl = "/logo.png",
                faviconUrl = "/favicon.ico"
            ))
        }
    }
}