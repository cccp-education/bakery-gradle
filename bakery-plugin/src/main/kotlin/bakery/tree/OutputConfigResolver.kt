package bakery.tree

import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site

class OutputConfigResolver(
    private val tree: SiteTree
) {
    fun effectiveConfig(node: SiteNode): OutputConfig {
        val chain = ancestorChain(node)
        var resolved = OutputConfig()
        for (path in chain.reversed()) {
            val config = findConfig(path)
            if (config != null) resolved = resolved.merge(config)
        }
        return resolved
    }

    fun resolveAll(): Map<String, OutputConfig> =
        tree.walk().associateBy({ it.path }, { effectiveConfig(it) })

    private fun ancestorChain(node: SiteNode): List<String> {
        if (node is Site) return listOf(node.path)
        val parts = node.path.split("/")
        val chain = mutableListOf<String>()
        chain.add(node.path)
        for (i in parts.size - 1 downTo 1) {
            chain.add(parts.subList(0, i).joinToString("/"))
        }
        chain.add("")
        return chain
    }

    private fun findConfig(path: String): OutputConfig? {
        val node = tree.findByPath(path) ?: return null
        return when (node) {
            is Site -> node.outputConfig
            is Section -> node.outputConfig
            is Article -> node.outputConfig
        }
    }

    private fun OutputConfig.merge(other: OutputConfig): OutputConfig =
        OutputConfig(
            template = other.template ?: template,
            layout = other.layout ?: layout,
            cssFiles = other.cssFiles ?: cssFiles,
            jsFiles = other.jsFiles ?: jsFiles,
            assets = other.assets?.merge(assets) ?: assets,
            theme = other.theme ?: theme
        )
}
