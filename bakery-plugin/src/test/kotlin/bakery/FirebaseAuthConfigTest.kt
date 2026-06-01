package bakery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FirebaseAuthConfigTest {

    private val mapper = ObjectMapper(YAMLFactory())

    @Test
    fun `parse site yml with firebaseAuth returns config with values`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            firebaseAuth:
              apiKey: "AIzaSyDemo123"
              authDomain: "my-app.firebaseapp.com"
              projectId: "my-app"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.firebaseAuth)
        assertEquals("AIzaSyDemo123", config.firebaseAuth!!.apiKey)
        assertEquals("my-app.firebaseapp.com", config.firebaseAuth!!.authDomain)
        assertEquals("my-app", config.firebaseAuth!!.projectId)
    }

    @Test
    fun `parse site yml without firebaseAuth returns null`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNull(config.firebaseAuth)
    }

    @Test
    fun `parse site yml with comments returns config with values`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            comments:
              enabled: true
              collection: "session-comments"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.comments)
        assertTrue(config.comments!!.enabled)
        assertEquals("session-comments", config.comments!!.collection)
    }

    @Test
    fun `parse site yml without comments returns null`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNull(config.comments)
    }

    @Test
    fun `parse site yml with firebaseAuth and comments together returns both`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            firebaseAuth:
              apiKey: "AIzaSyDemo456"
              authDomain: "other-app.firebaseapp.com"
              projectId: "other-app"
            comments:
              enabled: true
              collection: "comments"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.firebaseAuth)
        assertNotNull(config.comments)
        assertEquals("AIzaSyDemo456", config.firebaseAuth!!.apiKey)
        assertTrue(config.comments!!.enabled)
    }
}