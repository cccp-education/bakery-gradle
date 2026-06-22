import com.github.gradle.node.npm.task.NpxTask
import org.gradle.api.file.DuplicatesStrategy.EXCLUDE
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import java.time.Duration

plugins {
    signing
    `java-library`
    `maven-publish`
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publish)
    alias(libs.plugins.kover)
    alias(libs.plugins.node.gradle)
}

group = "education.cccp"
version = "0.0.1"
kotlin.jvmToolchain(24)

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // BOM — workspace version alignment (workspace-bom, MEMPHIS)
    implementation(platform("education.cccp:workspace-bom:0.0.1"))

    implementation(kotlin("stdlib-jdk8"))

    api(libs.bundles.jbake)
    api(libs.bundles.jgit)
    api(libs.commons.io)
    implementation(libs.node.gradle)

    implementation(libs.graphify.plugin)
    implementation(libs.codebase.contracts)
    implementation(libs.codebase.plugin)

    // LLM — LangChain4j + Ollama (BKY-IA-0)
    implementation(libs.langchain4j.ollama)

    // Coroutines — déjà en testImplementation via bundle,
    // on ajoute le core en implementation pour OllamaLlmService
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.jdk8)

    // Arrow-kt — functional programming (Either, Validation, Option, Raise DSL)
    implementation(libs.bundles.arrow)

    // RSS parsing — Rome (CS-8 remplace DocumentBuilderFactory DOM fragile)
    implementation(libs.rome)

    // Coroutines — IMPORTANT pour les tests asynchrones
    testImplementation(libs.bundles.coroutines)

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.slf4j.api)
    testRuntimeOnly(libs.logback.classic)

    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.junit.jupiter)

    // Thymeleaf — rendering tests (BKY-JB-9 Phase A)
    testImplementation(libs.thymeleaf)

    // Cucumber dependencies
    testImplementation(libs.bundles.cucumber)
}


tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    forkEvery = 0
    timeout.set(Duration.ofMinutes(5))
    outputs.cacheIf { true }
}

// Specific configuration for plugin tests
// EPIC 11 — CucumberTestRunner excluded from unit tests: only cucumberTest task runs Cucumber
// This prevents double execution (~5min wasted). Cucumber tests are in bakery.scenarios package.
tasks.named<Test>("test") {
    classpath += files(tasks.named("jar"))

    systemProperty("gradle.plugin.repository", project.rootDir.resolve("build/libs").absolutePath)

    useJUnitPlatform { excludeEngines("cucumber") }

    // Exclude Cucumber step definitions and runner from unit test discovery
    filter { excludeTestsMatching("bakery.scenarios.*") }

    maxParallelForks = 2
}


// 1. Créer le SourceSet functionalTest
val functionalTest: SourceSet by sourceSets.creating {
    java { srcDirs("src/functionalTest/kotlin") }
    resources { srcDirs("src/functionalTest/resources") }
}

// 2. Ajouter GradleTestKit à functionalTest (SANS hériter de testImplementation)
dependencies {
    add(functionalTest.implementationConfigurationName, platform("education.cccp:workspace-bom:0.1.0"))

    add(functionalTest.implementationConfigurationName, gradleTestKit())
    add(functionalTest.implementationConfigurationName, kotlin("stdlib-jdk8"))
    add(functionalTest.implementationConfigurationName, kotlin("test-junit5"))

    // Ajouter les dépendances nécessaires explicitement
    add(functionalTest.implementationConfigurationName, libs.slf4j.api)
    add(functionalTest.runtimeOnlyConfigurationName, libs.logback.classic)
    add(functionalTest.runtimeOnlyConfigurationName, "org.junit.platform:junit-platform-launcher")

    // CORRECTION: Ajouter AssertJ pour les assertions
    add(functionalTest.implementationConfigurationName, libs.assertj.core)

    // Ajouter Mockito si nécessaire
    add(functionalTest.implementationConfigurationName, libs.mockito.kotlin)
    add(functionalTest.implementationConfigurationName, libs.mockito.junit.jupiter)

    libs.bundles.coroutines.get().forEach { add(functionalTest.implementationConfigurationName, it) }

    libs.bundles.jgit.get().forEach { add(functionalTest.implementationConfigurationName, it) }
}

// 3. Tâche pour les tests fonctionnels
val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs functional tests."
    group = "verification"
    testClassesDirs = functionalTest.output.classesDirs
    classpath = configurations[functionalTest.runtimeClasspathConfigurationName] + functionalTest.output

    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    failOnNoDiscoveredTests = false

    systemProperty("test.timeout.multiplier", "2")

    maxParallelForks = (Runtime.getRuntime().availableProcessors()).coerceAtLeast(1)
}

// CORRECTION: Gérer les duplications de ressources pour functionalTest
tasks.named<ProcessResources>(functionalTest.processResourcesTaskName) { duplicatesStrategy = EXCLUDE }

// ────────────────────────────────────────────────────────────
// E2E Test SourceSet + Playwright (BKY-JB-9 Phase B)
// ────────────────────────────────────────────────────────────

