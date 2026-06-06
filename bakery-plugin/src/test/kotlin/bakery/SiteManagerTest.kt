package bakery

import bakery.FileSystemManager.from
import bakery.BakeryConstants.BAKERY_GROUP
import bakery.BakeryConstants.GENERATE_GROUP
import bakery.BakeryConstants.DEPLOY_GROUP
import bakery.BakeryConstants.TRANSFORM_GROUP
import bakery.BakeryConstants.INFO_GROUP
import bakery.BakeryConstants.COLLECT_GROUP
import bakery.SiteManager.configureConfigPath
import bakery.SiteManager.createJBakeRuntimeConfiguration
import bakery.SiteTaskRegistrar.registerCollectSiteConfigTask
import bakery.DeployTaskRegistrar.registerDeployMaquetteTask
import bakery.DeployTaskRegistrar.registerDeploySiteTask
import bakery.SiteTaskRegistrar.registerPagefindTask
import bakery.SiteTaskRegistrar.registerServeTask
import com.github.gradle.node.npm.task.NpxTask
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.JavaExec
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import java.io.File

class SiteManagerTest {

    @Nested
    inner class RegisterServeTaskTest {

        @TempDir
        lateinit var tempDir: File
        private lateinit var project: Project
        private lateinit var jbakeRuntime: Configuration

        @BeforeEach
        fun setUp() {
            project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            jbakeRuntime = project.configurations.create("jbakeRuntime")
            val siteDir = tempDir.resolve("jbake")
            siteDir.mkdirs()
        }

        @Test
        fun `should register serve task of type JavaExec`() {
            val site = SiteConfiguration(
                bake = BakeConfiguration("jbake", "bake")
            )

            project.registerServeTask(site, jbakeRuntime)

            val serveTask = project.tasks.getByName("serve")
            assertThat(serveTask).isInstanceOf(JavaExec::class.java)
            assertThat(serveTask.group).isEqualTo(INFO_GROUP)
            assertThat(serveTask.description).isEqualTo("Serves the baked site locally.")
        }

        @Test
        fun `serve task should have correct main class`() {
            val site = SiteConfiguration(
                bake = BakeConfiguration("jbake", "bake")
            )

            project.registerServeTask(site, jbakeRuntime)

            val serveTask = project.tasks.getByName("serve") as JavaExec
            assertThat(serveTask.mainClass.get()).isEqualTo("org.jbake.launcher.Main")
        }

        @Test
        fun `serve task should have correct args with src and dest paths`() {
            val site = SiteConfiguration(
                bake = BakeConfiguration("jbake", "bake")
            )

            project.registerServeTask(site, jbakeRuntime)

            val serveTask = project.tasks.getByName("serve") as JavaExec
            assertThat(serveTask.args).contains("-s")
            assertThat(serveTask.args).anyMatch { it.endsWith("jbake") }
            assertThat(serveTask.args).anyMatch { it.endsWith("bake") }
        }
    }

