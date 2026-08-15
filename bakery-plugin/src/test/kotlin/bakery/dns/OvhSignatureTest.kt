package bakery.dns

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-DNS-2 — Unit tests for the OVH HMAC SHA-1 signature.
 *
 * The OVH API signs every request with:
 * `$1$` + sha1hex(applicationSecret + "+" + consumerKey + "+" + method
 * + "+" + fullUrl + "+" + body + "+" + timestamp)
 *
 * Known vectors were computed offline (sha1sum / hashlib) so the test
 * pins the exact wire format.
 *
 * Methodology: DDD/TDD baby steps — pure object, no I/O.
 */
class OvhSignatureTest {

    private val applicationSecret = "7c1c7adf9f9c0a5c2f3e4d5b6a7b8c9d0e1f2a3b"
    private val consumerKey = "consumer-key-123"
    private val recordUrl = "https://eu.api.ovh.com/1.0/domain/zone/talaria.school/record"

    @Nested
    @DisplayName("OVH HMAC SHA-1 signature")
    inner class Signature {
        @Test
        @DisplayName("known vector: GET with empty body")
        fun `known vector GET empty body`() {
            val signature =
                OvhSignature.sign(
                    applicationSecret = applicationSecret,
                    consumerKey = consumerKey,
                    method = "GET",
                    url = recordUrl,
                    body = "",
                    timestamp = "1456398605",
                )

            assertThat(signature).isEqualTo("\$1\$e1d15d3d0492d8e4c16b45a6321b4fad1186b0ae")
        }

        @Test
        @DisplayName("known vector: POST with body")
        fun `known vector POST with body`() {
            val signature =
                OvhSignature.sign(
                    applicationSecret = applicationSecret,
                    consumerKey = consumerKey,
                    method = "POST",
                    url = recordUrl,
                    body = "{\"fieldType\":\"A\",\"subDomain\":\"\",\"target\":\"185.199.108.153\",\"ttl\":3600}",
                    timestamp = "1456398700",
                )

            assertThat(signature).isEqualTo("\$1\$ed9e882837c24b7b7db7dc1777e80b60f133c187")
        }

        @Test
        @DisplayName("signature changes when the timestamp changes")
        fun `signature changes with timestamp`() {
            val s1 = OvhSignature.sign("AS", "CK", "GET", "https://x.example/", "", "100")
            val s2 = OvhSignature.sign("AS", "CK", "GET", "https://x.example/", "", "101")

            assertThat(s1).isNotEqualTo(s2)
        }

        @Test
        @DisplayName("signature includes the request body")
        fun `signature includes body`() {
            val empty = OvhSignature.sign("AS", "CK", "POST", "https://x.example/", "", "100")
            val withBody = OvhSignature.sign("AS", "CK", "POST", "https://x.example/", "{\"a\":1}", "100")

            assertThat(empty).isNotEqualTo(withBody)
        }

        @Test
        @DisplayName("signature is prefixed with $1$ followed by a 40 hex char sha1")
        fun `signature prefix and hex length`() {
            val signature = OvhSignature.sign("AS", "CK", "GET", "https://x.example/", "", "100")

            assertThat(signature).startsWith("\$1\$")
            assertThat(signature.removePrefix("\$1\$")).hasSize(40)
            assertThat(signature.removePrefix("\$1\$")).matches { hex -> hex.matches(Regex("[0-9a-f]{40}")) }
        }
    }
}
