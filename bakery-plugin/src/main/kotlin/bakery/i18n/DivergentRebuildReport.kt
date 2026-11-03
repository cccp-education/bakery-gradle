package bakery.i18n

/**
 * GBL-010 P3 — deterministic report of the divergent articles scheduled for a
 * full reconstruction. The operator reads it before paying the metered rebuild
 * (`reportTranslationTree` on the site side names the same leaves).
 */
object DivergentRebuildReport {
    fun render(byLanguage: Map<String, List<String>>): String {
        val sorted = byLanguage.toSortedMap().mapValues { (_, paths) -> paths.sorted() }
        val body =
            if (sorted.isEmpty()) {
                "No structurally divergent variant was found.\n"
            } else {
                buildString {
                    appendLine("[cols=\"1,3\", options=\"header\"]")
                    appendLine("|===")
                    appendLine("| Language | Articles")
                    sorted.forEach { (language, paths) ->
                        appendLine("| $language | ${paths.joinToString(", ")}")
                    }
                    appendLine("|===")
                }
            }
        return buildString {
            appendLine("= Divergent article rebuild plan")
            appendLine()
            appendLine(
                "A structurally divergent variant is never delta-translated: " +
                    "the next run rebuilds it from the reference article.",
            )
            appendLine()
            append(body)
        }
    }
}
