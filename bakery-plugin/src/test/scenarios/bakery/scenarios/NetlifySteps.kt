package bakery.scenarios

import bakery.deploy.DeployPayload
import bakery.deploy.PayloadFile
import bakery.netlify.NetlifyDecision
import bakery.netlify.NetlifyDeployPlan
import bakery.netlify.NetlifyDeploymentRecord
import bakery.netlify.NetlifyPlanner
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.nio.file.Files

/**
 * BDD steps for the Netlify deployment plan (BKY-NETLIFY, US-6).
 *
 * Pure domain: a temporary baked tree, the same [DeployPayload] exclusions the
 * task uses, and [NetlifyPlanner]. No HTTP, no Gradle task.
 */
class NetlifySteps {
    private var root = Files.createTempDirectory("bakery-netlify-bdd").toFile()
    private var files: List<PayloadFile> = emptyList()
    private var previous: NetlifyDeploymentRecord? = null
    private var plan: NetlifyDeployPlan? = null

    @Given("a netlify baked site with a home page")
    fun aBakedSite() {
        root = Files.createTempDirectory("bakery-netlify-bdd").toFile()
        root.resolve("index.html").writeText("<html>home</html>")
        files = emptyList()
        previous = null
        plan = null
    }

    @Given("a netlify CNAME file")
    fun aCnameFile() {
        root.resolve("CNAME").writeText("example.org")
    }

    @Given("a netlify editor backup file")
    fun aBackupFile() {
        root.resolve("old.bak").writeText("stale")
    }

    @Given("a netlify language variant {string}")
    fun aLanguageVariant(language: String) {
        root.resolve(language).mkdirs()
        root.resolve("$language/index.html").writeText("<html>$language</html>")
    }

    @Given("a netlify search index")
    fun aSearchIndex() {
        root.resolve("pagefind").mkdirs()
        root.resolve("pagefind/pagefind.js").writeText("// index")
    }

    @Given("a previously deployed netlify digest for the same site")
    fun aPreviousDigestForTheSameSite() {
        files = collect()
        previous = NetlifyDeploymentRecord(siteId = "site-123", digest = DeployPayload.digest(files), deployId = "dep-previous")
    }

    @Given("a previously deployed netlify digest for another site")
    fun aPreviousDigestForAnotherSite() {
        files = collect()
        previous = NetlifyDeploymentRecord(siteId = "other-site", digest = DeployPayload.digest(files))
    }

    @When("I collect the netlify payload")
    fun iCollectThePayload() {
        files = collect()
    }

    @When("I plan a netlify deployment for site {string}")
    fun iPlanForSite(siteId: String) {
        files = collect()
        plan = NetlifyPlanner.plan(siteId, files, previous)
    }

    @When("I plan a netlify dry run for site {string}")
    fun iPlanADryRun(siteId: String) {
        files = collect()
        plan = NetlifyPlanner.plan(siteId, files, previous, dryRun = true)
    }

    @Then("the netlify plan is skipped")
    fun thePlanIsSkipped() {
        assertThat(plan!!.decision).isInstanceOf(NetlifyDecision.Skip::class.java)
    }

    @Then("the netlify plan is deployed")
    fun thePlanIsDeployed() {
        assertThat(plan!!.decision).isEqualTo(NetlifyDecision.Deploy)
    }

    @Then("the netlify plan does not upload")
    fun thePlanDoesNotUpload() {
        assertThat(plan!!.shouldUpload).isFalse
    }

    @Then("the netlify plan fails with {string}")
    fun thePlanFailsWith(text: String) {
        assertThat(plan!!.decision).isInstanceOf(NetlifyDecision.Fail::class.java)
        assertThat((plan!!.decision as NetlifyDecision.Fail).message).contains(text)
    }

    @Then("the netlify payload contains {string}")
    fun thePayloadContains(path: String) {
        assertThat(files.map { it.relativePath }).contains(path)
    }

    @Then("the netlify payload does not contain {string}")
    fun thePayloadDoesNotContain(path: String) {
        assertThat(files.map { it.relativePath }).doesNotContain(path)
    }

    private fun collect(): List<PayloadFile> = DeployPayload.collect(root, excludes)

    private val excludes: List<String> = DeployPayload.BASE_EXCLUDES + DeployPayload.NETLIFY_EXCLUDES
}
