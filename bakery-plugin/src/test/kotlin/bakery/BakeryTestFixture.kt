@file:Suppress("UNCHECKED_CAST")

package bakery

import bakery.llm.IaConfig
import java.io.File
import java.util.Optional
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskContainer
import org.jbake.gradle.JBakeExtension
import org.jbake.gradle.JBakeTask
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class BakeryTestFixture(
    val project: Project,
    val pluginContainer: PluginContainer,
    private val afterEvaluateActionRef: java.util.concurrent.atomic.AtomicReference<Action<Project>?>
) {

    companion object {
        fun create(): BakeryTestFixture = createInternal(configPathPresent = true)

        /** Fixture où `bakeryExtension.configPath` n'est PAS défini (Property absente). */
        fun createAbsentConfigPath(): BakeryTestFixture = createInternal(configPathPresent = false)

        private fun createInternal(configPathPresent: Boolean): BakeryTestFixture {
            val cpProp = mockProperty("site.yml", present = configPathPresent)
            val bakeryExt = mockBakeryExtension(cpProp)
            val extContainer = mockExtensionContainer(bakeryExt)
            val confContainer = mockConfigurationContainer()
            val depHandler = mockDependencyHandler()
            val taskContainer = mockTaskContainer()
            val pluginContainer = mockPluginContainer()
            val projectLayout = mockProjectLayout()
            val logger = mockLogger()

            // AtomicReference : Mockito peut invoquer le stub sur un thread différent
            // ou après le retour de la méthode appelante. Un AtomicReference évite
            // les problèmes de publication mémoire.
            val capturedActionRef = java.util.concurrent.atomic.AtomicReference<Action<Project>?>(null)
            val project = mockProject(
                extContainer = extContainer,
                confContainer = confContainer,
                depHandler = depHandler,
                taskContainer = taskContainer,
                pluginContainer = pluginContainer,
                projectLayout = projectLayout,
                logger = logger,
                onAfterEvaluate = { action -> capturedActionRef.set(action) }
            )

            return BakeryTestFixture(project, pluginContainer, capturedActionRef)
        }
    }

    /**
     * Déclenche manuellement le bloc `project.afterEvaluate { ... }` capturé par le mock.
     * Utile pour tester la logique qui s'exécute après évaluation du DSL utilisateur.
     */
    fun runAfterEvaluate() {
        val action = afterEvaluateActionRef.get()
            ?: error("afterEvaluate action was not captured by the mock — was the plugin applied?")
        action.execute(project)
    }
}

private fun mockProperty(value: String, present: Boolean): Property<String> {
    val prop = mock<Property<String>>()
    whenever(prop.get()).thenReturn(value)
    whenever(prop.isPresent).thenReturn(present)
    return prop
}

private fun mockBakeryExtension(configPath: Property<String>): BakeryExtension {
    val ext = mock<BakeryExtension>()
    val sitesBaseDirProp = mockProperty("", present = false)
    val siteNameProp = mockProperty("", present = false)
    val siteTypeProp = mockProperty("blog", present = true)
    whenever(ext.configPath).thenReturn(configPath)
    whenever(ext.sitesBaseDir).thenReturn(sitesBaseDirProp)
    whenever(ext.siteName).thenReturn(siteNameProp)
    whenever(ext.siteType).thenReturn(siteTypeProp)
    whenever(ext.ia).thenReturn(IaConfig())
    return ext
}

private fun mockExtensionContainer(bakeryExt: BakeryExtension): ExtensionContainer {
    val container = mock<ExtensionContainer>()
    whenever(container.create("bakery", BakeryExtension::class.java)).thenReturn(bakeryExt)
    whenever(container.getByType(BakeryExtension::class.java)).thenReturn(bakeryExt)

    val catalogExt = mock<VersionCatalogsExtension>()
    val catalog = mock<VersionCatalog>()
    val versionConstraint = mock<VersionConstraint>()

    fun mockLibrary(name: String) {
        val prov = mock<Provider<MinimalExternalModuleDependency>>()
        whenever(prov.get()).thenReturn(mock<MinimalExternalModuleDependency>())
        whenever(catalog.findLibrary(name)).thenReturn(Optional.of(prov))
    }

    mockLibrary("jbake")
    mockLibrary("slf4j-simple")
    mockLibrary("asciidoctorj-diagram")
    mockLibrary("asciidoctorj-diagram-plantuml")
    mockLibrary("commons-io")
    mockLibrary("commons-configuration")
    whenever(catalog.findVersion("jbake")).thenReturn(Optional.of(versionConstraint))
    whenever(catalogExt.named("libs")).thenReturn(catalog)
    whenever(container.getByType(VersionCatalogsExtension::class.java)).thenReturn(catalogExt)

    doAnswer { inv ->
        val action = inv.arguments[1] as Action<JBakeExtension>
        action.execute(mock())
        null
    }.whenever(container).configure(eq(JBakeExtension::class.java), any())

    return container
}

