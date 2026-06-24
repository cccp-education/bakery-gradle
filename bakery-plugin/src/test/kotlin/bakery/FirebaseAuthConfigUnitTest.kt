package bakery

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
class FirebaseAuthConfigUnitTest {

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
    fun `DSL firebaseAuth block compiles with config`() {
        ext.firebaseAuth {
            it.apiKey = "AIzaSy-test"
            it.authDomain = "test-project.firebaseapp.com"
            it.projectId = "test-project"
        }
        (project as ProjectInternal).evaluate()

        assertThat(ext.firebaseAuth.apiKey).isEqualTo("AIzaSy-test")
        assertThat(ext.firebaseAuth.authDomain).isEqualTo("test-project.firebaseapp.com")
        assertThat(ext.firebaseAuth.projectId).isEqualTo("test-project")
    }

    @Test
    fun `validateFirebaseConfig task is registered in validate group`() {
        ext.firebaseAuth {
            it.apiKey = "AIzaSy-test"
            it.authDomain = "test-project.firebaseapp.com"
            it.projectId = "test-project"
        }
        (project as ProjectInternal).evaluate()

        val task = project.tasks.findByName("validateFirebaseConfig")
        assertThat(task).isNotNull
        assertThat(task!!.group).isEqualTo("validate")
    }

    @Test
    fun `backward compatibility - site without firebase config still compiles`() {
        (project as ProjectInternal).evaluate()

        assertThat(ext.firebaseAuth.apiKey).isEmpty()
        assertThat(project.tasks.findByName("validateFirebaseConfig")).isNotNull
    }
}