package bakery.search

import java.io.File

/**
 * BKY-SEARCH-4 — the languages actually staged in a baked site tree.
 *
 * The `validateSearchIndex` guard must compare the Pagefind bundle with the
 * languages the deployment really publishes, never with a hardcoded list. The
 * baked tree carries the truth: the root `index.html` is the reference
 * language, and each direct sub-directory with an `index.html` is a variant.
 * Pagefind itself reads the same `<html lang>` attribute to build one
 * independent index per language.
 *
 * Pure filesystem domain: reads index files only, no Gradle, no LLM.
 */
object BakedLanguages {
    private val HTML_LANG = Regex("""<html\b[^>]*\blang="([^"]+)"""", RegexOption.IGNORE_CASE)

    fun discover(bakeRoot: File): Set<String> {
        val codes = sortedSetOf<String>()
        langOf(bakeRoot.resolve("index.html"))?.let { codes += it }
        bakeRoot
            .listFiles()
            .orEmpty()
            .filter { it.isDirectory }
            .sortedBy { it.name }
            .forEach { dir -> langOf(dir.resolve("index.html"))?.let { codes += it } }
        return codes
    }

    fun langOf(indexHtml: File): String? {
        if (!indexHtml.isFile) return null
        val raw = HTML_LANG.find(indexHtml.readText())?.groupValues?.get(1) ?: return null
        return raw.trim().lowercase()
    }
}
