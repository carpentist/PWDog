package com.pwdog.core

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class P0SelfTestTest {
    @Test
    fun fullSelfTestPasses() {
        val report = P0SelfTest.run(entryCount = 100)
        try {
            assertEquals(100, report.writeCount)
            assertEquals(100, report.readCount)
            assertTrue(report.wrongKeyRejected)
            assertTrue(report.fileLooksEncrypted)
            assertTrue(report.argon2Millis > 0.0)
            assertTrue(report.fileSizeBytes > 0L)
        } finally {
            deleteRecursively(report.workDir)
        }
    }

    private fun deleteRecursively(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
