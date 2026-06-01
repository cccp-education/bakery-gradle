package bakery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

class FirebaseAuthInjectionTest {

    @TempDir
    lateinit var tempDir: File

    private val mapper = com.fasterxml.jackson.dataformat.yaml.YAMLFactory()
        .let { com.fasterxml.jackson.databind.ObjectMapper(it) }

    @Test
    fun `site yml with firebaseAuth and comments injects properties into jbake properties`() {
        val siteYml = tempDir.resolve("site.yml")
        siteYml.writeText("""
            bake:
              srcPath: site
              destDirPath: bake
            firebaseAuth:
              apiKey: "AIzaSyDemo123"
              authDomain: "my-app.firebaseapp.com"
              projectId: "my-app"
            comments:
              enabled: true
              collection: "session-comments"
        """.trimIndent(), UTF_8)

        val siteDir = tempDir.resolve("site")
        siteDir.mkdirs()
        val jbakeProps = siteDir.resolve("jbake.properties")
        jbakeProps.writeText("template.index.file=index.thyme\nrender.feed=false\n", UTF_8)

        val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
        assertNotNull(config.firebaseAuth)
        assertNotNull(config.comments)

        config.firebaseAuth?.let { injectFirebaseAuthIntoJbakeProperties(jbakeProps, it) }
        config.comments?.let { injectCommentsIntoJbakeProperties(jbakeProps, it) }

        val props = jbakeProps.readText(UTF_8)
        assertTrue(props.contains("firebaseAuthApiKey=AIzaSyDemo123"), "must contain firebaseAuthApiKey")
        assertTrue(props.contains("firebaseAuthDomain=my-app.firebaseapp.com"), "must contain firebaseAuthDomain")
        assertTrue(props.contains("firebaseAuthProjectId=my-app"), "must contain firebaseAuthProjectId")
        assertTrue(props.contains("commentsEnabled=true"), "must contain commentsEnabled")
        assertTrue(props.contains("commentsCollection=session-comments"), "must contain commentsCollection")
    }

    @Test
    fun `site yml without firebaseAuth and comments does not inject any auth or comments properties`() {
        val siteYml = tempDir.resolve("site.yml")
        siteYml.writeText("""
            bake:
              srcPath: site
              destDirPath: bake
        """.trimIndent(), UTF_8)

        val siteDir = tempDir.resolve("site")
        siteDir.mkdirs()
        val jbakeProps = siteDir.resolve("jbake.properties")
        jbakeProps.writeText("template.index.file=index.thyme\nrender.feed=false\n", UTF_8)

        val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
        assertNull(config.firebaseAuth)
        assertNull(config.comments)

        config.firebaseAuth?.let { injectFirebaseAuthIntoJbakeProperties(jbakeProps, it) }
        config.comments?.let { injectCommentsIntoJbakeProperties(jbakeProps, it) }

        val props = jbakeProps.readText(UTF_8)
        assertFalse(props.contains("firebaseAuth"), "must NOT contain any firebaseAuth properties")
        assertFalse(props.contains("commentsEnabled"), "must NOT contain commentsEnabled")
        assertFalse(props.contains("commentsCollection"), "must NOT contain commentsCollection")
    }

    @Test
    fun `site yml with firebaseAuth but comments disabled only injects auth properties`() {
        val siteYml = tempDir.resolve("site.yml")
        siteYml.writeText("""
            bake:
              srcPath: site
              destDirPath: bake
            firebaseAuth:
              apiKey: "AIzaSyDemo456"
              authDomain: "other-app.firebaseapp.com"
              projectId: "other-app"
            comments:
              enabled: false
        """.trimIndent(), UTF_8)

        val siteDir = tempDir.resolve("site")
        siteDir.mkdirs()
        val jbakeProps = siteDir.resolve("jbake.properties")
        jbakeProps.writeText("template.index.file=index.thyme\n", UTF_8)

        val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
        assertNotNull(config.firebaseAuth)
        assertNotNull(config.comments)

        config.firebaseAuth?.let { injectFirebaseAuthIntoJbakeProperties(jbakeProps, it) }
        config.comments?.let { injectCommentsIntoJbakeProperties(jbakeProps, it) }

        val props = jbakeProps.readText(UTF_8)
        assertTrue(props.contains("firebaseAuthApiKey=AIzaSyDemo456"), "must contain firebaseAuthApiKey")
        assertTrue(props.contains("commentsEnabled=false"), "must contain commentsEnabled=false")
    }

    private fun injectFirebaseAuthIntoJbakeProperties(jbakeProps: File, firebaseAuth: FirebaseAuthConfig) {
        val lines = jbakeProps.readText(UTF_8).lines().toMutableList()
        fun updateProperty(key: String, value: String) {
            val idx = lines.indexOfFirst { it.startsWith("$key=") }
            if (idx >= 0) {
                lines[idx] = "$key=$value"
            } else {
                lines.add("$key=$value")
            }
        }
        updateProperty("firebaseAuthApiKey", firebaseAuth.apiKey)
        updateProperty("firebaseAuthDomain", firebaseAuth.authDomain)
        updateProperty("firebaseAuthProjectId", firebaseAuth.projectId)
        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
    }

    private fun injectCommentsIntoJbakeProperties(jbakeProps: File, comments: CommentsConfig) {
        val lines = jbakeProps.readText(UTF_8).lines().toMutableList()
        fun updateProperty(key: String, value: String) {
            val idx = lines.indexOfFirst { it.startsWith("$key=") }
            if (idx >= 0) {
                lines[idx] = "$key=$value"
            } else {
                lines.add("$key=$value")
            }
        }
        updateProperty("commentsEnabled", comments.enabled.toString())
        updateProperty("commentsCollection", comments.collection)
        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
    }
}