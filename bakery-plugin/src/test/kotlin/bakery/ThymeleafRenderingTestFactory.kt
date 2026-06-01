package bakery

import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.FileTemplateResolver
import java.io.File

/**
 * Helper for Thymeleaf rendering tests (BKY-JB-9).
 * Configures a TemplateEngine that resolves templates from `src/main/resources/site/templates/`
 * and renders them with contextual variables — proving that th:if/th:unless guards
 * and variable interpolation work correctly in the actual HTML output.
 */
class ThymeleafRenderingTestFactory {

    private val templatesDir = File("src/main/resources/site/templates")

    private val engine: TemplateEngine = TemplateEngine().apply {
        setTemplateResolver(
            FileTemplateResolver().apply {
                prefix = "${templatesDir.absolutePath}/"
                suffix = ".thyme"
                templateMode = TemplateMode.HTML
                characterEncoding = "UTF-8"
                isCacheable = false
            }
        )
    }

    fun render(templateName: String, variables: Map<String, Any?> = emptyMap()): String {
        val context = Context()
        variables.forEach { (key, value) -> context.setVariable(key, value) }
        return engine.process(templateName, context)
    }

    fun templatesDirExists(): Boolean = templatesDir.isDirectory

    fun templateExists(name: String): Boolean = templatesDir.resolve("$name.thyme").isFile
}