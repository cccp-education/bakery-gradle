package bakery

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-NETLIFY (US-2, D1) — a Netlify-only site has no Git push target and
 * must still satisfy the configuration guard. The token is never summarized.
 */
class VerifyConfigurationMappingNetlifyTest {
    private fun task(): VerifyConfigurationMappingTask {
        val project =
            ProjectBuilder
                .builder()
                .withName("netlify-mapping")
                .build()
        project.pluginManager.apply("java-base")
        return project.tasks
            .register("verifyConfigurationMapping", VerifyConfigurationMappingTask::class.java)
            .get()
    }

    private fun config(yaml: String): SiteConfiguration = FileSystemManager.yamlMapper.readValue(yaml)

    private val netlifyOnly =
        """
        bake:
          srcPath: site
          destDirPath: build/site
        pushMaquette:
          from: maquette
          to: cvs
        netlify:
          siteId: "site-123"
          tokenEnv: NETLIFY_AUTH_TOKEN
        """.trimIndent()

    private val noTarget =
        """
        bake:
          srcPath: site
          destDirPath: build/site
        pushMaquette:
          from: maquette
          to: cvs
        """.trimIndent()

    @Test
    @DisplayName("a Netlify-only site does not require pushPage")
    fun netlifyOnlyPasses() {
        task().validateRequiredFields(config(netlifyOnly))
    }

    @Test
    @DisplayName("a site without any deployment target still requires pushPage")
    fun noTargetFails() {
        assertThatThrownBy { task().validateRequiredFields(config(noTarget)) }
            .isInstanceOf(GradleException::class.java)
            .hasMessageContaining("pushPage.from")
    }

    @Test
    @DisplayName("the masked summary exposes the site id and the token variable name, never a token")
    fun summaryIsMasked() {
        val summary = task().buildMaskedSummary(config(netlifyOnly))

        assertThat(summary).contains("netlify.siteId=site-123")
        assertThat(summary).contains("netlify.tokenEnv=NETLIFY_AUTH_TOKEN")
    }
}
