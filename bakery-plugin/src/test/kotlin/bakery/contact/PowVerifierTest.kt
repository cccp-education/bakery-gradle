package bakery.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PowVerifierTest {
    @Nested
    @DisplayName("verify nonce")
    inner class Verify {
        @Test
        fun `valid nonce with difficulty 1`() {
            val challenge = "test-challenge"
            val nonce = findNonce(challenge, 1)
            assertThat(PowVerifier.verify(challenge, nonce, difficulty = 1)).isTrue
        }

        @Test
        fun `invalid nonce with difficulty 1`() {
            assertThat(PowVerifier.verify("challenge", "xyz", difficulty = 1)).isFalse
        }

        @Test
        fun `difficulty 0 accepts any nonce`() {
            assertThat(PowVerifier.verify("challenge", "anything", difficulty = 0)).isTrue
        }

        @Test
        fun `hashing is deterministic`() {
            val h1 = PowVerifier.sha256("abc")
            val h2 = PowVerifier.sha256("abc")
            assertThat(h1).isEqualTo(h2)
        }
    }

    private fun findNonce(challenge: String, difficulty: Int): String {
        var i = 0
        while (true) {
            val nonce = i.toString()
            if (PowVerifier.verify(challenge, nonce, difficulty)) return nonce
            i++
        }
    }
}

class ContentHeuristicsTest {
    @Nested
    @DisplayName("countUrls")
    inner class CountUrls {
        @Test
        fun `zero urls`() {
            assertThat(ContentHeuristics.countUrls("hello world")).isEqualTo(0)
        }

        @Test
        fun `one url`() {
            assertThat(ContentHeuristics.countUrls("visit https://example.com")).isEqualTo(1)
        }

        @Test
        fun `three urls`() {
            assertThat(ContentHeuristics.countUrls("https://a.com https://b.com https://c.com")).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("isDuplicateSubject")
    inner class DuplicateSubject {
        @Test
        fun `subject equals message`() {
            assertThat(ContentHeuristics.isDuplicateSubject("hello", "hello")).isTrue
        }

        @Test
        fun `subject differs from message`() {
            assertThat(ContentHeuristics.isDuplicateSubject("hello", "world")).isFalse
        }
    }

    @Nested
    @DisplayName("isDisposableDomain")
    inner class DisposableDomain {
        @Test
        fun `mailinator is disposable`() {
            assertThat(ContentHeuristics.isDisposableDomain("spam@mailinator.com")).isTrue
        }

        @Test
        fun `gmail is not disposable`() {
            assertThat(ContentHeuristics.isDisposableDomain("user@gmail.com")).isFalse
        }
    }

    @Nested
    @DisplayName("isGibberish")
    inner class Gibberish {
        @Test
        fun `latin text with spaces is not gibberish`() {
            assertThat(ContentHeuristics.isGibberish("hello world this is a test")).isFalse
        }

        @Test
        fun `non-latin text without spaces is gibberish`() {
            assertThat(ContentHeuristics.isGibberish("漢字漢字漢字漢字漢字漢字")).isTrue
        }

        @Test
        fun `empty text is not gibberish`() {
            assertThat(ContentHeuristics.isGibberish("")).isFalse
        }
    }
}