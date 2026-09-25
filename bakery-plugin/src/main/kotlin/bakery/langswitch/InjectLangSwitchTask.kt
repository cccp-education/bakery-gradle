package bakery.langswitch

import bakery.BakeryConstants
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Template injection — file I/O, non-cacheable")
abstract class InjectLangSwitchTask : DefaultTask() {
    @get:Internal
    var siteDir: File? = null

    @get:Internal
    var supportedLanguages: List<String> = listOf("fr")

    @get:Internal
    var defaultLanguage: String = "fr"

    @get:Internal
    @get:Option(option = "currentLang", description = "Current language to render (defaults to all supported languages)")
    abstract val currentLang: org.gradle.api.provider.Property<String>

    private val languageLabels: Map<String, String> = LanguageLabelCatalog.labels()

    init {
        group = BakeryConstants.TRANSFORM_GROUP
        description = "Injects the language switcher fragment into menu.thyme for each configured language"
        currentLang.convention("")
    }

    @TaskAction
    fun executeInjection() {
        val site =
            siteDir ?: run {
                logger.warn("[injectLangSwitch] siteDir not configured — skipping")
                return
            }
        if (!site.exists()) {
            logger.warn("[injectLangSwitch] site directory does not exist: ${site.absolutePath}")
            return
        }

        val langsToProcess =
            if (currentLang.get().isNotBlank()) {
                listOf(currentLang.get())
            } else {
                supportedLanguages
            }

        val injector = LangSwitchInjector()
        var injectedCount = 0

        for (lang in langsToProcess) {
            val currentPath = if (lang == defaultLanguage) "" else "$lang/"
            val menuThyme = resolveMenuThyme(site, lang)
            if (menuThyme == null) {
                logger.lifecycle("[injectLangSwitch] No menu.thyme for lang '$lang' — skipping")
                continue
            }

            val links = LangSwitchMenu(supportedLanguages, defaultLanguage, lang, currentPath).generateLinks()
            val fragment = LangSwitchThymeleafRenderer(languageLabels).render(links)
            val originalContent = menuThyme.readText()
            val updated = injector.inject(originalContent, fragment)

            if (updated != originalContent) {
                menuThyme.writeText(updated)
                injectedCount++
                logger.lifecycle("[injectLangSwitch] Injected language switcher into menu.thyme for '$lang'")
            } else {
                logger.lifecycle("[injectLangSwitch] No lang-switcher-container found in menu.thyme for '$lang' — skipped")
            }
        }

        logger.lifecycle("[injectLangSwitch] Processed ${langsToProcess.size} language(s), injected into $injectedCount file(s)")
    }

    private fun resolveMenuThyme(
        site: File,
        lang: String,
    ): File? = LangSwitchMenuLayout.resolve(site, lang, defaultLanguage)
}
