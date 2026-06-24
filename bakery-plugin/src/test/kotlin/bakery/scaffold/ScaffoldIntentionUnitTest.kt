package bakery.scaffold

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
class ScaffoldIntentionUnitTest {

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

    private fun configureScaffoldIntention(
        description: String = "test",
        siteType: String? = null,
        lang: String? = null,
        projectName: String? = null
    ) {
        ext.scaffoldIntention {
            it.description = description
            siteType?.let { t -> it.siteType = t }
            lang?.let { l -> it.lang = l }
            projectName?.let { n -> it.projectName = n }
        }
        (project as ProjectInternal).evaluate()
    }

    @Test
    fun `DSL scaffoldIntention block compiles with description only`() {
        configureScaffoldIntention(description = "Blog tech Kotlin")

        assertThat(ext.scaffoldIntention.description).isEqualTo("Blog tech Kotlin")
        assertThat(ext.scaffoldIntention.siteType).isEqualTo("blog")
        assertThat(ext.scaffoldIntention.lang).isEqualTo("fr")
    }

    @Test
    fun `DSL scaffoldIntention block compiles with full config`() {
        configureScaffoldIntention(
            description = "Portfolio professionnel Kotlin",
            siteType = "portfolio",
            lang = "en",
            projectName = "kotlin-dev"
        )

        assertThat(ext.scaffoldIntention.description).isEqualTo("Portfolio professionnel Kotlin")
        assertThat(ext.scaffoldIntention.siteType).isEqualTo("portfolio")
        assertThat(ext.scaffoldIntention.lang).isEqualTo("en")
        assertThat(ext.scaffoldIntention.projectName).isEqualTo("kotlin-dev")
    }

    @Test
    fun `generateSiteFromIntention task is registered with scaffoldIntention DSL`() {
        ext.scaffoldIntention {
            it.description = "Documentation API"
            it.siteType = "blog"
        }
        (project as ProjectInternal).evaluate()

        assertThat(project.tasks.findByName("generateSiteFromIntention")).isNotNull
    }

    @Test
    fun `backward compatibility - generateSite task still works without scaffoldIntention`() {
        (project as ProjectInternal).evaluate()

        assertThat(project.tasks.findByName("generateSite")).isNotNull
    }

    @Test
    fun `generateSiteFromIntention with scaffoldIntention and ia blocks both present`() {
        ext.scaffoldIntention {
            it.description = "Formation Kotlin"
            it.siteType = "blog"
        }
        ext.ia {
            it.baseUrl = "http://localhost:11464"
            it.modelName = "gpt-oss:120b-cloud"
        }
        (project as ProjectInternal).evaluate()

        assertThat(ext.scaffoldIntention.description).isEqualTo("Formation Kotlin")
        assertThat(ext.ia.modelName).isEqualTo("gpt-oss:120b-cloud")
        assertThat(project.tasks.findByName("generateSiteFromIntention")).isNotNull
    }
}