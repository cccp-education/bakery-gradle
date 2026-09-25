package bakery.i18n

import bakery.BakeryConstants
import bakery.i18n.variant.VariantLayout
import bakery.intention.ResolveIntention
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.Properties

/**
 * BKY-LANG-NAV-8 — materialises a deployable `{lang}` template tree from a
 * **frozen** message bundle, with **zero LLM call** (Ink Economy Law).
 *
 * `jbake-core:2.7.0` installs no MessageResolver, so a site cannot bake a
 * template written with `#{key}`. A site that already owns frozen
 * `messages_{lang}.properties` (the i18n golden masters, computed once by
 * `migrateToI18n` + the offline translation batch) can nevertheless obtain a
 * full-swap variant deterministically:
 *
 *   1. the reference templates are keyed with the same extraction
 *      [I18nMigrationService.extractHardcodedText] used by `migrateToI18n`;
 *   2. every `#{key}` is resolved against the frozen bundle with
 *      [MessageBundleResolver] — no metered service is touched.
 *
 * A language without a frozen bundle is **skipped**, never invented: the CLI
 * contract is explicit, and a partial corpus is deployable (D8). A complete
 * bundle converges to a strict no-op on the second run.
 */
@DisableCachingByDefault(because = "Matérialisation de templates — I/O fichiers, non-cacheable")
abstract class MaterializeTemplatesTask : DefaultTask() {
    @get:Internal
    var siteDir: File? = null

    @get:Input
    @get:Optional
    @get:Option(option = "materializeTargetLangs", description = "Langues cibles séparées par virgules (ex: en,de)")
    abstract val materializeTargetLangs: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "materializeSourceLang", description = "Langue source (ex: fr)")
    abstract val materializeSourceLang: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "materializeDryRun", description = "Mode dry-run (true/false)")
    abstract val materializeDryRun: Property<String>

    init {
        group = BakeryConstants.TRANSFORM_GROUP
        description =
            "Matérialise un arbre de templates {lang} déployable depuis un bundle gelé (zéro LLM, JBake sans MessageResolver)"
        materializeTargetLangs.convention("")
        materializeSourceLang.convention("")
        materializeDryRun.convention("")
    }

    @TaskAction
    fun execute() {
        val site =
            siteDir ?: run {
                logger.warn("[materializeTemplates] siteDir non configuré — ignoré")
                return
            }
        val referenceDir = site.resolve("templates")
        if (!referenceDir.exists()) {
            logger.warn("[materializeTemplates] Aucun répertoire templates dans {}", site.absolutePath)
            return
        }

        val targetLangs = ResolveIntention.fromCliList(materializeTargetLangs.orNull, null, emptyList())
        val sourceLang = ResolveIntention.fromCli(materializeSourceLang.orNull, null, "fr")
        val dryRun = ResolveIntention.fromCliBoolean(materializeDryRun.orNull, null, true)

        if (targetLangs.isEmpty()) {
            val discovered = VariantLayout(site).discoverBundledLanguages(sourceLang)
            if (discovered.isEmpty()) {
                logger.lifecycle(
                    "[materializeTemplates] Aucune langue cible fournie et aucun bundle gelé — ignoré.",
                )
                return
            }
            logger.lifecycle(
                "[materializeTemplates] Langues cibles découvertes depuis les bundles gelés : {}",
                discovered.joinToString(", "),
            )
            materializeFor(discovered, sourceLang, dryRun)
            return
        }
        if (dryRun) {
            logger.lifecycle("[materializeTemplates] DRY-RUN — aucune écriture. Langues : {}", targetLangs.joinToString(", "))
            return
        }

        materializeFor(targetLangs, sourceLang, dryRun)
    }

    private fun materializeFor(
        targetLangs: List<String>,
        sourceLang: String,
        dryRun: Boolean,
    ) {
        val site = siteDir ?: return
        val referenceDir = site.resolve("templates")
        val i18nRoot = site.resolve("i18n")

        if (dryRun) {
            logger.lifecycle("[materializeTemplates] DRY-RUN — aucune écriture. Langues : {}", targetLangs.joinToString(", "))
            return
        }

        val extractor = I18nMigrationService()
        val templateFiles = referenceDir.walkTopDown().filter { it.isFile && it.extension == "thyme" }.toList()

        for (language in targetLangs) {
            if (language == sourceLang) continue
            val bundleFile = referenceDir.resolve("messages_$language.properties")
            if (!bundleFile.exists()) {
                logger.lifecycle("[materializeTemplates] [{}] Aucun bundle gelé ({}) — ignoré, jamais inventé", language, bundleFile.name)
                continue
            }
            val bundle = loadBundle(bundleFile)
            if (bundle.isEmpty()) {
                logger.lifecycle("[materializeTemplates] [{}] Bundle vide — ignoré", language)
                continue
            }

            val targetDir = i18nRoot.resolve("$language/templates")
            var written = 0
            var unresolved = 0
            for (template in templateFiles) {
                val relative = template.relativeTo(referenceDir).path
                val keyed = keyTemplate(template, extractor)
                val result = MessageBundleResolver.resolve(keyed, bundle)
                unresolved += result.unresolvedKeys.size
                if (result.unresolvedKeys.isNotEmpty()) {
                    logger.warn(
                        "[materializeTemplates] [{}] {} clés non résolues dans {} : {}",
                        language,
                        result.unresolvedKeys.size,
                        relative,
                        result.unresolvedKeys.joinToString(", "),
                    )
                    continue
                }
                val target = targetDir.resolve(relative)
                if (target.exists() && target.readText() == result.content) continue
                target.parentFile.mkdirs()
                target.writeText(result.content)
                written++
            }
            logger.lifecycle("[materializeTemplates] [{}] {} templates matérialisés, {} clés non résolues", language, written, unresolved)
        }
    }

    /**
     * Rewrites the hardcoded text of a reference template with its `#{key}`, so
     * the frozen bundle can resolve it. Reuses the exact extraction of
     * `migrateToI18n` — the key space is identical to the one the golden master
     * was built with.
     */
    private fun keyTemplate(
        template: File,
        extractor: I18nMigrationService,
    ): String {
        // BKY-LANG-NAV-8 — the reference `menu.thyme` may already carry the
        // generated switcher (injected in place by `injectLangSwitch`, which
        // runs in this same pipeline). Its labels are generated names, not
        // author prose: keying them would create `menu.N` keys absent from the
        // frozen bundle and write nothing (stale variant). Strip the container
        // body first — it is re-injected afterwards.
        val content = LangSwitcherContainerMask.strip(template.readText())
        val extractions = extractor.extractHardcodedText(content, template.nameWithoutExtension)
        if (extractions.isEmpty()) return content

        var keyed = content
        extractions.forEach { (key, value) ->
            keyed = keyed.replace(value, "#{$key}")
        }
        // Template that already carries keys is idempotent (the replace of an
        // absent literal is a no-op).
        return keyed
    }

    private fun loadBundle(file: File): Map<String, String> {
        val props = Properties()
        // UTF-8, not the ISO-8859-1 default of `load(InputStream)`: the frozen
        // bundles carry `→` and accents, and a mojibake arrow would ship a
        // corrupted homepage (Ink Economy — the value is reused, not recomputed).
        file.reader(Charsets.UTF_8).use { props.load(it) }
        return props.map { it.key.toString() to it.value.toString() }.toMap()
    }
}
