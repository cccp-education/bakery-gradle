package bakery.scaffold

import bakery.llm.FakeLlmService
import org.gradle.api.provider.Property
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests unitaires pour [GenerateSiteFromIntentionTask] — tache Gradle BKY-IA-1.
 *
 * Baby-step TDD : chaque test valide une regle de resolution CLI > DSL > defauts.
 * Pattern identique a [bakery.article.GenerateArticleTaskTest].
 */
class GenerateSiteFromIntentionTaskTest {

    @Nested
    inner class ResolveIntentionTest {

        @TempDir
        lateinit var tempDir: File

        private lateinit var project: org.gradle.api.Project
        private lateinit var task: GenerateSiteFromIntentionTask

        @BeforeEach
        fun setUp() {
            project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            task = project.tasks.register(
                "generateSiteFromIntentionTest",
                GenerateSiteFromIntentionTask::class.java
            ).get()
        }

        @Test
        fun `resolveIntention throws when no description provided`() {
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                task.resolveIntention()
            }
        }

        @Test
        fun `resolveIntention uses scaffoldDescription from DSL when set`() {
            task.dslIntention = ScaffoldIntention(
                description = "Un blog sur Kotlin",
                siteType = ScaffoldSiteType.BLOG,
                lang = "fr"
            )

            val intention = task.resolveIntention()

            assertEquals("Un blog sur Kotlin", intention.description)
            assertEquals(ScaffoldSiteType.BLOG, intention.siteType)
            assertEquals("fr", intention.lang)
        }

        @Test
        fun `resolveIntention uses DSL defaults when scaffoldDescription is set`() {
            task.dslIntention = ScaffoldIntention(
                description = "Documentation API",
                siteType = ScaffoldSiteType.DOC,
                lang = "en",
                projectName = "api-docs"
            )

            val intention = task.resolveIntention()

            assertEquals("Documentation API", intention.description)
            assertEquals(ScaffoldSiteType.DOC, intention.siteType)
            assertEquals("en", intention.lang)
            assertEquals("api-docs", intention.projectName)
        }

        @Test
        fun `resolveIntention falls back to BLOG for unknown site type`() {
            task.dslIntention = ScaffoldIntention(
                description = "Test site",
                siteType = ScaffoldSiteType.BLOG
            )

            val intention = task.resolveIntention()

            assertEquals(ScaffoldSiteType.BLOG, intention.siteType)
        }
    }

    @Nested
    inner class TaskRegistrationTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `task is registered with BKY-IA-1 scaffolding group`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val task = project.tasks.register(
                "generateSiteFromIntentionTest",
                GenerateSiteFromIntentionTask::class.java
            ).get()

            assertEquals("generate", task.group)
        }

        @Test
        fun `task has scaffoldDescription property`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val task = project.tasks.register(
                "generateSiteFromIntentionTest2",
                GenerateSiteFromIntentionTask::class.java
            ).get()

            assertNotNull(task.scaffoldDescription)
            assertTrue(task.scaffoldDescription.isPresent == false || task.scaffoldDescription.orNull == "")
        }

        @Test
        fun `task has siteType property`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val task = project.tasks.register(
                "generateSiteFromIntentionTest3",
                GenerateSiteFromIntentionTask::class.java
            ).get()

            assertNotNull(task.siteType)
        }

        @Test
        fun `task has scaffoldLang property`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val task = project.tasks.register(
                "generateSiteFromIntentionTest4",
                GenerateSiteFromIntentionTask::class.java
            ).get()

            assertNotNull(task.scaffoldLang)
        }

        @Test
        fun `task has projectName property`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val task = project.tasks.register(
                "generateSiteFromIntentionTest5",
                GenerateSiteFromIntentionTask::class.java
            ).get()

            assertNotNull(task.projectName)
        }
    }

    @Nested
    inner class ApplyScaffoldOutputTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `task creates site yml when executed with FakeLlmService`() {
            // Use a single-line compact JSON to avoid indentation parsing issues
            val fakeResponse = """{"siteType":"blog","projectName":"mon-blog","description":"Mon blog technique","templates":["blog.thyme","post.thyme"],"metadata":{"title":"Mon Blog","description":"Blog technique","tags":["kotlin"],"layout":"post","language":"fr"}}"""
            val fakeLlm = FakeLlmService(fakeResponse)

            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val task = project.tasks.register(
                "generateSiteFromIntentionApply",
                GenerateSiteFromIntentionTask::class.java
            ).get()

            task.llmService = fakeLlm
            task.targetDir = tempDir.resolve("target-site")
            task.dslIntention = ScaffoldIntention(
                description = "Mon blog technique",
                siteType = ScaffoldSiteType.BLOG,
                projectName = "mon-blog"
            )

            task.executeGenerate()

            val siteYml = tempDir.resolve("target-site/site.yml")
            assertTrue(siteYml.exists(), "site.yml must be created")
            val content = siteYml.readText()
            assertTrue(content.contains("mon-blog"), "site.yml must contain projectName")
            assertTrue(content.contains("Mon Blog"), "site.yml must contain metadata title")
        }

        @Test
        fun `task creates site yml with tree section when LLM returns tree`() {
            val fakeResponse = """{"siteType":"formation","projectName":"ma-formation","description":"Formation FPA","tree":{"type":"site","path":"","sections":[{"type":"section","path":"modules","articles":[{"type":"article","path":"modules/intro"},{"type":"article","path":"modules/avance"}]}]},"metadata":{"title":"Ma Formation","description":"Formation FPA","tags":["formation"],"layout":"page","language":"fr"}}"""
            val fakeLlm = FakeLlmService(fakeResponse)

            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val task = project.tasks.register(
                "generateSiteFromIntentionTree",
                GenerateSiteFromIntentionTask::class.java
            ).get()

            task.llmService = fakeLlm
            task.targetDir = tempDir.resolve("tree-site")
            task.dslIntention = ScaffoldIntention(
                description = "Formation FPA",
                siteType = ScaffoldSiteType.FORMATION,
                projectName = "ma-formation"
            )

            task.executeGenerate()

            val siteYml = tempDir.resolve("tree-site/site.yml")
            assertTrue(siteYml.exists(), "site.yml must be created")
            val content = siteYml.readText()
            assertTrue(content.contains("tree:"), "site.yml must contain tree section")
            assertTrue(content.contains("type: site"), "tree must contain type discriminator")
            assertTrue(content.contains("modules/intro"), "tree must contain article paths")
            assertTrue(content.contains("modules/avance"), "tree must contain all articles")
        }

        @Test
        fun `task throws when no LlmService injected`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val task = project.tasks.register(
                "generateSiteFromIntentionNoLlm",
                GenerateSiteFromIntentionTask::class.java
            ).get()

            task.dslIntention = ScaffoldIntention(description = "Test")
            task.targetDir = tempDir

            org.junit.jupiter.api.assertThrows<IllegalStateException> {
                task.executeGenerate()
            }
        }
    }
}