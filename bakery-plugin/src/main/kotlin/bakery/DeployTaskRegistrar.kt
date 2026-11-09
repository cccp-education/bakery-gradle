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
        gitConfig: GitPushConfiguration,
    ) {
        tasks.register(taskName) { task ->
            task.apply {
                group = taskGroup
                description = taskDescription
                dependsOnTask?.let { dependsOn(it) }
                doFirst { doFirstAction(this) }
                doLast {
                    pushPages(fromPath, toPath, gitConfig, logger)
                        .onLeft { throw IllegalStateException(it) }
                }
            }
        }
    }

    internal fun Project.registerDeploySiteTask(site: SiteConfiguration) {
        val buildDir =
            layout.buildDirectory
                .get()
                .asFile.absolutePath
        val destDirPath = site.bake.destDirPath
        val pushPage = site.pushPage

        registerGitPushTask(
            taskName = "deploySite",
            taskDescription = "Deploy site online.",
            dependsOnTask = "pagefind",
            // BKY-LANG-NAV-8 — the deployable variant tree is produced by
            // `bakeVariants` (after `bake`). Without this dependency, the `{lang}/`
            // sub-trees would never be published (the reference alone would ship).
            doFirstAction = { site.createCnameFile(project) },
            fromPath = { "$buildDir$separator$destDirPath" },
            toPath = { "$buildDir$separator${pushPage.to}" },
            gitConfig = pushPage,
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
            gitConfig = site.pushMaquette,
        )
    }

    private fun Project.prepareAndCopyMaquette(site: SiteConfiguration) {
        val uiDir =
            layout.projectDirectory.asFile
                .resolve(site.pushMaquette.from)
        val uiBuildDir =
            layout.buildDirectory.asFile
                .get()
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
            .apply(logger::lifecycle)

        uiBuildDir.path
            .apply(logger::lifecycle)

        uiDir.copyRecursively(uiBuildDir, overwrite = true)
    }

    /**
     * BKY-CI-MIRROR — publishes the site source (single source of truth:
     * `office/sites/<site>`) plus the workflow overlay (`.ci-mirror/`) to a CI
     * runner repository.
     *
     * It reuses the exact same primitive as [registerDeploySiteTask]:
     * `registerGitPushTask` → `GitService.pushPages` (copy into `build/`,
     * `git init/add/commit`, force-push with the configured credentials).
     * Only the source and the target repository differ.
     *
     * `pushCiMirror.to` is the build-relative scratch dir (default `ci-mirror-push`);
     * `pushCiMirror.from` is ignored (the payload is always `build/ci-mirror`),
     * mirroring `deploySite` whose source is `bake.destDirPath`, not `pushPage.from`.
     */
    internal fun Project.registerCiMirrorTask(
        site: SiteConfiguration,
        configDir: File,
    ) {
        val buildDir = layout.buildDirectory.get().asFile
        val mirror = site.pushCiMirror
        val payloadDir = buildDir.resolve("ci-mirror")
        val scratchDir = buildDir.resolve(mirror.to.ifBlank { "ci-mirror-push" })
        val overlayDir = configDir.resolve(".ci-mirror")

        registerGitPushTask(
            taskName = "pushCiMirror",
            taskDescription =
                "Copies the site source + mirror workflows to build/ and pushes them to the CI runner repo.",
            doFirstAction = {
                if (mirror.repo.repository.isBlank()) {
                    throw IllegalStateException(
                        "pushCiMirror.repo.repository is not configured in " +
                            "${configDir.resolve("site.yml").absolutePath}",
                    )
                }
                CiMirrorPayload.assemble(configDir, payloadDir, overlayDir).fold(
                    ifLeft = { throw IllegalStateException(it) },
                    ifRight = {
                        logger.lifecycle(
                            "[bakery] CI mirror payload: ${it.siteFiles} site files + " +
                                "${it.overlayFiles} overlay files → ${it.payloadRoot}",
                        )
                    },
                )
            },
            fromPath = { payloadDir.absolutePath },
            toPath = { scratchDir.absolutePath },
            gitConfig = mirror,
        )
    }

    internal fun Project.registerPublishSiteTask() {
        tasks.register("publishSite") { task ->
            task.apply {
                group = BakeryConstants.PUBLISH_GROUP
                description = "Bake and deploy site (convenience aggregate)"
                dependsOn("bake", "deploySite")
                dependsOn("accessibilityAudit")
            }
        }
    }

    internal fun Project.registerDeployProfileTask() {
        tasks.register("deployProfile", DeployProfileTask::class.java) { task ->
            task.apply {
                group = BakeryConstants.DEPLOY_GROUP
                description = "Push profile files (e.g. README.md) to GitHub repository"
            }
        }
    }
}
