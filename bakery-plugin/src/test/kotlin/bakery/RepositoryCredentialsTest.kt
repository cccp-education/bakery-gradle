package bakery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RepositoryCredentialsTest {

    @Test
    fun `toString masks password and token`() {
        val creds = RepositoryCredentials("ghp_longtoken12345678", "my-password")
        assertThat(creds.toString())
            .contains("username='***[21 chars]'")
            .contains("password='***'")
            .doesNotContain("longtoken")
            .doesNotContain("my-password")
    }

    @Test
    fun `toString shows not set for empty values`() {
        val creds = RepositoryCredentials("", "")
        assertThat(creds.toString())
            .contains("username='(not set)'")
            .contains("password='(not set)'")
    }
}
