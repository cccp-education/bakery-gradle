package bakery.theme

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
class ThemeIntentionUnitTest {

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

    private fun configureThemeIntention(
        description: String = "test",
        variant: String? = null,
        accentColor: String? = null,
        backgroundColor: String? = null,
        textColor: String? = null,
        headingFont: String? = null
    ) {
        ext.themeIntention {
            it.description = description
            variant?.let { v -> it.variant = v }
            accentColor?.let { c -> it.accentColor = c }
            backgroundColor?.let { c -> it.backgroundColor = c }
            textColor?.let { c -> it.textColor = c }
            headingFont?.let { f -> it.headingFont = f }
        }
        (project as ProjectInternal).evaluate()
    }

    @Test
    fun `DSL themeIntention block compiles with description only`() {
        configureThemeIntention(description = "Blog tech moderne")

        assertThat(ext.themeIntention.description).isEqualTo("Blog tech moderne")
        assertThat(ext.themeIntention.variant).isEqualTo("minimal")
    }

    @Test
    fun `DSL themeIntention block compiles with full config`() {
        configureThemeIntention(
            description = "Portfolio créatif",
            variant = "portfolio",
            accentColor = "#FF5733",
            backgroundColor = "#F5F5F5",
            textColor = "#333333",
            headingFont = "Merriweather"
        )

        assertThat(ext.themeIntention.description).isEqualTo("Portfolio créatif")
        assertThat(ext.themeIntention.variant).isEqualTo("portfolio")
        assertThat(ext.themeIntention.accentColor).isEqualTo("#FF5733")
        assertThat(ext.themeIntention.backgroundColor).isEqualTo("#F5F5F5")
        assertThat(ext.themeIntention.textColor).isEqualTo("#333333")
        assertThat(ext.themeIntention.headingFont).isEqualTo("Merriweather")
    }

    @Test
    fun `generateTheme task is registered with themeIntention DSL`() {
        projectDir.resolve("site").mkdirs()
        projectDir.resolve("maquette").mkdirs()
        projectDir.resolve("site.yml").writeText("""
            bake:
              srcPath: site
              destDirPath: build/bake
            pushPage:
              from: site
              to: cvs
              repo:
                name: test
                repository: https://github.com/test/test.git
                credentials:
                  username: u
                  password: p
              branch: main
              message: test
            pushMaquette:
              from: maquette
              to: cvs
              repo:
                name: test
                repository: https://github.com/test/test.git
                credentials:
                  username: u
                  password: p
              branch: main
              message: test
        """.trimIndent())
        ext.themeIntention {
            it.description = "Documentation API"
            it.variant = "documentation"
        }
        (project as ProjectInternal).evaluate()

        assertThat(project.tasks.findByName("generateTheme")).isNotNull
    }

    @Test
    fun `backward compatibility - plugin applies without themeIntention`() {
        (project as ProjectInternal).evaluate()

        assertThat(ext.themeIntention).isNotNull
        assertThat(ext.themeIntention.description).isEmpty()
    }
}