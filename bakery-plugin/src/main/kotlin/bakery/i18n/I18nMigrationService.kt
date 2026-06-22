package bakery.i18n

import java.io.File
import java.util.Properties

class I18nMigrationService {

    /**
     * Whitelist de textes qui ne doivent pas être extraits pour l'i18n.
     *
     * Inclut :
     * - noms propres (Magic Stick, cccp.education)
     * - termes techniques et marques (GitHub, SourceForge, Bootstrap, USB, RSS)
     * - identifiants de version (v0.1.14)
     * - labels d'interface anglais non traduits (Light, Dark, High Contrast)
     * - termes UI Bootstrap (Toggle navigation)
     */
    private val extractionWhitelist = setOf(
        "Magic Stick",
        "cccp.education",
        "GitHub",
        "SourceForge",
        "Bootstrap",
        "USB",
        "RSS",
        "Flux RSS",
        "Toggle navigation",
        "Light",
        "Dark",
        "High Contrast",
        "width=device-width, initial-scale=1.0",
        "(lecture seule)",
        "(lecture/ecriture)",
        "magic",
        "ISO brute",
        "Xubuntu",
        "BIOS",
        "UEFI",
        "v0.1.14"
    )

    private val substringBlacklist = setOf(
        "Magic Stick",
        "cccp.education",
        "Xubuntu",
        "BIOS",
        "UEFI",
        "ISO brute",
        "v0.1.14"
    )

    fun migrate(
        siteDir: File,
        languages: List<String>,
        defaultLanguage: String,
        dryRun: Boolean
    ): I18nMigrationResult {
        val templatesDir = siteDir.resolve("templates")
        val templateFiles = scanTemplates(templatesDir)

        if (templateFiles.isEmpty()) {
            return I18nMigrationResult(keysExtracted = 0, filesGenerated = 0, templatesModified = 0, dryRun = dryRun)
        }

        val allExtractions = mutableMapOf<String, MutableMap<String, String>>()
        var totalKeys = 0

        for (templateFile in templateFiles) {
            val extractions = extractHardcodedText(templateFile)
            if (extractions.isNotEmpty()) {
                allExtractions[templateFile.name] = extractions
                totalKeys += extractions.size
            }
        }

        if (totalKeys == 0) {
            return I18nMigrationResult(keysExtracted = 0, filesGenerated = 0, templatesModified = 0, dryRun = dryRun)
        }

        val messageFiles = generateMessageFiles(allExtractions, languages, templatesDir)

        var templatesModified = 0
        if (!dryRun) {
            for ((fileName, extractions) in allExtractions) {
                val templateFile = templatesDir.resolve(fileName)
                replaceHardcodedWithMessageKeys(templateFile, extractions)
                templatesModified++
            }
            writeMessageFiles(messageFiles, allExtractions)
            injectSiteLanguage(siteDir, defaultLanguage)
        }

        val filesGenerated = if (dryRun) 0 else messageFiles.size

        return I18nMigrationResult(
            keysExtracted = totalKeys,
            filesGenerated = filesGenerated,
            templatesModified = templatesModified,
            dryRun = dryRun
        )
    }

    fun scanTemplates(templatesDir: File): List<File> {
        if (!templatesDir.exists()) return emptyList()
        return templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .toList()
    }

