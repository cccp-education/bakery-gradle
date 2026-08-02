package bakery.util

fun String.slugify(): String =
    this
        .lowercase()
        .replace(Regex("[éèêë]"), "e")
        .replace(Regex("[àâä]"), "a")
        .replace(Regex("[ùûü]"), "u")
        .replace(Regex("[ôö]"), "o")
        .replace(Regex("[îï]"), "i")
        .replace(Regex("[ç]"), "c")
        .replace(Regex("[^a-z0-9\\s-]"), "")
        .replace(Regex("\\s+"), "-")
        .replace(Regex("-+"), "-")
        .trim('-')
