package bakery.tree

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object SiteTreeYaml {

    private val mapper: ObjectMapper by lazy {
        YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .let(::ObjectMapper)
            .registerKotlinModule()
            .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    fun serialize(node: SiteNode): String = mapper.writeValueAsString(node.toDto())

    fun parse(yaml: String): SiteNode = mapper.readValue(yaml, SiteNodeDto::class.java).toDomain()

    fun parseOrNull(yaml: String?): SiteNode? =
        if (yaml.isNullOrBlank()) null
        else try { parse(yaml) } catch (_: Exception) { null }
}