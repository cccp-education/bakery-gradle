package bakery.scaffold

import bakery.tree.SiteNode

/**
 * Structure de site generee par le LLM — output du scaffold assiste par IA.
 *
 * Le LLM retourne une specification JSON que [ScaffoldGenerator] parse
 * en cette data class pour creer la structure de site concrete.
 *
 * Pattern identique a [bakery.article.ArticleOutput] :
 * - Data class immutable, pas de logique
 * - Parsee depuis la reponse LLM par [ScaffoldGenerator.parseResponse]
 * - Consommee par le scaffold mecanique existant ([bakery.SiteScaffolder])
 *
 * TREE-7 : `tree` (optionnel) — arbre hierarchique Site→Section→Article.
 * Backward compat : `tree` null → `templates` liste plate (legacy).
 * `tree` present → `templates` est derive via [bakery.tree.flattenTemplates].
 */
data class ScaffoldOutput(
    val siteType: ScaffoldSiteType,
    val projectName: String,
    val description: String,
    val templates: List<String> = emptyList(),
    val tree: SiteNode? = null,
    val metadata: ScaffoldMetadata = ScaffoldMetadata()
)

/**
 * Metadonnees JBake suggerees par le LLM.
 *
 * Inclut les proprietes que le LLM peut deviner (titre, tags, layout)
 * et qui seront injectees dans `jbake.properties` et `site.yml`.
 */
data class ScaffoldMetadata(
    val title: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val layout: String = "post",
    val language: String = "fr"
)