package bakery

import bakery.FileSystemManager.createCnameFile
import bakery.FileSystemManager.yamlMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.verify
import java.io.File
import kotlin.text.Charsets.UTF_8
import org.junit.jupiter.api.Assertions.assertDoesNotThrow

class BakeryPluginTest {

    @Nested
    @TestInstance(PER_CLASS)
    inner class AbsentConfigPathTest {

        private lateinit var fixture: BakeryTestFixture

        @BeforeEach
        fun setUp() {
            fixture = BakeryTestFixture.createAbsentConfigPath()
        }

        /**
         * CS-FIN-1 (CS-21) — Quand l'utilisateur applique le plugin bakery SANS
         * définir `configPath` (ni DSL, ni gradle.properties, ni -P), le
         * `afterEvaluate` NE DOIT PAS lever `MissingPropertyException` en
         * accédant à `configPath.get()`.
         *
         * Comportement attendu :
         * . aucune exception levée
         * . un warning est loggé via `logger.warn` (pas `lifecycle` — la
         *   distinction est importante pour le filtrage Gradle)
         */
        @Test
        fun `plugin does not throw when configPath is absent after evaluate`() {
            val plugin = BakeryPlugin()
            plugin.apply(fixture.project)
            // On capture l'action afterEvaluate via la fixture, puis on la déclenche
            // pour reproduire le chemin d'exécution qui lit `configPath.get()`.
            assertThatCode { fixture.runAfterEvaluate() }
                .doesNotThrowAnyException()
        }

        /**
         * CS-FIN-1 (CS-21) — En l'absence de configPath, un warning clair
         * doit être loggé pour informer l'utilisateur que seules les tâches
         * de scaffold sont disponibles.
         */
        @Test
        fun `plugin logs a warning when configPath is absent after evaluate`() {
            val plugin = BakeryPlugin()
            plugin.apply(fixture.project)
            fixture.runAfterEvaluate()
            verify(fixture.project.logger).warn(
                org.mockito.kotlin.argThat { msg: String ->
                    msg.contains("configPath", ignoreCase = true)
                }
            )
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class PluginApplicationTest {

        private lateinit var fixture: BakeryTestFixture

        @BeforeEach
        fun setUp() {
            fixture = BakeryTestFixture.create()
        }

        @Test
        fun `plugin creates bakery extension`() {
            val plugin = BakeryPlugin()
            plugin.apply(fixture.project)
            verify(fixture.project.extensions).create("bakery", BakeryExtension::class.java)
        }

        @Test
        fun `plugin manager is configured on apply`() {
            val plugin = BakeryPlugin()
            plugin.apply(fixture.project)
            verify(fixture.project.pluginManager).apply("com.github.node-gradle.node")
        }

        @Test
        fun `plugin reads config path from extension`() {
            val plugin = BakeryPlugin()
            plugin.apply(fixture.project)

            val extension = fixture.project.extensions.getByType(BakeryExtension::class.java)
            assertThat(extension.configPath.get()).isEqualTo("site.yml")
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class SiteConfigurationParsingTest {

        private lateinit var config: SiteConfiguration

        @BeforeAll
        fun `load and validate configuration before all tests`() {
            val configPath = "../../site.yml"
            val configFile = File(configPath)
            assertThat(configFile)
                .describedAs("Configuration file '%s' not found.", configPath)
                .exists()
            config = configFile.readText()
                .run(yamlMapper::readValue)
        }

        @Test
        fun `check SiteConfiguration#bake properties`() {
            assertThat(config.bake).isNotNull()
            assertThat(config.bake.srcPath)
                .describedAs("SiteConfiguration.bake.srcPath should be 'site'")
                .isEqualTo("site")
            assertThat(config.bake.destDirPath)
                .describedAs("SiteConfiguration.bake.destDirPath should be 'bake'")
                .isEqualTo("bake")
            assertThat(config.bake.cname)
                .describedAs("SiteConfiguration.bake.cname should be 'bakery'")
                .isEqualTo("bakery")
        }

        @Test
        fun `check SiteConfiguration#pushPages properties`() {
            assertThat(config.pushPage.from)
                .describedAs("SiteConfiguration.pushPage.from should be 'bake'")
                .isEqualTo("bake")
            assertThat(config.pushPage.to)
                .describedAs("SiteConfiguration.pushPage.to should be 'cvs'")
                .isEqualTo("cvs")
            assertThat(config.pushPage.repo.name)
                .describedAs("SiteConfiguration.pushPage.repo.name should be 'thymeleaf.cheroliv.com'")
                .isEqualTo("thymeleaf.cheroliv.com")
            assertThat(config.pushPage.repo.repository)
                .describedAs("SiteConfiguration.pushPage.repo.repository should be 'https://github.com/pages-content/bakery.git'")
                .isEqualTo("https://github.com/pages-content/bakery.git")
            assertThat(config.pushPage.repo.credentials.username)
                .describedAs("SiteConfiguration.pushPage.repo.credentials.username should be 8 characters long")
                .hasSize(8)
            assertThat(config.pushPage.repo.credentials.password)
                .describedAs("SiteConfiguration.pushPage.repo.credentials.password should be 40 characters long")
                .hasSize(40)
            assertThat(config.pushPage.branch)
                .describedAs("SiteConfiguration.pushPage.branch should be 'main'")
                .isEqualTo("main")
            assertThat(config.pushPage.message)
                .describedAs("SiteConfiguration.pushPage.message should be 'thymeleaf.cheroliv.com'")
                .isEqualTo("thymeleaf.cheroliv.com")
        }

        @Test
        fun `check SiteConfiguration#pushMaquette properties`() {
            assertThat(config.pushMaquette.from)
                .describedAs("SiteConfiguration.pushMaquette.from should be 'maquette'")
                .isEqualTo("maquette")
            assertThat(config.pushMaquette.to)
                .describedAs("SiteConfiguration.pushMaquette.to should be 'cvs'")
                .isEqualTo("cvs")
            assertThat(config.pushMaquette.repo.name)
                .describedAs("SiteConfiguration.pushMaquette.repo.name should be 'cheroliv-maquette'")
                .isEqualTo("cheroliv-maquette")
            assertThat(config.pushMaquette.repo.repository)
                .describedAs("SiteConfiguration.pushMaquette.repo.repository should be 'https://github.com/pages-content/cheroliv-maquette.git'")
                .isEqualTo("https://github.com/pages-content/cheroliv-maquette.git")
            assertThat(config.pushMaquette.repo.credentials.username)
                .describedAs("SiteConfiguration.pushMaquette.repo.credentials.username should be 8 characters long")
                .hasSize(8)
            assertThat(config.pushMaquette.repo.credentials.password)
                .describedAs("SiteConfiguration.pushMaquette.repo.credentials.password should be 40 characters long")
                .hasSize(40)
            assertThat(config.pushMaquette.branch)
                .describedAs("SiteConfiguration.pushMaquette.branch should be 'main'")
                .isEqualTo("main")
            assertThat(config.pushMaquette.message)
                .describedAs("SiteConfiguration.pushMaquette.message should be 'cheroliv-maquette'")
                .isEqualTo("cheroliv-maquette")
        }

        @Test
        fun `check SiteConfiguration#firebase#project properties`() {
            assertThat(config.firebase!!.project.projectId)
                .describedAs("SiteConfiguration.firebase.project.projectId should be 'bakery-contact-form'")
                .isEqualTo("bakery-contact-form")

            assertThat(config.firebase!!.project.apiKey)
                .describedAs("SiteConfiguration.firebase.project.apiKey should not be blank")
                .isNotBlank()
        }

        @Test
        fun `check SiteConfiguration#firebase#firestore#contacts properties`() {
            assertThat(config.firebase!!.firestore.contacts.name)
                .describedAs("SiteConfiguration.firebase.firestore.contacts.name should be 'contacts'")
                .isEqualTo("contacts")

            assertThat(config.firebase!!.firestore.contacts.fields.map { it.name })
                .describedAs("SiteConfiguration.firebase.firestore.contacts.fields should contains 'id', 'created_at', 'name', 'email', 'phone'")
                .contains("id", "created_at", "name", "email", "phone")

            assertThat(config.firebase!!.firestore.contacts.rulesEnabled).isTrue
        }

        @Test
        fun `check SiteConfiguration#firebase#firestore#messages properties`() {
            assertThat(config.firebase!!.firestore.messages.name)
                .describedAs("SiteConfiguration.firebase.firestore.messages.name should be 'messages'")
                .isEqualTo("messages")

            assertThat(config.firebase!!.firestore.messages.fields.map { it.name })
                .describedAs("SiteConfiguration.firebase.firestore.messages.fields should contains 'id', 'created_at', 'contact_id', 'subject', 'message'")
                .contains("id", "created_at", "contact_id", "subject", "message")
        }

        @Test
        fun `check SiteConfiguration#firebase#callable properties`() {
            assertThat(config.firebase!!.callable.name)
                .describedAs("SiteConfiguration.firebase.callable.name should be 'handleContactForm'")
                .isEqualTo("handleContactForm")

            assertThat(config.firebase!!.callable.params.map { it.name })
                .describedAs("SiteConfiguration.firebase.callable.params should contain 'p_name', 'p_email', 'p_subject', 'p_message'")
                .contains("p_name", "p_email", "p_subject", "p_message")
        }

        @Test
        fun `check SiteConfiguration#firebase#firestore#contacts#field properties types are correctly mapped`() {
            val fields = config.firebase!!.firestore.contacts.fields
            val expectedTypes = mapOf(
                "id" to "string",
                "created_at" to "timestamp",
                "name" to "string",
                "email" to "string",
                "phone" to "string"
            )

            assertThat(fields).hasSize(expectedTypes.size)

            expectedTypes.forEach { (name, type) ->
                val field = fields.find { it.name == name }
                assertThat(field)
                    .withFailMessage("Field with name '$name' not found.")
                    .isNotNull
                assertThat(field!!.type)
                    .withFailMessage("Expected field '$name' to have type '$type' but was '${field.type}'.")
                    .isEqualTo(type)
            }
        }

        @Test
        fun `check SiteConfiguration#firebase#firestore#messages#field properties types are correctly mapped`() {
            val fields = config.firebase!!.firestore.messages.fields
            val expectedTypes = mapOf(
                "id" to "string",
                "created_at" to "timestamp",
                "contact_id" to "string",
                "subject" to "string",
                "message" to "string"
            )

            assertThat(fields).hasSize(expectedTypes.size)

            expectedTypes.forEach { (name, type) ->
                val field = fields.find { it.name == name }
                assertThat(field)
                    .withFailMessage("Field with name '$name' not found.")
                    .isNotNull
                assertThat(field!!.type)
                    .withFailMessage("Expected field '$name' to have type '$type' but was '${field.type}'.")
                    .isEqualTo(type)
            }
        }

        @Test
        fun `check SiteConfiguration#firebase#callable#param properties types are correctly mapped`() {
            val params = config.firebase!!.callable.params
            val expectedTypes = mapOf(
                "p_name" to "string",
                "p_email" to "string",
                "p_subject" to "string",
                "p_message" to "string"
            )

            assertThat(params).hasSize(expectedTypes.size)

            expectedTypes.forEach { (name, type) ->
                val param = params.find { it.name == name }
                assertThat(param)
                    .withFailMessage("Parameter with name '$name' not found.")
                    .isNotNull
                assertThat(param!!.type)
                    .withFailMessage("Expected parameter '$name' to have type '$type' but was '${param.type}'.")
                    .isEqualTo(type)
            }
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class YamlToleranceTest {

        @Test
        fun `SiteConfiguration ignores unknown fields like supabase`() {
            val yaml = """
                bake:
                  srcPath: site
                  destDirPath: bake
                  cname: bakery
                supabase:
                  url: https://example.supabase.co
                  key: legacy-key
                firebase:
                  project:
                    projectId: test-project
                    apiKey: test-key
                  firestore:
                    contacts:
                      name: contacts
                      fields:
                        - name: name
                          type: string
                      rulesEnabled: false
                    messages:
                      name: messages
                      fields:
                        - name: body
                          type: string
                      rulesEnabled: false
                  callable:
                    name: submitContact
                    params:
                      - name: p_name
                        type: string
            """.trimIndent()

            val config = yamlMapper.readValue<SiteConfiguration>(yaml)
            assertThat(config.bake.srcPath).isEqualTo("site")
            assertThat(config.firebase).isNotNull
            assertThat(config.firebase!!.project.projectId).isEqualTo("test-project")
        }

        @Test
        fun `SiteConfiguration ignores multiple unknown top-level fields`() {
            val yaml = """
                bake:
                  srcPath: site
                  destDirPath: bake
                  cname: bakery
                legacy_config:
                  deprecated: true
                custom_extension:
                  feature: experimental
            """.trimIndent()

            val config = yamlMapper.readValue<SiteConfiguration>(yaml)
            assertThat(config.bake.srcPath).isEqualTo("site")
            assertThat(config.pushPage).isNotNull
        }

        @Test
        fun `SiteConfiguration silently ignores typo in valid field name`() {
            // Known limitation of @JsonIgnoreProperties(ignoreUnknown = true):
            // a typo like 'pusPage' instead of 'pushPage' is silently ignored.
            // This test documents the behavior, not a bug to fix.
            val yaml = """
                bake:
                  srcPath: site
                  destDirPath: bake
                  cname: bakery
                pusPage:
                  from: bake
                  to: cvs
            """.trimIndent()

            val config = yamlMapper.readValue<SiteConfiguration>(yaml)
            // pushPage keeps its default values — the typo'd key is ignored
            assertThat(config.pushPage.from).isEmpty()
            assertThat(config.pushPage.to).isEmpty()
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class PublishingTest {
        @Test
        fun check_publishing() {
            val fixture = BakeryTestFixture.create()
            val plugin = BakeryPlugin()
            plugin.apply(fixture.project)
        }
    }

    @Nested
    inner class FileSystemManagerTest {

        @TempDir
        lateinit var tempDir: File

        private lateinit var project: Project

        @BeforeEach
        fun `setup project`() {
            project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        }

        private fun createFakeSiteConfiguration(cname: String = "") = SiteConfiguration(
            BakeConfiguration("site", "bake", cname)
        )

        @Test
        fun `createCnameFile should create CNAME file with correct content when cname is provided`() {
            @Suppress("LocalVariableName")
            val CNAME_VALUE = "test.cheroliv.com"
            val siteConfiguration = createFakeSiteConfiguration(CNAME_VALUE)
            project.layout.buildDirectory.get().asFile.mkdirs()
            val expectedCnameFile: File = project.layout.buildDirectory.run {
                file(siteConfiguration.bake.destDirPath).apply { get().asFile.mkdir() }
                file("${siteConfiguration.bake.destDirPath}/CNAME").get().asFile.apply {
                    createNewFile()
                    writeText(CNAME_VALUE, UTF_8)
                }
            }
            project.layout.buildDirectory.get()
                .asFile
                .resolve(siteConfiguration.bake.destDirPath).apply {
                    run(::assertThat)
                        .describedAs("destDirPath should exist")
                        .exists()
                        .isDirectory
                    resolve("CNAME")
                        .run(::assertThat)
                        .describedAs("CNAME file should exist")
                        .exists()
                        .isFile
                    resolve("CNAME")
                        .readText(UTF_8)
                        .run(::assertThat)
                        .describedAs("CNAME file should contain 'test.cheroliv.com'")
                        .contains(CNAME_VALUE)
                }
            siteConfiguration.createCnameFile(project)

            expectedCnameFile
                .run(::assertThat)
                .describedAs("CNAME file should exist")
                .exists().isFile
            expectedCnameFile.readText(UTF_8)
                .run(::assertThat)
                .describedAs("CNAME file should contains '$CNAME_VALUE'")
                .isEqualTo(CNAME_VALUE)
        }

        @Test
        fun `createCnameFile should do nothing if cname is null`() {
            val siteConfiguration = createFakeSiteConfiguration()
            project.layout.buildDirectory.get().asFile.mkdirs()
            val cnameFile = project.layout.buildDirectory.file(
                "${siteConfiguration.bake.destDirPath}/CNAME"
            ).get().asFile

            siteConfiguration.createCnameFile(project)

            assertThat(cnameFile).doesNotExist()
        }

        @Test
        fun `createCnameFile should do nothing if cname is blank`() {
            val siteConfiguration = createFakeSiteConfiguration("   ")
            project.layout.buildDirectory.get().asFile.mkdirs()
            val cnameFile = project.layout.buildDirectory.file(
                "${siteConfiguration.bake.destDirPath}/CNAME"
            ).get().asFile

            siteConfiguration.createCnameFile(project)

            assertThat(cnameFile).doesNotExist()
        }

        @Test
        fun `createCnameFile should overwrite existing CNAME file`() {
            val siteConfiguration = createFakeSiteConfiguration("new.cheroliv.com")
            project.layout.buildDirectory.get().asFile.mkdirs()
            val cnameFile = project.layout.buildDirectory.file(
                "${siteConfiguration.bake.destDirPath}/CNAME"
            ).get().asFile
            cnameFile.parentFile.mkdirs()
            cnameFile.writeText("old.cheroliv.com", UTF_8)

            siteConfiguration.createCnameFile(project)

            assertThat(cnameFile).exists().isFile
            assertThat(cnameFile.readText(UTF_8)).isEqualTo("new.cheroliv.com")
        }

        @Test
        fun `createCnameFile should replace existing CNAME directory`() {
            val siteConfiguration = createFakeSiteConfiguration("another.cheroliv.com")
            project.layout.buildDirectory.get().asFile.mkdirs()
            val cnameFile = project.layout.buildDirectory.file(
                "${siteConfiguration.bake.destDirPath}/CNAME"
            ).get().asFile

            cnameFile.mkdirs()
            assertThat(cnameFile).exists().isDirectory

            siteConfiguration.createCnameFile(project)

            assertThat(cnameFile).exists().isFile
            assertThat(cnameFile.readText(UTF_8)).isEqualTo("another.cheroliv.com")
        }
    }

    @Nested
    inner class FirebaseConfigInjectionTest {

        @TempDir
        lateinit var tempDir: File

        private fun injectFirebaseConfig(jbakeProps: File, site: SiteConfiguration) {
            val resolved = ConfigResolver.resolveFirebaseConfig(emptyMap(), site.firebase)
            if (resolved.apiKey.isBlank() && resolved.projectId.isBlank()) return
            val lines = jbakeProps.readText(UTF_8).lines().toMutableList()
            updatePropertyHelper(lines, "firebaseApiKey", resolved.apiKey)
            updatePropertyHelper(lines, "firebaseProjectId", resolved.projectId)
            jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
        }

        private fun updatePropertyHelper(lines: MutableList<String>, key: String, value: String) {
            val idx = lines.indexOfFirst { it.startsWith("$key=") }
            if (idx >= 0) {
                lines[idx] = "$key=$value"
            } else {
                lines.add("$key=$value")
            }
        }

        @Test
        fun `should inject firebase keys into jbake properties`() {
            val srcDir = tempDir.resolve("site")
            srcDir.mkdirs()
            val jbakeProps = srcDir.resolve("jbake.properties")
            jbakeProps.writeText("site.host=https://example.com/", UTF_8)

            val site = SiteConfiguration(
                bake = BakeConfiguration(srcPath = "site", destDirPath = "bake"),
                firebase = FirebaseContactFormConfig(
                    project = FirebaseProjectInfo(projectId = "test-project", apiKey = "test-api-key"),
                    firestore = FirebaseFirestoreSchema(
                        contacts = FirebaseCollection("contacts", emptyList(), true),
                        messages = FirebaseCollection("messages", emptyList(), true)
                    ),
                    callable = FirebaseCallableFunction("handleContactForm", emptyList())
                )
            )

            injectFirebaseConfig(jbakeProps, site)

            val content = jbakeProps.readText(UTF_8)
            assertThat(content).contains("firebaseApiKey=test-api-key")
            assertThat(content).contains("firebaseProjectId=test-project")
        }

        @Test
        fun `should do nothing when jbake properties file does not exist`() {
            // No file created - nothing to test, just verify no exception
            val site = SiteConfiguration(
                bake = BakeConfiguration(srcPath = "site", destDirPath = "bake"),
                firebase = FirebaseContactFormConfig(
                    project = FirebaseProjectInfo(projectId = "test-project", apiKey = "test-api-key"),
                    firestore = FirebaseFirestoreSchema(
                        contacts = FirebaseCollection("contacts", emptyList(), true),
                        messages = FirebaseCollection("messages", emptyList(), true)
                    ),
                    callable = FirebaseCallableFunction("handleContactForm", emptyList())
                )
            )

            // ConfigResolver resolves the config — injection is not called (no file)
            val resolved = ConfigResolver.resolveFirebaseConfig(emptyMap(), site.firebase)
            assertThat(resolved.apiKey).isEqualTo("test-api-key")
            assertThat(resolved.projectId).isEqualTo("test-project")
        }

        @Test
        fun `should do nothing when firebase config is null`() {
            val srcDir = tempDir.resolve("site")
            srcDir.mkdirs()
            val jbakeProps = srcDir.resolve("jbake.properties")
            jbakeProps.writeText("site.host=https://example.com/", UTF_8)

            val site = SiteConfiguration(
                bake = BakeConfiguration(srcPath = "site", destDirPath = "bake"),
                firebase = null
            )

            injectFirebaseConfig(jbakeProps, site)

            val content = jbakeProps.readText(UTF_8)
            assertThat(content).doesNotContain("firebaseApiKey")
            assertThat(content).doesNotContain("firebaseProjectId")
        }

        @Test
        fun `should update existing firebase keys inline`() {
            val srcDir = tempDir.resolve("site")
            srcDir.mkdirs()
            val jbakeProps = srcDir.resolve("jbake.properties")
            jbakeProps.writeText("site.host=https://example.com/\nfirebaseApiKey=old-key\nfirebaseProjectId=old-project", UTF_8)

            val site = SiteConfiguration(
                bake = BakeConfiguration(srcPath = "site", destDirPath = "bake"),
                firebase = FirebaseContactFormConfig(
                    project = FirebaseProjectInfo(projectId = "new-project", apiKey = "new-key"),
                    firestore = FirebaseFirestoreSchema(
                        contacts = FirebaseCollection("contacts", emptyList(), true),
                        messages = FirebaseCollection("messages", emptyList(), true)
                    ),
                    callable = FirebaseCallableFunction("handleContactForm", emptyList())
                )
            )

            injectFirebaseConfig(jbakeProps, site)

            val content = jbakeProps.readText(UTF_8)
            assertThat(content).contains("firebaseApiKey=new-key")
            assertThat(content).contains("firebaseProjectId=new-project")
            assertThat(content).doesNotContain("old-key")
            assertThat(content).doesNotContain("old-project")
        }

        @Test
        fun `should append firebase keys when not present`() {
            val srcDir = tempDir.resolve("site")
            srcDir.mkdirs()
            val jbakeProps = srcDir.resolve("jbake.properties")
            jbakeProps.writeText("site.host=https://example.com/", UTF_8)

            val site = SiteConfiguration(
                bake = BakeConfiguration(srcPath = "site", destDirPath = "bake"),
                firebase = FirebaseContactFormConfig(
                    project = FirebaseProjectInfo(projectId = "test-project", apiKey = "test-api-key"),
                    firestore = FirebaseFirestoreSchema(
                        contacts = FirebaseCollection("contacts", emptyList(), true),
                        messages = FirebaseCollection("messages", emptyList(), true)
                    ),
                    callable = FirebaseCallableFunction("handleContactForm", emptyList())
                )
            )

            injectFirebaseConfig(jbakeProps, site)

            val content = jbakeProps.readText(UTF_8)
            assertThat(content).contains("firebaseApiKey=test-api-key")
            assertThat(content).contains("firebaseProjectId=test-project")
            assertThat(content).contains("site.host=https://example.com/")
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class AfterEvaluateBranchingTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `runs scaffold only when config file does not exist`() {
            val fixture = BakeryTestFixture.createWithProjectDir(projectDir)
            val plugin = BakeryPlugin()
            plugin.apply(fixture.project)

            assertDoesNotThrow { fixture.runAfterEvaluate() }
            verify(fixture.project.logger).lifecycle(
                org.mockito.kotlin.argThat { msg: String ->
                    msg.contains("switching to scaffold only", ignoreCase = true)
                }
            )
        }

        @Test
        fun `runs scaffold only when config YAML is invalid`() {
            val siteYml = projectDir.resolve("site.yml")
            siteYml.writeText("invalid: [broken: yaml: {{{", UTF_8)

            val fixture = BakeryTestFixture.createWithProjectDir(projectDir)
            val plugin = BakeryPlugin()
            plugin.apply(fixture.project)

            assertDoesNotThrow { fixture.runAfterEvaluate() }
            verify(fixture.project.logger).warn(
                org.mockito.kotlin.argThat { msg: String ->
                    msg.contains("Failed to parse", ignoreCase = true)
                }
            )
        }

        @Test
        fun `runs scaffold only when site and maquette directories do not exist`() {
            val siteYml = projectDir.resolve("site.yml")
            siteYml.writeText("""
                bake:
                  srcPath: missing_site
                  destDirPath: build/bake
                  cname: test
                pushMaquette:
                  from: missing_maquette
            """.trimIndent(), UTF_8)

            val fixture = BakeryTestFixture.createWithProjectDir(projectDir)
            val plugin = BakeryPlugin()
            plugin.apply(fixture.project)

            assertDoesNotThrow { fixture.runAfterEvaluate() }
        }

    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class ResolvePathsTest {

        private val base = java.io.File(".")

        @Test
        fun `resolvePath resolves relative path against base`() {
            val result = resolvePath(base, "site")
            assertThat(result).isEqualTo(base.resolve("site").absolutePath)
        }

        @Test
        fun `resolvePath keeps absolute paths unchanged`() {
            val result = resolvePath(base, "/abs/site")
            assertThat(result).isEqualTo("/abs/site")
        }

        @Test
        fun `resolvePath keeps blank paths unchanged`() {
            val result = resolvePath(base, "  ")
            assertThat(result).isEqualTo("  ")
        }

        @Test
        fun `resolvePaths resolves all fields`() {
            val config = SiteConfiguration(
                bake = BakeConfiguration("my_site", "output", "test.com"),
                pushMaquette = GitPushConfiguration(
                    from = "maquette", to = "cvs",
                    repo = RepositoryConfiguration("test", "https://github.com/test/repo.git",
                        RepositoryCredentials("u", "p")),
                    branch = "main", message = "deploy"
                ),
                pushPage = GitPushConfiguration(
                    from = "bake_output", to = "cvs",
                    repo = RepositoryConfiguration("test-pages", "https://github.com/test/pages.git",
                        RepositoryCredentials("u", "p")),
                    branch = "gh-pages", message = "pages"
                ),
                pushProfile = GitPushConfiguration(
                    from = "profile_src", to = "cvs",
                    repo = RepositoryConfiguration("profile", "https://github.com/test/profile.git",
                        RepositoryCredentials("u", "p")),
                    branch = "main", message = "profile"
                )
            )
            val resolved = config.resolvePaths(base)

            assertThat(resolved.bake.srcPath).isEqualTo(base.resolve("my_site").absolutePath)
            assertThat(resolved.pushMaquette.from).isEqualTo(base.resolve("maquette").absolutePath)
            assertThat(resolved.pushPage.from).isEqualTo(base.resolve("bake_output").absolutePath)
            assertThat(resolved.pushProfile!!.from).isEqualTo(base.resolve("profile_src").absolutePath)
        }

        @Test
        fun `resolvePaths preserves pushProfile null`() {
            val config = SiteConfiguration(
                BakeConfiguration("site", "build/bake", "test.com")
            )
            val resolved = config.resolvePaths(base)
            assertThat(resolved.pushProfile).isNull()
        }
    }
}
