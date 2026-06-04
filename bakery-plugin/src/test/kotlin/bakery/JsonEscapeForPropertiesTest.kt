package bakery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * CS-FIN-2 (CS-13) — Tests unitaires pour l'échappement JSON dans les
 * fichiers Java `.properties`.
 *
 * Valide que les caractères spéciaux dans un JSON sérialisé (par Jackson)
 * sont correctement échappés pour être stockés comme valeur dans un
 * fichier `.properties` (qui interprète `\n`, `\r`, `\t` littéralement
 * et utilise `\` comme caractère d'échappement).
 *
 * Méthodologie : DDD/TDD baby steps. On extrait la logique d'échappement
 * dans une fonction pure testable, et on remplace le code manuel par
 * cette fonction.
 */
class JsonEscapeForPropertiesTest {

    @Nested
    @DisplayName("CS-FIN-2 — Échappement JSON pour Java properties")
    inner class EscapeJsonForJavaProperties {

        @Test
        @DisplayName("échappe les sauts de ligne (\\\\n dans properties → newline dans JSON)")
        fun `escapes newlines in JSON content`() {
            val json = """{"title":"ligne1\nligne2"}"""
            val escaped = escapeJsonForJavaProperties(json)
            // Le \n LITTÉRAL dans le JSON doit être doublé en \\n pour que
            // .properties ne le confonde pas avec un saut de ligne de fin de ligne.
            assertThat(escaped).contains("""ligne1\\nligne2""")
        }

        @Test
        @DisplayName("échappe les retours chariot")
        fun `escapes carriage returns in JSON content`() {
            val json = """{"data":"\rvalue"}"""
            val escaped = escapeJsonForJavaProperties(json)
            // \r (0x0D) doit devenir \\r — l'échappement actuel oublie ce cas
            assertThat(escaped).contains("""\\r""")
            assertThat(escaped).doesNotContain("\r")
        }

        @Test
        @DisplayName("échappe les tabulations")
        fun `escapes tabs in JSON content`() {
            val json = """{"data":"col1\tcol2"}"""
            val escaped = escapeJsonForJavaProperties(json)
            assertThat(escaped).contains("""col1\\tcol2""")
        }

        @Test
        @DisplayName("double les backslashes littéraux (un \\\\ en JSON devient \\\\\\\\)")
        fun `escapes backslashes in JSON content`() {
            val json = """{"path":"C:\\Users\\file"}"""
            val escaped = escapeJsonForJavaProperties(json)
            // Les backslashes de Windows dans le JSON (\\) doivent devenir \\\\
            // dans le fichier .properties pour préserver le sens JSON.
            assertThat(escaped).contains("""C:\\\\Users\\\\file""")
        }

        @Test
        @DisplayName("préserve les guillemets (caractère normal dans .properties)")
        fun `preserves double quotes without escaping`() {
            val json = """{"title":"hello \"world\""}"""
            val escaped = escapeJsonForJavaProperties(json)
            // Les \" dans le JSON (1 backslash + guillemet) deviennent \\
            // (2 backslashes + guillemet) après échappement des backslashes
            // pour .properties. Le guillemet lui-même reste inchangé.
            assertThat(escaped).isEqualTo("""{"title":"hello \\"world\\""}""")
        }

        @Test
        @DisplayName("conserve un JSON unicode intact")
        fun `preserves unicode characters unchanged`() {
            val json = """{"title":"éàü — café"}"""
            val escaped = escapeJsonForJavaProperties(json)
            assertThat(escaped).isEqualTo(json)
        }

        @Test
        @DisplayName("JSON simple (sans caractères spéciaux) → identité")
        fun `simple JSON passes through unchanged`() {
            val json = """{"scoredNodes":[],"total":0}"""
            assertThat(escapeJsonForJavaProperties(json)).isEqualTo(json)
        }
    }
}
