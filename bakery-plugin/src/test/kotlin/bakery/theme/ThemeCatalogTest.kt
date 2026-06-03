package bakery.theme

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

class ThemeCatalogTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PresetFor {

        @Test
        fun `returns minimal preset with expected defaults`() {
            val preset = ThemeCatalog.presetFor(ThemeVariant.MINIMAL)
            assertThat(preset.variant).isEqualTo(ThemeVariant.MINIMAL)
            assertThat(preset.primaryColor).isEqualTo("#2c3e50")
            assertThat(preset.secondaryColor).isEqualTo("#95a5a6")
            assertThat(preset.accentColor).isEqualTo("#34495e")
            assertThat(preset.backgroundColor).isEqualTo("#ffffff")
            assertThat(preset.textColor).isEqualTo("#333333")
            assertThat(preset.fontFamily).isEqualTo("system-ui, sans-serif")
            assertThat(preset.headingFont).isEqualTo("system-ui, sans-serif")
            assertThat(preset.logoUrl).isEmpty()
            assertThat(preset.faviconUrl).isEmpty()
        }

        @Test
        fun `returns magazine preset with serif fonts`() {
            val preset = ThemeCatalog.presetFor(ThemeVariant.MAGAZINE)
            assertThat(preset.variant).isEqualTo(ThemeVariant.MAGAZINE)
            assertThat(preset.primaryColor).isEqualTo("#e74c3c")
            assertThat(preset.fontFamily).contains("Georgia")
            assertThat(preset.headingFont).contains("Playfair Display")
        }

        @Test
        fun `returns documentation preset with monospace font`() {
            val preset = ThemeCatalog.presetFor(ThemeVariant.DOCUMENTATION)
            assertThat(preset.variant).isEqualTo(ThemeVariant.DOCUMENTATION)
            assertThat(preset.primaryColor).isEqualTo("#2980b9")
            assertThat(preset.fontFamily).contains("monospace")
            assertThat(preset.headingFont).contains("Inter")
        }

        @Test
        fun `returns portfolio preset with dark background`() {
            val preset = ThemeCatalog.presetFor(ThemeVariant.PORTFOLIO)
            assertThat(preset.variant).isEqualTo(ThemeVariant.PORTFOLIO)
            assertThat(preset.primaryColor).isEqualTo("#2ecc71")
            assertThat(preset.backgroundColor).isEqualTo("#1a1a2e")
            assertThat(preset.textColor).isEqualTo("#e0e0e0")
        }

        @Test
        fun `returns formation preset with bootstrap blue`() {
            val preset = ThemeCatalog.presetFor(ThemeVariant.FORMATION)
            assertThat(preset.variant).isEqualTo(ThemeVariant.FORMATION)
            assertThat(preset.primaryColor).isEqualTo("#0d6efd")
            assertThat(preset.accentColor).isEqualTo("#198754")
        }

        @Test
        fun `all presets have distinct primary colors`() {
            val colors = ThemeVariant.entries.map { ThemeCatalog.presetFor(it).primaryColor }
            assertThat(colors).doesNotHaveDuplicates()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AvailableVariants {

        @Test
        fun `returns all 5 variants`() {
            val variants = ThemeCatalog.availableVariants()
            assertThat(variants).hasSize(5)
            assertThat(variants).containsExactly(
                ThemeVariant.MINIMAL,
                ThemeVariant.MAGAZINE,
                ThemeVariant.DOCUMENTATION,
                ThemeVariant.PORTFOLIO,
                ThemeVariant.FORMATION
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Resolve {

        @Test
        fun `resolves with no overrides returns preset as-is`() {
            val resolved = ThemeCatalog.resolve(ThemeVariant.MINIMAL)
            val preset = ThemeCatalog.presetFor(ThemeVariant.MINIMAL)
            assertThat(resolved).isEqualTo(preset)
        }

        @Test
        fun `overrides primary color while keeping other preset values`() {
            val resolved = ThemeCatalog.resolve(
                ThemeVariant.MINIMAL,
                ThemeOverrides(primaryColor = "#ff6600")
            )
            assertThat(resolved.primaryColor).isEqualTo("#ff6600")
            assertThat(resolved.secondaryColor).isEqualTo("#95a5a6") // from preset
            assertThat(resolved.fontFamily).isEqualTo("system-ui, sans-serif") // from preset
        }

        @Test
        fun `overrides multiple properties`() {
            val resolved = ThemeCatalog.resolve(
                ThemeVariant.DOCUMENTATION,
                ThemeOverrides(
                    primaryColor = "#custom",
                    fontFamily = "Custom Font",
                    headingFont = "Custom Heading"
                )
            )
            assertThat(resolved.primaryColor).isEqualTo("#custom")
            assertThat(resolved.secondaryColor).isEqualTo("#ecf0f1") // from preset
            assertThat(resolved.fontFamily).isEqualTo("Custom Font")
            assertThat(resolved.headingFont).isEqualTo("Custom Heading")
        }

        @Test
        fun `null override preserves preset value`() {
            val resolved = ThemeCatalog.resolve(
                ThemeVariant.MAGAZINE,
                ThemeOverrides(primaryColor = null, fontFamily = null)
            )
            val preset = ThemeCatalog.presetFor(ThemeVariant.MAGAZINE)
            assertThat(resolved.primaryColor).isEqualTo(preset.primaryColor)
            assertThat(resolved.fontFamily).isEqualTo(preset.fontFamily)
        }

        @Test
        fun `empty string override does NOT override — null means use preset`() {
            val resolved = ThemeCatalog.resolve(
                ThemeVariant.MINIMAL,
                ThemeOverrides(logoUrl = null, faviconUrl = null)
            )
            assertThat(resolved.logoUrl).isEmpty() // preset default
            assertThat(resolved.faviconUrl).isEmpty() // preset default
        }

        @Test
        fun `overrides logo and favicon`() {
            val resolved = ThemeCatalog.resolve(
                ThemeVariant.FORMATION,
                ThemeOverrides(logoUrl = "/img/logo.png", faviconUrl = "/img/favicon.ico")
            )
            assertThat(resolved.logoUrl).isEqualTo("/img/logo.png")
            assertThat(resolved.faviconUrl).isEqualTo("/img/favicon.ico")
        }
    }
}