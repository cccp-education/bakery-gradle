package bakery.search

import bakery.BakeConfiguration
import bakery.GitPushConfiguration
import bakery.SiteConfiguration
import bakery.SiteTaskRegistrar.registerValidateSearchIndexTask
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ValidateSearchIndexTaskTest {
    @TempDir
    lateinit var tempDir: File

    private fun task(bakeRoot: File): ValidateSearchIndexTask {
        val project: Project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val site =
            SiteConfiguration(
                bake = BakeConfiguration("jbake", "bake"),
                pushPage = GitPushConfiguration(),
            )
        project.registerValidateSearchIndexTask(site)
        check(bakeRoot.exists()) { "bake root must exist: $bakeRoot" }
        return project.tasks.getByName("validateSearchIndex") as ValidateSearchIndexTask
    }

    private fun stage(
        root: File,
        reference: String,
        variants: List<String>,
    ) {
        root.resolve("index.html").also { it.parentFile.mkdirs() }.writeText("<html lang=\"$reference\"></html>")
        variants.forEach { language ->
            root.resolve(language).mkdirs()
            root.resolve("$language/index.html").writeText("<html lang=\"$language\"></html>")
        }
    }

    private fun entry(
        root: File,
        pageCounts: Map<String, Int>,
    ) {
        val languages =
            pageCounts.entries.joinToString(",") { (code, pages) ->
                """"$code":{"hash":"${code}_hash","wasm":"$code","page_count":$pages}"""
            }
        root.resolve("pagefind").mkdirs()
        root.resolve("pagefind/pagefind-entry.json").writeText(
            """{"version":"1.6.0","languages":{$languages},"include_characters":["_"]}""",
        )
    }

    private fun execute(task: ValidateSearchIndexTask) {
        task.actions.forEach { it.execute(task) }
    }

    @Test
    fun `passes when every staged language has a non-empty index`() {
        val root = tempDir.resolve("build/bake").also { it.mkdirs() }
        stage(root, "fr", listOf("en"))
        entry(root, mapOf("fr" to 464, "en" to 120))
        val task = task(root)

        assertThatCode { execute(task) }.doesNotThrowAnyException()
    }

    @Test
    fun `fails when a staged language has no index`() {
        val root = tempDir.resolve("build/bake").also { it.mkdirs() }
        stage(root, "fr", listOf("en"))
        entry(root, mapOf("fr" to 464))
        val task = task(root)

        assertThatThrownBy { execute(task) }
            .isInstanceOf(GradleException::class.java)
            .hasMessageContaining("missing-language")
    }

    @Test
    fun `fails when the entry file is absent`() {
        val root = tempDir.resolve("build/bake").also { it.mkdirs() }
        stage(root, "fr", emptyList())
        val task = task(root)

        assertThatThrownBy { execute(task) }
            .isInstanceOf(GradleException::class.java)
            .hasMessageContaining("Pagefind entry not found")
    }

    @Test
    fun `fails when the entry file is malformed`() {
        val root = tempDir.resolve("build/bake").also { it.mkdirs() }
        stage(root, "fr", emptyList())
        root.resolve("pagefind").mkdirs()
        root.resolve("pagefind/pagefind-entry.json").writeText("{not json")
        val task = task(root)

        assertThatThrownBy { execute(task) }
            .isInstanceOf(GradleException::class.java)
            .hasMessageContaining("Malformed Pagefind entry")
    }
}
