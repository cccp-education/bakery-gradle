package bakery.contact

import bakery.BakeryConstants
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Scaffold injection — file I/O, non-cacheable")
abstract class ScaffoldContactSecTask : DefaultTask() {
    @get:Internal
    var siteDir: File? = null

    @get:Internal
    var contactConfig: ContactSecConfig? = null

    @get:Internal
    var defaultLanguage: String = "fr"

    @get:Internal
    var languages: List<String> = listOf("fr")

    init {
        group = BakeryConstants.TRANSFORM_GROUP
        description = "Scaffolds a hardened contact form (Turnstile, PoW, honeypot, rate limit) into footer.thyme + contact.js + firestore.rules"
    }

    @TaskAction
    fun executeScaffold() {
        val site =
            siteDir ?: run {
                logger.warn("[scaffoldContactSec] siteDir not configured — skipping")
                return
            }
        val config =
            contactConfig ?: run {
                logger.lifecycle("[scaffoldContactSec] No contact config — skipping (no-op)")
                return
            }
        if (!config.enabled) {
            logger.lifecycle("[scaffoldContactSec] Contact sec disabled — skipping (no-op)")
            return
        }
        if (!site.exists()) {
            logger.warn("[scaffoldContactSec] site directory does not exist: ${site.absolutePath}")
            return
        }

        val renderer = ContactSecRenderer()
        val footerFragment = renderer.renderFooterFragment(config)
        val contactJs = renderer.renderContactJs(config)
        val firestoreRules = renderer.renderFirestoreRules(config)
        var injectedCount = 0

        for (lang in languages) {
            val templatesDir =
                if (lang == defaultLanguage) {
                    site.resolve("templates")
                } else {
                    site.resolve(lang).resolve("templates")
                }
            val footerThyme = templatesDir.resolve("footer.thyme")

            val original = if (footerThyme.exists()) footerThyme.readText() else ""
            val updated = injectFooter(original, footerFragment)
            if (updated != original) {
                footerThyme.parentFile.mkdirs()
                footerThyme.writeText(updated)
                injectedCount++
                logger.lifecycle("[scaffoldContactSec] Injected contact form into footer.thyme for '$lang'")
            } else {
                logger.lifecycle("[scaffoldContactSec] No change for '$lang' (already injected)")
            }

            val assetsJsDir =
                if (lang == defaultLanguage) {
                    site.resolve("assets/js")
                } else {
                    site.resolve(lang).resolve("assets/js")
                }
            assetsJsDir.mkdirs()
            val jsFile = assetsJsDir.resolve("contact.js")
            jsFile.writeText(contactJs)
        }

        val rulesFile = site.resolve("firestore.rules")
        rulesFile.writeText(firestoreRules)
        logger.lifecycle("[scaffoldContactSec] Generated firestore.rules at ${rulesFile.name}")

        logger.lifecycle("[scaffoldContactSec] Processed ${languages.size} language(s), injected into $injectedCount file(s)")
    }

    private fun injectFooter(
        footerThyme: String,
        fragment: String,
    ): String {
        val markerRegex =
            Regex(
                "<!-- CONTACT-SEC: bakery -->[\\s\\S]*?<!-- /CONTACT-SEC: bakery -->",
                RegexOption.MULTILINE,
            )
        val markerMatch = markerRegex.find(footerThyme)
        if (markerMatch != null) {
            return footerThyme.replace(markerMatch.value, fragment)
        }
        if (footerThyme.contains(fragment)) {
            return footerThyme
        }
        val bodyCloseRegex = Regex("(</body>)(\\s*\\n?)")
        val bodyMatch = bodyCloseRegex.find(footerThyme)
        if (bodyMatch != null) {
            return footerThyme.replace(bodyMatch.value, "$fragment\n${bodyMatch.groupValues[1]}${bodyMatch.groupValues[2]}")
        }
        return if (footerThyme.isBlank()) fragment else "$footerThyme\n$fragment"
    }
}