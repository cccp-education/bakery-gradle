package contracts.i18n

import java.util.Locale

interface MessageBundle {
    fun get(key: String, locale: Locale): String?
    fun keys(): Set<String>
    fun locale(): Locale
}
