package bakery.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SessionTokenManagerTest {
    private val manager = SessionTokenManager()

    @Nested
    @DisplayName("issue and validate")
    inner class IssueValidate {
        @Test
        fun `issued token is valid`() {
            val token = manager.issue(now = 1000L)
            assertThat(manager.validate(token, now = 1000L)).isTrue
        }

        @Test
        fun `expired token is invalid`() {
            val token = manager.issue(now = 1000L)
            assertThat(manager.validate(token, now = 1000L + 600001L)).isFalse
        }

        @Test
        fun `unknown token is invalid`() {
            assertThat(manager.validate(SessionToken("unknown", issuedAt = 1000L), now = 1000L)).isFalse
        }

        @Test
        fun `empty token is invalid`() {
            assertThat(manager.validate(SessionToken("", issuedAt = 1000L), now = 1000L)).isFalse
        }
    }

    @Nested
    @DisplayName("consume single-use")
    inner class Consume {
        @Test
        fun `consume issued token succeeds`() {
            val token = manager.issue(now = 1000L)
            assertThat(manager.consume(token)).isTrue
        }

        @Test
        fun `double consume fails`() {
            val token = manager.issue(now = 1000L)
            manager.consume(token)
            assertThat(manager.consume(token)).isFalse
        }
    }
}