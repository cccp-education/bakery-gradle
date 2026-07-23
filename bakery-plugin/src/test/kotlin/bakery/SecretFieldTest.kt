package bakery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SecretFieldTest {

    @Nested
    inner class PasswordTest {

        @Test
        fun `Password blank value returns not set`() {
            assertThat(maskSecret(SecretField.Password(""))).isEqualTo("(not set)")
        }

        @Test
        fun `Password non-blank value returns stars`() {
            assertThat(maskSecret(SecretField.Password("ghp_secret123"))).isEqualTo("***")
        }

        @Test
        fun `Password whitespace value returns not set`() {
            assertThat(maskSecret(SecretField.Password("   "))).isEqualTo("(not set)")
        }
    }

    @Nested
    inner class ApiKeyTest {

        @Test
        fun `ApiKey blank value returns not set`() {
            assertThat(maskSecret(SecretField.ApiKey(""))).isEqualTo("(not set)")
        }

        @Test
        fun `ApiKey shows first and last 4 chars`() {
            assertThat(maskSecret(SecretField.ApiKey("AIzaSy-test-api-key")))
                .isEqualTo("AIza***-key")
        }

        @Test
        fun `ApiKey short value returns stars`() {
            assertThat(maskSecret(SecretField.ApiKey("abc")))
                .isEqualTo("abc***abc")
        }
    }

    @Nested
    inner class TokenTest {

        @Test
        fun `Token blank value returns not set`() {
            assertThat(maskSecret(SecretField.Token(""))).isEqualTo("(not set)")
        }

        @Test
        fun `Token non-blank value returns char count`() {
            assertThat(maskSecret(SecretField.Token("gho_longtoken12345")))
                .isEqualTo("***[18 chars]")
        }

        @Test
        fun `Token whitespace value returns not set`() {
            assertThat(maskSecret(SecretField.Token("  "))).isEqualTo("(not set)")
        }
    }

    @Nested
    inner class DeviceKeyTest {

        @Test
        fun `DeviceKey blank value returns not set`() {
            assertThat(maskSecret(SecretField.DeviceKey(""))).isEqualTo("(not set)")
        }

        @Test
        fun `DeviceKey non-blank value returns masked ssh prefix`() {
            val key = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAI-very-secret-key"
            assertThat(maskSecret(SecretField.DeviceKey(key)))
                .startsWith("ssh-ed25519***")
                .endsWith("${key.length} chars]")
        }

        @Test
        fun `DeviceKey whitespace value returns not set`() {
            assertThat(maskSecret(SecretField.DeviceKey("   "))).isEqualTo("(not set)")
        }

        @Test
        fun `DeviceKey long ed25519 key does not leak content`() {
            val key = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAILongKeyThatShouldNeverAppearInLogsOrConsoleOutputInProduction"
            val masked = maskSecret(SecretField.DeviceKey(key))
            assertThat(masked).doesNotContain("LongKey")
            assertThat(masked).doesNotContain("AAAAC3Nza")
            assertThat(masked).startsWith("ssh-ed25519***")
        }
    }
}