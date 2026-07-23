package bakery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OllamaConfigYamlTest {

    private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    @Test
    fun `parse site yml with ollama section returns config with 29 device keys`() {
        val keysYaml = (11437..11465).joinToString("\n") { port ->
            "        - keyName: ollama-$port\n" +
            "          privateKey: ssh-ed25519-fake-key-$port"
        }
        val yaml = """
bake:
  srcPath: src
  destDirPath: build
ollama:
  model: gemma4:31b-cloud
  portStart: 11437
  portEnd: 11465
  timeoutSeconds: 300
  deviceKeys:
$keysYaml
""".trimStart('\n')
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.ollama)
        assertEquals("gemma4:31b-cloud", config.ollama!!.model)
        assertEquals(11437, config.ollama!!.portStart)
        assertEquals(11465, config.ollama!!.portEnd)
        assertEquals(300L, config.ollama!!.timeoutSeconds)
        assertEquals(29, config.ollama!!.deviceKeys.size)
        assertEquals("ollama-11437", config.ollama!!.deviceKeys[0].keyName)
        assertTrue(config.ollama!!.deviceKeys[0].privateKey.contains("fake-key-11437"))
        assertEquals("ollama-11465", config.ollama!!.deviceKeys[28].keyName)
    }

    @Test
    fun `parse site yml without ollama section returns null`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNull(config.ollama)
    }

    @Test
    fun `parse site yml with minimal ollama section uses defaults`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            ollama:
              deviceKeys:
                - keyName: ollama-11437
                  privateKey: ssh-ed25519-demo-key
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.ollama)
        assertEquals("gemma4:31b-cloud", config.ollama!!.model)
        assertEquals(11437, config.ollama!!.portStart)
        assertEquals(11465, config.ollama!!.portEnd)
        assertEquals(300L, config.ollama!!.timeoutSeconds)
        assertEquals(1, config.ollama!!.deviceKeys.size)
        assertTrue(config.ollama!!.isConfigured)
    }

    @Test
    fun `ollama section with no device keys is not configured`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            ollama:
              model: gemma4:31b-cloud
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.ollama)
        assertFalse(config.ollama!!.isConfigured)
        assertTrue(config.ollama!!.deviceKeys.isEmpty())
    }

    @Test
    fun `port range 11437 to 11465 generates 29 base urls`() {
        val config = contracts.i18n.OllamaConfig(portStart = 11437, portEnd = 11465)
        val urls = config.baseUrls()
        assertEquals(29, urls.size)
        assertEquals("http://localhost:11437", urls.first())
        assertEquals("http://localhost:11465", urls.last())
    }

    @Test
    fun `portRange derived property is consistent with portStart and portEnd`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            ollama:
              deviceKeys:
                - keyName: my-key
                  privateKey: ssh-ed25519-demo
              portStart: 11440
              portEnd: 11450
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.ollama)
        assertEquals(11440, config.ollama!!.portStart)
        assertEquals(11450, config.ollama!!.portEnd)
        assertEquals(11440..11450, config.ollama!!.portRange)
        assertEquals(11, config.ollama!!.baseUrls().size)
        assertEquals("http://localhost:11440", config.ollama!!.baseUrls().first())
        assertEquals("http://localhost:11450", config.ollama!!.baseUrls().last())
    }
}
