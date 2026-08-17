package bakery.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TurnstileClientTest {
    @Nested
    @DisplayName("siteverify URL")
    inner class SiteverifyUrl {
        @Test
        fun `builds siteverify url`() {
            val req = TurnstileVerifyRequest(secret = "s", response = "r")
            assertThat(req.siteverifyUrl())
                .isEqualTo("https://challenges.cloudflare.com/turnstile/v0/siteverify")
        }
    }

    @Nested
    @DisplayName("VerifyResult")
    inner class VerifyResults {
        @Test
        fun `success result`() {
            val r = VerifyResult.Success
            assertThat(r.success).isTrue
        }

        @Test
        fun `failure carries code`() {
            val r = VerifyResult.Failure(code = "invalid-input-response")
            assertThat(r.success).isFalse
            assertThat((r as VerifyResult.Failure).code).isEqualTo("invalid-input-response")
        }

        @Test
        fun `empty response yields failure`() {
            val r = TurnstileVerifier.verify(TurnstileVerifyRequest(secret = "s", response = ""))
            assertThat(r).isInstanceOf(VerifyResult.Failure::class.java)
        }

        @Test
        fun `empty secret yields failure`() {
            val r = TurnstileVerifier.verify(TurnstileVerifyRequest(secret = "", response = "r"))
            assertThat(r).isInstanceOf(VerifyResult.Failure::class.java)
        }

        @Test
        fun `valid request returns success (stub verifier)`() {
            val r = TurnstileVerifier.verify(TurnstileVerifyRequest(secret = "s", response = "r"))
            assertThat(r).isInstanceOf(VerifyResult.Success::class.java)
        }
    }
}