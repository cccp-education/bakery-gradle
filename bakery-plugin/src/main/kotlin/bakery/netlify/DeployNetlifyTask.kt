package bakery.netlify

import bakery.deploy.DeployOutcome
import bakery.deploy.DeployPayload
import bakery.deploy.DeployTarget
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.time.Instant

/**
 * Deploys the baked tree to Netlify (US-5).
 *
 * Order of decisions (D7/D8):
 * 1. plan the payload (pure) — `CNAME`, backups and the `.git` directory are excluded;
 * 2. `Skipped` when the digest matches the last successful deploy;
 * 3. `dryRun` prints the plan and never touches the network nor the token;
 * 4. otherwise the token is required (explicit failure, never a silent skip).
 *
 * The token is `@Internal`: never hashed, never cached, never logged.
 */
@DisableCachingByDefault(because = "Deployment side effect on the live Netlify site")
abstract class DeployNetlifyTask : DefaultTask() {
    init {
        // A deployment is never "up to date": the digest-skip in the plan is the
        // idempotence mechanism. Gradle up-to-date would hide a failed upload
        // whose report had already been written.
        outputs.upToDateWhen { false }
    }

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val payloadRoot: DirectoryProperty

    @get:Input
    abstract val siteId: Property<String>

    @get:Input
    abstract val tokenEnv: Property<String>

    @get:Internal
    abstract val token: Property<String>

    @get:Input
    abstract val excludes: ListProperty<String>

    @get:Input
    abstract val message: Property<String>

    @get:Input
    abstract val dryRun: Property<Boolean>

    @get:Input
    abstract val force: Property<Boolean>

    @get:Input
    abstract val verifyUrl: Property<String>

    /** Internal seam for tests — never a user-facing setting. */
    @get:Internal
    abstract val apiBaseUrl: Property<String>

    // Local state, NOT an output: Gradle cleans stale outputs before a first
    // execution, which would delete the previous deployment digest and turn a
    // legitimate skip into a fresh upload (discovered by the CI functional run).
    @get:LocalState
    abstract val recordFile: RegularFileProperty

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun deploy() {
        val root = payloadRoot.get().asFile
        val files =
            DeployPayload.collect(
                root,
                (DeployPayload.BASE_EXCLUDES + DeployPayload.NETLIFY_EXCLUDES + excludes.get()).distinct(),
            )
        val previous = NetlifyDeploymentRecordStore.read(recordFile.get().asFile)
        val plan = NetlifyPlanner.plan(siteId.get(), files, previous, force.get(), dryRun.get())

        logger.lifecycle(
            "[netlify] site={} files={} bytes={} digest={} dryRun={} force={}",
            plan.siteId,
            plan.fileCount,
            plan.bytes,
            plan.digest,
            plan.dryRun,
            plan.force,
        )

        when (val decision = plan.decision) {
            is NetlifyDecision.Fail -> {
                writeReport(plan, DeployOutcome.Failed(DeployTarget.Netlify, decision.message), deployId = null)
                throw GradleException("Netlify deployment refused: ${decision.message}")
            }

            is NetlifyDecision.Skip -> {
                logger.lifecycle("[netlify] skipped — {}", decision.reason)
                writeReport(plan, DeployOutcome.Skipped(DeployTarget.Netlify, decision.reason), deployId = null)
            }

            NetlifyDecision.Deploy -> {
                if (plan.dryRun) {
                    logger.lifecycle("[netlify] dry-run — no upload, no token read")
                    writeReport(plan, DeployOutcome.Skipped(DeployTarget.Netlify, "dry-run"), deployId = null)
                    return
                }
                val tokenValue = token.get()
                if (tokenValue.isBlank()) {
                    val message = "Netlify token is empty: set the environment variable '${tokenEnv.get()}' (never a Gradle property)"
                    writeReport(plan, DeployOutcome.Failed(DeployTarget.Netlify, message), deployId = null)
                    throw GradleException(message)
                }

                val zip = NetlifyArchive.build(root, files)
                val client =
                    NetlifyApiClient(
                        http = JavaNetlifyHttp(),
                        token = tokenValue,
                        baseUrl = apiBaseUrl.get(),
                    )
                when (val result = client.deploy(plan.siteId, zip)) {
                    is NetlifyApiResult.Failure -> {
                        writeReport(plan, DeployOutcome.Failed(DeployTarget.Netlify, result.message), deployId = null)
                        throw GradleException("Netlify deployment failed: ${result.message}")
                    }

                    is NetlifyApiResult.Success -> {
                        val deploy = result.deploy
                        NetlifyDeploymentRecordStore.write(
                            recordFile.get().asFile,
                            NetlifyDeploymentRecord(
                                siteId = plan.siteId,
                                digest = plan.digest,
                                deployId = deploy.id,
                                deployUrl = deploy.url,
                                deployedAt = Instant.now().toString(),
                            ),
                        )
                        writeReport(plan, DeployOutcome.Deployed(DeployTarget.Netlify, deploy.url, deploy.id), deploy.id)
                        logger.lifecycle("[netlify] deployed {} (id={})", deploy.url, deploy.id)
                        smoke(deploy.url)
                    }
                }
            }
        }
    }

    private fun smoke(deployUrl: String) {
        val url = verifyUrl.get()
        if (url.isBlank()) return
        val response = JavaNetlifyHttp().get(url, "")
        if (response.status !in 200..399 || response.body.isBlank()) {
            throw GradleException(
                "Netlify smoke check failed for '$url' (HTTP ${response.status}) — deploy URL was $deployUrl",
            )
        }
        logger.lifecycle("[netlify] smoke OK — {} answered {}", url, response.status)
    }

    private fun writeReport(
        plan: NetlifyDeployPlan,
        outcome: DeployOutcome,
        deployId: String?,
    ) {
        val report =
            linkedMapOf(
                "siteId" to plan.siteId,
                "digest" to plan.digest,
                "fileCount" to plan.fileCount.toString(),
                "bytes" to plan.bytes.toString(),
                "dryRun" to plan.dryRun.toString(),
                "force" to plan.force.toString(),
                "decision" to plan.decision::class.simpleName.orEmpty(),
                "outcome" to outcome::class.simpleName.orEmpty(),
                "deployId" to (deployId ?: ""),
                "message" to message.get(),
            )
        val file = reportFile.get().asFile
        file.parentFile?.mkdirs()
        file.writeText(jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report))
    }
}
