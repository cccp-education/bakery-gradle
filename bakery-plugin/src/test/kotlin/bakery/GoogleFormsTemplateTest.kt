package bakery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class GoogleFormsTemplateTest {

    private val templatesDir = File("src/main/resources/site/templates")

    @Test
    fun `google-forms thyme template contains iframe with formId placeholder`() {
        val template = templatesDir.resolve("google-forms.thyme")
        assertTrue(template.exists(), "google-forms.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("google-forms-container"), "must have container div")
        assertTrue(content.contains("iframe"), "must contain an iframe element")
        assertTrue(content.contains("googleFormsFormId"), "iframe src must use googleFormsFormId variable")
        assertTrue(content.contains("docs.google.com/forms"), "iframe src must point to Google Forms")
    }

    @Test
    fun `google-forms thyme template renders nothing when formId is absent`() {
        val template = templatesDir.resolve("google-forms.thyme")
        assertTrue(template.exists(), "google-forms.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("th:if"), "must have th:if condition to hide when formId is absent")
        assertTrue(
            content.contains("googleFormsFormId") && content.contains("th:if"),
            "th:if must guard on googleFormsFormId"
        )
    }
}