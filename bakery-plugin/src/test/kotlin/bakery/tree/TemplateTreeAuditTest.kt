package bakery.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

/**
 * GBL-010 G-1 — the template branch of the translation tree.
 *
 * bakery owns the `templates/` branch: for every reference template × language it
 * must know whether the variant template is **missing**, **untranslated**
 * (byte-identical to the reference — the copy-FR trap) or **translated**. This is
 * the per-node status the unified tree needs; `VariantDeployability` only knows
 * the *missing* half.
 */
class TemplateTreeAuditTest {
    @Test
    fun `a variant template byte-identical to the reference is untranslated`() {
        val root = root()
        template(reference(root), "footer.thyme", "Bonjour")
        template(variant(root, "de"), "footer.thyme", "Bonjour")

        val tree = audit(root, "de")

        assertThat(tree.nodes.single().status).isEqualTo(TemplateTranslationStatus.UNTRANSLATED)
    }

    @Test
    fun `a variant template that differs from the reference is translated`() {
        val root = root()
        template(reference(root), "footer.thyme", "Bonjour")
        template(variant(root, "en"), "footer.thyme", "Hello")

        val tree = audit(root, "en")

        assertThat(tree.nodes.single().status).isEqualTo(TemplateTranslationStatus.TRANSLATED)
    }

    @Test
    fun `a reference template absent from the variant is missing`() {
        val root = root()
        template(reference(root), "archive.thyme", "Archives")

        val tree = audit(root, "ja")

        assertThat(tree.nodes.single().status).isEqualTo(TemplateTranslationStatus.MISSING)
    }

    @Test
    fun `a frozen message bundle co-located in templates is ignored`() {
        val root = root()
        template(reference(root), "footer.thyme", "Bonjour")
        template(reference(root), "messages_en.properties", "a=b")
        template(variant(root, "en"), "footer.thyme", "Hello")

        val tree = audit(root, "en")

        assertThat(tree.nodes.map { it.template }).containsExactly("footer.thyme")
    }

    @Test
    fun `the tree converges when every variant template is translated`() {
        val root = root()
        template(reference(root), "footer.thyme", "Bonjour")
        template(reference(root), "header.thyme", "Menu")
        template(variant(root, "en"), "footer.thyme", "Hello")
        template(variant(root, "en"), "header.thyme", "Menu EN")

        val tree = audit(root, "en")

        assertThat(tree.isConverged).isTrue
        assertThat(tree.unconverged).isEmpty()
    }

    @Test
    fun `a nested template is identified by its relative path`() {
        val root = root()
        template(reference(root), "blog/post.thyme", "Bonjour")
        template(variant(root, "en"), "blog/post.thyme", "Hello")

        val tree = audit(root, "en")

        assertThat(tree.nodes.single().template).isEqualTo("blog/post.thyme")
        assertThat(tree.nodes.single().status).isEqualTo(TemplateTranslationStatus.TRANSLATED)
    }

    @Test
    fun `the render is deterministic and names the statuses`() {
        val root = root()
        template(reference(root), "footer.thyme", "Bonjour")
        template(variant(root, "en"), "footer.thyme", "Bonjour")

        val rendered = audit(root, "en").render()

        assertThat(rendered).contains("footer.thyme").contains("UNTRANSLATED")
        assertThat(rendered).doesNotContain("2026-")
    }

    private fun audit(
        root: File,
        vararg languages: String,
    ): TemplateTree =
        TemplateTreeAudit.of(
            referenceTemplates = reference(root),
            variantTemplates = languages.associateWith { variant(root, it) },
        )

    private fun root(): File = Files.createTempDirectory("bakery-template-tree").toFile()

    private fun reference(root: File): File = root.resolve("templates")

    private fun variant(
        root: File,
        language: String,
    ): File = root.resolve("i18n/$language/templates")

    private fun template(
        dir: File,
        name: String,
        text: String,
    ) {
        dir.mkdirs()
        dir.resolve(name).writeText(text)
    }
}
