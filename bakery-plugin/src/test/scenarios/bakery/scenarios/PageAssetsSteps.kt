package bakery.scenarios

import bakery.tree.AssetRef
import bakery.tree.OutputConfig
import bakery.tree.PageAssets
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

class PageAssetsSteps {

    private var assetRef: AssetRef? = null
    private var pageAssets: PageAssets? = null
    private var outputConfig: OutputConfig? = null
    private var parentAssets: PageAssets? = null
    private var childAssets: PageAssets? = null
    private var mergedAssets: PageAssets? = null

    @Given("an asset ref {string}")
    fun anAssetRef(path: String) {
        assetRef = AssetRef(path = path)
    }

    @Given("an asset ref {string} with integrity {string}")
    fun anAssetRefWithIntegrity(path: String, integrity: String) {
        assetRef = AssetRef(path = path, integrity = integrity)
    }

    @Given("an asset ref {string} with defer")
    fun anAssetRefWithDefer(path: String) {
        assetRef = AssetRef(path = path, defer = true)
    }

    @Given("an asset ref {string} with async")
    fun anAssetRefWithAsync(path: String) {
        assetRef = AssetRef(path = path, async = true)
    }

    @Then("the asset path is {string}")
    fun theAssetPathIs(expected: String) {
        assertThat(assetRef!!.path).isEqualTo(expected)
    }

    @Then("the asset has no integrity hash")
    fun theAssetHasNoIntegrityHash() {
        assertThat(assetRef!!.integrity).isNull()
    }

    @Then("the asset is not async")
    fun theAssetIsNotAsync() {
        assertThat(assetRef!!.async).isNull()
    }

    @Then("the asset is not deferred")
    fun theAssetIsNotDeferred() {
        assertThat(assetRef!!.defer).isNull()
    }

    @Then("the asset integrity is {string}")
    fun theAssetIntegrityIs(expected: String) {
        assertThat(assetRef!!.integrity).isEqualTo(expected)
    }

    @Then("the asset is async")
    fun theAssetIsAsync() {
        assertThat(assetRef!!.async).isTrue()
    }

    @Then("the asset is deferred")
    fun theAssetIsDeferred() {
        assertThat(assetRef!!.defer).isTrue()
    }

    @Given("page assets with css {string} {string}")
    fun pageAssetsWithCss(css1: String, css2: String) {
        pageAssets = PageAssets(css = listOf(AssetRef(path = css1), AssetRef(path = css2)))
    }

    @Given("page assets with css {string} and js {string} {string}")
    fun pageAssetsWithCssAndJs(css: String, js1: String, js2: String) {
        pageAssets = PageAssets(
            css = listOf(AssetRef(path = css)),
            js = listOf(AssetRef(path = js1), AssetRef(path = js2))
        )
    }

    @Then("the page assets have {int} css entries")
    fun thePageAssetsHaveCssEntries(count: Int) {
        assertThat(pageAssets!!.css).hasSize(count)
    }

    @Then("the page assets have no js entries")
    fun thePageAssetsHaveNoJsEntries() {
        assertThat(pageAssets!!.js).isNull()
    }

    @Then("the page assets have {int} js entries")
    fun thePageAssetsHaveJsEntries(count: Int) {
        assertThat(pageAssets!!.js).hasSize(count)
    }

    @Given("an output config with page assets css {string} and js {string} with defer")
    fun anOutputConfigWithPageAssets(css: String, js: String) {
        val assets = PageAssets(
            css = listOf(AssetRef(path = css)),
            js = listOf(AssetRef(path = js, defer = true))
        )
        outputConfig = OutputConfig(assets = assets)
    }

    @Then("the output config has {int} css assets")
    fun theOutputConfigHasCssAsset(count: Int) {
        assertThat(outputConfig!!.assets!!.css).hasSize(count)
    }

    @Then("the output config has {int} js assets")
    fun theOutputConfigHasJsAsset(count: Int) {
        assertThat(outputConfig!!.assets!!.js).hasSize(count)
    }

    @Then("the js asset is deferred")
    fun theJsAssetIsDeferred() {
        assertThat(outputConfig!!.assets!!.js!![0].defer).isTrue()
    }

    @Given("parent page assets with css {string}")
    fun parentPageAssetsWithCss(css: String) {
        parentAssets = PageAssets(css = listOf(AssetRef(path = css)))
    }

    @Given("child page assets with css {string}")
    fun childPageAssetsWithCss(css: String) {
        childAssets = PageAssets(css = listOf(AssetRef(path = css)))
    }

    @Given("parent page assets with js {string}")
    fun parentPageAssetsWithJs(js: String) {
        parentAssets = PageAssets(js = listOf(AssetRef(path = js)))
    }

    @When("I merge child into parent")
    fun iMergeChildIntoParent() {
        mergedAssets = childAssets!!.merge(parentAssets)
    }

    @Then("the merged css is {string}")
    fun theMergedCssIs(expected: String) {
        assertThat(mergedAssets!!.css).containsExactly(AssetRef(path = expected))
    }

    @Then("the merged js is null")
    fun theMergedJsIsNull() {
        assertThat(mergedAssets!!.js).isNull()
    }

    @Then("the merged js is {string}")
    fun theMergedJsIs(expected: String) {
        assertThat(mergedAssets!!.js).containsExactly(AssetRef(path = expected))
    }
}
