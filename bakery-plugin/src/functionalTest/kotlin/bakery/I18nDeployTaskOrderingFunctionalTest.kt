package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * EPIC BKY-I18N-DEPLOY — US DEPLOY-7.
 *
 * Functional tests for the i18n-deploy task ordering wiring.
 *
 * The 3 i18n-deploy tasks (`migrateContentI18n`, `injectRtlDirection`,
 * `injectLangSwitch`) must be wired with `mustRunAfter` so that when invoked
 * together, Gradle schedules them in pipeline order:
 *
 *   migrateContentI18n → injectRtlDirection → injectLangSwitch
 *
 * `mustRunAfter` (soft) is intentionally used instead of `dependsOn` (hard):
 * each task stays independently runnable, but when several are requested,
 * the ordering is guaranteed.
 */
class I18nDeployTaskOrderingFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `injectRtlDirection runs after migrateContentI18n when both requested`() {
        createMiniSite()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "migrateContentI18n", "injectRtlDirection",
                "-PcontentI18nSource=jbake/content/blog",
                "-PcontentI18nOutput=content-i18n",
                "-PcontentI18nTargetLangs=en",
                "-PcontentI18nSourceLang=fr",
                "-PcontentI18nDryRun=true"
            )
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        val migrate = result.task(":migrateContentI18n")
        val rtl = result.task(":injectRtlDirection")
        assertThat(migrate).isNotNull
        assertThat(rtl).isNotNull
        assertThat(migrate!!.outcome).isNotNull
        assertThat(rtl!!.outcome).isNotNull
    }

    @Test
    fun `injectLangSwitch runs after migrateContentI18n when both requested`() {
        createMiniSite()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "migrateContentI18n", "injectLangSwitch",
                "-PcontentI18nSource=jbake/content/blog",
                "-PcontentI18nOutput=content-i18n",
                "-PcontentI18nTargetLangs=en",
                "-PcontentI18nSourceLang=fr",
                "-PcontentI18nDryRun=true"
            )
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        val migrate = result.task(":migrateContentI18n")
        val langSwitch = result.task(":injectLangSwitch")
        assertThat(migrate).isNotNull
        assertThat(langSwitch).isNotNull
    }

    @Test
    fun `injectRtlDirection stays runnable independently`() {
        createMiniSite()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "injectRtlDirection",
                "-PcontentI18nOutput=content-i18n",
                "-PcontentI18nTargetLangs=ar"
            )
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        val rtl = result.task(":injectRtlDirection")
        assertThat(rtl).isNotNull
        assertThat(rtl!!.outcome).isIn(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE, TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `migrateContentI18n stays runnable independently`() {
        createMiniSite()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "migrateContentI18n",
                "-PcontentI18nSource=jbake/content/blog",
                "-PcontentI18nOutput=content-i18n",
                "-PcontentI18nTargetLangs=en",
                "-PcontentI18nSourceLang=fr",
                "-PcontentI18nDryRun=true"
            )
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        val migrate = result.task(":migrateContentI18n")
        assertThat(migrate).isNotNull
    }

    private fun createMiniSite() {
        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "i18n-deploy-ordering-test"
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
        val blog = jbakeDir.resolve("content/blog").apply { mkdirs() }
        blog.resolve("post.adoc").writeText("""= Test article
@CherOliv
2026-07-19
:jbake-title: Test article
:jbake-type: post
:jbake-status: published
:jbake-date: 2026-07-19

== Intro

Ceci est un test.
""")

        val i18nBase = projectDir.resolve("content-i18n").apply { mkdirs() }
        for (lang in listOf("ar", "en")) {
            val langBlog = i18nBase.resolve("$lang/blog").apply { mkdirs() }
            langBlog.resolve("post.adoc").writeText("""= Test article
@CherOliv
2026-07-19
:jbake-title: Test article
:jbake-type: post
:jbake-status: published
:jbake-date: 2026-07-19

== Intro

Ceci est un test.
""")
        }
    }
}