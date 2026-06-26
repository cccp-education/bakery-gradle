package bakery.tree

import bakery.LayoutType

data class NodeMetadata(
    val title: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    val layout: LayoutType? = null
)