    @Nested
    inner class RegisterPagefindTaskTest {

        @TempDir
        lateinit var tempDir: File
        private lateinit var project: Project

        @BeforeEach
        fun setUp() {
            project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            project.pluginManager.apply("com.github.node-gradle.node")
            val bakedDir = tempDir.resolve("build/bake")
            bakedDir.mkdirs()
            bakedDir.resolve("index.html").createNewFile()
        }

        @Test
        fun `should register pagefind task of type NpxTask`() {
            val site = SiteConfiguration(
                bake = BakeConfiguration("jbake", "bake"),
                pushPage = GitPushConfiguration()
            )

            project.registerPagefindTask(site)

            val pagefindTask = project.tasks.getByName("pagefind")
            assertThat(pagefindTask).isInstanceOf(NpxTask::class.java)
            assertThat(pagefindTask.group).isEqualTo(TRANSFORM_GROUP)
            assertThat(pagefindTask.description).isEqualTo("Index the baked site with Pagefind for full-text search.")
        }

        @Test
        fun `pagefind task should depend on bake task`() {
            val site = SiteConfiguration(
                bake = BakeConfiguration("jbake", "bake"),
                pushPage = GitPushConfiguration()
            )

            project.registerPagefindTask(site)

            val pagefindTask = project.tasks.getByName("pagefind") as NpxTask
            assertThat(pagefindTask.dependsOn.map { it.toString() })
                .anyMatch { it.contains("bake") }
        }

        @Test
        fun `pagefind task should use pagefind command`() {
            val site = SiteConfiguration(
                bake = BakeConfiguration("jbake", "bake"),
                pushPage = GitPushConfiguration()
            )

            project.registerPagefindTask(site)

            val pagefindTask = project.tasks.getByName("pagefind") as NpxTask
            assertThat(pagefindTask.command.get()).isEqualTo("pagefind")
        }

        @Test
        fun `pagefind task should throw when baked dir is empty`() {
            val emptyDir = tempDir.resolve("empty-build")
            emptyDir.mkdirs()
            val emptyProject = ProjectBuilder.builder().withProjectDir(emptyDir).build()
            emptyProject.pluginManager.apply("com.github.node-gradle.node")

            emptyDir.resolve("build/empty-bake").mkdirs()

            val site = SiteConfiguration(
                bake = BakeConfiguration("jbake", "empty-bake"),
                pushPage = GitPushConfiguration()
            )

            emptyProject.registerPagefindTask(site)

            val pagefindTask = emptyProject.tasks.getByName("pagefind") as NpxTask

            assertThatThrownBy {
                pagefindTask.actions.forEach { it.execute(pagefindTask) }
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Baked directory is empty")
        }
    }

    @Nested
    inner class RegisterDeployMaquetteTaskTest {

        @TempDir
        lateinit var tempDir: File
        private lateinit var project: Project

        @BeforeEach
        fun setUp() {
            project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            tempDir.resolve("maquette").mkdirs()
            tempDir.resolve("maquette/index.html").createNewFile()
            project.pluginManager.apply("com.github.node-gradle.node")
        }

        @Test
        fun `should register deployMaquette task`() {
            val site = SiteConfiguration(
                pushMaquette = GitPushConfiguration(from = "maquette", to = "cvs-output"),
                bake = BakeConfiguration("jbake", "bake")
            )

            project.registerDeployMaquetteTask(site)

            val task = project.tasks.getByName("deployMaquette")
            assertThat(task.group).isEqualTo(DEPLOY_GROUP)
            assertThat(task.description).isEqualTo("Deploy maquette online.")
        }

        @Test
        fun `maquette task should fail when maquette dir does not exist`() {
            val noMaquetteDir = tempDir.resolve("nomaquette")
            noMaquetteDir.mkdirs()
            val noMdProject = ProjectBuilder.builder().withProjectDir(noMaquetteDir).build()

            val site = SiteConfiguration(
                pushMaquette = GitPushConfiguration(from = "maquette", to = "cvs-output"),
                bake = BakeConfiguration("jbake", "bake")
            )

            noMdProject.registerDeployMaquetteTask(site)

            val task = noMdProject.tasks.getByName("deployMaquette")

            assertThatThrownBy {
                task.actions.forEach { it.execute(task) }
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("does not exist")
        }
    }

    @Nested
    inner class RegisterDeploySiteTaskTest {

        @TempDir
        lateinit var tempDir: File
        private lateinit var project: Project

        @BeforeEach
        fun setUp() {
            project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            tempDir.resolve("jbake").mkdirs()
            project.pluginManager.apply("com.github.node-gradle.node")
        }

        @Test
        fun `should register deploySite task`() {
            val site = SiteConfiguration(
                bake = BakeConfiguration("jbake", "bake"),
                pushPage = GitPushConfiguration(from = "bake", to = "cvs-output")
            )

            project.registerDeploySiteTask(site)

            val task = project.tasks.getByName("deploySite")
            assertThat(task.group).isEqualTo(DEPLOY_GROUP)
            assertThat(task.description).isEqualTo("Deploy site online.")
        }

        @Test
        fun `deploySite task should depend on pagefind`() {
            val site = SiteConfiguration(
                bake = BakeConfiguration("jbake", "bake"),
                pushPage = GitPushConfiguration(from = "bake", to = "cvs-output")
            )

            project.registerDeploySiteTask(site)

            val task = project.tasks.getByName("deploySite")
            assertThat(task.dependsOn.map { it.toString() })
                .anyMatch { it.contains("pagefind") }
        }
    }

    @Nested
    inner class RegisterCollectSiteConfigTaskTest {

        @TempDir
        lateinit var tempDir: File
        private lateinit var project: Project

        @BeforeEach
        fun setUp() {
            project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            project.pluginManager.apply("com.github.node-gradle.node")
        }

        @Test
        fun `should register collectSiteConfig task`() {
            val site = SiteConfiguration(
                bake = BakeConfiguration("jbake", "bake"),
                pushPage = GitPushConfiguration()
            )

            project.registerCollectSiteConfigTask(site, false)

            val task = project.tasks.getByName("collectSiteConfig")
            assertThat(task.group).isEqualTo(COLLECT_GROUP)
            assertThat(task.description).isEqualTo("Initialize Bakery configuration.")
        }
    }

    @Nested
    inner class ConfigPathResolutionTest {

        @TempDir
        lateinit var tempDir: File
        private lateinit var project: Project

        @BeforeEach
        fun setUp() {
            project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        }

        @Test
        fun `should return Either when gradle properties found but not loaded by test fixture`() {
            val gradleProperties = tempDir.resolve("gradle.properties")
            gradleProperties.writeText("bakery.config.path=my-site.yml")

            val result = project.configureConfigPath(mock(), false)

            assertThat(result.isLeft()).isTrue()
        }

        @Test
        fun `should return right when gradle properties is enabled via DSL`() {
            val gradleProperties = tempDir.resolve("gradle.properties")
            gradleProperties.writeText("bakery.config.path=my-site.yml")

            val result = project.configureConfigPath(mock(), true)

            assertThat(result.isRight()).isTrue()
        }
    }

    @Nested
    inner class CreateJBakeRuntimeConfigurationTest {

        @TempDir
        lateinit var tempDir: File
        private lateinit var project: Project

        @BeforeEach
        fun setUp() {
            project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        }

        @Test
        fun `should create jbakeRuntime configuration`() {
            val config = project.createJBakeRuntimeConfiguration()

            assertThat(config.name).isEqualTo("jbakeRuntime")
            assertThat(config.description).isEqualTo("Classpath for running Jbake core directly")
        }
    }

    @Nested
    inner class YAMLToleranceTest {

        @TempDir
        lateinit var tempDir: File
        private lateinit var project: Project

        @BeforeEach
        fun setUp() {
            project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            tempDir.resolve("maquette").mkdirs()
            tempDir.resolve("maquette/index.html").createNewFile()
            project.pluginManager.apply("com.github.node-gradle.node")
        }

        @Test
        fun `should not fail when site yml contains unknown fields`() {
            val siteYml = tempDir.resolve("site.yml")
            siteYml.writeText("bake:\n  srcPath: \"site\"\n  destDirPath: \"bake\"\n" +
                    "pushPage:\n  from: \"bake\"\n  to: \"cvs\"\n" +
                    "pushMaquette:\n  from: \"maquette\"\n  to: \"cvs\"\n" +
                    "superbase: \"legacy-field\"\nunknownSection:\n  key: value\n")

            val site: SiteConfiguration = project.from(siteYml.path)

            assertThat(site.bake).isNotNull
            assertThat(site.bake.srcPath).isEqualTo("site")
            assertThat(site.bake.destDirPath).isEqualTo("bake")
        }
    }
}
