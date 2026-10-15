package bakery.ci

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * CI shard coverage guard.
 *
 * The monolithic `cucumberTest` BDD run and the `functionalTest` run are split
 * in `.github/workflows/test.yml` into exhaustive, disjoint shards, each on its
 * own CI machine. This guard kills the "test fantôme" risk (S-269): a feature
 * or a functional test class that belongs to no shard (or to two) would
 * silently stop being executed.
 *
 * It asserts, from the repository sources (no Gradle model, pure file parsing):
 * 1. the Cucumber shard runners form a **disjoint** partition whose union is
 *    **every** feature file under `src/test/features/`, and each shard
 *    preserves the aggregate exclusion (`not @personal`);
 * 2. every Cucumber shard runner is registered as a Gradle task **and** invoked
 *    by the CI workflow;
 * 3. the functional shard tasks partition **every** `@Test` functional class and
 *    all are invoked by the CI workflow;
 * 4. the `unit` job runs the plain `test` task.
 */
class BakeryCucumberShardCoverageTest {

    private val repoRoot: File = run {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        while (dir.parentFile != null && !dir.resolve(".github/workflows/test.yml").isFile) {
            dir = dir.parentFile
        }
        check(dir.resolve(".github/workflows/test.yml").isFile) {
            "test.yml not found above ${System.getProperty("user.dir")}"
        }
        dir
    }

    private val shardsDir = repoRoot.resolve("bakery-plugin/src/test/kotlin/bakery/ci/shards")
    private val featuresDir = repoRoot.resolve("bakery-plugin/src/test/features")
    private val functionalDir = repoRoot.resolve("bakery-plugin/src/functionalTest/kotlin")
    private val buildScript = repoRoot.resolve("bakery-plugin/build.gradle.kts")
    private val workflow = repoRoot.resolve(".github/workflows/test.yml")

    private fun allFeatures(): Set<String> =
        featuresDir.listFiles { f -> f.isFile && f.name.endsWith(".feature") }!!
            .map { it.name }
            .toSet()

    private fun shardRunners(): List<File> =
        shardsDir.listFiles { f -> f.isFile && f.name.startsWith("CucumberShard") && f.name.endsWith("Runner.kt") }!!
            .sortedBy { it.name }

    private fun runnerFeatures(runner: File): List<String> =
        Regex("""@SelectClasspathResource\("features/([^"]+\.feature)"\)""")
            .findAll(runner.readText())
            .map { it.groupValues[1] }
            .toList()

    private fun runnerTags(runner: File): String =
        Regex("""FILTER_TAGS_PROPERTY_NAME,\s*value = "([^"]*)"""")
            .find(runner.readText())?.groupValues?.get(1) ?: ""

    private fun allFunctionalClasses(): Set<String> =
        functionalDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".kt") && it.readText().contains("@Test") }
            .map { it.nameWithoutExtension }
            .toSet()

    private fun buildFunctionalClasses(): List<String> =
        Regex("""includeTestsMatching\("bakery\.([A-Za-z0-9]+)"\)""")
            .findAll(buildScript.readText())
            .map { it.groupValues[1] }
            .toList()

    @Test
    fun `the cucumber shards cover every feature exactly once and exclude personal`() {
        val runners = shardRunners()
        assertThat(runners)
            .describedAs("CI shard runners must exist (8)")
            .hasSizeGreaterThanOrEqualTo(8)

        runners.forEach { r ->
            assertThat(runnerFeatures(r))
                .describedAs("%s must select at least one feature", r.name)
                .isNotEmpty()
            assertThat(runnerTags(r))
                .describedAs("%s must exclude @personal (aggregate contract)", r.name)
                .contains("not @personal")
        }

        val byFeature = mutableMapOf<String, MutableList<File>>()
        runners.forEach { r -> runnerFeatures(r).forEach { byFeature.getOrPut(it) { mutableListOf() }.add(r) } }

        assertThat(byFeature.keys)
            .describedAs("the shards must cover every feature file")
            .containsExactlyInAnyOrderElementsOf(allFeatures())

        assertThat(byFeature.filterValues { it.size > 1 }.keys)
            .describedAs("a feature must belong to exactly one shard (disjoint partition)")
            .isEmpty()
    }

    @Test
    fun `every cucumber shard runner is a Gradle task and is run by the CI workflow`() {
        val runnerClasses = shardRunners().map { it.nameWithoutExtension }
        val build = buildScript.readText()
        val ci = workflow.readText()

        runnerClasses.forEach { runner ->
            assertThat(build)
                .describedAs("%s must be registered as a CucumberTaskSpec", runner)
                .contains(runner)
        }

        val buildShardTasks = Regex("""cucumberTestShard[A-Za-z0-9]+""")
            .findAll(build).map { it.value }.toSet()
        val ciShardTasks = Regex("""cucumberTestShard[A-Za-z0-9]+""")
            .findAll(ci).map { it.value }.toSet()

        assertThat(buildShardTasks)
            .describedAs("shard CucumberTaskSpec tasks")
            .hasSizeGreaterThanOrEqualTo(8)
        assertThat(ciShardTasks)
            .describedAs("test.yml must run every shard task")
            .containsExactlyInAnyOrderElementsOf(buildShardTasks)
    }

    @Test
    fun `the functional shards cover every functional class exactly once and are run by CI`() {
        val classes = buildFunctionalClasses()
        assertThat(classes)
            .describedAs("functional shard tasks must select functional classes")
            .isNotEmpty()
        assertThat(classes)
            .describedAs("every functional class must be selected by exactly one shard")
            .containsExactlyInAnyOrderElementsOf(allFunctionalClasses())
        assertThat(classes.size)
            .describedAs("no functional class may be selected twice (disjoint partition)")
            .isEqualTo(classes.toSet().size)

        // Every functional shard task declared in the build must be wired into CI
        // (S-259: the partition is 6 shards A–F; the assertion is derived from the
        // build script so adding/removing a shard cannot silently desync).
        val shardTasks =
            Regex("""functionalTestShard[A-Z]""")
                .findAll(buildScript.readText())
                .map { it.value }
                .toSet()
        assertThat(shardTasks)
            .describedAs("functional shard tasks declared in build.gradle.kts")
            .hasSizeGreaterThanOrEqualTo(6)

        val ci = workflow.readText()
        shardTasks.forEach { task ->
            assertThat(ci)
                .describedAs("test.yml must run %s", task)
                .contains(task)
        }
    }

    @Test
    fun `the unit job runs the plain test task`() {
        assertThat(workflow.readText())
            .describedAs("test.yml must run the unit `test` task")
            .contains("./gradlew test")
    }
}
