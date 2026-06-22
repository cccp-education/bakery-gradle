package bakery

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.RefSpec
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.slf4j.LoggerFactory
import java.io.File

class GitServiceTest {

    private val logger = LoggerFactory.getLogger(GitServiceTest::class.java)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ValidatePrePushTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `validatePrePush returns Valid when dest has content and remote is configured`() {
            val destPath = tempDir.resolve("dest").apply {
                mkdirs()
                resolve("index.html").writeText("<h1>Hello</h1>")
            }
            val git = GitPushConfiguration(
                repo = RepositoryConfiguration(repository = "https://github.com/test/test.git")
            )

            val result = GitService.validatePrePush({ destPath.absolutePath }, git)

            assertThat(result).isEqualTo(GitService.PrePushValidation.Valid)
        }

        @Test
        fun `validatePrePush returns RemoteNotConfigured when repository URL is blank`() {
            val destPath = tempDir.resolve("dest").apply {
                mkdirs()
                resolve("index.html").writeText("<h1>Hello</h1>")
            }
            val git = GitPushConfiguration(
                repo = RepositoryConfiguration(repository = "")
            )

            val result = GitService.validatePrePush({ destPath.absolutePath }, git)

            assertThat(result).isEqualTo(GitService.PrePushValidation.RemoteNotConfigured)
        }

        @Test
        fun `validatePrePush returns ContentAbsent when dest directory is empty`() {
            val destPath = tempDir.resolve("dest").apply { mkdirs() }
            val git = GitPushConfiguration(
                repo = RepositoryConfiguration(repository = "https://github.com/test/test.git")
            )

            val result = GitService.validatePrePush({ destPath.absolutePath }, git)

            assertThat(result).isEqualTo(GitService.PrePushValidation.ContentAbsent)
        }

