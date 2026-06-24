package bakery.tree

import bakery.pivot.PivotArticle
import bakery.pivot.PivotBlock
import bakery.pivot.PivotInline
import bakery.pivot.TextTranslatableClassifier

data class Content(val pivot: PivotArticle) {

    fun blocs(): List<PivotBlock> = pivot.blocks

    fun inlineTexts(): List<PivotInline> =
        pivot.blocks.flatMap { inlineOf(it) }

    private fun inlineOf(block: PivotBlock): List<PivotInline> = when (block) {
        is PivotBlock.Paragraph -> block.inline
        is PivotBlock.ListBlock -> block.items.flatten()
        is PivotBlock.Table -> block.header.flatten() + block.rows.flatten().flatten()
        is PivotBlock.Admonition -> block.blocks.flatMap { inlineOf(it) }
        is PivotBlock.Heading -> emptyList()
        is PivotBlock.Source -> emptyList()
        is PivotBlock.Hr -> emptyList()
    }

    fun translatableSegments(): List<PivotInline> =
        inlineTexts().filter { it.translatable && TextTranslatableClassifier.isTranslatable(it.textValue()) }
}

private fun PivotInline.textValue(): String = when (this) {
    is PivotInline.Text -> text
    is PivotInline.Bold -> text
    is PivotInline.Code -> text
    is PivotInline.Link -> label
}