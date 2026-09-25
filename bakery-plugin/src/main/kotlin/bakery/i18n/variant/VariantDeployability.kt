package bakery.i18n.variant

import java.io.File

/**
 * BKY-LANG-NAV-8 — is an `i18n/{lang}` variant structurally bakeable?
 *
 * A variant owns a translated `templates/`; a template like `archive.thyme`
 * does `th:replace="menu.thyme::menu"`. When one reference template is missing,
 * JBake aborts the **whole** render — the lesson of S-049 (the CI aborted the
 * deploy step, and the 21 already-baked languages were never published).
 *
 * The rule therefore is: a variant must carry **every** reference template to
 * be baked. Incomplete variants are reported, never baked — the partial corpus
 * stays deployable, without the defective language that would block everything.
 *
 * Only renderable templates (`.thyme`) count: a frozen `messages_{lang}.properties`
 * bundle co-located in the reference `templates/` is not a template JBake renders.
 *
 * Pure filesystem domain: no Gradle, no LLM, no network.
 */
object VariantDeployability {
    /** File extensions JBake renders as templates. */
    private val TEMPLATE_EXTENSIONS = setOf("thyme", "ftl", "html")

    private fun isTemplate(file: File): Boolean =
        file.isFile && file.extension.lowercase() in TEMPLATE_EXTENSIONS

    /**
     * The reference template names absent from the variant. Empty = the variant
     * is deployable. Sorted, for a deterministic report.
     */
    fun undeployableTemplates(
        referenceTemplates: File,
        variantTemplates: File,
    ): List<String> {
        val variant = variantTemplates.listFiles().orEmpty().map { it.name }.toSet()
        return referenceTemplates
            .listFiles()
            .orEmpty()
            .filter(::isTemplate)
            .map { it.name }
            .filterNot { it in variant }
            .sorted()
    }

    /**
     * Languages whose variant exists but is incomplete. A language without any
     * variant tree is never reported — it is simply not part of the corpus.
     */
    fun undeployableVariants(
        layout: VariantLayout,
        supportedLanguages: List<String>,
        referenceLanguage: String,
    ): Map<String, List<String>> {
        val reference = layout.referenceTemplates
        if (!reference.isDirectory) return emptyMap()

        return supportedLanguages
            .filter { it != referenceLanguage }
            .filter { layout.variant(it).exists() }
            .associateWith { undeployableTemplates(reference, layout.templates(it)) }
            .filterValues { it.isNotEmpty() }
    }
}
