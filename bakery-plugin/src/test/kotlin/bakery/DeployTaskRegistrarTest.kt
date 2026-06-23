package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DeployTaskRegistrarTest {

    @TempDir
    lateinit var tempDir: File

    private fun createProject(): org.gradle.api.Project =
        ProjectBuilder.builder().withProjectDir(tempDir.resolve("project").apply { mkdirs() }).build()

    @Test
    fun `registerPublishSiteTask creates publishSite aggregate with bake and deploySite dependencies`() {
        val project = createProject()

        DeployTaskRegistrar.run { project.registerPublishSiteTask() }

        val publishSite = project.tasks.findByName("publishSite")
        assertThat(publishSite).isNotNull
        assertThat(publishSite!!.group).isEqualTo(BakeryConstants.PUBLISH_GROUP)
        assertThat(publishSite.description).isEqualTo("Bake and deploy site (convenience aggregate)")
        assertThat(publishSite.dependsOn)
            .contains("bake", "deploySite")
    }

    @Test
    fun `registerDeployProfileTask creates deployProfile task in deploy group`() {
        val project = createProject()

        DeployTaskRegistrar.run { project.registerDeployProfileTask() }

        val deployProfile = project.tasks.findByName("deployProfile")
        assertThat(deployProfile).isNotNull
        assertThat(deployProfile!!.group).isEqualTo(BakeryConstants.DEPLOY_GROUP)
        assertThat(deployProfile.description).isEqualTo("Push profile files (e.g. README.md) to GitHub repository")
        assertThat(deployProfile).isInstanceOf(DeployProfileTask::class.java)
    }
}
