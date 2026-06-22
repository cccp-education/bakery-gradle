package bakery.intention

import arrow.core.Either
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ResolveIntention — helper CLI > DSL > defaults")
class ResolveIntentionTest {

    @Nested
    @DisplayName("fromCli() — String avec default")
    inner class FromCliTest {

        @Test
        fun `CLI set overrides DSL and default`() {
            assertThat(ResolveIntention.fromCli("cli-value", "dsl-value", "default"))
                .isEqualTo("cli-value")
        }

        @Test
        fun `CLI blank falls back to DSL`() {
            assertThat(ResolveIntention.fromCli("  ", "dsl-value", "default"))
                .isEqualTo("dsl-value")
        }

        @Test
        fun `CLI null falls back to DSL`() {
            assertThat(ResolveIntention.fromCli(null, "dsl-value", "default"))
                .isEqualTo("dsl-value")
        }

        @Test
        fun `DSL blank falls back to default`() {
            assertThat(ResolveIntention.fromCli(null, "", "default"))
                .isEqualTo("default")
        }

        @Test
        fun `DSL null falls back to default`() {
            assertThat(ResolveIntention.fromCli(null, null, "default"))
                .isEqualTo("default")
        }

        @Test
        fun `all blank returns default`() {
            assertThat(ResolveIntention.fromCli("  ", "", "default"))
                .isEqualTo("default")
        }
    }

    @Nested
    @DisplayName("fromCliNullable() — String sans default")
    inner class FromCliNullableTest {

        @Test
        fun `CLI set returns CLI value`() {
            assertThat(ResolveIntention.fromCliNullable("cli-value", "dsl-value"))
                .isEqualTo("cli-value")
        }

        @Test
        fun `CLI blank falls back to DSL`() {
            assertThat(ResolveIntention.fromCliNullable("", "dsl-value"))
                .isEqualTo("dsl-value")
        }

        @Test
        fun `DSL null returns null when CLI blank`() {
            assertThat(ResolveIntention.fromCliNullable(null, null))
                .isNull()
        }

        @Test
        fun `both blank returns null`() {
            assertThat(ResolveIntention.fromCliNullable("  ", ""))
                .isNull()
        }
    }

    @Nested
    @DisplayName("fromCliRequired() — Either error or value")
    inner class FromCliRequiredTest {

        private val error = ResolveIntentionError.MissingRequiredField(
            cliFlag = "-Ptopic",
            dslPath = "bakery { articleIntention { topic = \"...\" } }",
        )

        @Test
        fun `CLI set returns Right with CLI value`() {
            assertThat(ResolveIntention.fromCliRequired("cli-topic", "dsl-topic", error))
                .isEqualTo(Either.Right("cli-topic"))
        }

        @Test
        fun `CLI blank falls back to DSL returns Right`() {
            assertThat(ResolveIntention.fromCliRequired("", "dsl-topic", error))
                .isEqualTo(Either.Right("dsl-topic"))
        }

        @Test
        fun `both blank returns Left with error`() {
            assertThat(ResolveIntention.fromCliRequired(null, null, error))
                .isEqualTo(Either.Left(error))
        }

        @Test
        fun `both blank returns Left whose toException returns IllegalArgumentException`() {
            val left = ResolveIntention.fromCliRequired(null, null, error)
            assertThat(left).isInstanceOf(Either.Left::class.java)
            val err = (left as Either.Left).value as ResolveIntentionError.MissingRequiredField
            val ex = err.toException()
            assertThat(ex).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(ex).hasMessageContaining("-Ptopic")
            assertThat(ex).hasMessageContaining("bakery { articleIntention")
        }

        @Test
        fun `toException accessible via sealed parent type`() {
            val left = ResolveIntention.fromCliRequired(null, null, error)
            val err: ResolveIntentionError = (left as Either.Left).value
            val ex = err.toException()
            assertThat(ex).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("fromCliList() — List<String> avec default")
    inner class FromCliListTest {

        @Test
        fun `CLI set parses comma-separated`() {
            assertThat(ResolveIntention.fromCliList("a, b, c", listOf("dsl"), emptyList()))
                .containsExactly("a", "b", "c")
        }

        @Test
        fun `CLI blank entries filtered out`() {
            assertThat(ResolveIntention.fromCliList("a, , b,  ", listOf("dsl"), emptyList()))
                .containsExactly("a", "b")
        }

        @Test
        fun `CLI only blank entries falls back to DSL`() {
            assertThat(ResolveIntention.fromCliList(" , , ", listOf("dsl"), emptyList()))
                .containsExactly("dsl")
        }

        @Test
        fun `CLI null falls back to DSL`() {
            assertThat(ResolveIntention.fromCliList(null, listOf("dsl1", "dsl2"), emptyList()))
                .containsExactly("dsl1", "dsl2")
        }

        @Test
        fun `both null falls back to default`() {
            assertThat(ResolveIntention.fromCliList(null, null, listOf("default")))
                .containsExactly("default")
        }

        @Test
        fun `DSL null falls back to default`() {
            assertThat(ResolveIntention.fromCliList(null, null, listOf("default")))
                .containsExactly("default")
        }
    }

    @Nested
    @DisplayName("fromCliBoolean() — Boolean avec default")
    inner class FromCliBooleanTest {

        @Test
        fun `CLI true overrides DSL and default`() {
            assertThat(ResolveIntention.fromCliBoolean("true", false, false)).isTrue()
        }

        @Test
        fun `CLI false overrides DSL and default`() {
            assertThat(ResolveIntention.fromCliBoolean("false", true, true)).isFalse()
        }

        @Test
        fun `CLI blank falls back to DSL`() {
            assertThat(ResolveIntention.fromCliBoolean("", true, false)).isTrue()
        }

        @Test
        fun `CLI null falls back to DSL`() {
            assertThat(ResolveIntention.fromCliBoolean(null, true, false)).isTrue()
        }

        @Test
        fun `DSL null falls back to default`() {
            assertThat(ResolveIntention.fromCliBoolean(null, null, true)).isTrue()
        }

        @Test
        fun `CLI invalid string falls back to DSL`() {
            assertThat(ResolveIntention.fromCliBoolean("notabool", true, false)).isTrue()
        }
    }
}