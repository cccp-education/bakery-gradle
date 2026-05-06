package bakery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RepositoryCredentialsTest {

    @Test
    fun `toString masks password`() {
        val creds = RepositoryCredentials("user", "ghp_secretToken12345")
        assertThat(creds.toString())
            .contains("username='user'")
            .contains("password='***'")
            .doesNotContain("secretToken")
    }

    @Test
    fun `toString shows empty user`() {
        val creds = RepositoryCredentials("", "someToken")
        assertThat(creds.toString())
            .contains("username=''")
            .contains("password='***'")
            .doesNotContain("someToken")
    }
}
