package bakery.i18n.js

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-4.
 *
 * Pure publication-copy planner for the client-side i18n dictionaries.
 *
 * Decision S-039 (talaria): `maquette/js/` is the development source, the site
 * copy `jbake/assets/js/` must be byte-identical. Reference behaviour: the
 * `propagate()` function of the talaria node harness `tools/translate-i18n.mjs`
 * (S-044) — the source is copied verbatim, no transformation.
 *
 * This object is domain-pure: no I/O, no Gradle, no LLM. It only computes the
 * source→target mapping and the drift between an already-published copy and the
 * development source (Ink Economy Law: a byte-identical copy is never rewritten).
 */
object I18nClientPropagation {
    private const val SOURCE_SEGMENT = "maquette"
    private const val TARGET_SEGMENT = "jbake/assets"

    /**
     * Returns the `jbake/assets/js/...` publication path of a `maquette/js/...`
     * source, or `null` when [sourcePath] does not live under a `maquette/`
     * directory. Only the first `maquette` path segment is replaced.
     */
    fun targetFor(sourcePath: String): String? {
        val segments = sourcePath.split('/')
        val index = segments.indexOf(SOURCE_SEGMENT)
        if (index < 0 || index == segments.lastIndex) return null
        return (segments.take(index) + TARGET_SEGMENT + segments.drop(index + 1)).joinToString("/")
    }

    /**
     * Source→target pairs for every propagable path of [sourcePaths], in input
     * order. Non-propagable paths (outside `maquette/`) are skipped.
     */
    fun pairs(sourcePaths: List<String>): Map<String, String> {
        val pairs = LinkedHashMap<String, String>()
        for (sourcePath in sourcePaths) {
            val target = targetFor(sourcePath) ?: continue
            pairs[sourcePath] = target
        }
        return pairs
    }

    /**
     * Sources whose publication copy is missing or not byte-identical to the
     * development source, in input order. A non-propagable source is never
     * drift. An empty result means the publication tree is already aligned —
     * propagating it would be a strict no-op (Ink Economy Law).
     */
    fun drift(
        sources: Map<String, String>,
        targets: Map<String, String>,
    ): List<String> =
        sources.keys.filter { source ->
            val target = targetFor(source) ?: return@filter false
            targets[target] != sources[source]
        }
}
