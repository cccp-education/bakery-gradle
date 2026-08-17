package bakery.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RateLimiterTest {
    @Nested
    @DisplayName("allow under cap")
    inner class Allow {
        @Test
        fun `allow when under perHour cap`() {
            val limiter = RateLimiter(perHour = 3, perDay = 30, globalCap = 50)
            val now = 1000L
            assertThat(limiter.allow("key1", now)).isTrue
        }

        @Test
        fun `deny when perHour cap exceeded`() {
            val limiter = RateLimiter(perHour = 2, perDay = 30, globalCap = 50)
            val now = 1000L
            limiter.allow("key1", now)
            limiter.allow("key1", now)
            assertThat(limiter.allow("key1", now)).isFalse
        }

        @Test
        fun `deny when perDay cap exceeded`() {
            val limiter = RateLimiter(perHour = 100, perDay = 2, globalCap = 50)
            val now = 1000L
            limiter.allow("key1", now)
            limiter.allow("key1", now + 3700000)
            assertThat(limiter.allow("key1", now + 7300000)).isFalse
        }

        @Test
        fun `reset clears counters`() {
            val limiter = RateLimiter(perHour = 1, perDay = 30, globalCap = 50)
            val now = 1000L
            limiter.allow("key1", now)
            assertThat(limiter.allow("key1", now)).isFalse
            limiter.reset("key1")
            assertThat(limiter.allow("key1", now)).isTrue
        }

        @Test
        fun `global cap 50 per day`() {
            val limiter = RateLimiter(perHour = 100, perDay = 100, globalCap = 2)
            val now = 1000L
            limiter.allow("k1", now)
            limiter.allow("k2", now)
            assertThat(limiter.allow("k3", now)).isFalse
        }

        @Test
        fun `expiry TTL allows again after window`() {
            val limiter = RateLimiter(perHour = 1, perDay = 30, globalCap = 50)
            val now = 1000L
            limiter.allow("key1", now)
            assertThat(limiter.allow("key1", now)).isFalse
            assertThat(limiter.allow("key1", now + 3600001)).isTrue
        }
    }
}