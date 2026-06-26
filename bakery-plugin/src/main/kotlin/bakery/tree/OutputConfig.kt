package bakery.tree

import bakery.LayoutType
import bakery.ThemeConfig

data class OutputConfig(
    val template: String? = null,
    val layout: LayoutType? = null,
    val cssFiles: List<String>? = null,
    val jsFiles: List<String>? = null,
    val theme: ThemeConfig? = null
)
