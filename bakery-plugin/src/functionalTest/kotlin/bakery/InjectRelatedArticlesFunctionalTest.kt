@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class InjectRelatedArticlesFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    fun setUp() {
        setupGradleProject()
        setupSiteContentWithPlaceholder()
    }

    @Test
    fun `injectRelatedArticles task is registered under transform group`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group", "transform")
            .build()

        assertThat(result.output).contains("injectRelatedArticles")
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `should inject related articles into baked HTML when suggestions exist`() {
        // Appel en une seule fois : bake → collect → inject
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectRelatedArticles")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")

        val bakedDir = projectDir.resolve("build/bake")
        val kotlinHtml = bakedDir.resolve("blog/2024/kotlin.html")
        assertThat(kotlinHtml).exists()
        val html = kotlinHtml.readText()

        // Verifier que le placeholder a ete remplace par un bloc d'articles connexes
        assertThat(html).doesNotContain("RELATED_ARTICLES_PLACEHOLDER")
        assertThat(html).contains("related-articles")
        assertThat(html).contains("Articles connexes")
    }

    @Test
    fun `should remove placeholder when no suggestions exist`() {
        // Creer un seul article → pas de suggestions
        val contentDir = projectDir.resolve("site/content/blog/2024")
        contentDir.resolve("gradle.adoc").delete()
        contentDir.resolve("java.adoc").delete()

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectRelatedArticles")
            .build()

        val bakedDir = projectDir.resolve("build/bake")
        val kotlinHtml = bakedDir.resolve("blog/2024/kotlin.html")
        assertThat(kotlinHtml).exists()
        val html = kotlinHtml.readText()

        // Placeholder retire, pas de section related-articles
        assertThat(html).doesNotContain("RELATED_ARTICLES_PLACEHOLDER")
        assertThat(html).doesNotContain("related-articles")
    }

    // ——— helpers ———

    private fun setupGradleProject() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "kg-inject-test"
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

    private fun setupSiteContentWithPlaceholder() {
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
            = Introduction a Kotlin
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
            = Gradle pour les debutants
            Jane Smith
            2024-07-25
            :jbake-type: post
            :jbake-tags: gradle, build, kotlin
            :jbake-status: published

            Apprendre a builder avec Gradle.
            """.trimIndent()
        )

        contentDir.resolve("blog/2024/java.adoc").writeText(
            """
            = Java 24 nouveautes
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
        templatesDir.resolve("post.thyme").writeText(
            """
            <html>
            <body>
            <h1 th:text="${'$'}{content.title}">Title</h1>
            <div th:utext="${'$'}{content.body}">body</div>
            <!-- RELATED_ARTICLES_PLACEHOLDER -->
            </body>
            </html>
            """.trimIndent()
        )
        templatesDir.resolve("page.thyme").writeText("<html th:text=\"\${content.body}\"></html>")
        templatesDir.resolve("index.thyme").writeText(
            """<html><ul th:each="post : ${'$'}{posts}"><li th:text="${'$'}{post.title}"></li></ul></html>"""
        )
        templatesDir.resolve("tags.thyme").writeText("<html></html>")
        templatesDir.resolve("tag.thyme").writeText("<html></html>")
        templatesDir.resolve("feed.thyme").writeText(
            """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
<channel>
  <title>Test Blog</title>
  <link>http://localhost:8820</link>
  <description>Test</description>
  <atom:link href="http://localhost:8820/feed.xml" rel="self" type="application/rss+xml"/>
  <th:block th:each="post : ${'$'}{published_posts}">
  <item>
    <title th:text="${'$'}{post.title}">Title</title>
    <link th:text="'http://localhost:8820/' + ${'$'}{post.uri}">Link</link>
    <pubDate th:text="${'$'}{post.date}">Date</pubDate>
    <th:block th:each="tag : ${'$'}{post.tags}">
    <category th:text="${'$'}{tag}">tag</category>
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
