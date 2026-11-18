package bakery.netlify

import bakery.BakeryConstants
import bakery.SiteConfiguration
import org.gradle.api.Project

/**
 * Registers the Netlify tasks (US-5).
 *
 * Registration is conditional (patron `DnsTaskRegistrar`): a site without a
 * `netlify:` section (and without DSL/CLI configuration) never sees these
 * tasks. When the section is present, `deployNetlify` fails loudly on a missing
 * token/site id rather than silently skipping.
 */
object NetlifyTaskRegistrar {
    internal fun Project.registerNetlifyTasks(
        site: SiteConfiguration,
        config: NetlifyConfig,
    ) {
        val buildDir = layout.buildDirectory.get().asFile
        val payloadRoot = buildDir.resolve(site.bake.destDirPath)
        val outputDir = layout.buildDirectory.dir("bakery/netlify")
        val tokenEnvName = config.tokenEnv.ifBlank { DEFAULT_TOKEN_ENV }

        tasks.register("generateNetlifyToml", GenerateNetlifyTomlTask::class.java) { task ->
            task.group = BakeryConstants.GENERATE_GROUP
            task.description = "Writes netlify.toml + platform files (output only, opt-in)"
            task.platformFilesEnabled.set(config.toml.enabled)
            task.publishDir.set(config.toml.publishDir)
            task.notFound.set(config.toml.notFound)
            task.securityHeaders.set(config.toml.securityHeaders)
            task.redirects.set(config.toml.redirects)
            task.outputDir.set(outputDir)
        }

        tasks.register("deployNetlify", DeployNetlifyTask::class.java) { task ->
            task.group = BakeryConstants.DEPLOY_GROUP
            task.description = "Deploys the baked tree to Netlify (digest-aware, dry-run available)"
            task.payloadRoot.set(payloadRoot)
            task.siteId.set(config.siteId)
            task.tokenEnv.set(tokenEnvName)
            // D6 — the token is only ever read from the environment.
            task.token.set(providers.environmentVariable(tokenEnvName).orElse(""))
            task.excludes.set(config.deploy.exclude)
            task.message.set(config.deploy.message)
            task.dryRun.set(config.deploy.dryRun)
            task.force.set(config.deploy.force)
            task.verifyUrl.set(config.verifyUrl)
            task.apiBaseUrl.set(
                providers
                    .gradleProperty("netlify.apiBaseUrl")
                    .orElse(NetlifyApiClient.DEFAULT_BASE_URL),
            )
            task.recordFile.set(outputDir.map { it.file("last-deploy.json") })
            task.reportFile.set(outputDir.map { it.file("deploy.json") })
        }

        tasks.register("publishSiteNetlify") { task ->
            task.group = BakeryConstants.PUBLISH_GROUP
            task.description = "Bake and deploy site to Netlify (convenience aggregate)"
            task.dependsOn("bake", "deployNetlify", "accessibilityAudit")
        }

        // `publishSiteAll` only exists when there is also a Git/Pages target.
        val gitRepository = site.pushPage.repo.repository
        if (gitRepository.isNotBlank()) {
            tasks.register("publishSiteAll") { task ->
                task.group = BakeryConstants.PUBLISH_GROUP
                task.description = "Bake and deploy to every configured target (Pages + Netlify)"
                task.dependsOn("publishSite", "publishSiteNetlify")
            }
        }

        tasks.named("generateNetlifyToml") { task ->
            task.dependsOn("bake")
            task.mustRunAfter("bakeVariants")
        }
        tasks.named("deployNetlify") { task ->
            task.dependsOn("bake", "bakeVariants", "pagefind")
            // D15 — never two writers of the same payload concurrently.
            task.mustRunAfter("deploySite")
        }
    }

    private const val DEFAULT_TOKEN_ENV = "NETLIFY_AUTH_TOKEN"
}
