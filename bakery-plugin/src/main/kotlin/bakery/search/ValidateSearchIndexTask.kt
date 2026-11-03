package bakery.search

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * BKY-SEARCH-4 — the deployment guard of the Pagefind bundle.
 *
 * Pagefind writes one independent index per `<html lang>`; a missing or empty
 * language index silently returns no result instead of failing. This task
 * compares `pagefind-entry.json` with the languages actually staged in the
 * baked tree ([BakedLanguages]) and fails (or warns in non-strict mode) when one
 * is missing or indexed zero pages.
 *
 * Pipeline: `bake → pagefind → validateSearchIndex` (the task depends on
 * `pagefind`; a consumer that indexes an assembled tree runs it with
 * `-x bake -x bakeVariants`).
 */
@DisableCachingByDefault(because = "Reads the baked Pagefind bundle")
abstract class ValidateSearchIndexTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bakeDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val referenceLanguage: Property<String>

    @get:Input
    @get:Optional
    abstract val expectedLanguages: SetProperty<String>

    @get:Input
    abstract val minimumPages: Property<Int>

    @get:Input
    abstract val strict: Property<Boolean>

    @TaskAction
    fun validate() {
        val root = bakeDir.get().asFile
        val entry = root.resolve(SearchBundle.DIRECTORY).resolve(SearchBundle.ENTRY)
        val strictMode = if (strict.isPresent) strict.get() else true

        if (!entry.isFile) {
            failOrWarn(
                strictMode,
                "Pagefind entry not found at ${entry.absolutePath}; run `pagefind` before `validateSearchIndex`.",
            )
            return
        }

        val index =
            PagefindEntry
                .parse(entry.readText())
                .fold(
                    ifLeft = { error ->
                        val reason = (error as? SearchIndexError.Malformed)?.reason ?: error.toString()
                        failOrWarn(strictMode, "Malformed Pagefind entry ${entry.absolutePath}: $reason")
                        return
                    },
                    ifRight = { it },
                )

        val explicit = if (expectedLanguages.isPresent) expectedLanguages.get() else emptySet()
        val discovered =
            if (explicit.isNotEmpty()) {
                explicit.map { it.lowercase() }.toSet()
            } else {
                BakedLanguages.discover(root)
            }
        val reference = if (referenceLanguage.isPresent) referenceLanguage.get().lowercase() else ""
        val languages = (discovered + reference).filter { it.isNotBlank() }.toSet()

        if (languages.isEmpty()) {
            failOrWarn(strictMode, "No language found in ${root.absolutePath}; cannot validate the search index.")
            return
        }

        val minimum = if (minimumPages.isPresent) minimumPages.get() else 1
        val violations = PagefindIndexContract.verify(index, languages, minimum)
        if (violations.isNotEmpty()) {
            failOrWarn(
                strictMode,
                "Pagefind index does not cover the staged languages: " +
                    violations.joinToString("; "),
            )
            return
        }

        logger.lifecycle(
            "✓ Pagefind index covers ${languages.size} language(s): ${languages.sorted().joinToString(", ")}",
        )
    }

    private fun failOrWarn(
        strict: Boolean,
        message: String,
    ) {
        if (strict) {
            throw GradleException(message)
        }
        logger.warn(message)
    }
}
