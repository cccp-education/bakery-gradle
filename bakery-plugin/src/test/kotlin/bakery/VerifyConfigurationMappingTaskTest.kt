package bakery

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * US-61a — Tests unitaires pour [VerifyConfigurationMappingTask].
 *
 * Valide le parsing YAML, le masquage des secrets, et les erreurs
 * de configuration.
 */
class VerifyConfigurationMappingTaskTest {

    @TempDir
    lateinit var tempDir: File

    private fun createTask(): VerifyConfigurationMappingTask {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .withName("test-verify-mapping")
            .build()
        project.pluginManager.apply("java-base")
        return project.tasks.register(
            "verifyConfigurationMapping",
            VerifyConfigurationMappingTask::class.java
        ).get()
    }

    private fun writeValidSiteYml(file: File) {
        file.writeText(
            """
            bake:
              srcPath: "site"
              destDirPath: "build/bake"
              cname: "test.example.com"
            pushPage:
              from: "site"
              to: "cvs"
              repo:
                name: "test-site"
                repository: "https://github.com/user/repo.git"
                credentials:
                  username: "user"
                  password: "secret-token-42"
              branch: "main"
              message: "Deploy test"
            pushMaquette:
              from: "maquette"
              to: "cvs"
              repo:
                name: "test-maquette"
                repository: "https://github.com/user/maquette.git"
                credentials:
                  username: "user"
                  password: "another-secret"
              branch: "main"
              message: "Deploy maquette"
            firebase:
              project:
                projectId: "test-project"
                apiKey: "AIzaSy-test-api-key"
              firestore:
                contacts:
                  name: "contacts"
                  fields:
                    - name: "name"
                      type: "string"
                  rulesEnabled: false
                messages:
                  name: "messages"
                  fields:
                    - name: "body"
                      type: "string"
                  rulesEnabled: false
              callable:
                name: "handleContact"
                params:
                  - name: "p_name"
                    type: "string"
            firebaseAuth:
              apiKey: "AIzaSy-auth-api-key"
              authDomain: "test-project.firebaseapp.com"
              projectId: "test-project"
            """.trimIndent()
        )
    }

    private fun writeSiteYmlWithFirebaseAuth(file: File) {
        file.writeText(
            """
            bake:
              srcPath: "site"
              destDirPath: "build/bake"
            pushPage:
              from: "site"
              to: "cvs"
              repo:
                name: "test-site"
                repository: "https://github.com/user/repo.git"
                credentials:
                  username: "user"
                  password: "secret"
              branch: "main"
              message: "Deploy test"
            pushMaquette:
              from: "maquette"
              to: "cvs"
              repo:
                name: "test-maquette"
                repository: "https://github.com/user/maquette.git"
                credentials:
                  username: "user"
                  password: "secret2"
              branch: "main"
              message: "Deploy maquette"
            firebaseAuth:
              apiKey: "AIzaSy-auth-api-key"
              authDomain: "test-project.firebaseapp.com"
              projectId: "test-project"
            """.trimIndent()
        )
    }

