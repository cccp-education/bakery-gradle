package bakery.injection

internal fun updateProperty(lines: MutableList<String>, key: String, value: String) {
    val idx = lines.indexOfFirst { it.startsWith("$key=") }
    if (idx >= 0) {
        lines[idx] = "$key=$value"
    } else {
        lines.add("$key=$value")
    }
}
