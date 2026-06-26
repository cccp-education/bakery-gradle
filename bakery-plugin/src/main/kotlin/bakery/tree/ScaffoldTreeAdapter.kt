package bakery.tree

/**
 * Adapter flatten — tree → liste plate de templates.
 *
 * Backward compat scaffold legacy : ScaffoldOutput.templates: List<String>
 * est toujours accepte. Quand le LLM retourne un arbre, on le flatten en
 * liste plate pour le scaffold mecanique existant ([bakery.SiteScaffolder]).
 *
 * Convention : article path + ".thyme" = template name.
 * Domaine pur, zero I/O, testable sans Gradle.
 */
fun SiteNode.flattenTemplates(): List<String> =
    walkLeaves().map { "$it.thyme" }

private fun SiteNode.walkLeaves(): List<String> = when (this) {
    is SiteNode.Site -> sections.flatMap { it.walkLeaves() }
    is SiteNode.Section -> articles.flatMap { it.walkLeaves() }
    is SiteNode.Article -> listOf(path)
}