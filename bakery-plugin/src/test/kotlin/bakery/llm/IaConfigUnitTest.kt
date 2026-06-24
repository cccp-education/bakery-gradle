package bakery.llm

import bakery.BakeryExtension
import bakery.BakeryPlugin
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IaConfigUnitTest {

    @TempDir
    lateinit var projectDir: File

    private lateinit var project: Project
    private lateinit var ext: BakeryExtension

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply(BakeryPlugin::class.java)
        ext = project.extensions.getByType(BakeryExtension::class.java)
        ext.configPath.set(projectDir.resolve("site.yml").absolutePath)
    }

    private fun configureIa(
        iaBaseUrl: String? = null,
        iaModelName: String? = null,
        iaEnabled: Boolean? = null
    ) {
        ext.ia {
            iaBaseUrl?.let { b -> it.baseUrl = b }
            iaModelName?.let { m -> it.modelName = m }
            iaEnabled?.let { e -> it.enabled = e }
        }
        (project as ProjectInternal).evaluate()
    }

    @Test
    fun `DSL ia block compiles with default values`() {
        configureIa()

        assertThat(ext.ia.baseUrl).isEqualTo("http://localhost:11464")
        assertThat(ext.ia.modelName).isEqualTo("gpt-oss:120b-cloud")
        assertThat(ext.ia.enabled).isFalse()
    }

    @Test
    fun `DSL ia block compiles with custom values`() {
        configureIa(
            iaBaseUrl = "https://ollama.custom.example.com:11462",
            iaModelName = "custom-model:7b"
        )

        assertThat(ext.ia.baseUrl).isEqualTo("https://ollama.custom.example.com:11462")
        assertThat(ext.ia.modelName).isEqualTo("custom-model:7b")
    }

    @Test
    fun `DSL ia block with disabled flag compiles without error`() {
        configureIa(iaEnabled = false)

        assertThat(ext.ia.enabled).isFalse()
    }

    @Test
    fun `missing ia block does not break plugin application`() {
        (project as ProjectInternal).evaluate()

        assertThat(ext.ia).isNotNull
        assertThat(ext.ia.baseUrl).isEqualTo("http://localhost:11464")
    }
}