package bakery.theme

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

class ThemeIntentionTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Construction {

        @Test
        fun `creates intention with description only — defaults to MINIMAL`() {
            val intention = ThemeIntention(description = "Blog tech moderne")
            assertThat(intention.description).isEqualTo("Blog tech moderne")
            assertThat(intention.variant).isEqualTo(ThemeVariant.MINIMAL)
            assertThat(intention.overrides).isEqualTo(ThemeOverrides())
        }

        @Test
        fun `creates intention with variant and description`() {
            val intention = ThemeIntention(
                description = "Site de documentation",
                variant = ThemeVariant.DOCUMENTATION
            )
            assertThat(intention.variant).isEqualTo(ThemeVariant.DOCUMENTATION)
        }

        @Test
        fun `creates intention with full overrides`() {
            val intention = ThemeIntention(
                description = "Portfolio creatif",
                variant = ThemeVariant.PORTFOLIO,
                overrides = ThemeOverrides(primaryColor = "#ff6600", fontFamily = "Custom")
            )
            assertThat(intention.overrides.primaryColor).isEqualTo("#ff6600")
            assertThat(intention.overrides.fontFamily).isEqualTo("Custom")
        }

        @Test
        fun `rejects blank description`() {
            assertThatThrownBy { ThemeIntention(description = "") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("description est obligatoire")

            assertThatThrownBy { ThemeIntention(description = "   ") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("description est obligatoire")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToPromptContext {

        @Test
        fun `generates prompt context with description and variant`() {
            val intention = ThemeIntention(
                description = "Blog tech Kotlin",
                variant = ThemeVariant.MAGAZINE
            )
            val context = intention.toPromptContext()
            assertThat(context).contains("Blog tech Kotlin")
            assertThat(context).contains("magazine")
            assertThat(context).contains("Variantes disponibles")
        }

        @Test
        fun `includes overrides in prompt context when present`() {
            val intention = ThemeIntention(
                description = "Site formation",
                variant = ThemeVariant.FORMATION,
                overrides = ThemeOverrides(primaryColor = "#custom", fontFamily = "Fira")
            )
            val context = intention.toPromptContext()
            assertThat(context).contains("#custom")
            assertThat(context).contains("Fira")
        }

        @Test
        fun `does not include null overrides in prompt context`() {
            val intention = ThemeIntention(
                description = "Minimal site",
                variant = ThemeVariant.MINIMAL,
                overrides = ThemeOverrides(primaryColor = null)
            )
            val context = intention.toPromptContext()
            assertThat(context).doesNotContain("Couleur primaire souhaitee")
        }

        @Test
        fun `lists all available variants`() {
            val intention = ThemeIntention(description = "Test")
            val context = intention.toPromptContext()
            ThemeVariant.entries.forEach { variant ->
                assertThat(context).contains(variant.label)
            }
        }
    }
}