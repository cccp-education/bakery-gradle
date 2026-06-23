package bakery.a11y

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ColorContrastRuleTest {

    @Test
    fun `black on white passes AA threshold`() {
        val result = evaluateContrast("#000000", "#FFFFFF")
        assertThat(result.ratio).isGreaterThan(4.5)
        assertThat(result.passAa).isTrue()
    }

    @Test
    fun `white on white fails AA threshold`() {
        val result = evaluateContrast("#FFFFFF", "#FFFFFF")
        assertThat(result.ratio).isEqualTo(1.0)
        assertThat(result.passAa).isFalse()
    }

    @Test
    fun `gray on gray is just below AA threshold`() {
        val result = evaluateContrast("#777777", "#777777")
        assertThat(result.ratio).isEqualTo(1.0)
        assertThat(result.passAa).isFalse()
    }

    @Test
    fun `orange on white fails AA threshold`() {
        val result = evaluateContrast("#FFA500", "#FFFFFF")
        assertThat(result.ratio).isLessThan(4.5)
        assertThat(result.passAa).isFalse()
    }

    @Test
    fun `dark blue on white passes AA threshold`() {
        val result = evaluateContrast("#00008B", "#FFFFFF")
        assertThat(result.ratio).isGreaterThan(4.5)
        assertThat(result.passAa).isTrue()
    }
}
