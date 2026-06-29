package bakery.tree

import bakery.escapeJsonForJavaProperties
import bakery.injection.updateProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import kotlin.text.Charsets.UTF_8

object TreeBakeService {

    private val jsonMapper: ObjectMapper by lazy {
        ObjectMapper().registerKotlinModule()
    }

    fun injectTreeConfig(tree: SiteNodeDto, srcDir: File) {
        val jbakeProps = srcDir.resolve("jbake.properties")
        if (!jbakeProps.exists()) return

        val siteNode = tree.toDomain()
        val siteTree = SiteTree(siteNode)
        val outputResolver = OutputConfigResolver(siteTree)
        val metaResolver = NodeMetadataResolver(siteTree)
        val resolvedOutputs = outputResolver.resolveAll()
        val resolvedMetas = metaResolver.resolveAll()

        val leafConfigs: Map<String, Map<String, Any?>> = siteTree.leaves()
            .associate { article ->
                val out = resolvedOutputs[article.path] ?: OutputConfig()
                val meta = resolvedMetas[article.path] ?: NodeMetadata()
                article.path to buildConfigMap(out, meta)
            }

        val treePayload = mapOf(
            "present" to true,
            "nodes" to leafConfigs
        )
        val rawJson = jsonMapper.writeValueAsString(treePayload)
        val escaped = escapeJsonForJavaProperties(rawJson)

        val lines = jbakeProps.readText(UTF_8).lines().toMutableList()
        updateProperty(lines, "bakeTreeConfig", escaped)
        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
    }

    private fun buildConfigMap(out: OutputConfig, meta: NodeMetadata): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        if (out.template != null) map["template"] = out.template
        if (out.layout != null) map["layout"] = out.layout!!.name
        if (out.cssFiles != null) map["cssFiles"] = out.cssFiles
        if (out.jsFiles != null) map["jsFiles"] = out.jsFiles
        if (out.theme != null) map["theme"] = mapOf(
            "mode" to out.theme!!.mode,
            "primaryColor" to out.theme!!.primaryColor,
            "secondaryColor" to out.theme!!.secondaryColor,
        )
        if (meta.title != null) map["title"] = meta.title
        if (meta.description != null) map["description"] = meta.description
        if (meta.tags != null) map["tags"] = meta.tags
        val assets = out.assets
        if (assets != null) {
            val assetsMap = mutableMapOf<String, List<Map<String, Any?>>>()
            val css = assets.css?.map { a ->
                val m: MutableMap<String, Any?> = mutableMapOf("path" to a.path)
                if (a.integrity != null) m["integrity"] = a.integrity
                m
            }
            val js = assets.js?.map { a ->
                val m: MutableMap<String, Any?> = mutableMapOf("path" to a.path)
                if (a.integrity != null) m["integrity"] = a.integrity
                if (a.async == true) m["async"] = true
                if (a.defer == true) m["defer"] = true
                m
            }
            if (css != null) assetsMap["css"] = css
            if (js != null) assetsMap["js"] = js
            if (assetsMap.isNotEmpty()) map["assets"] = assetsMap
        }
        return map
    }
}
