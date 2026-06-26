package bakery.tree

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SiteNodeDto.SiteDto::class, name = "site"),
    JsonSubTypes.Type(value = SiteNodeDto.SectionDto::class, name = "section"),
    JsonSubTypes.Type(value = SiteNodeDto.ArticleDto::class, name = "article")
)
sealed interface SiteNodeDto {
    val path: String
    val metadata: NodeMetadata?
    val outputConfig: OutputConfig?

    @JsonTypeName("site")
    data class SiteDto(
        override val path: String,
        val sections: List<SectionDto> = emptyList(),
        override val metadata: NodeMetadata? = null,
        override val outputConfig: OutputConfig? = null
    ) : SiteNodeDto

    @JsonTypeName("section")
    data class SectionDto(
        override val path: String,
        val articles: List<ArticleDto> = emptyList(),
        override val metadata: NodeMetadata? = null,
        override val outputConfig: OutputConfig? = null
    ) : SiteNodeDto

    @JsonTypeName("article")
    data class ArticleDto(
        override val path: String,
        override val metadata: NodeMetadata? = null,
        override val outputConfig: OutputConfig? = null
    ) : SiteNodeDto
}

fun SiteNode.toDto(): SiteNodeDto = when (this) {
    is SiteNode.Site -> SiteNodeDto.SiteDto(
        path = path,
        sections = sections.map { it.toDto() as SiteNodeDto.SectionDto },
        metadata = metadata,
        outputConfig = outputConfig
    )
    is SiteNode.Section -> SiteNodeDto.SectionDto(
        path = path,
        articles = articles.map { it.toDto() as SiteNodeDto.ArticleDto },
        metadata = metadata,
        outputConfig = outputConfig
    )
    is SiteNode.Article -> SiteNodeDto.ArticleDto(
        path = path,
        metadata = metadata,
        outputConfig = outputConfig
    )
}

fun SiteNodeDto.toDomain(): SiteNode = when (this) {
    is SiteNodeDto.SiteDto -> SiteNode.Site(
        path = path,
        sections = sections.map { it.toDomain() as SiteNode.Section },
        metadata = metadata,
        outputConfig = outputConfig
    )
    is SiteNodeDto.SectionDto -> SiteNode.Section(
        path = path,
        articles = articles.map { it.toDomain() as SiteNode.Article },
        metadata = metadata,
        outputConfig = outputConfig
    )
    is SiteNodeDto.ArticleDto -> SiteNode.Article(
        path = path,
        metadata = metadata,
        outputConfig = outputConfig
    )
}