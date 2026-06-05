package bakery

fun interface ConfigPromptM<out A> {
    fun run(env: ConfigPromptEnvironment): A

    fun <B> map(f: (A) -> B): ConfigPromptM<B> = ConfigPromptM { env -> f(this.run(env)) }

    fun <B> flatMap(f: (A) -> ConfigPromptM<B>): ConfigPromptM<B> =
        ConfigPromptM { env -> f(this.run(env)).run(env) }

    companion object {
        fun pure(value: String): ConfigPromptM<String> = ConfigPromptM { value }

        fun fromCli(propertyName: String, cliProperty: String): ConfigPromptM<String> =
            ConfigPromptM { env ->
                resolveWithoutPrompt(env, propertyName, cliProperty)
            }

        fun prompt(
            propertyName: String,
            example: String? = null
        ): ConfigPromptM<String> = ConfigPromptM { env ->
            promptNormalValue(env, propertyName, example)
        }

        fun promptSensitive(propertyName: String): ConfigPromptM<String> = ConfigPromptM { env ->
            promptSensitiveValue(env, propertyName)
        }

        fun fromCliOrPrompt(
            propertyName: String,
            cliProperty: String,
            sensitive: Boolean = false,
            example: String? = null,
            default: String? = null
        ): ConfigPromptM<String> = ConfigPromptM { env ->
            resolveConfigValue(env, propertyName, cliProperty, sensitive, example, default)
        }
    }
}

private fun resolveWithoutPrompt(
    env: ConfigPromptEnvironment,
    propertyName: String,
    cliProperty: String
): String {
    if (env.project.hasProperty(cliProperty)) {
        val value = env.project.property(cliProperty) as? String
        if (!value.isNullOrBlank()) return value
    }
    val envVar = cliProperty
        .replace(Regex("([a-z])([A-Z])"), "$1_$2")
        .uppercase()
    System.getenv(envVar)?.takeIf { it.isNotBlank() }?.let { return it }
    throw IllegalStateException("No value found for $propertyName (cliProperty=$cliProperty)")
}

private fun promptNormalValue(
    env: ConfigPromptEnvironment,
    propertyName: String,
    example: String?
): String {
    val exampleText = example?.let { " (e.g., $it)" } ?: ""
    var input: String?
    do {
        env.writeOutput("Enter $propertyName$exampleText: ")
        input = env.readInput()
        if (input.isNullOrBlank())
            env.logger.warn("$propertyName cannot be empty. Please try again.")
    } while (input.isNullOrBlank())
    return input
}

private fun promptSensitiveValue(
    env: ConfigPromptEnvironment,
    propertyName: String
): String {
    var input: CharArray?
    do {
        env.writeOutput("Enter $propertyName (hidden): ")
        input = env.readPassword()
        if (input == null || input.isEmpty())
            env.logger.warn("$propertyName cannot be empty. Please try again.")
    } while (input == null || input.isEmpty())
    return String(input).also { input.fill('0') }
}

private fun resolveConfigValue(
    env: ConfigPromptEnvironment,
    propertyName: String,
    cliProperty: String,
    sensitive: Boolean,
    example: String?,
    default: String?
): String {
    if (env.project.hasProperty(cliProperty)) {
        val value = (env.project.property(cliProperty) as? String)
        if (!value.isNullOrBlank()) return value
    }
    val envVar = cliProperty
        .replace(Regex("([a-z])([A-Z])"), "$1_$2")
        .uppercase()
    System.getenv(envVar)?.takeIf { it.isNotBlank() }?.let { return it }
    default?.let { return it }
    if (sensitive) return promptSensitiveValue(env, propertyName)
    return promptNormalValue(env, propertyName, example)
}