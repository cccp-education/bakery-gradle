package bakery

import org.thymeleaf.IEngineConfiguration
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver
import org.thymeleaf.templateresolver.FileTemplateResolver
import java.io.File
import java.util.*

/**
 * Helper for Thymeleaf rendering tests (BKY-JB-9).
 *
 * Configures a [TemplateEngine] that resolves templates from `src/main/resources/site/templates/`
 * and renders them with contextual variables — proving that th:if/th:unless guards
 * and variable interpolation work correctly in the actual HTML output.
 *
 * Uses a [DualConventionResolver] that handles bakery's two Thymeleaf reference conventions:
 * - `~{name :: fragment}` (header.thyme, footer.thyme style — no file extension)
 * - `~{name.thyme::fragment}` (menu.thyme, page.thyme style — with file extension)
 */
class ThymeleafRenderingTestFactory {

    private val templatesDir = File("src/main/resources/site/templates")

    private val engine: TemplateEngine = TemplateEngine().apply {
        setTemplateResolver(DualConventionResolver(templatesDir))
    }

    fun render(templateName: String, variables: Map<String, Any?> = emptyMap()): String {
        val context = Context()
        variables.forEach { (key, value) -> context.setVariable(key, value) }
        return engine.process(templateName, context)
    }

    fun templatesDirExists(): Boolean = templatesDir.isDirectory

    fun templateExists(name: String): Boolean = templatesDir.resolve("$name.thyme").isFile
}

/**
 * A [FileTemplateResolver] that handles bakery's dual Thymeleaf reference convention.
 *
 * Standard Thymeleaf: `~{name :: fragment}` → resolver adds suffix `.thyme` → resolves `name.thyme`.
 * Bakery variant: `~{name.thyme::fragment}` → resolver adds suffix `.thyme` → `name.thyme.thyme` (404).
 *
 * This resolver overrides [computeResourceName] to detect the double-suffix case and
 * strip it before the file lookup fails.
 */
private class DualConventionResolver(private val templatesDir: File) : FileTemplateResolver() {

    init {
        prefix = "${templatesDir.absolutePath}/"
        suffix = ".thyme"
        templateMode = TemplateMode.HTML
        characterEncoding = "UTF-8"
        isCacheable = false
    }

    override fun computeResourceName(
        configuration: IEngineConfiguration?,
        ownerTemplate: String?,
        template: String?,
        prefix: String?,
        suffix: String?,
        forceSuffix: Boolean,
        templateAliases: Map<String, String>?,
        templateResolutionAttributes: Map<String, Any>?
    ): String? {
        val resourceName = super.computeResourceName(
            configuration, ownerTemplate, template, prefix, suffix, forceSuffix,
            templateAliases, templateResolutionAttributes
        ) ?: return null

        // Check if the resolved file exists (e.g. "analytics-script.thyme" → exists ✓)
        val file = File(resourceName)
        if (file.isFile) return resourceName

        // Double-suffix case: "auth-header.thyme.thyme" → try "auth-header.thyme"
        if (resourceName.endsWith(".thyme.thyme")) {
            val fixed = resourceName.removeSuffix(".thyme")
            val fixedFile = File(fixed)
            if (fixedFile.isFile) return fixed
        }

        // No-suffix case: some templates reference without .thyme but the suffix wasn't applied
        // Try the template name as-is (for references like ~{theme-script :: ...})
        val asIs = File("${prefix}${template}")
        if (asIs.isFile) return asIs.absolutePath

        return resourceName // let Thymeleaf raise the proper error
    }
}