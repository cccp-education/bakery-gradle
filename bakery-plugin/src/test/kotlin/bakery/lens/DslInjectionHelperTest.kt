package bakery.lens

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * TDD unit tests for DSL insertion helper — insertBeforeClosingBrace.
 *
 * Validates that Cucumber step DSL injection (augmentedContext, lens, etc.)
 * produces valid Kotlin DSL syntax when inserted into build.gradle.kts.
 *
 * Baby step DDD/TDD : 1 fonction → 1 fichier test → 5+ edge cases.
 */
@DisplayName("DSL Injection Helper — insertBeforeClosingBrace")
class DslInjectionHelperTest {

    private val helper = DslInjectionHelper

    @Nested
    @DisplayName("Single-line bakery block")
    inner class SingleLineBlock {

        @Test
        @DisplayName("Inserts augmentedContext before closing brace of single-line bakery block")
        fun insertsBeforeClosingBraceSingleLine() {
            val content = """plugins { id("education.cccp.bakery") }
bakery { configPath = file("site.yml").absolutePath }"""
            val block = "augmentedContext {\n    enabled = true\n}"
            val result = helper.insertBeforeClosingBrace(content, "bakery", block)

            assertThat(result).contains("augmentedContext")
            assertThat(result).contains("configPath = file(\"site.yml\").absolutePath")
            // The augmentedContext block is inserted BEFORE the final '}' of bakery { ... }
            // which means AFTER configPath in the result — both are valid Kotlin
            val lastClosingBrace = result.lastIndexOf('}')
            val augmentedIndex = result.indexOf("augmentedContext")
            assertThat(augmentedIndex).isLessThan(lastClosingBrace)
        }

        @Test
        @DisplayName("Result is valid Kotlin DSL — bakery block remains well-formed")
        fun resultIsValidKotlinDsl() {
            val content = """plugins { id("education.cccp.bakery") }
bakery { configPath = file("site.yml").absolutePath }"""
            val block = "augmentedContext {\n    enabled = true\n}"
            val result = helper.insertBeforeClosingBrace(content, "bakery", block)

            // Count braces — must be balanced
            val openBraces = result.count { it == '{' }
            val closeBraces = result.count { it == '}' }
            assertThat(openBraces).isEqualTo(closeBraces)
        }
    }

    @Nested
    @DisplayName("Multi-line bakery block")
    inner class MultiLineBlock {

        @Test
        @DisplayName("Inserts before closing brace of multi-line bakery block")
        fun insertsBeforeClosingBraceMultiLine() {
            val content = """
plugins { id("education.cccp.bakery") }
bakery {
    configPath = file("site.yml").absolutePath
}
            """.trimIndent()
            val block = "augmentedContext {\n    enabled = true\n}"
            val result = helper.insertBeforeClosingBrace(content, "bakery", block)

            assertThat(result).contains("augmentedContext")
            assertThat(result).contains("configPath")
            // augmentedContext appears before the closing '}'
            assertThat(result.indexOf("augmentedContext")).isLessThan(result.lastIndexOf("}"))
        }

        @Test
        @DisplayName("Preserves existing nested blocks when inserting new one")
        fun preservesExistingNestedBlocks() {
            val content = """
plugins { id("education.cccp.bakery") }
bakery {
    configPath = file("site.yml").absolutePath
    ia {
        baseUrl = "http://localhost:11464"
    }
}
            """.trimIndent()
            val block = "augmentedContext {\n    enabled = true\n}"
            val result = helper.insertBeforeClosingBrace(content, "bakery", block)

            assertThat(result).contains("baseUrl = \"http://localhost:11464\"")
            assertThat(result).contains("augmentedContext")
            // Braces balanced
            val openBraces = result.count { it == '{' }
            val closeBraces = result.count { it == '}' }
            assertThat(openBraces).isEqualTo(closeBraces)
        }
    }

    @Nested
    @DisplayName("Edge cases")
    inner class EdgeCases {

        @Test
        @DisplayName("Returns content unchanged when blockName not found")
        fun returnsUnchangedWhenBlockNameNotFound() {
            val content = """plugins { id("education.cccp.bakery") }
something { configPath = file("site.yml").absolutePath }"""
            val block = "augmentedContext {\n    enabled = true\n}"
            val result = helper.insertBeforeClosingBrace(content, "bakery", block)

            assertThat(result).isEqualTo(content)
        }

        @Test
        @DisplayName("Handles budget block with nested DSL (2 levels of braces)")
        fun handlesBudgetWithNestedDsl() {
            val content = """plugins { id("education.cccp.bakery") }
bakery { configPath = file("site.yml").absolutePath }"""
            val block = """
augmentedContext {
    enabled = true
    budget {
        maxArticlesPerPage = 4
        minSimilarity = 0.7
    }
}
            """.trimIndent()
            val result = helper.insertBeforeClosingBrace(content, "bakery", block)

            assertThat(result).contains("maxArticlesPerPage = 4")
            assertThat(result).contains("configPath")
            // Braces balanced
            val openBraces = result.count { it == '{' }
            val closeBraces = result.count { it == '}' }
            assertThat(openBraces).isEqualTo(closeBraces)
        }
    }
}

/**
 * Utility for inserting DSL blocks into build.gradle.kts content.
 * Extracted from LensSteps for testability.
 */
object DslInjectionHelper {

    /**
     * Insères [blockToInsert] avant le '}' fermant du bloc [blockName] { ... }.
     * Gère les blocs sur une ou plusieurs lignes, avec accolades imbriquées.
     */
    fun insertBeforeClosingBrace(content: String, blockName: String, blockToInsert: String): String {
        val blockStart = content.indexOf("$blockName {")
        if (blockStart == -1) return content

        // Compter les accolades pour trouver le '}' fermant correspondant
        var depth = 0
        var closingBraceIndex = -1
        for (i in blockStart until content.length) {
            when (content[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        closingBraceIndex = i
                        break
                    }
                }
            }
        }

        if (closingBraceIndex == -1) return content

        // Insérer le bloc avant le '}' fermant
        val indentedBlock = blockToInsert.prependIndent("    ")
        return content.substring(0, closingBraceIndex) +
                "\n$indentedBlock\n" +
                content.substring(closingBraceIndex)
    }
}