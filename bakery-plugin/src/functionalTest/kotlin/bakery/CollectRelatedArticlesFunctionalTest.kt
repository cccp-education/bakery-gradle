@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CollectRelatedArticlesFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `collectRelatedArticles task is registered under collect group`() {
        setupGradleProject()
        setupSiteContent()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group", "collect")
            .build()

        assertThat(result.output).contains("collectRelatedArticles")
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `should produce related-articles json from baked site`() {
        setupGradleProject()
        setupSiteContent()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("collectRelatedArticles")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")

        val outputFile = projectDir.resolve("build/bakery/related-articles.json")
        assertThat(outputFile).exists()

        val json = outputFile.readText()
        assertThat(json).contains("\"version\"")
        assertThat(json).contains("\"suggestions\"")
    }

    @Test
    fun `should produce valid related-articles json structure`() {
        setupGradleProject()
        setupSiteContent()

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("collectRelatedArticles")
            .build()

        val outputFile = projectDir.resolve("build/bakery/related-articles.json")
        assertThat(outputFile).exists()

        val json = outputFile.readText()
        // Structure JSON valide : version + suggestions + generatedAt
        assertThat(json).contains("\"version\"")
        assertThat(json).contains("\"suggestions\"")
        assertThat(json).contains("\"generatedAt\"")
    }

    // ——— helpers ———

    private fun setupGradleProject() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "kg-test"
            """.trimIndent()
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("education.cccp.bakery") }
            bakery { configPath = file("site.yml").absolutePath }
            """.trimIndent()
        )

        projectDir.resolve("site.yml").writeText(
            """
            bake: { srcPath: site, destDirPath: build/bake }
            pushPage: { from: site, to: pages }
            pushMaquette: { from: maquette, to: maquette-pages }
            """.trimIndent()
        )
    }

    private fun setupSiteContent() {
        val siteDir = projectDir.resolve("site")
        siteDir.mkdirs()
        siteDir.resolve("jbake.properties").writeText(
            """
            template.folder=templates
            content.folder=content
            render.tags=true
            """.trimIndent()
        )

        val contentDir = siteDir.resolve("content")
        contentDir.mkdirs()
        contentDir.resolve("blog").mkdirs()
        contentDir.resolve("blog/2024").mkdirs()

        contentDir.resolve("blog/2024/kotlin.adoc").writeText(
            """
            = Introduction à Kotlin
            John Doe
            2024-07-24
            :jbake-type: post
            :jbake-tags: kotlin, programmation
            :jbake-status: published

            Kotlin est un langage moderne.
            """.trimIndent()
        )

        contentDir.resolve("blog/2024/gradle.adoc").writeText(
            """
            = Gradle pour les débutants
            Jane Smith
            2024-07-25
            :jbake-type: post
            :jbake-tags: gradle, build, kotlin
            :jbake-status: published

            Apprendre à builder avec Gradle.
            """.trimIndent()
        )

        contentDir.resolve("blog/2024/java.adoc").writeText(
            """
            = Java 24 nouveautés
            Alice Martin
            2024-07-26
            :jbake-type: post
            :jbake-tags: java, jvm
            :jbake-status: published

            Les features de Java 24.
            """.trimIndent()
        )

        val templatesDir = siteDir.resolve("templates")
        templatesDir.mkdirs()
        templatesDir.resolve("menu.thyme").writeText("<html></html>")
        templatesDir.resolve("post.thyme").writeText("<html th:text=\"\${content.body}\"></html>")
        templatesDir.resolve("page.thyme").writeText("<html th:text=\"\${content.body}\"></html>")
        templatesDir.resolve("index.thyme").writeText(
            """<html><ul th:each="post : ${"$"}{posts}"><li th:text="${"$"}{post.title}"></li></ul></html>"""
        )
        templatesDir.resolve("tags.thyme").writeText("<html></html>")
        templatesDir.resolve("tag.thyme").writeText("<html></html>")
        // Feed RSS avec catégories pour extraction des tags
        templatesDir.resolve("feed.thyme").writeText(
            """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
<channel>
  <title>Test Blog</title>
  <link>http://localhost:8820</link>
  <description>Test</description>
  <atom:link href="http://localhost:8820/feed.xml" rel="self" type="application/rss+xml"/>
  <th:block th:each="post : ${"$"}{published_posts}">
  <item>
    <title th:text="${"$"}{post.title}">Title</title>
    <link th:text="'http://localhost:8820/' + ${"$"}{post.uri}">Link</link>
    <pubDate th:text="${"$"}{post.date}">Date</pubDate>
    <th:block th:each="tag : ${"$"}{post.tags}">
    <category th:text="${"$"}{tag}">tag</category>
    </th:block>
  </item>
  </th:block>
</channel>
</rss>"""
        )
        templatesDir.resolve("archive.thyme").writeText("<html></html>")
        templatesDir.resolve("sitemap.thyme").writeText("<html></html>")
    }
}
