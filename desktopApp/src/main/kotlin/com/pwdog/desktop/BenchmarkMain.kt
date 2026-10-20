package com.pwdog.desktop

import com.pwdog.core.P0Benchmark
import com.pwdog.core.P0SelfTest
import java.util.Locale

fun main() {
    val selfTest = P0SelfTest.run(entryCount = 500)
    println("P0 self-test")
    println("  sqlite=${selfTest.sqliteVersion}")
    println("  argon2=${formatMs(selfTest.argon2Millis)}")
    println("  write500=${formatMs(selfTest.dbCreateMillis)}")
    println("  read500=${formatMs(selfTest.dbReadMillis)}")
    println("  wrongKeyRejected=${selfTest.wrongKeyRejected}")
    println("  encryptedFile=${selfTest.fileLooksEncrypted}")

    val benchmark = P0Benchmark.run(entryCount = 10_000)
    try {
        println("P0 database benchmark")
        println("  entries=${benchmark.entryCount}")
        println("  seed=${formatMs(benchmark.seedMillis)}")
        println("  exactDomain=${formatMicros(benchmark.exactDomainMicros)}")
        println("  substringSearch=${formatMs(benchmark.substringSearchMillis)}")
        println("  reopen=${formatMs(benchmark.reopenMillis)}")
        println("  fileSize=${benchmark.fileSizeBytes / 1024} KB")
    } finally {
        P0Benchmark.cleanup(benchmark.workDir)
    }
}

private fun formatMs(millis: Double): String =
    String.format(Locale.US, "%.1f ms", millis)

private fun formatMicros(micros: Double): String =
    String.format(Locale.US, "%.1f us", micros)
