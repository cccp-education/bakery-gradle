package bakery.tree

import bakery.ThemeConfig

data class ResolvedTheme(
    val theme: ThemeConfig,
    val resolvedAtPath: String
)

class ThemeResolver(
    private val tree: SiteTree,
    private val overrides: Map<String, ThemeConfig> = emptyMap(),
    private val default: ThemeConfig = ThemeConfig()
) {
    init {
        val knownPaths = tree.walk().map { it.path }.toSet()
        val orphans = overrides.keys - knownPaths
        require(orphans.isEmpty()) {
            "Override paths not found in tree: $orphans"
        }
    }

    fun effectiveTheme(node: SiteNode): ResolvedTheme {
        val chain = ancestorChain(node)
        for (path in chain) {
            val override = overrides[path]
            if (override != null) {
                return ResolvedTheme(override, path)
            }
        }
        return ResolvedTheme(default, "")
    }

    fun resolveAll(): Map<String, ResolvedTheme> =
        tree.walk().associateBy({ it.path }, { effectiveTheme(it) })

    private fun ancestorChain(node: SiteNode): List<String> =
        if (node is SiteNode.Site) {
            listOf(node.path)
        } else {
            listOf(node.path) + parentPath(node.path)
        }

    private fun parentPath(path: String): List<String> {
        val parts = path.split("/")
        val chain = mutableListOf<String>()
        for (i in parts.size - 1 downTo 1) {
            chain.add(parts.subList(0, i).joinToString("/"))
        }
        return chain
    }
}