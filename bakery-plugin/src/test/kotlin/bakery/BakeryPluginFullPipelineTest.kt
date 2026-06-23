package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

class BakeryPluginFullPipelineTest {

    @TempDir
    lateinit var projectDir: File

    private fun writeSiteYml() {
        projectDir.resolve("site.yml").writeText(
            """
            bake:
              srcPath: site
              destDirPath: bake
              cname: test.com
            pushPage:
              from: bake
              to: cvs
              repo:
                name: pages
                repository: https://github.com/test/pages.git
                credentials:
                  username: u
                  password: p
              branch: gh-pages
              message: pages
            pushMaquette:
              from: maquette
              to: cvs
              repo:
                name: maquette
                repository: https://github.com/test/maquette.git
                credentials:
                  username: u
                  password: p
              branch: main
              message: maquette
            """.trimIndent(),
            UTF_8
        )
    }

    private fun createSiteAndMaquetteDirs() {
        projectDir.resolve("site").mkdirs()
        projectDir.resolve("site/jbake.properties").writeText("site.host=http://localhost", UTF_8)
        projectDir.resolve("maquette").mkdirs()
    }

    private fun buildProject(pushProfile: GitPushConfiguration?): org.gradle.api.Project {
        writeSiteYml()
        createSiteAndMaquetteDirs()
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply(BakeryPlugin::class.java)
        project.extensions.getByType(BakeryExtension::class.java).configPath.set(
            projectDir.resolve("site.yml").absolutePath
        )
        if (pushProfile != null) {
            // pushProfile ne peut pas être injecté via le DSL ; on le patch dans la config YAML directement
            val profile = """
                pushProfile:
                  from: profile
                  to: cvs
                  repo:
                    name: profile
                    repository: https://github.com/test/profile.git
                    credentials:
                      username: u
                      password: p
                  branch: main
                  message: profile
            """.trimIndent()
            projectDir.resolve("site.yml").appendText("\n$profile", UTF_8)
            projectDir.resolve("profile").mkdirs()
        }
        (project as ProjectInternal).evaluate()
        return project
    }

    @Test
    fun `full pipeline tasks are registered when config site and maquette directories exist`() {
        val project = buildProject(pushProfile = null)

        assertThat(project.tasks.findByName("deploySite")).isNotNull
        assertThat(project.tasks.findByName("deployMaquette")).isNotNull
        assertThat(project.tasks.findByName("publishSite")).isNotNull
        assertThat(project.tasks.findByName("pagefind")).isNotNull
        assertThat(project.tasks.findByName("serve")).isNotNull
        assertThat(project.tasks.findByName("collectSiteConfig")).isNotNull
        assertThat(project.tasks.findByName("collectSiteContext")).isNotNull
        assertThat(project.tasks.findByName("collectAugmentedContext")).isNotNull
        assertThat(project.tasks.findByName("generateArticle")).isNotNull
        assertThat(project.tasks.findByName("generateTheme")).isNotNull
        assertThat(project.tasks.findByName("validateFirebaseConfig")).isNotNull
        assertThat(project.tasks.findByName("migrateToI18n")).isNotNull
        assertThat(project.tasks.findByName("deployProfile")).isNotNull
            .isInstanceOf(DeployProfileTask::class.java)
    }

    @Test
    fun `deployProfile task is registered when pushProfile is configured`() {
        val project = buildProject(pushProfile = GitPushConfiguration(
            from = "profile", to = "cvs",
            repo = RepositoryConfiguration("profile", "https://github.com/test/profile.git", RepositoryCredentials("u", "p")),
            branch = "main", message = "profile"
        ))

        assertThat(project.tasks.findByName("deployProfile")).isNotNull
            .isInstanceOf(DeployProfileTask::class.java)
    }
}
