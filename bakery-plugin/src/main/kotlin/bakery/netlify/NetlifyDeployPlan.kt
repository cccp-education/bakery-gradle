package bakery.netlify

import bakery.deploy.DeployPayload
import bakery.deploy.PayloadFile

/**
 * Last successful Netlify deployment, persisted under `build/bakery/netlify/`.
 *
 * The digest is the only thing the next run needs to decide a skip; the rest is
 * reporting (deploy id, URL) and must never contain a credential.
 */
data class NetlifyDeploymentRecord(
    val siteId: String,
    val digest: String,
    val deployId: String = "",
    val deployUrl: String = "",
    val deployedAt: String = "",
)

/**
 * Pure deployment plan (US-3): no network, no file system.
 *
 * A plan is `Skipped` when the payload digest matches the last successful
 * deployment for the same site (Ink Economy, D7), unless `force` is set.
 */
data class NetlifyDeployPlan(
    val siteId: String,
    val digest: String,
    val fileCount: Int,
    val bytes: Long,
    val dryRun: Boolean,
    val force: Boolean,
    val decision: NetlifyDecision,
) {
    val shouldUpload: Boolean get() = decision is NetlifyDecision.Deploy && !dryRun
}

sealed interface NetlifyDecision {
    data object Deploy : NetlifyDecision

    data class Skip(
        val reason: String,
    ) : NetlifyDecision

    data class Fail(
        val message: String,
    ) : NetlifyDecision
}

object NetlifyPlanner {
    fun plan(
        siteId: String,
        files: List<PayloadFile>,
        previous: NetlifyDeploymentRecord?,
        force: Boolean = false,
        dryRun: Boolean = false,
    ): NetlifyDeployPlan {
        val digest = DeployPayload.digest(files)
        val bytes = DeployPayload.size(files)
        val decision =
            when {
                siteId.isBlank() -> NetlifyDecision.Fail("netlify.siteId is not configured")
                files.isEmpty() -> NetlifyDecision.Fail("the deployment payload is empty")
                !force && previous != null && previous.siteId == siteId && previous.digest == digest ->
                    NetlifyDecision.Skip("payload unchanged since the last deployment (${previous.deployId})")
                else -> NetlifyDecision.Deploy
            }
        return NetlifyDeployPlan(
            siteId = siteId,
            digest = digest,
            fileCount = files.size,
            bytes = bytes,
            dryRun = dryRun,
            force = force,
            decision = decision,
        )
    }
}
