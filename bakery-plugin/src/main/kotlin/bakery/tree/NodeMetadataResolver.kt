package bakery.tree

import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site

class NodeMetadataResolver(
    private val tree: SiteTree
) {
    fun effectiveMetadata(node: SiteNode): NodeMetadata {
        val chain = ancestorChain(node)
        var resolved = NodeMetadata()
        for (path in chain.reversed()) {
            val meta = findMetadata(path)
            if (meta != null) resolved = resolved.merge(meta)
        }
        return resolved
    }

    fun resolveAll(): Map<String, NodeMetadata> =
        tree.walk().associateBy({ it.path }, { effectiveMetadata(it) })

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

    private fun findMetadata(path: String): NodeMetadata? {
        val node = tree.findByPath(path) ?: return null
        return when (node) {
            is Site -> node.metadata
            is Section -> node.metadata
            is Article -> node.metadata
        }
    }

    private fun NodeMetadata.merge(other: NodeMetadata): NodeMetadata =
        NodeMetadata(
            title = other.title ?: title,
            description = other.description ?: description,
            tags = other.tags ?: tags,
            layout = other.layout ?: layout
        )
}
