package contracts.i18n

import java.util.Locale

interface LocaleResolver {
    fun resolve(config: I18nConfig): Locale
    fun resolveWithFallback(config: I18nConfig): Locale
}
