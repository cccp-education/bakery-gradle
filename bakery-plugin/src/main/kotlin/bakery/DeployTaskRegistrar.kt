package bakery

import bakery.FileSystemManager.createCnameFile
import bakery.GitService.pushPages
import org.gradle.api.Project
import java.io.File
import java.io.File.separator

object DeployTaskRegistrar {

    internal fun Project.registerGitPushTask(
        taskName: String,
        taskDescription: String,
        taskGroup: String = BakeryConstants.DEPLOY_GROUP,
        dependsOnTask: String? = null,
        doFirstAction: (org.gradle.api.Task).() -> Unit = {},
        fromPath: () -> String,
        toPath: () -> String,
        gitConfig: GitPushConfiguration
    ) {
        tasks.register(taskName) { task ->
            task.apply {
                group = taskGroup
                description = taskDescription
                dependsOnTask?.let { dependsOn(it) }
                doFirst { doFirstAction(this) }
                doLast {
                    pushPages(fromPath, toPath, gitConfig, logger)
                }
            }
        }
    }

    internal fun Project.registerDeploySiteTask(site: SiteConfiguration) {
        val buildDir = layout.buildDirectory.get().asFile.absolutePath
        val destDirPath = site.bake.destDirPath
        val pushPage = site.pushPage

        registerGitPushTask(
            taskName = "deploySite",
            taskDescription = "Deploy site online.",
            dependsOnTask = "pagefind",
            doFirstAction = { site.createCnameFile(project) },
            fromPath = { "$buildDir$separator$destDirPath" },
            toPath = { "$buildDir$separator${pushPage.to}" },
            gitConfig = pushPage
        )
    }

    internal fun Project.registerDeployMaquetteTask(site: SiteConfiguration) {
        registerGitPushTask(
            taskName = "deployMaquette",
            taskDescription = "Deploy maquette online.",
            doFirstAction = { prepareAndCopyMaquette(site) },
            fromPath = {
                val buildDir = layout.buildDirectory.asFile.get()
                buildDir.resolve(site.pushMaquette.from).absolutePath
            },
            toPath = {
                val buildDir = layout.buildDirectory.get().asFile
                buildDir.resolve(site.pushMaquette.to).absolutePath
            },
            gitConfig = site.pushMaquette
        )
    }

    private fun Project.prepareAndCopyMaquette(site: SiteConfiguration) {
        val uiDir = layout.projectDirectory.asFile
            .resolve(site.pushMaquette.from)
        val uiBuildDir = layout.buildDirectory.asFile.get()
            .resolve(site.pushMaquette.from)

        // Validation
        if (!uiDir.exists()) throw IllegalStateException("$uiDir does not exist")
        if (!uiDir.isDirectory) throw IllegalStateException("$uiDir should be a directory")

        // Préparation du répertoire de build
        if (uiBuildDir.exists()) uiBuildDir.deleteRecursively()
        uiBuildDir.mkdirs()

        if (!uiBuildDir.isDirectory) throw IllegalStateException("$uiBuildDir should be directory")

        // Logging et copie
        uiDir.absolutePath
            .apply(logger::info)
            .run(::println)

        uiBuildDir.path
            .apply(logger::info)
            .run(::println)

        uiDir.copyRecursively(uiBuildDir, overwrite = true)
    }

    internal fun Project.registerDeployProfileTask(site: SiteConfiguration) {
        tasks.register("deployProfile", PublishProfileTask::class.java) { task ->
            task.apply {
                group = BakeryConstants.DEPLOY_GROUP
                description = "Push profile files (e.g. README.md) to GitHub repository"
            }
        }
    }
}
