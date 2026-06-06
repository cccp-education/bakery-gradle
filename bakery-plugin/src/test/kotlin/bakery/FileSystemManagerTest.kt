package bakery

import arrow.core.Either.Left
import arrow.core.Either.Right
import bakery.FileSystemManager.copyBakedFilesToRepo
import bakery.FileSystemManager.copyFromFileSystemUrl
import bakery.FileSystemManager.copyFromJarUrl
import bakery.FileSystemManager.copyResourceDirectory
import bakery.FileSystemManager.copyFromResourceUrl
import bakery.FileSystemManager.createRepoDir
import bakery.FileSystemManager.isYmlUri
import bakery.FileSystemManager.read
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class FileSystemManagerTest {

    private val logger = LoggerFactory.getLogger(FileSystemManagerTest::class.java)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateRepoDirTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `creates new directory when path does not exist`() {
            val target = tempDir.resolve("newRepo")
            assertThat(target).doesNotExist()

            val result = createRepoDir(target.absolutePath, logger)

            assertThat(result).exists().isDirectory
            assertThat(result.absolutePath).isEqualTo(target.absolutePath)
        }

        @Test
        fun `recreates directory when it already exists`() {
            val target = tempDir.resolve("existingRepo").apply { mkdirs() }
            target.resolve("stale.txt").writeText("old")
            assertThat(target.resolve("stale.txt")).exists()

            val result = createRepoDir(target.absolutePath, logger)

            assertThat(result).exists().isDirectory
            assertThat(result.resolve("stale.txt")).doesNotExist()
        }

        @Test
        fun `deletes existing file and creates directory with same name`() {
            val target = tempDir.resolve("fileNotDir").apply { writeText("I am a file") }
            assertThat(target).exists().isFile

            val result = createRepoDir(target.absolutePath, logger)

            assertThat(result).exists().isDirectory
        }

        @Test
        fun `throws when path exists as non-deletable file`() {
            val target = tempDir.resolve("lockedFile").apply { writeText("data") }
            target.setReadOnly()

            val result = runCatching { createRepoDir(target.absolutePath, logger) }

            assertThat(result.isSuccess || result.exceptionOrNull() is IOException).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CopyBakedFilesToRepoTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `copies files from bakeDir to repoDir and deletes bakeDir`() {
            val bakeDir = tempDir.resolve("bake").apply { mkdirs() }
            bakeDir.resolve("index.html").writeText("hello")
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }

            val result = copyBakedFilesToRepo(bakeDir.absolutePath, repoDir, logger)

            assertThat(result).isInstanceOf(Right::class.java)
            assertThat(repoDir.resolve("index.html")).exists().hasContent("hello")
            assertThat(bakeDir).doesNotExist()
        }

        @Test
        fun `returns failure when bakeDir does not exist`() {
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }

            val result = copyBakedFilesToRepo(
                tempDir.resolve("nonexistent").absolutePath,
                repoDir,
                logger
            )

            assertThat(result).isInstanceOf(Left::class.java)
        }

        @Test
        fun `copies nested directories`() {
            val bakeDir = tempDir.resolve("bake").apply { mkdirs() }
            val subDir = bakeDir.resolve("assets").apply { mkdirs() }
            subDir.resolve("style.css").writeText("body{}")
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }

            val result = copyBakedFilesToRepo(bakeDir.absolutePath, repoDir, logger)

            assertThat(result).isInstanceOf(Right::class.java)
            assertThat(repoDir.resolve("assets/style.css")).exists().hasContent("body{}")
            assertThat(bakeDir).doesNotExist()
        }

        @Test
        fun `overwrites existing files in repoDir`() {
            val bakeDir = tempDir.resolve("bake").apply { mkdirs() }
            bakeDir.resolve("index.html").writeText("new")
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }
            repoDir.resolve("index.html").writeText("old")

            copyBakedFilesToRepo(bakeDir.absolutePath, repoDir, logger)

            assertThat(repoDir.resolve("index.html")).hasContent("new")
        }

        @Test
        fun `returns failure when copy throws IOException`() {
            val bakeDir = tempDir.resolve("bake").apply { mkdirs() }
            bakeDir.resolve("file.txt").writeText("content")
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }
            repoDir.setReadOnly()

            val result = copyBakedFilesToRepo(bakeDir.absolutePath, repoDir, logger)

            assertThat(result).isInstanceOf(Left::class.java)
        }
    }

    @Nested
    inner class IsYmlUriTest {

        @Test
        fun `valid yml URIs are recognized`() {
            assertThat("config.yml".isYmlUri).isTrue()
            assertThat("site.yml".isYmlUri).isTrue()
            assertThat("path/to/SITE.YML".isYmlUri).isTrue()
            assertThat("https://example.com/config.yml".isYmlUri).isTrue()
        }

        @Test
        fun `invalid URIs are rejected`() {
            assertThat("config.json".isYmlUri).isFalse()
            assertThat("site.txt".isYmlUri).isFalse()
            assertThat("siteyml".isYmlUri).isFalse()
            assertThat("https://example.com/".isYmlUri).isFalse()
            assertThat("".isYmlUri).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CopyResourceDirectoryEdgeCaseTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `returns Failure when resource path does not exist`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val targetDir = tempDir.resolve("target")

            val result = copyResourceDirectory("nonexistent-resource-path-xyz", targetDir, project)

            assertThat(result).isInstanceOf(Left::class.java)
            assertThat((result as Left).value).contains("Resource directory not found")
        }

        @Test
        fun `returns Success when copying valid filesystem resource`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val targetDir = tempDir.resolve("target")

            val result = copyResourceDirectory("site", targetDir, project)

            assertThat(result).isInstanceOf(Right::class.java)
            assertThat(targetDir.resolve("site")).exists().isDirectory
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CopyFromResourceUrlTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `returns Failure for unsupported protocol`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val targetDir = tempDir.resolve("target")
            val httpUrl = java.net.URI("http://example.com/data").toURL()

            val result = copyFromResourceUrl("data", targetDir, project, httpUrl)

            assertThat(result).isInstanceOf(Left::class.java)
            assertThat((result as Left).value).contains("Unsupported resource protocol")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CopyFromJarUrlTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `copies files from JAR to target directory`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val targetDir = tempDir.resolve("target")
            val jarFile = createJarWithEntries(
                "templates/index.thyme" to "<html></html>",
                "templates/header.thyme" to "<header>Bakery</header>",
                "templates/sub/footer.thyme" to "<footer>End</footer>"
            )

            val result = copyFromJarUrl("templates", targetDir, project, jarFile.toURI().toURL())

            assertThat(result).isInstanceOf(Right::class.java)
            val destDir = targetDir.resolve("templates")
            assertThat(destDir).exists().isDirectory
            assertThat(destDir.resolve("index.thyme")).exists().hasContent("<html></html>")
            assertThat(destDir.resolve("header.thyme")).exists().hasContent("<header>Bakery</header>")
            assertThat(destDir.resolve("sub/footer.thyme")).exists().hasContent("<footer>End</footer>")
        }

        @Test
        fun `copies files from JAR with trailing slash path`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val targetDir = tempDir.resolve("target")
            val jarFile = createJarWithEntries("site/readme.txt" to "readme content")

            val result = copyFromJarUrl("site/", targetDir, project, jarFile.toURI().toURL())

            assertThat(result).isInstanceOf(Right::class.java)
            assertThat(targetDir.resolve("site/readme.txt")).exists().hasContent("readme content")
        }

        @Test
        fun `skips directory entries in JAR`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val targetDir = tempDir.resolve("target")
            val jarFile = tempDir.resolve("dir-${System.nanoTime()}.jar")
            JarOutputStream(jarFile.outputStream()).use { jos ->
                jos.putNextEntry(JarEntry("res/"))
                jos.closeEntry()
                jos.putNextEntry(JarEntry("res/sub/"))
                jos.closeEntry()
                jos.putNextEntry(JarEntry("res/file.txt"))
                jos.write("data".toByteArray())
                jos.closeEntry()
            }

            val result = copyFromJarUrl("res", targetDir, project, jarFile.toURI().toURL())

            assertThat(result).isInstanceOf(Right::class.java)
            assertThat(targetDir.resolve("res/file.txt")).exists().hasContent("data")
            assertThat(targetDir.resolve("res/sub")).doesNotExist()
        }

        @Test
        fun `returns Failure when JAR file does not exist`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val targetDir = tempDir.resolve("target")
            val nonexistentJar = tempDir.resolve("nonexistent.jar")

            val result = copyFromJarUrl(
                "templates", targetDir, project, nonexistentJar.toURI().toURL()
            )

            assertThat(result).isInstanceOf(Left::class.java)
        }

        @Test
        fun `returns Failure when JAR URL points to a directory`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val targetDir = tempDir.resolve("target")

            val result = copyFromJarUrl(
                "templates", targetDir, project, tempDir.toURI().toURL()
            )

            assertThat(result).isInstanceOf(Left::class.java)
        }

        private fun createJarWithEntries(vararg entries: Pair<String, String>): File {
            val jarFile = tempDir.resolve("entries-${System.nanoTime()}.jar")
            JarOutputStream(jarFile.outputStream()).use { jos ->
                val parents = mutableSetOf<String>()
                for ((path, _) in entries) {
                    var parent = File(path).parent ?: continue
                    while (parent != "" && parent != "." && parents.add(parent)) {
                        jos.putNextEntry(JarEntry("$parent/"))
                        jos.closeEntry()
                        parent = File(parent).parent ?: ""
                    }
                }
                for ((path, content) in entries) {
                    jos.putNextEntry(JarEntry(path))
                    jos.write(content.toByteArray())
                    jos.closeEntry()
                }
            }
            return jarFile
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CopyFromFileSystemUrlTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `returns Failure when source directory does not exist`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val targetDir = tempDir.resolve("target")
            val nonexistentDir = tempDir.resolve("nonexistent")

            val result = copyFromFileSystemUrl(
                "faked", targetDir, project, nonexistentDir.toURI().toURL()
            )

            assertThat(result).isInstanceOf(Left::class.java)
            assertThat((result as Left).value).contains("does not exist")
        }

        @Test
        fun `returns Failure when source is a file not a directory`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val targetDir = tempDir.resolve("target")
            val sourceFile = tempDir.resolve("plainFile.txt").apply { writeText("hello") }

            val result = copyFromFileSystemUrl(
                "faked", targetDir, project, sourceFile.toURI().toURL()
            )

            assertThat(result).isInstanceOf(Left::class.java)
            assertThat((result as Left).value).contains("not a directory")
        }

        @Test
        fun `copies single file from filesystem to target`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val targetDir = tempDir.resolve("target")
            val sourceDir = tempDir.resolve("sourceCopy").apply { mkdirs() }
            sourceDir.resolve("index.html").writeText("<h1>Bakery</h1>")
            sourceDir.resolve("assets/css/style.css").apply { parentFile.mkdirs(); writeText("body{}") }

            val result = copyFromFileSystemUrl(
                "siteCopy", targetDir, project, sourceDir.toURI().toURL()
            )

            assertThat(result).isInstanceOf(Right::class.java)
            val dest = targetDir.resolve("siteCopy")
            assertThat(dest.resolve("index.html")).exists().hasContent("<h1>Bakery</h1>")
            assertThat(dest.resolve("assets/css/style.css")).exists().hasContent("body{}")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ReadInvalidYamlTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `returns default SiteConfiguration when YAML is invalid`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val malformedFile = tempDir.resolve("malformed.yml").apply {
                writeText("bake: { invalid: \n\t\tbad: [unclosed")
            }

            val result = project.read(malformedFile)

            assertThat(result).isNotNull
            assertThat(result.bake.destDirPath).isEmpty()
        }
    }
}
