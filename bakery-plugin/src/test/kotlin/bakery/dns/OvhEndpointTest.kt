package bakery.dns

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-DNS-2 — Unit tests for the OVH API endpoint path builders.
 *
 * Pure DDD object — no I/O.
 */
class OvhEndpointTest {

    @Nested
    @DisplayName("OVH API path builders")
    inner class PathBuilders {
        @Test
        @DisplayName("record list path")
        fun `record list path`() {
            assertThat(OvhEndpoint.recordListPath("talaria.school"))
                .isEqualTo("/domain/zone/talaria.school/record")
        }

        @Test
        @DisplayName("record path with id")
        fun `record path with id`() {
            assertThat(OvhEndpoint.recordPath("talaria.school", 123456L))
                .isEqualTo("/domain/zone/talaria.school/record/123456")
        }

        @Test
        @DisplayName("refresh path")
        fun `refresh path`() {
            assertThat(OvhEndpoint.refreshPath("talaria.school"))
                .isEqualTo("/domain/zone/talaria.school/refresh")
        }

        @Test
        @DisplayName("base url points to eu api v1")
        fun `base url points to eu api v1`() {
            assertThat(OvhEndpoint.BASE_URL).isEqualTo("https://eu.api.ovh.com/1.0")
        }
    }
}
