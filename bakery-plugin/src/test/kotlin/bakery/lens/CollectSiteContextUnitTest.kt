package bakery.lens

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
class CollectSiteContextUnitTest {

    @TempDir
    lateinit var projectDir: File

    private lateinit var project: Project
    private lateinit var ext: BakeryExtension

    private val siteYamlContent: String
        get() = """bake:
              |  srcPath: site
              |  destDirPath: build/bake
              |pushPage:
              |  from: site
              |  to: cvs
              |  repo:
              |    name: test
              |    repository: https://github.com/test/test.git
              |    credentials:
              |      username: u
              |      password: p
              |  branch: main
              |  message: test
              |pushMaquette:
              |  from: maquette
              |  to: cvs
              |  repo:
              |    name: test
              |    repository: https://github.com/test/test.git
              |    credentials:
              |      username: u
              |      password: p
              |  branch: main
              |  message: test
            """.trimMargin()

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply(BakeryPlugin::class.java)
        ext = project.extensions.getByType(BakeryExtension::class.java)
        ext.configPath.set(projectDir.resolve("site.yml").absolutePath)
        projectDir.resolve("site").mkdirs()
        projectDir.resolve("maquette").mkdirs()
        projectDir.resolve("site.yml").writeText(siteYamlContent)
    }

    @Test
    fun `collectSiteContext task is registered under collect group`() {
        (project as ProjectInternal).evaluate()

        val task = project.tasks.findByName("collectSiteContext")
        assertThat(task).isNotNull
        assertThat(task!!.group).isEqualTo("collect")
    }

    @Test
    fun `collectSiteContext with augmented context enabled registers task`() {
        ext.augmentedContext { it.enabled = true }
        (project as ProjectInternal).evaluate()

        assertThat(project.tasks.findByName("collectSiteContext")).isNotNull
    }

    @Test
    fun `collectSiteContext with augmented context disabled still registers task`() {
        ext.augmentedContext { it.enabled = false }
        (project as ProjectInternal).evaluate()

        assertThat(project.tasks.findByName("collectSiteContext")).isNotNull
    }

    @Test
    fun `collectAugmentedContext task registered when enabled`() {
        ext.augmentedContext { it.enabled = true }
        (project as ProjectInternal).evaluate()

        assertThat(project.tasks.findByName("collectAugmentedContext")).isNotNull
        assertThat(project.tasks.findByName("collectSiteContext")).isNotNull
    }
}