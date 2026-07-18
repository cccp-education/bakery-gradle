package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * EPIC BKY-I18N-DEPLOY — US DEPLOY-4.
 *
 * Functional tests for the `injectRtlDirection` task. The task walks
 * `content-i18n/{lang}/**/*.adoc` (persistent, source-controlled output of
 * `migrateContentI18n` non-dry-run — NOT `build/` which is wiped by `clean`,
 * per the Ink Economy Law) and applies [bakery.i18n.rtl.RtlDirectionInjector]
 * to each article frontmatter:
 * - RTL languages (ar, ur) get `:jbake-lang: {lang}` + `:lang: rtl`
 * - LTR languages get `:jbake-lang: {lang}` and any pre-existing `:lang: rtl` is dropped
 *
 * Idempotent: re-running yields the same output. Integrated into
 * [bakery.ContentTaskRegistrar] after `registerMigrateContentI18nTask`.
 */
class InjectRtlDirectionFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `injectRtlDirection injects jbake-lang and rtl directive for arabic articles`() {
        createMiniSiteWithTranslatedCopy()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "injectRtlDirection",
                "--contentI18nOutput=content-i18n",
                "--contentI18nTargetLangs=ar"
            )
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        val arArticle = projectDir.resolve("content-i18n/ar/blog/post.adoc").readText()
        assertThat(arArticle).contains(":jbake-lang: ar")
        assertThat(arArticle).contains(":lang: rtl")
    }

    @Test
    fun `injectRtlDirection injects jbake-lang without rtl for french articles`() {
        createMiniSiteWithTranslatedCopy()

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "injectRtlDirection",
                "--contentI18nOutput=content-i18n",
                "--contentI18nTargetLangs=fr"
            )
            .build()

        val frArticle = projectDir.resolve("content-i18n/fr/blog/post.adoc").readText()
        assertThat(frArticle).contains(":jbake-lang: fr")
        assertThat(frArticle).doesNotContain(":lang: rtl")
    }

    @Test
    fun `injectRtlDirection is idempotent on second invocation`() {
        createMiniSiteWithTranslatedCopy()

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "injectRtlDirection",
                "--contentI18nOutput=content-i18n",
                "--contentI18nTargetLangs=ar"
            )
            .build()

        val firstContent = projectDir.resolve("content-i18n/ar/blog/post.adoc").readText()

        val result2 = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "injectRtlDirection",
                "--contentI18nOutput=content-i18n",
                "--contentI18nTargetLangs=ar"
            )
            .build()

        assertThat(result2.output).contains("BUILD SUCCESSFUL")
        val secondContent = projectDir.resolve("content-i18n/ar/blog/post.adoc").readText()
        assertThat(secondContent).isEqualTo(firstContent)
    }

    @Test
    fun `injectRtlDirection drops pre-existing rtl marker when translating to ltr`() {
        createMiniSiteWithTranslatedCopy()

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "injectRtlDirection",
                "--contentI18nOutput=content-i18n",
                "--contentI18nTargetLangs=ar"
            )
            .build()

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "injectRtlDirection",
                "--contentI18nOutput=content-i18n",
                "--contentI18nTargetLangs=fr"
            )
            .build()

        val frArticle = projectDir.resolve("content-i18n/fr/blog/post.adoc").readText()
        assertThat(frArticle).contains(":jbake-lang: fr")
        assertThat(frArticle).doesNotContain(":lang: rtl")
    }

    @Test
    fun `injectRtlDirection skips languages with no directory`() {
        createMiniSiteWithTranslatedCopy()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "injectRtlDirection",
                "--contentI18nOutput=content-i18n",
                "--contentI18nTargetLangs=ar,zh"
            )
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        val arArticle = projectDir.resolve("content-i18n/ar/blog/post.adoc").readText()
        assertThat(arArticle).contains(":jbake-lang: ar")
    }

    @Test
    fun `injectRtlDirection is registered in transform group`() {
        createMiniSiteWithTranslatedCopy()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group", "transform")
            .build()

        assertThat(result.output).contains("injectRtlDirection")
    }

    @Test
    fun `injectRtlDirection handles 10 target languages`() {
        createMiniSiteWithTranslatedCopy()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "injectRtlDirection",
                "--contentI18nOutput=content-i18n",
                "--contentI18nTargetLangs=en,zh,hi,es,ar,bn,pt,ru,ur"
            )
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        val arArticle = projectDir.resolve("content-i18n/ar/blog/post.adoc").readText()
        assertThat(arArticle).contains(":jbake-lang: ar")
        assertThat(arArticle).contains(":lang: rtl")
        val urArticle = projectDir.resolve("content-i18n/ur/blog/post.adoc").readText()
        assertThat(urArticle).contains(":jbake-lang: ur")
        assertThat(urArticle).contains(":lang: rtl")
        val enArticle = projectDir.resolve("content-i18n/en/blog/post.adoc").readText()
        assertThat(enArticle).contains(":jbake-lang: en")
        assertThat(enArticle).doesNotContain(":lang: rtl")
    }

    private fun createMiniSiteWithTranslatedCopy() {
        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "inject-rtl-direction-test"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins { id("education.cccp.bakery") }
            bakery { configPath = "site.yml" }
        """.trimIndent())

        projectDir.resolve("site.yml").writeText("""
            bake:
              srcPath: "jbake"
              destDirPath: "bake"
              cname: "cheroliv.com"
        """.trimIndent())

        val jbakeDir = projectDir.resolve("jbake").apply { mkdirs() }
        jbakeDir.resolve("templates").mkdirs()
        jbakeDir.resolve("content").mkdirs()

        val adocContent = """= Article de test
@CherOliv
2026-07-18
:jbake-title: Article de test
:jbake-type: post
:jbake-status: published
:jbake-date: 2026-07-18
:summary: un article de test

== Introduction

Ceci est un paragraphe de test.
"""

        val i18nBase = projectDir.resolve("content-i18n")
        for (lang in listOf("fr", "en", "ar", "ur", "zh", "hi", "es", "bn", "pt", "ru")) {
            val langBlog = i18nBase.resolve("$lang/blog").apply { mkdirs() }
            langBlog.resolve("post.adoc").writeText(adocContent)
        }
    }
}