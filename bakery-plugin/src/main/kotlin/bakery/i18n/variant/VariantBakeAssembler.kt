package bakery.i18n.variant

import java.io.File

/**
 * BKY-LANG-NAV-8 — assemble a throwaway bake root for one language variant.
 *
 * JBake's `Oven` reads `<root>/content`, `<root>/templates`, `<root>/assets` and
 * `<root>/jbake.properties`. A variant owns only `content/` and `templates/`, so
 * the bake root is assembled by overlaying the shared `assets/` and
 * `jbake.properties` onto a copy of the variant. The shared assets are never
 * written back into the variant.
 *
 * A **partial corpus** (D8) may ship a variant's templates before its articles:
 * JBake refuses a root without a `content/` folder, and copying the French
 * reference in would be the copy-FR trap. An empty content shell is materialised
 * instead — the reference content is never inherited.
 *
 * Pure filesystem domain: no Gradle, no LLM, no network.
 */
class VariantBakeAssembler(
    private val layout: VariantLayout,
) {
    fun assemble(
        language: String,
        target: File,
    ) {
        val variant = layout.variant(language)
        require(variant.exists()) { "Variante i18n absente pour '$language': ${variant.absolutePath}" }

        target.deleteRecursively()
        target.mkdirs()

        val variantContent = layout.content(language)
        val contentTarget = target.resolve("content")
        if (variantContent.isDirectory) {
            variantContent.copyRecursively(contentTarget, overwrite = true)
        } else {
            // Partial corpus (D8): the variant ships templates only. JBake still
            // needs a content folder — an empty shell, never the French reference.
            contentTarget.mkdirs()
        }

        layout.templates(language).copyRecursively(target.resolve("templates"), overwrite = true)

        if (layout.sharedAssets.exists()) {
            layout.sharedAssets.copyRecursively(target.resolve("assets"), overwrite = true)
        }
        if (layout.jbakeProperties.exists()) {
            layout.jbakeProperties.copyTo(target.resolve("jbake.properties"), overwrite = true)
        }
    }
}