private fun mockConfigurationContainer(): ConfigurationContainer {
    val container = mock<ConfigurationContainer>()
    val conf = mock<Configuration>()
    whenever(conf.name).thenReturn("jbakeRuntime")
    whenever(conf.asPath).thenReturn("/mock/classpath")
    whenever(container.create(eq("jbakeRuntime"))).thenReturn(conf)
    doAnswer { inv ->
        val action = inv.arguments[1] as Action<Configuration>
        action.execute(conf)
        conf
    }.whenever(container).create(eq("jbakeRuntime"), any<Action<Configuration>>())
    return container
}

private fun mockDependencyHandler(): DependencyHandler {
    val handler = mock<DependencyHandler>()
    whenever(handler.add(any(), any())).thenReturn(mock())
    return handler
}

private fun mockTaskContainer(): TaskContainer {
    val taskContainer = mock<TaskContainer>()
    val jbakeTaskCollection = mock<TaskCollection<JBakeTask>>()
    val jbakeTask = mock<JBakeTask>()
    whenever(jbakeTaskCollection.getByName("bake")).thenReturn(jbakeTask)
    whenever(taskContainer.withType(JBakeTask::class.java)).thenReturn(jbakeTaskCollection)

    whenever(taskContainer.register(any<String>(), any<Action<Task>>())).thenReturn(mock())
    whenever(taskContainer.register(eq("deploySite"), any<Action<Task>>())).thenReturn(mock())
    whenever(taskContainer.register(eq("deployMaquette"), any<Action<Task>>())).thenReturn(mock())
    whenever(taskContainer.register(eq("collectSiteConfig"), any<Action<Task>>())).thenReturn(mock())

    return taskContainer
}

private fun mockPluginContainer(): PluginContainer {
    return mock()
}

private fun mockProject(
    extContainer: ExtensionContainer,
    confContainer: ConfigurationContainer,
    depHandler: DependencyHandler,
    taskContainer: TaskContainer,
    pluginContainer: PluginContainer,
    projectLayout: ProjectLayout,
    logger: Logger,
    onAfterEvaluate: (Action<Project>) -> Unit = { /* default: run inline */ }
): Project {
    val project = mock<Project>()
    val pluginManager = mock<org.gradle.api.plugins.PluginManager>()
    whenever(project.extensions).thenReturn(extContainer)
    whenever(project.configurations).thenReturn(confContainer)
    whenever(project.dependencies).thenReturn(depHandler)
    whenever(project.tasks).thenReturn(taskContainer)
    whenever(project.plugins).thenReturn(pluginContainer)
    whenever(project.pluginManager).thenReturn(pluginManager)
    whenever(project.layout).thenReturn(projectLayout)
    whenever(project.logger).thenReturn(logger)
    whenever(project.projectDir).thenReturn(File(".").canonicalFile)
    whenever(project.file(any<String>())).thenAnswer { inv ->
        File(File(".").canonicalFile, inv.arguments[0] as String)
    }

    doAnswer { inv ->
        val action = inv.arguments[0] as Action<Project>
        onAfterEvaluate(action)
        null
    }.whenever(project).afterEvaluate(any<Action<Project>>())

    return project
}

private fun mockProjectLayout(): ProjectLayout {
    val layout = mock<ProjectLayout>()
    val projectDir = mock<Directory>()
    val buildDir = mock<Directory>()
    val buildDirProp = mock<DirectoryProperty>()

    whenever(projectDir.asFile).thenReturn(File(".").canonicalFile)
    whenever(buildDir.asFile).thenReturn(File(".", "build"))
    whenever(buildDirProp.get()).thenReturn(buildDir)

    val asFileProvider = mock<Provider<File>>()
    whenever(asFileProvider.get()).thenReturn(File(".", "build"))
    whenever(buildDirProp.asFile).thenReturn(asFileProvider)

                doAnswer { inv ->
                    @Suppress("UNCHECKED_CAST")
                    val path = inv.arguments[0] as String
                    val dir = mock<Directory>()
                    val dirProvider: Provider<Directory> = mock()
                    whenever(dir.asFile).thenReturn(File(File(".", "build"), path))
                    whenever(dirProvider.get()).thenReturn(dir)
                    dirProvider
                }.whenever(buildDirProp).dir(org.mockito.kotlin.any<String>())

    whenever(layout.projectDirectory).thenReturn(projectDir)
    whenever(layout.buildDirectory).thenReturn(buildDirProp)

    return layout
}

private fun mockLogger(): Logger = mock()
