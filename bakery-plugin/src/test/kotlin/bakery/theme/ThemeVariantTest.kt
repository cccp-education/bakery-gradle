package bakery.theme

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

class ThemeVariantTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FromStringOrDefault {

        @Test
        fun `resolves minimal variant case insensitive`() {
            assertThat(ThemeVariant.fromStringOrDefault("minimal")).isEqualTo(ThemeVariant.MINIMAL)
            assertThat(ThemeVariant.fromStringOrDefault("MINIMAL")).isEqualTo(ThemeVariant.MINIMAL)
            assertThat(ThemeVariant.fromStringOrDefault("Minimal")).isEqualTo(ThemeVariant.MINIMAL)
        }

        @Test
        fun `resolves magazine variant`() {
            assertThat(ThemeVariant.fromStringOrDefault("magazine")).isEqualTo(ThemeVariant.MAGAZINE)
        }

        @Test
        fun `resolves documentation variant`() {
            assertThat(ThemeVariant.fromStringOrDefault("documentation")).isEqualTo(ThemeVariant.DOCUMENTATION)
        }

        @Test
        fun `resolves portfolio variant`() {
            assertThat(ThemeVariant.fromStringOrDefault("portfolio")).isEqualTo(ThemeVariant.PORTFOLIO)
        }

        @Test
        fun `resolves formation variant`() {
            assertThat(ThemeVariant.fromStringOrDefault("formation")).isEqualTo(ThemeVariant.FORMATION)
        }

        @Test
        fun `returns minimal for null input`() {
            assertThat(ThemeVariant.fromStringOrDefault(null)).isEqualTo(ThemeVariant.MINIMAL)
        }

        @Test
        fun `returns minimal for blank input`() {
            assertThat(ThemeVariant.fromStringOrDefault("")).isEqualTo(ThemeVariant.MINIMAL)
            assertThat(ThemeVariant.fromStringOrDefault("   ")).isEqualTo(ThemeVariant.MINIMAL)
        }

        @Test
        fun `returns minimal for unknown variant`() {
            assertThat(ThemeVariant.fromStringOrDefault("unknown")).isEqualTo(ThemeVariant.MINIMAL)
            assertThat(ThemeVariant.fromStringOrDefault("custom")).isEqualTo(ThemeVariant.MINIMAL)
        }
    }

    @Test
    fun `all variants have distinct labels`() {
        val labels = ThemeVariant.entries.map { it.label }
        assertThat(labels).hasSize(ThemeVariant.entries.size)
        assertThat(labels).doesNotHaveDuplicates()
    }

    @Test
    fun `all variants have non-blank descriptions`() {
        ThemeVariant.entries.forEach { variant ->
            assertThat(variant.description).isNotBlank()
        }
    }
}