package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * EPIC BKY-I18N-DEPLOY — US DEPLOY-5.
 *
 * Dry-run dogfooding FT on a representative sample of cheroliv.com (3
 * JBake-native articles: simple, with PlantUML, with source code). Validates
 * that `migrateContentI18n` parses all articles without crash and writes
 * nothing in dry-run mode (Ink Economy Law — the LLM is never invoked until
 * the real non-dry-run dogfooding is explicitly launched by the operator).
 *
 * The real dogfooding command (LLM-on, 108 articles) is documented in
 * `.agents/I18N_DEPLOY_BOUNDARY.adoc` and run manually by the operator —
 * it is NOT a CI test (LLM metered, non-deterministic, ~1080 calls).
 */
class CherolivComDogfoodingDryRunFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `dry-run migrateContentI18n on cheroliv sample parses all articles without writing`() {
        createCherolivSample()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "migrateContentI18n",
                "--contentI18nSource=content/blog",
                "--contentI18nOutput=content-i18n",
                "--contentI18nSourceLang=fr",
                "--contentI18nTargetLangs=en",
                "--contentI18nDryRun=true"
            )
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).contains("DRY-RUN")
        assertThat(result.output).contains("aucun fichier modifié")
        assertThat(projectDir.resolve("content-i18n").exists()).isFalse()
    }

    @Test
    fun `dry-run migrateContentI18n on cheroliv sample handles plantuml block via adapter`() {
        createCherolivSample()
        val plantumlArticle = projectDir.resolve("jbake/content/blog/2022/0031_memo_design_system_post.adoc").readText()
        assertThat(plantumlArticle).contains("[plantuml]")

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "migrateContentI18n",
                "--contentI18nSource=content/blog",
                "--contentI18nOutput=content-i18n",
                "--contentI18nSourceLang=fr",
                "--contentI18nTargetLangs=en",
                "--contentI18nDryRun=true"
            )
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).doesNotContain("ERREUR")
    }

    @Test
    fun `dry-run migrateContentI18n on cheroliv sample preserves JBake native headers`() {
        createCherolivSample()
        val sourceSimple = projectDir.resolve("jbake/content/blog/2020/0016_simple_post.adoc").readText()
        assertThat(sourceSimple).contains("= Asciidoc/Markdown Memo")
        assertThat(sourceSimple).contains(":jbake-title: Asciidoc/Markdown Mémo")
        assertThat(sourceSimple).contains(":jbake-type: post")

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "migrateContentI18n",
                "--contentI18nSource=content/blog",
                "--contentI18nOutput=content-i18n",
                "--contentI18nSourceLang=fr",
                "--contentI18nTargetLangs=en",
                "--contentI18nDryRun=true"
            )
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        val after = projectDir.resolve("jbake/content/blog/2020/0016_simple_post.adoc").readText()
        assertThat(after).isEqualTo(sourceSimple)
    }

    @Test
    fun `dry-run migrateContentI18n on cheroliv sample supports 10 target languages`() {
        createCherolivSample()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "migrateContentI18n",
                "--contentI18nSource=content/blog",
                "--contentI18nOutput=content-i18n",
                "--contentI18nSourceLang=fr",
                "--contentI18nTargetLangs=en,zh,hi,es,ar,bn,pt,ru,ur",
                "--contentI18nDryRun=true"
            )
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).contains("en, zh, hi, es, ar, bn, pt, ru, ur")
    }

    private fun createCherolivSample() {
        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "cheroliv-com-dogfooding"
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

        val blog2020 = projectDir.resolve("jbake/content/blog/2020").apply { mkdirs() }
        blog2020.resolve("0016_simple_post.adoc").writeText(
            """= Asciidoc/Markdown Memo
@CherOliv
2020-09-15
:jbake-title: Asciidoc/Markdown Mémo
:jbake-type: post
:jbake-tags: blog, ticket, asciidoc, markdown, memo
:jbake-status: published
:jbake-date: 2020-09-15
:summary: simple mémo ascidoc/markdown

.Pourquoi asciidoc ou markdown ?
Ici, je m'oriente vers asciidoc, car j'ai déjà quelques tickets écrits en asciidoc.
+
Asciidoc est un langage de balisage, qui permet de produire du contenu, en apportant une mise en page qui garde le texte lisible pour un humain.

.Liens :

* https://blog.oxiane.com/2018/06/13/asciidoc-documentation-as-code/[jmdoudoux sur blog.oxiane.com, window="_blank"]
"""
        )

        val blog2022 = projectDir.resolve("jbake/content/blog/2022").apply { mkdirs() }
        blog2022.resolve("0031_memo_design_system_post.adoc").writeText(
            """= Mémo Design System
@CherOliv
2022-01-15
:jbake-title: Mémo Design System
:jbake-type: post
:jbake-tags: blog, design, system
:jbake-status: published
:jbake-date: 2022-01-15
:summary: mémo design system

== Architecture

[plantuml]
----
@startuml
actor "Utilisateur" as User
participant "Service" as Service
User -> Service: Requête
Service --> User: Réponse
@enduml
----

Le design system organise les composants.
"""
        )

        val blog2025 = projectDir.resolve("jbake/content/blog/2025").apply { mkdirs() }
        blog2025.resolve("0087_pypi_cicd_post.adoc").writeText(
            """= CI/CD Python – Partie 3
@CherOliv
2025-07-19
:jbake-title: CI/CD Python – Partie 3
:jbake-type: post
:jbake-status: published
:jbake-date: 2025-07-19
:summary: Partie 3 - Guide CI/CD robuste

== Intégration avec Poetry

[source,bash]
----
poetry install
poetry run pytest
----

Poetry remplace les anciens outils de packaging.
"""
        )
    }
}