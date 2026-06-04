package bakery.scenarios

import io.cucumber.java.en.Given
import org.assertj.core.api.Assertions.assertThat
import kotlin.text.Charsets.UTF_8

/**
 * Cucumber steps for BKY-IA-3: Validate Firebase Config — Validation mécanique du bloc Firebase.
 *
 * Shared steps used:
 * - `Given a new Bakery project with site configured but without IA` → GenerateArticleSteps
 * - `When I am executing the task '...'` → MinimalSteps
 * - `Then the build should succeed` → MinimalSteps
 * - `Then the build should fail` → GenerateArticleSteps
 * - `Then the output should contain {string}` → GenerateArticleSteps
 */
class ValidateFirebaseConfigSteps(private val world: BakeryWorld) {

    @Given("the firebaseAuth DSL is configured with apiKey {string} and authDomain {string} and projectId {string}")
    fun firebaseAuthDslConfiguredWith(apiKey: String, authDomain: String, projectId: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val buildFile = projectDir.resolve("build.gradle.kts")
        val content = buildFile.readText(UTF_8)

        // Insert firebaseAuth block before the last closing brace of bakery { }
        val lastBraceIndex = content.lastIndexOf('}')
        val updatedContent = if (lastBraceIndex >= 0) {
            content.substring(0, lastBraceIndex) +
                "\n    firebaseAuth {\n" +
                "        apiKey = \"$apiKey\"\n" +
                "        authDomain = \"$authDomain\"\n" +
                "        projectId = \"$projectId\"\n" +
                "    }\n}"
        } else {
            content
        }
        buildFile.writeText(updatedContent, UTF_8)
    }

    @Given("the contact form firestore has a phone field with type {string}")
    fun contactFormFirestoreHasPhoneFieldWithType(type: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val siteYml = projectDir.resolve("site.yml")

        val firebaseBlock = """
firebase:
  project:
    projectId: "test-project"
    apiKey: "AIzaSyB-test"
  firestore:
    contacts:
      name: "contacts"
      fields:
        - name: "name"
          type: "string"
        - name: "phone"
          type: "$type"
      rulesEnabled: true
    messages:
      name: "messages"
      fields:
        - name: "text"
          type: "string"
      rulesEnabled: true
  callable:
    name: "sendMessage"
    params:
      - name: "message"
        type: "string"
""".trimIndent()

        val currentContent = if (siteYml.exists()) siteYml.readText(UTF_8) else ""
        val newContent = currentContent + "\n" + firebaseBlock
        siteYml.writeText(newContent, UTF_8)
    }
}
