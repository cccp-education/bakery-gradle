package bakery.i18n.snapshot

/**
 * BKY-I18N-REAL-1 — Pure domain representing a site state after i18n
 * migration.
 *
 * Immutable data class. No I/O. Testable without Gradle.
 *
 * - [templatesMigrated]: template name -> migrated content (with th:text, th:placeholder, etc.)
 * - [messagesFr]: i18n key -> French value (extracted from templates)
 * - [messagesEn]: i18n key -> English value (translation)
 */
data class PostMigrationSnapshot(
    val templatesMigrated: Map<String, String>,
    val messagesFr: Map<String, String>,
    val messagesEn: Map<String, String>
) {

    /**
     * Checks that every FR key has a non-blank EN translation.
     */
    fun hasAllKeysTranslated(): Boolean {
        if (messagesFr.isEmpty()) return false
        return messagesFr.all { (key, _) ->
            messagesEn[key]?.isNotBlank() == true
        }
    }

    /**
     * Extracts i18n keys from migrated templates.
     *
     * Detects th:text="#{key}", th:placeholder="#{key}",
     * th:attr="aria-label=#{key}", th:content="#{key}",
     * th:alt="#{key}", th:title="#{key}".
     */
    fun templateKeys(): Set<String> {
        val keyRegex = Regex("""#\{([^}]+)}""")
        return templatesMigrated.values.flatMap { content ->
            keyRegex.findAll(content).map { it.groupValues[1] }.toList()
        }.toSet()
    }

    /**
     * A snapshot is complete when:
     * - at least one migrated template
     * - at least one FR message
     * - all FR keys translated to EN
     */
    fun isComplete(): Boolean {
        return templatesMigrated.isNotEmpty() &&
            messagesFr.isNotEmpty() &&
            hasAllKeysTranslated()
    }
}