package bakery.article

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
class ArticleIntentionUnitTest {

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

    private fun configureArticleIntention(
        topic: String = "test",
        ton: String? = null,
        audience: String? = null,
        keywords: List<String>? = null,
        lang: String? = null
    ) {
        ext.articleIntention {
            it.topic = topic
            ton?.let { t -> it.ton = t }
            audience?.let { a -> it.audience = a }
            keywords?.let { k -> it.keywords = k }
            lang?.let { l -> it.lang = l }
        }
        (project as ProjectInternal).evaluate()
    }

    @Test
    fun `DSL articleIntention block compiles with topic`() {
        configureArticleIntention(topic = "Kotlin pour Gradle")

        assertThat(ext.articleIntention.topic).isEqualTo("Kotlin pour Gradle")
        assertThat(ext.articleIntention.ton).isEqualTo("informatif")
        assertThat(ext.articleIntention.audience).isEqualTo("general")
    }

    @Test
    fun `DSL articleIntention block compiles with full config`() {
        configureArticleIntention(
            topic = "Kotlin Coroutines",
            ton = "technique",
            audience = "developpeur",
            keywords = listOf("suspend", "flow"),
            lang = "en"
        )

        assertThat(ext.articleIntention.topic).isEqualTo("Kotlin Coroutines")
        assertThat(ext.articleIntention.ton).isEqualTo("technique")
        assertThat(ext.articleIntention.audience).isEqualTo("developpeur")
        assertThat(ext.articleIntention.keywords).containsExactly("suspend", "flow")
        assertThat(ext.articleIntention.lang).isEqualTo("en")
    }

    @Test
    fun `DSL articleIntention with pedagogique ton compiles`() {
        configureArticleIntention(
            topic = "Introduction à Gradle",
            ton = "pedagogique",
            audience = "formateur"
        )

        assertThat(ext.articleIntention.ton).isEqualTo("pedagogique")
        assertThat(ext.articleIntention.audience).isEqualTo("formateur")
    }

    @Test
    fun `generateArticle task is registered with articleIntention DSL`() {
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
        ext.articleIntention { it.topic = "Test article" }
        (project as ProjectInternal).evaluate()

        assertThat(project.tasks.findByName("generateArticle")).isNotNull
    }
}