// 1. Créer le SourceSet e2eTest
val e2eTest: SourceSet by sourceSets.creating {
    java { srcDirs("src/e2eTest/kotlin") }
    resources { srcDirs("src/e2eTest/resources") }
}

// 2. Dépendances e2eTest : Playwright + JUnit5 + AssertJ + full test runtime
dependencies {
    add(e2eTest.implementationConfigurationName, platform("education.cccp:workspace-bom:0.1.0"))

    add(e2eTest.implementationConfigurationName, sourceSets.main.get().output)
    add(e2eTest.implementationConfigurationName, sourceSets.test.get().output)
    add(e2eTest.implementationConfigurationName, libs.playwright)
    add(e2eTest.implementationConfigurationName, kotlin("stdlib-jdk8"))
    add(e2eTest.implementationConfigurationName, kotlin("test-junit5"))
    add(e2eTest.implementationConfigurationName, libs.assertj.core)
    add(e2eTest.implementationConfigurationName, libs.thymeleaf)
    add(e2eTest.implementationConfigurationName, libs.bundles.jbake)
    add(e2eTest.implementationConfigurationName, libs.bundles.jgit)
    add(e2eTest.implementationConfigurationName, libs.commons.io)
    add(e2eTest.implementationConfigurationName, libs.bundles.cucumber)
    add(e2eTest.implementationConfigurationName, libs.langchain4j.ollama)
    add(e2eTest.implementationConfigurationName, libs.node.gradle)
    add(e2eTest.implementationConfigurationName, libs.graphify.plugin)
    add(e2eTest.implementationConfigurationName, libs.codebase.contracts)
    add(e2eTest.implementationConfigurationName, libs.bundles.coroutines)
    add(e2eTest.implementationConfigurationName, libs.kotlinx.coroutines.test)
    add(e2eTest.implementationConfigurationName, libs.slf4j.api)
    add(e2eTest.implementationConfigurationName, libs.mockito.kotlin)
    add(e2eTest.implementationConfigurationName, libs.mockito.junit.jupiter)
    add(e2eTest.runtimeOnlyConfigurationName, libs.logback.classic)
    add(e2eTest.runtimeOnlyConfigurationName, "org.junit.platform:junit-platform-launcher")
}