    @Nested
    inner class ValidConfigurationTest {

        @Test
        fun `task reads valid site yml and produces masked config summary`() {
            val siteYml = tempDir.resolve("site.yml")
            writeValidSiteYml(siteYml)

            val task = createTask()
            val summary = task.buildMaskedSummary(
                FileSystemManager.yamlMapper.readValue(siteYml)
            )

            assertThat(summary).contains("srcPath=site")
            assertThat(summary).contains("destDirPath=build/bake")
            assertThat(summary).contains("cname=test.example.com")
            assertThat(summary).contains("repo=test-site")
            assertThat(summary).contains("branch=main")
        }

        @Test
        fun `task masks credentials password in output`() {
            val siteYml = tempDir.resolve("site.yml")
            writeValidSiteYml(siteYml)

            val task = createTask()
            val summary = task.buildMaskedSummary(
                FileSystemManager.yamlMapper.readValue(siteYml)
            )

            assertThat(summary).contains("credentials.password=***")
            assertThat(summary).doesNotContain("secret-token-42")
        }

        @Test
        fun `task masks firebase apiKey with first and last 4 chars`() {
            val siteYml = tempDir.resolve("site.yml")
            writeValidSiteYml(siteYml)

            val task = createTask()
            val summary = task.buildMaskedSummary(
                FileSystemManager.yamlMapper.readValue(siteYml)
            )

            assertThat(summary).contains("firebase.apiKey=AIza***-key")
            assertThat(summary).doesNotContain("AIzaSy-test-api-key")
        }

        @Test
        fun `task masks firebaseAuth apiKey with first and last 4 chars`() {
            val siteYml = tempDir.resolve("site.yml")
            writeSiteYmlWithFirebaseAuth(siteYml)

            val task = createTask()
            val summary = task.buildMaskedSummary(
                FileSystemManager.yamlMapper.readValue(siteYml)
            )

            assertThat(summary).contains("firebaseAuth.apiKey=AIza***-key")
            assertThat(summary).doesNotContain("AIzaSy-auth-api-key")
        }

        @Test
        fun `task succeeds with valid configuration`() {
            val siteYml = tempDir.resolve("site.yml")
            writeValidSiteYml(siteYml)

            val task = createTask()
            task.configPath.set(siteYml.absolutePath)
            task.verify()
        }
    }

    @Nested
    inner class ErrorHandlingTest {

        @Test
        fun `task fails when configPath file does not exist`() {
            val task = createTask()
            task.configPath.set(tempDir.resolve("nonexistent.yml").absolutePath)

            val exception = assertThrows<GradleException> {
                task.verify()
            }
            assertThat(exception.message).contains("Configuration file not found")
        }

        @Test
        fun `task fails when YAML is malformed`() {
            val siteYml = tempDir.resolve("site.yml")
            siteYml.writeText("this is not: valid yaml: : :")

            val task = createTask()
            task.configPath.set(siteYml.absolutePath)

            val exception = assertThrows<GradleException> {
                task.verify()
            }
            assertThat(exception.message).contains("Failed to parse configuration file")
        }

        @Test
        fun `task fails when required field bake is missing`() {
            val siteYml = tempDir.resolve("site.yml")
            siteYml.writeText(
                """
                pushPage:
                  from: "site"
                  to: "cvs"
                  repo:
                    name: "test-site"
                    repository: "https://github.com/user/repo.git"
                    credentials:
                      username: "user"
                      password: "token"
                  branch: "main"
                  message: "Deploy test"
                pushMaquette:
                  from: "maquette"
                  to: "cvs"
                  repo:
                    name: "test-maquette"
                    repository: "https://github.com/user/maquette.git"
                    credentials:
                      username: "user"
                      password: "token"
                  branch: "main"
                  message: "Deploy maquette"
                """.trimIndent()
            )

            val task = createTask()
            task.configPath.set(siteYml.absolutePath)

            val exception = assertThrows<GradleException> {
                task.verify()
            }
            assertThat(exception.message).contains("Missing required configuration fields")
            assertThat(exception.message).contains("bake.srcPath")
        }
    }

    @Nested
    inner class MaskingUnitTest {

        @Test
        fun `maskSecret Password returns not set for blank`() {
            assertThat(maskSecret(SecretField.Password(""))).isEqualTo("(not set)")
            assertThat(maskSecret(SecretField.Password("   "))).isEqualTo("(not set)")
        }

        @Test
        fun `maskSecret Password returns stars for non blank`() {
            assertThat(maskSecret(SecretField.Password("secret"))).isEqualTo("***")
        }

        @Test
        fun `maskSecret ApiKey shows first and last 4 chars`() {
            assertThat(maskSecret(SecretField.ApiKey("AIzaSy-test-api-key"))).isEqualTo("AIza***-key")
        }

        @Test
        fun `maskSecret Token shows char count`() {
            assertThat(maskSecret(SecretField.Token("ghp_longtoken123456"))).isEqualTo("***[19 chars]")
        }
    }
}