    fun extractHardcodedText(templateFile: File): MutableMap<String, String> {
        val content = templateFile.readText()
        val extractions = linkedMapOf<String, String>()
        val baseName = templateFile.nameWithoutExtension
        var counter = 1

        val alreadyI18n = Regex("""th:(text|utext|placeholder|content|alt|title)\s*=\s*"#\{[^}]+}"""")
        val thAttrWithI18n = Regex("""th:attr\s*=\s*"[^"]*#\{[^}]+}[^"]*"""")
        val thValueExpr = Regex("""th:value\s*=\s*"#\{[^}]+}"""")

        val lines = content.lines()
        val result = StringBuilder()

        for (line in lines) {
            var modifiedLine = line

            if (!alreadyI18n.containsMatchIn(line) && !thAttrWithI18n.containsMatchIn(line) && !thValueExpr.containsMatchIn(line)) {

                modifiedLine = extractTextContent(modifiedLine, baseName, counter, extractions).also {
                    counter += extractions.count { it.key.startsWith("$baseName.") } - (counter - 1)
                }

                modifiedLine = extractPlaceholderAttribute(modifiedLine, baseName, counter, extractions).also {
                    counter = baseName.let { bn -> extractions.keys.count { k -> k.startsWith("$bn.") } } + 1
                }

                modifiedLine = extractAriaLabelAttribute(modifiedLine, baseName, counter, extractions).also {
                    counter = baseName.let { bn -> extractions.keys.count { k -> k.startsWith("$bn.") } } + 1
                }

                modifiedLine = extractMetaContentAttribute(modifiedLine, baseName, counter, extractions).also {
                    counter = baseName.let { bn -> extractions.keys.count { k -> k.startsWith("$bn.") } } + 1
                }

                modifiedLine = extractAltAttribute(modifiedLine, baseName, counter, extractions).also {
                    counter = baseName.let { bn -> extractions.keys.count { k -> k.startsWith("$bn.") } } + 1
                }

                modifiedLine = extractTitleAttribute(modifiedLine, baseName, counter, extractions).also {
                    counter = baseName.let { bn -> extractions.keys.count { k -> k.startsWith("$bn.") } } + 1
                }
            }

            result.appendLine(modifiedLine)
        }

        return extractions
    }

    private fun extractTextContent(
        line: String,
        baseName: String,
        counter: Int,
        extractions: MutableMap<String, String>
    ): String {
        val textContentRegex = Regex("""<(?!script\b|style\b)(\w+)((?:\s+[^>]*?)?)\s*>\s*([^<]{2,})\s*</\1>""")
        return textContentRegex.replace(line) { match ->
            val tagName = match.groupValues[1]
            val attrs = match.groupValues[2]
            val text = match.groupValues[3].trim()

            if (text.matches(Regex("""^\s*#\{[^}]+}\s*$"""))) return@replace match.value
            if (text.isBlank() || text.length < 2) return@replace match.value
            if (attrs.contains("th:text") || attrs.contains("th:utext")) return@replace match.value
            if (isWhitelisted(text)) return@replace match.value

            val key = "$baseName.$counter"
            extractions[key] = text
            "<$tagName$attrs th:text=\"#{${key}}\">$text</$tagName>"
        }
    }

    private fun isWhitelisted(text: String): Boolean {
        if (extractionWhitelist.contains(text)) return true
        if (substringBlacklist.any { text.contains(it, ignoreCase = true) }) return true
        if (text.matches(Regex("""^v?\d+(\.\d+)+.*"""))) return true
        if (text.matches(Regex("""^(http|https|mailto|ftp):.*"""))) return true
        return false
    }

    private fun extractPlaceholderAttribute(
        line: String,
        baseName: String,
        counter: Int,
        extractions: MutableMap<String, String>
    ): String {
        val placeholderRegex = Regex("""placeholder\s*=\s*"([^"]{2,})"(?!\s*th:)""")
        return placeholderRegex.replace(line) { match ->
            val text = match.groupValues[1]
            if (text.matches(Regex("""#\{[^}]+}"""))) return@replace match.value
            if (line.contains("th:placeholder")) return@replace match.value
            if (isWhitelisted(text)) return@replace match.value

            val key = "$baseName.$counter"
            extractions[key] = text
            """th:placeholder="#{$key}" placeholder="$text""""
        }
    }

    private fun extractAriaLabelAttribute(
        line: String,
        baseName: String,
        counter: Int,
        extractions: MutableMap<String, String>
    ): String {
        val ariaLabelRegex = Regex("""aria-label\s*=\s*"([^"]{2,})"(?!\s*th:)""")
        return ariaLabelRegex.replace(line) { match ->
            val text = match.groupValues[1]
            if (text.matches(Regex("""#\{[^}]+}"""))) return@replace match.value
            if (line.contains("th:attr") && line.contains("aria-label")) return@replace match.value
            if (isWhitelisted(text)) return@replace match.value

            val key = "$baseName.$counter"
            extractions[key] = text
            val existingThAttr = Regex("""th:attr\s*=\s*"([^"]*)"""").find(line)
            if (existingThAttr != null) {
                val currentAttr = existingThAttr.groupValues[1]
                line.replace(
                    """th:attr="$currentAttr"""",
                    """th:attr="$currentAttr,aria-label=#{${key}}"""
                ) + " aria-label=\"$text\""
            } else {
                line.replace(
                    """aria-label="$text"""",
                    """th:attr="aria-label=#{${key}}" aria-label="$text"""
                )
            }
        }
    }

    private fun extractMetaContentAttribute(
        line: String,
        baseName: String,
        counter: Int,
        extractions: MutableMap<String, String>
    ): String {
        val metaContentRegex = Regex("""<meta\s[^>]*?\bcontent\s*=\s*"([^"]{2,})"(?!\s*th:)[^>]*?>""")
        return metaContentRegex.replace(line) { match ->
            val text = match.groupValues[1]
            if (text.matches(Regex("""#\{[^}]+}"""))) return@replace match.value
            if (match.value.contains("th:content")) return@replace match.value
            if (isWhitelisted(text)) return@replace match.value

            val key = "$baseName.$counter"
            extractions[key] = text
            match.value.replace("""content="$text""", """th:content="#{$key}" content="$text"""")
        }
    }

    private fun extractAltAttribute(
        line: String,
        baseName: String,
        counter: Int,
        extractions: MutableMap<String, String>
    ): String {
        val altRegex = Regex("""\balt\s*=\s*"([^"]{2,})"(?!\s*th:)""")
        return altRegex.replace(line) { match ->
            val text = match.groupValues[1]
            if (text.matches(Regex("""#\{[^}]+}"""))) return@replace match.value
            if (line.contains("th:alt")) return@replace match.value
            if (isWhitelisted(text)) return@replace match.value

            val key = "$baseName.$counter"
            extractions[key] = text
            """th:alt="#{$key}" alt="$text""""
        }
    }

    private fun extractTitleAttribute(
        line: String,
        baseName: String,
        counter: Int,
        extractions: MutableMap<String, String>
    ): String {
        val titleRegex = Regex("""\btitle\s*=\s*"([^"]{2,})"(?!\s*th:title)""")
        return titleRegex.replace(line) { match ->
            val text = match.groupValues[1]
            if (text.matches(Regex("""#\{[^}]+}"""))) return@replace match.value
            if (line.contains("th:title")) return@replace match.value
            if (isWhitelisted(text)) return@replace match.value

            val key = "$baseName.$counter"
            extractions[key] = text
            """th:title="#{$key}" title="$text""""
        }
    }

    fun generateMessageFiles(
        allExtractions: Map<String, Map<String, String>>,
        languages: List<String>,
        templatesDir: File
    ): Map<String, File> {
        val allKeys = linkedMapOf<String, String>()
        for ((_, extractions) in allExtractions) {
            allKeys.putAll(extractions)
        }

        val messageFiles = linkedMapOf<String, File>()
        for (lang in languages) {
            val props = Properties()
            for ((key, value) in allKeys) {
                props.setProperty(key, if (lang == "fr") value else "")
            }
            val file = templatesDir.resolve("messages_$lang.properties")
            messageFiles["messages_$lang.properties"] = file
        }

        return messageFiles
    }

    fun replaceHardcodedWithMessageKeys(
        templateFile: File,
        extractions: Map<String, String>
    ): File {
        val content = templateFile.readText()
        val alreadyI18n = Regex("""th:(text|utext|placeholder|content|alt|title)\s*=\s*"#\{[^}]+}"""")
        val thAttrWithI18n = Regex("""th:attr\s*=\s*"[^"]*#\{[^}]+}[^"]*"""")
        val thValueExpr = Regex("""th:value\s*=\s*"#\{[^}]+}"""")

        val lines = content.lines()
        val result = StringBuilder()

        for (line in lines) {
            var modifiedLine = line

            if (!alreadyI18n.containsMatchIn(line) && !thAttrWithI18n.containsMatchIn(line) && !thValueExpr.containsMatchIn(line)) {
                modifiedLine = applyTextContentReplacement(modifiedLine, extractions)
                modifiedLine = applyPlaceholderReplacement(modifiedLine, extractions)
                modifiedLine = applyAriaLabelReplacement(modifiedLine, extractions)
                modifiedLine = applyMetaContentReplacement(modifiedLine, extractions)
                modifiedLine = applyAltReplacement(modifiedLine, extractions)
                modifiedLine = applyTitleReplacement(modifiedLine, extractions)
            }

            result.appendLine(modifiedLine)
        }

        templateFile.writeText(result.toString().trimEnd())
        return templateFile
    }

    private fun applyTextContentReplacement(line: String, extractions: Map<String, String>): String {
        val textContentRegex = Regex("""<(?!script\b|style\b)(\w+)((?:\s+[^>]*?)?)\s*>\s*([^<]{2,})\s*</\1>""")
        return textContentRegex.replace(line) { match ->
            val tagName = match.groupValues[1]
            val attrs = match.groupValues[2]
            val text = match.groupValues[3].trim()
            if (attrs.contains("th:text") || attrs.contains("th:utext")) return@replace match.value
            val entry = extractions.entries.find { it.value == text } ?: return@replace match.value
            "<$tagName$attrs th:text=\"#{${entry.key}}\">$text</$tagName>"
        }
    }

    private fun applyPlaceholderReplacement(line: String, extractions: Map<String, String>): String {
        val placeholderRegex = Regex("""placeholder\s*=\s*"([^"]{2,})"(?!\s*th:)""")
        return placeholderRegex.replace(line) { match ->
            val text = match.groupValues[1]
            if (line.contains("th:placeholder")) return@replace match.value
            val entry = extractions.entries.find { it.value == text } ?: return@replace match.value
            """th:placeholder="#{${entry.key}}" placeholder="$text""""
        }
    }

    private fun applyAriaLabelReplacement(line: String, extractions: Map<String, String>): String {
        val ariaLabelRegex = Regex("""aria-label\s*=\s*"([^"]{2,})"(?!\s*th:)""")
        return ariaLabelRegex.replace(line) { match ->
            val text = match.groupValues[1]
            if (line.contains("th:attr") && line.contains("aria-label")) return@replace match.value
            val entry = extractions.entries.find { it.value == text } ?: return@replace match.value
            val existingThAttr = Regex("""th:attr\s*=\s*"([^"]*)"""").find(line)
            if (existingThAttr != null) {
                val currentAttr = existingThAttr.groupValues[1]
                line.replace(
                    """th:attr="$currentAttr"""",
                    """th:attr="$currentAttr,aria-label=#{${entry.key}}""""
                ) + " aria-label=\"$text\""
            } else {
                line.replace(
                    """aria-label="$text"""",
                    """th:attr="aria-label=#{${entry.key}}" aria-label="$text""""
                )
            }
        }
    }

    private fun applyMetaContentReplacement(line: String, extractions: Map<String, String>): String {
        val metaContentRegex = Regex("""<meta\s[^>]*?\bcontent\s*=\s*"([^"]{2,})"(?!\s*th:)[^>]*?>""")
        return metaContentRegex.replace(line) { match ->
            val text = match.groupValues[1]
            if (match.value.contains("th:content")) return@replace match.value
            val entry = extractions.entries.find { it.value == text } ?: return@replace match.value
            match.value.replace("""content="$text"""", """th:content="#{${entry.key}}" content="$text"""")
        }
    }

    private fun applyAltReplacement(line: String, extractions: Map<String, String>): String {
        val altRegex = Regex("""\balt\s*=\s*"([^"]{2,})"(?!\s*th:)""")
        return altRegex.replace(line) { match ->
            val text = match.groupValues[1]
            if (line.contains("th:alt")) return@replace match.value
            val entry = extractions.entries.find { it.value == text } ?: return@replace match.value
            """th:alt="#{${entry.key}}" alt="$text""""
        }
    }

    private fun applyTitleReplacement(line: String, extractions: Map<String, String>): String {
        val titleRegex = Regex("""\btitle\s*=\s*"([^"]{2,})"(?!\s*th:title)""")
        return titleRegex.replace(line) { match ->
            val text = match.groupValues[1]
            if (line.contains("th:title")) return@replace match.value
            val entry = extractions.entries.find { it.value == text } ?: return@replace match.value
            """th:title="#{${entry.key}}" title="$text""""
        }
    }

    fun writeMessageFiles(
        messageFiles: Map<String, File>,
        allExtractions: Map<String, Map<String, String>>
    ) {
        val allKeys = linkedMapOf<String, String>()
        for ((_, extractions) in allExtractions) {
            allKeys.putAll(extractions)
        }

        for ((fileName, file) in messageFiles) {
            file.parentFile.mkdirs()
            val lang = fileName.removePrefix("messages_").removeSuffix(".properties")
            val props = Properties()
            for ((key, value) in allKeys) {
                props.setProperty(key, if (lang == "fr") value else "")
            }
            file.outputStream().use { props.store(it, null) }
        }
    }

    fun injectSiteLanguage(siteDir: File, defaultLanguage: String): Boolean {
        val jbakeProperties = siteDir.resolve("jbake.properties")
        if (!jbakeProperties.exists()) return false

        val content = jbakeProperties.readText()
        if (content.contains("site.language=")) return false

        val updated = if (content.endsWith("\n")) {
            content + "site.language=$defaultLanguage\n"
        } else {
            content + "\nsite.language=$defaultLanguage\n"
        }
        jbakeProperties.writeText(updated)
        return true
    }
}
