package bakery.tree

import java.io.File

/**
 * GBL-010 G-1 — the **template branch** of the translation tree.
 *
 * *"Gérer la traduction des sites comme un arbre … une branche pour le template
 * jbake, l'autre pour la data."* bakery owns the template branch: for every
 * reference template × language it must expose whether the variant template is
 * **missing**, **untranslated** (byte-identical to the reference — the copy-FR
 * trap S-043/S-044) or **translated**.
 *
 * [bakery.i18n.variant.VariantDeployability] only knows the *missing* half; the
 * unified tree (GBL-010 G-4) needs the full per-node status. This audit supplies
 * it, generically, for any site layout the caller hands in.
 *
 * Pure domain: reads the filesystem, no Gradle, no LLM, no network.
 */
enum class TemplateTranslationStatus {
    MISSING,
    UNTRANSLATED,
    TRANSLATED,
}

data class TemplateNode(
    val language: String,
    val template: String,
    val status: TemplateTranslationStatus,
)

data class TemplateTree(
    val referenceLanguage: String,
    val nodes: List<TemplateNode>,
) {
    val unconverged: List<TemplateNode> get() = nodes.filter { it.status != TemplateTranslationStatus.TRANSLATED }

    val isConverged: Boolean get() = unconverged.isEmpty()

    fun forLanguage(language: String): List<TemplateNode> = nodes.filter { it.language == language }

    fun count(status: TemplateTranslationStatus): Int = nodes.count { it.status == status }

    /** Deterministic AsciiDoc rendering (ink economy: no timestamp). */
    fun render(): String =
        buildString {
            appendLine("= Template tree — templates branch")
            appendLine()
            appendLine(
                "templates: ${nodes.size} · translated: ${count(TemplateTranslationStatus.TRANSLATED)} · " +
                    "untranslated: ${count(TemplateTranslationStatus.UNTRANSLATED)} · " +
                    "missing: ${count(TemplateTranslationStatus.MISSING)}",
            )
            appendLine()
            unconverged.forEach { node ->
                appendLine("* ${node.language}/${node.template} — ${node.status.name}")
            }
        }
}

object TemplateTreeAudit {
    /** File extensions JBake renders as templates (mirrors VariantDeployability). */
    private val TEMPLATE_EXTENSIONS = setOf("thyme", "ftl", "html")

    fun of(
        referenceTemplates: File,
        variantTemplates: Map<String, File>,
        referenceLanguage: String = "fr",
    ): TemplateTree {
        val references =
            referenceTemplates
                .listFiles()
                .orEmpty()
                .filter { it.isFile && it.extension.lowercase() in TEMPLATE_EXTENSIONS }
                .sortedBy { it.name }

        val nodes =
            variantTemplates.entries
                .sortedBy { it.key }
                .flatMap { (language, variantDir) ->
                    references.map { reference ->
                        val variant = variantDir.resolve(reference.name)
                        TemplateNode(language, reference.name, statusOf(reference, variant))
                    }
                }

        return TemplateTree(referenceLanguage, nodes)
    }

    private fun statusOf(
        reference: File,
        variant: File,
    ): TemplateTranslationStatus =
        when {
            !variant.isFile -> TemplateTranslationStatus.MISSING
            variant.readText() == reference.readText() -> TemplateTranslationStatus.UNTRANSLATED
            else -> TemplateTranslationStatus.TRANSLATED
        }
}
