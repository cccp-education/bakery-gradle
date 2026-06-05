package bakery

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.System.console
import java.lang.System.getenv

data class ConfigPromptEnvironment(
    val readInput: () -> String?,
    val readPassword: () -> CharArray?,
    val writeOutput: (String) -> Unit,
    val logger: Logger,
    val project: Project
) {
    companion object {
        fun defaultFor(logger: Logger, project: Project): ConfigPromptEnvironment {
            val console = console()
            val fallbackReader = BufferedReader(InputStreamReader(System.`in`))
            return if (console != null) {
                ConfigPromptEnvironment(
                    readInput = { console.readLine() },
                    readPassword = { console.readPassword() },
                    writeOutput = { print(it) },
                    logger = logger,
                    project = project
                )
            } else {
                ConfigPromptEnvironment(
                    readInput = { fallbackReader.readLine() },
                    readPassword = { null },
                    writeOutput = { print(it); logger.lifecycle("Console not available. Using standard input.") },
                    logger = logger,
                    project = project
                )
            }
        }
    }
}