package bakery.tree

import com.fasterxml.jackson.annotation.JsonIgnore

sealed interface SiteNode {
    val path: String

    @JsonIgnore
    fun isLeaf(): Boolean

    @JsonIgnore
    fun isSection(): Boolean

    data class Site(
        override val path: String,
        val sections: List<Section>
    ) : SiteNode {
        override fun isLeaf(): Boolean = false
        override fun isSection(): Boolean = true
    }

    data class Section(
        override val path: String,
        val articles: List<Article>
    ) : SiteNode {
        init {
            require(path.isNotBlank()) { "Section path must not be blank" }
        }

        override fun isLeaf(): Boolean = false
        override fun isSection(): Boolean = true
    }

    data class Article(
        override val path: String,
        @JsonIgnore val content: Content? = null
    ) : SiteNode {
        init {
            require(path.isNotBlank()) { "Article path must not be blank" }
        }

        override fun isLeaf(): Boolean = true
        override fun isSection(): Boolean = false
    }
}