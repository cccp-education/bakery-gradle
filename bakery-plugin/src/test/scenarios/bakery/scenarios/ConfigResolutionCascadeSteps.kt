package bakery.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import bakery.*

class ConfigResolutionCascadeSteps {

    private lateinit var project: Project
    private lateinit var extension: BakeryExtension
    private var resolvedConfigs: ResolvedConfigs? = null
    private var errors: List<ConfigResolutionError> = emptyList()

    @Given("a new Bakery project")
    fun createNewBakeryProject() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "bakery-cascade-test-${System.currentTimeMillis()}").apply { mkdirs() }
        project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        extension = project.extensions.create("bakery_cascade_test", BakeryExtension::class.java) as BakeryExtension
    }

    @When("I resolve all configs with defaults")
    fun resolveAllWithDefaults() {
        val site = SiteConfiguration()
        val (configs, errs) = ConfigResolver.resolveAll(emptyMap(), extension, site)
        resolvedConfigs = configs
        errors = errs
    }

    @When("I resolve all configs with CLI override {string}")
    fun resolveAllWithCliOverride(cliProps: String) {
        val props = cliProps.split(",").associate { pair ->
            val (key, value) = pair.split("=", limit = 2)
            key.trim() to value.trim()
        }
        val site = SiteConfiguration()
        val (configs, errs) = ConfigResolver.resolveAll(props, extension, site)
        resolvedConfigs = configs
        errors = errs
    }

    @Then("all 8 resolved configs should have default values")
    fun allConfigsHaveDefaults() {
        val configs = resolvedConfigs ?: throw IllegalStateException("Configs not resolved")
        assertThat(configs.firebase.apiKey).isEmpty()
        assertThat(configs.firebase.projectId).isEmpty()
        assertThat(configs.googleForms.formId).isEmpty()
        assertThat(configs.firebaseAuth.apiKey).isEmpty()
        assertThat(configs.comments.enabled).isFalse
        assertThat(configs.analytics.provider).isEmpty()
        assertThat(configs.newsletter.enabled).isFalse()
        assertThat(configs.theme.mode).isEqualTo("auto")
        assertThat(configs.layout.layoutType).isEqualTo(LayoutType.FULL_WIDTH)
    }

    @Then("no resolution errors should be reported")
    fun noErrors() {
        assertThat(errors).isEmpty()
    }

    @Then("the resolved config should have googleForms.formId = {string}")
    fun resolvedGoogleFormsFormId(formId: String) {
        assertThat(resolvedConfigs?.googleForms?.formId).isEqualTo(formId)
    }

    @Then("the resolved config should have analytics.provider = {string}")
    fun resolvedAnalyticsProvider(provider: String) {
        assertThat(resolvedConfigs?.analytics?.provider).isEqualTo(provider)
    }

    @Then("the resolved config should have theme.mode = {string}")
    fun resolvedThemeMode(mode: String) {
        assertThat(resolvedConfigs?.theme?.mode).isEqualTo(mode)
    }

    @Given("a fully populated ResolvedConfigs")
    fun fullyPopulatedResolvedConfigs() {
        resolvedConfigs = ResolvedConfigs(
            firebase = FirebaseProjectInfo(projectId = "proj-1", apiKey = "key-1"),
            googleForms = GoogleFormsConfig(formId = "form-1", width = "800", height = "600", allowMultiple = true),
            firebaseAuth = FirebaseAuthConfig(apiKey = "auth-key", authDomain = "auth-domain", projectId = "proj-1"),
            comments = CommentsConfig(enabled = true, collection = "my-comments"),
            analytics = AnalyticsConfig(provider = "matomo", domain = "stats.example.com", scriptSrc = "https://cdn.matomo"),
            newsletter = NewsletterConfig(enabled = true, provider = "mailchimp", endpoint = "https://api.mc"),
            theme = ThemeConfig(mode = "dark", primaryColor = "#333", secondaryColor = "#666", variant = "magazine"),
            layout = LayoutConfig(layoutType = LayoutType.SIDEBAR_LEFT)
        )
    }

    @When("I convert it to a resolver function")
    fun convertToResolver() {
        // No-op: toResolver() is called in the Then steps
    }

    @Then("the resolver should map firebaseApiKey to the firebase api key")
    fun resolverMapsFirebaseApiKey() {
        val resolver = resolvedConfigs?.toResolver() ?: throw IllegalStateException("No configs")
        assertThat(resolver("firebaseApiKey", "")).isEqualTo("key-1")
    }

    @Then("the resolver should map themeMode to the theme mode")
    fun resolverMapsThemeMode() {
        val resolver = resolvedConfigs?.toResolver() ?: throw IllegalStateException("No configs")
        assertThat(resolver("themeMode", "")).isEqualTo("dark")
    }

    @Then("the resolver should map layoutType to the layout type name")
    fun resolverMapsLayoutType() {
        val resolver = resolvedConfigs?.toResolver() ?: throw IllegalStateException("No configs")
        assertThat(resolver("layoutType", "")).isEqualTo("SIDEBAR_LEFT")
    }
}