        @Test
        fun `validatePrePush returns ContentAbsent when dest directory does not exist`() {
            val destPath = tempDir.resolve("nonexistent")
            val git = GitPushConfiguration(
                repo = RepositoryConfiguration(repository = "https://github.com/test/test.git")
            )

            val result = GitService.validatePrePush({ destPath.absolutePath }, git)

            assertThat(result).isEqualTo(GitService.PrePushValidation.ContentAbsent)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CleanupDirTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `cleanupDir deletes existing directory`() {
            val target = tempDir.resolve("toDelete").apply { mkdirs() }
            target.resolve("file.txt").writeText("content")
            assertThat(target).exists()

            GitService.cleanupDir(target, logger)

            assertThat(target).doesNotExist()
        }

        @Test
        fun `cleanupDir is silent when directory does not exist`() {
            val nonExistent = tempDir.resolve("neverExisted")
            assertThat(nonExistent).doesNotExist()

            GitService.cleanupDir(nonExistent, logger)

            assertThat(nonExistent).doesNotExist()
        }

        @Test
        fun `cleanupDir catches exception and logs error without throwing`() {
            val target = tempDir.resolve("lockedDir").apply { mkdirs() }
            target.resolve("file.txt").apply {
                writeText("content")
                setReadOnly()
            }
            target.setReadOnly()

            assertThatCode {
                GitService.cleanupDir(target, logger)
            }.doesNotThrowAnyException()

            target.setWritable(true)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InitAddCommitTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `initAddCommit initializes repo and creates commit`() {
            val repoDir = tempDir.resolve("repo")
            repoDir.mkdirs()
            repoDir.resolve("index.html").writeText("<h1>Hello</h1>")

            val config = GitPushConfiguration(
                from = "",
                to = "",
                repo = RepositoryConfiguration(
                    name = "test",
                    repository = "https://github.com/test/test.git",
                    credentials = RepositoryCredentials("user", "token")
                ),
                branch = "main",
                message = "deploy: cheroliv.com"
            )

            val revCommit = GitService.initAddCommit(repoDir, config, logger)

            assertThat(revCommit.shortMessage).isEqualTo("deploy: cheroliv.com")
            assertThat(repoDir.resolve("index.html")).exists()

            val git = Git.open(repoDir)
            val log = git.log().call().toList()
            assertThat(log).hasSize(1)
            assertThat(log[0].fullMessage).contains("deploy: cheroliv.com")
            git.close()
        }

        @Test
        fun `initAddCommit does not mask commit message`() {
            val repoDir = tempDir.resolve("repo2")
            repoDir.mkdirs()
            repoDir.resolve("file.txt").writeText("data")

            val config = GitPushConfiguration(
                from = "",
                to = "",
                repo = RepositoryConfiguration(
                    name = "test",
                    repository = "https://github.com/test/test.git",
                    credentials = RepositoryCredentials("user", "ghp_abc123")
                ),
                branch = "main",
                message = "deploy: test"
            )

            val commit = GitService.initAddCommit(repoDir, config, logger)
            assertThat(commit.fullMessage).contains("deploy: test")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PushToRemoteHistoryPreservationTest {

        @TempDir
        lateinit var tempDir: File

        private fun createBareRemoteRepository(): Git {
            val remoteDir = tempDir.resolve("remote.git")
            remoteDir.mkdirs()
            return Git.init()
                .setDirectory(remoteDir)
                .setBare(true)
                .call()
        }

        private fun pushHeadToRemote(git: Git, branch: String, remoteName: String = "origin") {
            git.push()
                .setRemote(remoteName)
                .setRefSpecs(RefSpec("+HEAD:refs/heads/$branch"))
                .call()
        }

        private fun commitOnBranch(localClone: Git, fileName: String, content: String, message: String): RevCommit {
            localClone.repository.workTree.resolve(fileName).writeText(content)
            localClone.add().addFilepattern(fileName).call()
            return localClone.commit().setMessage(message).call()
        }

        @Test
        fun `pushToRemote with preserveHistory clones remote and keeps existing files`() {
            val remoteGit = createBareRemoteRepository()
            val remoteUri = remoteGit.repository.directory.toURI().toString()

            val localRemote = tempDir.resolve("localRemote")
            val cloneGit = Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(localRemote)
                .call()
            try {
                cloneGit.repository.workTree.resolve("old.txt").writeText("old content")
                cloneGit.add().addFilepattern("old.txt").call()
                cloneGit.commit().setMessage("initial commit").call()
                pushHeadToRemote(cloneGit, "main")
            } finally {
                cloneGit.close()
            }

            val repoDir = tempDir.resolve("repoDir")
            repoDir.mkdirs()
            repoDir.resolve("new.txt").writeText("new content")

            val config = GitPushConfiguration(
                from = "",
                to = "",
                repo = RepositoryConfiguration(
                    name = "test",
                    repository = remoteUri,
                    credentials = RepositoryCredentials("", "")
                ),
                branch = "main",
                message = "update"
            )

            GitService.pushToRemote(
                repoDir = repoDir,
                git = config,
                logger = logger,
                preserveHistory = true
            )

            assertThat(repoDir.resolve("old.txt")).exists()
            assertThat(repoDir.resolve("old.txt").readText()).isEqualTo("old content")
            assertThat(repoDir.resolve("new.txt")).exists()
            assertThat(repoDir.resolve("new.txt").readText()).isEqualTo("new content")

            val log = remoteGit.log().call().toList()
            assertThat(log).hasSize(2)
        }

        @Test
        fun `pushToRemote without preserveHistory does not clone and overwrites`() {
            val remoteGit = createBareRemoteRepository()
            val remoteUri = remoteGit.repository.directory.toURI().toString()

            val localRemote = tempDir.resolve("localRemote2")
            val cloneGit = Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(localRemote)
                .call()
            try {
                cloneGit.repository.workTree.resolve("old.txt").writeText("old content")
                cloneGit.add().addFilepattern("old.txt").call()
                cloneGit.commit().setMessage("initial commit").call()
                pushHeadToRemote(cloneGit, "main")
            } finally {
                cloneGit.close()
            }

            val repoDir = tempDir.resolve("repoDir2")
            repoDir.mkdirs()
            repoDir.resolve("new.txt").writeText("new content")

            val config = GitPushConfiguration(
                from = "",
                to = "",
                repo = RepositoryConfiguration(
                    name = "test",
                    repository = remoteUri,
                    credentials = RepositoryCredentials("", "")
                ),
                branch = "main",
                message = "update"
            )

            GitService.pushToRemote(
                repoDir = repoDir,
                git = config,
                logger = logger,
                preserveHistory = false
            )

            val log = remoteGit.log().call().toList()
            assertThat(log).hasSize(1)
        }

        @Test
        fun `pushToRemote with preserveHistory falls back to init if clone fails`() {
            val repoDir = tempDir.resolve("repoDir3")
            repoDir.mkdirs()
            repoDir.resolve("file.txt").writeText("content")

            val config = GitPushConfiguration(
                from = "",
                to = "",
                repo = RepositoryConfiguration(
                    name = "test",
                    repository = "invalid://no-such-repo.git",
                    credentials = RepositoryCredentials("", "")
                ),
                branch = "main",
                message = "update"
            )

            assertThatThrownBy {
                GitService.pushToRemote(
                    repoDir = repoDir,
                    git = config,
                    logger = logger,
                    preserveHistory = true
                )
            }.isInstanceOf(Exception::class.java)

            assertThat(repoDir.resolve("file.txt")).exists()
        }

        @Test
        fun `preserveHistory clones remote even when repoDir is initially empty`() {
            val remoteGit = createBareRemoteRepository()
            val remoteUri = remoteGit.repository.directory.toURI().toString()

            val localRemote = tempDir.resolve("localRemote3")
            val cloneGit = Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(localRemote)
                .call()
            try {
                cloneGit.repository.workTree.resolve("remote_only.txt").writeText("remote")
                cloneGit.add().addFilepattern("remote_only.txt").call()
                cloneGit.commit().setMessage("initial commit").call()
                pushHeadToRemote(cloneGit, "main")
            } finally {
                cloneGit.close()
            }

            val repoDir = tempDir.resolve("repoDirEmpty")
            repoDir.mkdirs()

            val config = GitPushConfiguration(
                from = "",
                to = "",
                repo = RepositoryConfiguration(
                    name = "test",
                    repository = remoteUri,
                    credentials = RepositoryCredentials("", "")
                ),
                branch = "main",
                message = "update"
            )

            GitService.pushToRemote(
                repoDir = repoDir,
                git = config,
                logger = logger,
                preserveHistory = true
            )

            assertThat(repoDir.resolve("remote_only.txt")).exists().hasContent("remote")
        }

        @Test
        fun `preserveHistory overlay overwrites existing remote file with local content`() {
            val remoteGit = createBareRemoteRepository()
            val remoteUri = remoteGit.repository.directory.toURI().toString()

            val localRemote = tempDir.resolve("localRemote4")
            val cloneGit = Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(localRemote)
                .call()
            try {
                cloneGit.repository.workTree.resolve("shared.txt").writeText("remote_version")
                cloneGit.add().addFilepattern("shared.txt").call()
                cloneGit.commit().setMessage("initial commit").call()
                pushHeadToRemote(cloneGit, "main")
            } finally {
                cloneGit.close()
            }

            val repoDir = tempDir.resolve("repoDirOverlay")
            repoDir.mkdirs()
            repoDir.resolve("shared.txt").writeText("local_version")

            val config = GitPushConfiguration(
                from = "",
                to = "",
                repo = RepositoryConfiguration(
                    name = "test",
                    repository = remoteUri,
                    credentials = RepositoryCredentials("", "")
                ),
                branch = "main",
                message = "update"
            )

            GitService.pushToRemote(
                repoDir = repoDir,
                git = config,
                logger = logger,
                preserveHistory = true
            )

            assertThat(repoDir.resolve("shared.txt")).exists().hasContent("local_version")
        }

        @Test
        fun `preserveHistory works when repoDir contains nested directories`() {
            val remoteGit = createBareRemoteRepository()
            val remoteUri = remoteGit.repository.directory.toURI().toString()

            val localRemote = tempDir.resolve("localRemote5")
            val cloneGit = Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(localRemote)
                .call()
            try {
                cloneGit.repository.workTree.resolve("root.txt").writeText("root")
                cloneGit.add().addFilepattern("root.txt").call()
                cloneGit.commit().setMessage("initial commit").call()
                pushHeadToRemote(cloneGit, "main")
            } finally {
                cloneGit.close()
            }

            val repoDir = tempDir.resolve("repoDirNested")
            repoDir.mkdirs()
            val subDir = repoDir.resolve("sub").apply { mkdirs() }
            subDir.resolve("nested.txt").writeText("nested content")

            val config = GitPushConfiguration(
                from = "",
                to = "",
                repo = RepositoryConfiguration(
                    name = "test",
                    repository = remoteUri,
                    credentials = RepositoryCredentials("", "")
                ),
                branch = "main",
                message = "update"
            )

            GitService.pushToRemote(
                repoDir = repoDir,
                git = config,
                logger = logger,
                preserveHistory = true
            )

            assertThat(repoDir.resolve("root.txt")).exists().hasContent("root")
            assertThat(repoDir.resolve("sub/nested.txt")).exists().hasContent("nested content")
        }

        @Test
        fun `pushToRemote with force push sends without cloning`() {
            val remoteGit = createBareRemoteRepository()
            val remoteUri = remoteGit.repository.directory.toURI().toString()

            val localRemote = tempDir.resolve("localForce")
            val cloneGit = Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(localRemote)
                .call()
            try {
                cloneGit.repository.workTree.resolve("old.txt").writeText("old")
                cloneGit.add().addFilepattern("old.txt").call()
                cloneGit.commit().setMessage("initial").call()
                pushHeadToRemote(cloneGit, "main")
            } finally {
                cloneGit.close()
            }

            val repoDir = tempDir.resolve("forceRepoDir")
            repoDir.mkdirs()
            repoDir.resolve("new.txt").writeText("forced content")

            val config = GitPushConfiguration(
                from = "",
                to = "",
                repo = RepositoryConfiguration(
                    name = "test",
                    repository = remoteUri,
                    credentials = RepositoryCredentials("", "")
                ),
                branch = "main",
                message = "force push"
            )

            GitService.pushToRemote(
                repoDir = repoDir,
                git = config,
                logger = logger,
                force = true,
                preserveHistory = false
            )

            val log = remoteGit.log().call().toList()
            assertThat(log).hasSize(1)
            assertThat(log[0].fullMessage).contains("force push")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PushPagesTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `pushPages returns Right when content exists and remote is configured`() {
            val destDir = tempDir.resolve("bakedOutput").apply { mkdirs() }
            destDir.resolve("index.html").writeText("<h1>Site</h1>")
            val repoDir = tempDir.resolve("gh-pages")

            val remoteDir = tempDir.resolve("remote.git").apply { mkdirs() }
            val remoteGit = Git.init().setDirectory(remoteDir).setBare(true).call()
            try {
                val config = GitPushConfiguration(
                    from = "",
                    to = "",
                    repo = RepositoryConfiguration(
                        name = "pages",
                        repository = remoteDir.toURI().toString(),
                        credentials = RepositoryCredentials("testuser", "testtoken")
                    ),
                    branch = "gh-pages",
                    message = "ci: deploy bakery site"
                )

                val result = GitService.pushPages(
                    destPath = { destDir.absolutePath },
                    pathTo = { repoDir.absolutePath },
                    git = config,
                    logger = logger
                )

                assertThat(result.isRight()).isTrue()
            } finally {
                remoteGit.close()
            }
        }

        @Test
        fun `pushPages returns Left when pushToRemote throws an exception`() {
            val destDir = tempDir.resolve("bakedOutputWithException").apply { mkdirs() }
            destDir.resolve("index.html").writeText("<h1>Site</h1>")
            val repoDir = tempDir.resolve("gh-pages-exception")

            val config = GitPushConfiguration(
                from = "",
                to = "",
                repo = RepositoryConfiguration(
                    name = "pages",
                    repository = "invalid://no-such-remote.git"
                ),
                branch = "gh-pages",
                message = "ci: deploy bakery site"
            )

            val result = GitService.pushPages(
                destPath = { destDir.absolutePath },
                pathTo = { repoDir.absolutePath },
                git = config,
                logger = logger
            )

            assertThat(result.isLeft()).isTrue()
            result.onLeft { error ->
                assertThat(error).isNotBlank()
            }
        }

        @Test
        fun `pushPages fails with RemoteNotConfigured when repository URL is blank`() {
            val destDir = tempDir.resolve("bakedOutput").apply { mkdirs() }
            destDir.resolve("index.html").writeText("<h1>Site</h1>")
            val repoDir = tempDir.resolve("gh-pages")

            val config = GitPushConfiguration(
                from = "",
                to = "",
                repo = RepositoryConfiguration(repository = ""),
                branch = "gh-pages",
                message = "deploy"
            )

            val result = GitService.pushPages(
                destPath = { destDir.absolutePath },
                pathTo = { repoDir.absolutePath },
                git = config,
                logger = logger
            )

            assertThat(result.isLeft()).isTrue()
            result.onLeft { error ->
                assertThat(error).contains("not configured")
            }
        }

        @Test
        fun `pushPages fails with ContentAbsent when dest directory is empty`() {
            val destDir = tempDir.resolve("emptyOutput").apply { mkdirs() }
            val repoDir = tempDir.resolve("gh-pages")

            val config = GitPushConfiguration(
                from = "",
                to = "",
                repo = RepositoryConfiguration(
                    name = "pages",
                    repository = "https://github.com/test/pages.git"
                ),
                branch = "gh-pages",
                message = "deploy"
            )

            val result = GitService.pushPages(
                destPath = { destDir.absolutePath },
                pathTo = { repoDir.absolutePath },
                git = config,
                logger = logger
            )

            assertThat(result.isLeft()).isTrue()
            result.onLeft { error ->
                assertThat(error).contains("No baked content")
            }
        }
    }
}