// 3. Tâche e2eTest — Playwright browser tests (séparés de check)
val e2eTestTask = tasks.register<Test>("e2eTest") {
    description = "Runs E2E tests with Playwright browser."
    group = "verification"
    testClassesDirs = e2eTest.output.classesDirs
    classpath = configurations[e2eTest.runtimeClasspathConfigurationName] + e2eTest.output

    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    failOnNoDiscoveredTests = false

    // Les tests E2E nécessitent Chromium installé.
    // La tâche installPlaywright (NpxTask) gère l'installation automatiquement.
    dependsOn("installPlaywright")

    maxParallelForks = 1
    forkEvery = 1

    timeout.set(Duration.ofMinutes(10))

    // Éviter les conflits de ressources avec d'autres JVM
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

// 4. Tâche installPlaywright — installe Chromium via le plugin Gradle Node
// Utilise NpxTask au lieu d'un appel manuel en ligne de commande.

tasks.register<NpxTask>("installPlaywright") {
    group = "verification"
    description = "Install Playwright Chromium browser for E2E tests."
    command.set("playwright")
    args.addAll("install", "chromium")
}

// 5. Gérer les duplications de ressources pour e2eTest
tasks.named<ProcessResources>(e2eTest.processResourcesTaskName) {
    duplicatesStrategy = EXCLUDE
}

// NOTE : e2eTest n'est PAS dans tasks.check{} — il doit être lancé explicitement
// via ./gradlew e2eTest. Playwright + Chromium sont installés automatiquement
// via la tâche installPlaywright (NpxTask, plugin Gradle Node).

// 4. Configurer les sources sets pour Cucumber (test standard)
sourceSets {
    test {
        resources { srcDir("src/test/features") }
        // Steps dans scenarios/
        java { srcDir("src/test/scenarios") }
    }
}

// 5. Faire hériter testImplementation de functionalTest (pas l'inverse !)
configurations.named("testImplementation").configure {
    extendsFrom(configurations.named(functionalTest.implementationConfigurationName).get())
}

configurations.named("testRuntimeOnly").configure {
    extendsFrom(configurations.named(functionalTest.runtimeOnlyConfigurationName).get())
}

// 6. Ajouter les classes compilées de functionalTest au classpath de test
dependencies { testImplementation(functionalTest.output) }

configurations {
    // Exclure logback-classic du classpath de test
    named("testRuntimeClasspath") { exclude(group = "ch.qos.logback", module = "logback-classic") }
    named("testImplementation") { exclude(group = "ch.qos.logback", module = "logback-classic") }
    // Exclure logback-classic du classpath de functionalTest
    named(functionalTest.runtimeClasspathConfigurationName) {
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
}

// 7. Tâche dédiée aux tests Cucumber
val cucumberTest = tasks.register<Test>("cucumberTest") {
    description = "Runs Cucumber BDD tests"
    group = "verification"

    testClassesDirs = sourceSets.test.get().output.classesDirs

    classpath = configurations.testRuntimeClasspath.get() +
        sourceSets.test.get().output +
        functionalTest.output +
        sourceSets.main.get().output +
        files(tasks.jar.get().archiveFile)

    dependsOn(tasks.classes)

    useJUnitPlatform {
        excludeEngines("junit-jupiter")
    }

    systemProperty("cucumber.junit-platform.naming-strategy", "long")
    systemProperty("org.gradle.daemon", "false")

    maxHeapSize = "1g"

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
        exceptionFormat = FULL
    }

    dependsOn(functionalTest.classesTaskName)
    dependsOn(tasks.classes)

    maxParallelForks = 1
    forkEvery = 0

    timeout.set(Duration.ofMinutes(5))

    doLast {
        val tempDir = File(System.getProperty("java.io.tmpdir"))
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)

        tempDir.listFiles { file ->
            file.isDirectory && file.name.startsWith("gradle-test-") &&
                file.lastModified() < oneHourAgo
        }?.forEach { oldDir ->
            try {
                if (oldDir.deleteRecursively()) println("  Cleaned: ${oldDir.name}")
            } catch (_: Exception) {
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    // Permet de masquer l'avertissement relatif au chargement dynamique d'agents
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

tasks.check {
    dependsOn(functionalTestTask)
    dependsOn(cucumberTest)
}

kover {
    currentProject {
        sources {
            includedSourceSets.addAll("main", "functionalTest")
        }
    }
    reports {
        total {
            html {
                onCheck.set(true)
                htmlDir.set(layout.buildDirectory.dir("reports/kover/html"))
            }
            xml {
                onCheck.set(true)
                xmlFile.set(layout.buildDirectory.file("reports/kover/xml/report.xml"))
            }
        }
    }
}

tasks.register("koverThresholdCheck") {
    description = "kover threshold check"
    dependsOn("koverXmlReport")

    doLast {
        val reportFile = layout.buildDirectory.file("reports/kover/xml/report.xml").get().asFile
        if (!reportFile.exists()) {
            throw GradleException("Kover report not found. Run 'koverXmlReport' first.")
        }
        val xml = reportFile.readText()
        val coverageRegex = Regex("""<counter type="INSTRUCTION" missed="(\d+)" covered="(\d+)"/>""")
        val matches = coverageRegex.findAll(xml)
        var totalMissed = 0L
        var totalCovered = 0L
        for (match in matches) {
            totalMissed += match.groupValues[1].toLong()
            totalCovered += match.groupValues[2].toLong()
        }
        val total = totalMissed + totalCovered
        val coverage = if (total > 0) (totalCovered.toDouble() / total) * 100 else 0.0
        println(
            "Instruction coverage: ${
                String.format(
                    "%.2f",
                    coverage
                )
            }% (missed=$totalMissed, covered=$totalCovered)"
        )
        if (coverage < 85.0) {
            throw GradleException("Coverage ${String.format("%.2f", coverage)}% is below threshold 85%")
        }
    }
}

tasks.check { dependsOn("koverThresholdCheck") }

gradlePlugin {
    plugins {
        create("bakery") {
            id = libs.plugins.bakery.get().pluginId
            implementationClass = "bakery.BakeryPlugin"
            displayName = "Bakery Plugin"
            description = "Gradle plugin for static site generation."
            tags.set(listOf("jbake", "static-site-generator", "blog", "jgit", "asciidoc", "markdown", "thymeleaf"))
        }
    }
    website = "https://cccp-education.github.io/"
    vcsUrl = "https://github.com/cccp-education/bakery-gradle.git"
    testSourceSets(functionalTest)
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set(gradlePlugin.plugins.getByName("bakery").displayName)
                description.set(gradlePlugin.plugins.getByName("bakery").description)
                url.set(gradlePlugin.website.get())
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("cccp-education")
                        name.set("CCCP Education")
                        email.set("cccp.edu@gmail.com")
                    }
                }
                scm {
                    connection.set(gradlePlugin.vcsUrl.get())
                    developerConnection.set(gradlePlugin.vcsUrl.get())
                    url.set(gradlePlugin.vcsUrl.get())
                }
                project.findProperty("relocationGroup")?.let { targetGroup ->
                    withXml {
                        val pom = asElement()
                        val doc = pom.ownerDocument
                        val distMgmt = doc.createElement("distributionManagement")
                        val relocation = doc.createElement("relocation")
                        relocation.appendChild(doc.createElement("groupId")).also { it.textContent = targetGroup.toString() }
                        relocation.appendChild(doc.createElement("artifactId")).also { it.textContent = project.name }
                        distMgmt.appendChild(relocation)
                        pom.appendChild(distMgmt)
                    }
                }
            }
        }
    }
    repositories {
        mavenCentral()
    }
}

signing {
    if (System.getenv("CI") != "true" && !version.toString().endsWith("-SNAPSHOT")) {
        sign(publishing.publications)
    }
    useGpgCmd()
}
