package bakery.i18n

import document.translation.PivotArticle
import document.translation.PivotBlock

/**
 * GBL-010 P3 — structural alignment of a variant against its reference article.
 *
 * The translation delta preserves blocks **by index**. A variant whose block
 * structure differs from the source must be rebuilt by a full translation, never
 * aligned by index (a duplicated diagram can mask a lost paragraph, and the
 * preserved block would be written at the wrong position).
 *
 * This rule mirrors `document.translation.DocumentTranslator.isStructurallyAligned`
 * (same block count **and** same kind sequence). The vectors pinned by
 * `StructuralDivergenceTest` are the contract between the two layers: changing the
 * alignment rule in document-gradle must update them in the same release cycle.
 */
object StructuralDivergence {
    fun isAligned(
        source: PivotArticle,
        variant: PivotArticle,
    ): Boolean =
        source.blocks.size == variant.blocks.size &&
            signature(source.blocks) == signature(variant.blocks)

    fun signature(blocks: List<PivotBlock>): List<String> = blocks.map(::kindOf)

    private fun kindOf(block: PivotBlock): String =
        when (block) {
            is PivotBlock.Heading -> "heading:${block.level}"
            is PivotBlock.Paragraph -> "paragraph"
            is PivotBlock.ListBlock -> "list:${block.ordered}"
            is PivotBlock.Table -> "table"
            is PivotBlock.Admonition -> "admonition:${block.kind}"
            is PivotBlock.Source -> "source:${block.language.lowercase()}"
            is PivotBlock.DescriptionList -> "description-list"
            is PivotBlock.BlockMacro -> "block-macro:${block.name}"
            PivotBlock.Hr -> "hr"
        }
}
