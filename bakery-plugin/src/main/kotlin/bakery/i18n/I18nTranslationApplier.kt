package bakery.i18n

import java.io.File
import java.util.Properties

/**
 * Applique des traductions sur un fichier properties existant.
 *
 * Utilisé pour remplir manuellement `messages_en.properties` à partir
 * d'un map de traductions validées, après migration automatique.
 */
object I18nTranslationApplier {

    fun applyTranslations(targetFile: File, translations: Map<String, String>): Boolean {
        if (!targetFile.exists()) return false

        val props = Properties()
        targetFile.inputStream().use { props.load(it) }

        var updated = false
        for ((key, value) in translations) {
            if (props.containsKey(key)) {
                props.setProperty(key, value)
                updated = true
            }
        }

        if (updated) {
            targetFile.outputStream().use { props.store(it, null) }
        }
        return updated
    }
}
