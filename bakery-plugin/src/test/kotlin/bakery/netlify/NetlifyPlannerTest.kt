package bakery.netlify

import bakery.deploy.PayloadFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-NETLIFY (US-3, D7/D8) — the plan decides before any network call.
 */
class NetlifyPlannerTest {
    private val files =
        listOf(
            PayloadFile("index.html", 10, "a".repeat(64)),
            PayloadFile("en/index.html", 12, "b".repeat(64)),
        )
    private val digest = bakery.deploy.DeployPayload.digest(files)

    @Test
    @DisplayName("a blank site id is a failure")
    fun blankSiteId() {
        val plan = NetlifyPlanner.plan("", files, previous = null)

        assertThat(plan.decision).isInstanceOf(NetlifyDecision.Fail::class.java)
        assertThat((plan.decision as NetlifyDecision.Fail).message).contains("siteId")
    }

    @Test
    @DisplayName("an empty payload is a failure")
    fun emptyPayload() {
        val plan = NetlifyPlanner.plan("site", emptyList(), previous = null)

        assertThat(plan.decision).isInstanceOf(NetlifyDecision.Fail::class.java)
        assertThat((plan.decision as NetlifyDecision.Fail).message).contains("empty")
    }

    @Test
    @DisplayName("an unchanged payload for the same site is skipped")
    fun unchangedIsSkipped() {
        val previous = NetlifyDeploymentRecord(siteId = "site", digest = digest, deployId = "dep-1")

        val plan = NetlifyPlanner.plan("site", files, previous)

        assertThat(plan.decision).isEqualTo(NetlifyDecision.Skip("payload unchanged since the last deployment (dep-1)"))
        assertThat(plan.shouldUpload).isFalse
    }

    @Test
    @DisplayName("force uploads even when the digest matches")
    fun forceUploads() {
        val previous = NetlifyDeploymentRecord(siteId = "site", digest = digest)

        val plan = NetlifyPlanner.plan("site", files, previous, force = true)

        assertThat(plan.decision).isEqualTo(NetlifyDecision.Deploy)
    }

    @Test
    @DisplayName("a digest from another site is not a reason to skip")
    fun otherSiteDoesNotSkip() {
        val previous = NetlifyDeploymentRecord(siteId = "other", digest = digest)

        val plan = NetlifyPlanner.plan("site", files, previous)

        assertThat(plan.decision).isEqualTo(NetlifyDecision.Deploy)
    }

    @Test
    @DisplayName("a dry run never uploads")
    fun dryRunNeverUploads() {
        val plan = NetlifyPlanner.plan("site", files, previous = null, dryRun = true)

        assertThat(plan.decision).isEqualTo(NetlifyDecision.Deploy)
        assertThat(plan.shouldUpload).isFalse
    }
}
