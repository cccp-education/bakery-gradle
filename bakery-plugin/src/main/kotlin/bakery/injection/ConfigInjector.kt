package bakery.injection

fun interface ConfigInjector {
    fun inject(lines: MutableList<String>, resolver: (String, String) -> String)
}
