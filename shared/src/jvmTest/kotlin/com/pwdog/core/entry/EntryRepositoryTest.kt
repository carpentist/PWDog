package com.pwdog.core.entry

import com.pwdog.core.account.AccountService
import com.pwdog.core.crypto.KdfParams
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EntryRepositoryTest {
    private val params = KdfParams(
        memoryKiB = 8 * 1024,
        iterations = 1,
        parallelism = 1,
    )

    @Test
    fun crudAndSearchWorkInsideEncryptedVault() {
        val base = Files.createTempDirectory("pwdog-entry-test-")
        val password = "correct-password".toByteArray()
        val service = AccountService(base, params)
        val session = service.createAccount("Alice", password).session

        try {
            val repository = EntryRepository(session.connection)
            val github = repository.create(
                title = "GitHub",
                url = "https://github.com",
                username = "alice",
                password = "secret-1",
                notes = "work account",
            )
            repository.create(
                title = "GitHub - Work",
                url = "https://github.com",
                username = "alice@work.example",
                password = "secret-work",
                notes = "second account for the same site",
            )
            repository.create(
                title = "Mail",
                url = "https://mail.example.com",
                username = "alice@example.com",
                password = "secret-2",
                notes = "",
            )
            repository.create(
                title = "100% Site",
                url = "https://percent.example",
                username = "bob",
                password = "secret-3",
                notes = "",
            )
            repository.create(
                title = "100X Site",
                url = "https://plain.example",
                username = "carol",
                password = "secret-4",
                notes = "",
            )

            assertEquals(5, repository.count())
            assertEquals(5, repository.search("").size)
            assertEquals(
                setOf("GitHub", "GitHub - Work"),
                repository.search("git").map { it.title }.toSet(),
            )
            assertEquals(listOf("Mail"), repository.search("example.com").map { it.title })
            assertEquals(
                setOf("GitHub", "GitHub - Work", "Mail"),
                repository.search("alice").map { it.title }.toSet(),
            )
            assertEquals(listOf("100% Site"), repository.search("100%").map { it.title })

            val githubAccounts = repository.findByHost("github.com")
            assertEquals(2, githubAccounts.size)
            assertEquals(
                setOf("alice", "alice@work.example"),
                githubAccounts.map { it.username }.toSet(),
            )
            assertTrue(repository.findByHost("login.github.com").isEmpty())

            val updated = repository.update(
                id = github.id,
                title = "GitHub",
                url = "https://github.com",
                username = "alice2",
                password = "new-secret",
                notes = "updated",
            )
            assertEquals("alice2", repository.get(github.id)?.username)
            assertEquals("new-secret", updated.password)
            assertTrue(updated.updatedAt >= github.updatedAt)

            assertTrue(repository.delete(updated.id))
            assertEquals(4, repository.count())
            assertEquals(1, repository.findByHost("github.com").size)
            assertEquals(null, repository.get(updated.id))

            val missing = repository.search("does-not-exist")
            assertNotNull(missing)
            assertTrue(missing.isEmpty())
        } finally {
            session.close()
            service.close()
            password.fill(0)
            deleteRecursively(base)
        }
    }

    @Test
    fun importsCredentialsAndSkipsExactDuplicates() {
        val base = Files.createTempDirectory("pwdog-import-test-")
        val password = "correct-password".toByteArray()
        val service = AccountService(base, params)
        val session = service.createAccount("Alice", password).session

        try {
            val repository = EntryRepository(session.connection)
            repository.create(
                title = "GitHub",
                url = "https://github.com",
                username = "alice",
                password = "secret",
                notes = "",
            )

            val summary = repository.importAll(
                listOf(
                    ImportedCredential("GitHub", "https://github.com", "alice", "secret", ""),
                    ImportedCredential("GitLab", "https://gitlab.com", "bob", "gitlab-secret", ""),
                    ImportedCredential("GitLab", "https://gitlab.com", "bob", "gitlab-secret", ""),
                ),
            )

            assertEquals(1, summary.imported)
            assertEquals(2, summary.duplicates)
            assertEquals(2, repository.count())
            assertEquals(
                setOf("GitHub", "GitLab"),
                repository.search("").map { it.title }.toSet(),
            )
        } finally {
            session.close()
            service.close()
            password.fill(0)
            deleteRecursively(base)
        }
    }

    private fun deleteRecursively(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
