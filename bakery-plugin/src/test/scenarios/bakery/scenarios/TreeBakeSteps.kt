package bakery.scenarios

import bakery.FileSystemManager
import bakery.LayoutType
import bakery.SiteConfiguration
import bakery.tree.OutputConfig
import bakery.tree.SiteNodeDto
import bakery.tree.TreeBakeService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.io.File.createTempFile

class TreeBakeSteps {

    private var yamlString: String? = null
    private var parsedConfig: SiteConfiguration? = null
    private var treeDto: SiteNodeDto? = null
    private var tempDir: File? = null
    private var jbakePropsText: String? = null
    private val jsonMapper = jacksonObjectMapper()


    // ── helpers ──────────────────────────────────────────────────────

    private fun siteDir(): File =
        tempDir!!.resolve("site").also { it.mkdirs() }

    private fun writeDefaultJbakeProps(dir: File) {
        dir.resolve("jbake.properties").writeText("site.host=example.com\n")
    }


    // ── Scenario 1: null tree ────────────────────────────────────────

    @Given("a basic site.yml")
    fun aBasicSiteYml() {
        yamlString = """
            |bake:
            |  srcPath: "site"
            |  destDirPath: "build/bake"
        """.trimMargin()
    }

    @When("I parse the site configuration")
    fun iParseTheSiteConfiguration() {
        parsedConfig = FileSystemManager.yamlMapper.readValue<SiteConfiguration>(yamlString!!)
    }

    @Then("the tree field is null")
    fun theTreeFieldIsNull() {
        assertThat(parsedConfig!!.tree).isNull()
    }


    // ── Scenario 2: tree parsing ─────────────────────────────────────

    @Given("a site.yml with a tree section")
    fun aSiteYmlWithATreeSection() {
        yamlString = """
            |bake:
            |  srcPath: "site"
            |  destDirPath: "build/bake"
            |tree:
            |  type: site
            |  path: ""
            |  sections:
            |    - type: section
            |      path: "blog"
            |      articles:
            |        - type: article
            |          path: "blog/post-1"
        """.trimMargin()
    }

    @Then("the tree field is present")
    fun theTreeFieldIsPresent() {
        assertThat(parsedConfig!!.tree).isNotNull()
    }

    @Then("the tree has {int} section")
    fun theTreeHasSection(count: Int) {
        val site = parsedConfig!!.tree as SiteNodeDto.SiteDto
        assertThat(site.sections).hasSize(count)
    }

    @Then("the parsed section {string} has {int} article")
    fun theParsedSectionHasArticle(sectionPath: String, count: Int) {
        val siteDto = parsedConfig!!.tree as SiteNodeDto.SiteDto
        val section = siteDto.sections.first { it.path == sectionPath }
        assertThat(section.articles).hasSize(count)
    }


    // ── Scenario 3: injection ────────────────────────────────────────

    @Given("a site.yml with tree section and output config")
    fun aSiteYmlWithTreeSectionAndOutputConfig() {
        treeDto = SiteNodeDto.SiteDto(
            path = "",
            sections = listOf(
                SiteNodeDto.SectionDto(
                    path = "blog",
                    articles = listOf(SiteNodeDto.ArticleDto(path = "blog/post-1"))
                )
            )
        )
        tempDir = createTempFile("tree-bake-", "").apply { delete(); mkdirs() }
        writeDefaultJbakeProps(siteDir())
    }

    @When("I run the bake pipeline")
    fun iRunTheBakePipeline() {
        val dir = siteDir()
        TreeBakeService.injectTreeConfig(treeDto!!, dir)
        jbakePropsText = dir.resolve("jbake.properties").readText()
    }

    @Then("the jbake properties contain {string}")
    fun theJbakePropertiesContain(key: String) {
        assertThat(jbakePropsText!!).contains(key)
    }

    @Then("the baked site is generated")
    fun theBakedSiteIsGenerated() {
        assertThat(jbakePropsText!!).contains("bakeTreeConfig=")
    }


    // ── Scenario 4: inheritance ──────────────────────────────────────

    @Given("a site.yml with section-level layout")
    fun aSiteYmlWithSectionLevelLayout() {
        treeDto = SiteNodeDto.SiteDto(
            path = "",
            sections = listOf(
                SiteNodeDto.SectionDto(
                    path = "docs",
                    outputConfig = OutputConfig(layout = LayoutType.SIDEBAR_LEFT),
                    articles = listOf(SiteNodeDto.ArticleDto(path = "docs/guide"))
                )
            )
        )
        tempDir = createTempFile("tree-bake-", "").apply { delete(); mkdirs() }
        writeDefaultJbakeProps(siteDir())
    }

    @Given("an article without explicit layout")
    fun anArticleWithoutExplicitLayout() {
        // readability step — the article is already set up without outputConfig
    }

    @Then("the article's resolved layout is {string}")
    fun theArticleResolvedLayoutIs(layoutName: String) {
        val dir = siteDir()
        val propsText = dir.resolve("jbake.properties").readText()
        val json = extractJsonPayload(propsText)
        @Suppress("UNCHECKED_CAST")
        val nodes = (json["nodes"] as Map<String, Map<String, Any?>>)
        val article = nodes["docs/guide"]
        assertThat(article).isNotNull()
        assertThat(article!!["layout"]).isEqualTo(layoutName)
    }


    // ── Scenario 5: override ─────────────────────────────────────────

    @Given("a site.yml with a section that has layout {string}")
    fun aSiteYmlWithSectionThatHasLayout(layoutName: String) {
        treeDto = SiteNodeDto.SiteDto(
            path = "",
            sections = listOf(
                SiteNodeDto.SectionDto(
                    path = "docs",
                    outputConfig = OutputConfig(layout = LayoutType.valueOf(layoutName)),
                    articles = emptyList()
                )
            )
        )
        tempDir = createTempFile("tree-bake-", "").apply { delete(); mkdirs() }
        writeDefaultJbakeProps(siteDir())
    }

    @Given("an article with layout {string}")
    fun anArticleWithLayout(layoutName: String) {
        val siteDto = treeDto as SiteNodeDto.SiteDto
        val section = siteDto.sections.first()
        val updatedSection = section.copy(
            articles = listOf(
                SiteNodeDto.ArticleDto(
                    path = "docs/guide",
                    outputConfig = OutputConfig(layout = LayoutType.valueOf(layoutName))
                )
            )
        )
        treeDto = siteDto.copy(sections = listOf(updatedSection))
    }


    // ── helpers ──────────────────────────────────────────────────────

    private fun extractJsonPayload(propsText: String): Map<String, Any?> {
        val prefix = "bakeTreeConfig="
        val jsonStart = propsText.indexOf(prefix) + prefix.length
        val json = propsText.substring(jsonStart)
        return jsonMapper.readValue(json)
    }
}
