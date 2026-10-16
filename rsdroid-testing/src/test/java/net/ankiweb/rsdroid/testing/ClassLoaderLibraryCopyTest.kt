// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb.rsdroid.testing

import net.ankiweb.rsdroid.testing.ClassLoaderLibraryWorker.Scenario
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit.SECONDS

class ClassLoaderLibraryCopyTest {
    @TempDir
    lateinit var temporaryFolder: File

    @Test
    fun independentClassLoadersCanCallTheNativeBackend() {
        runNativeScenario(Scenario.INDEPENDENT)
    }

    @Test
    fun interruptedCleanupPreservesInterruptAndReportsFailure() {
        try {
            Thread.currentThread().interrupt()
            assertFalse(awaitClassLoaderCleanup())
            assertTrue(Thread.currentThread().isInterrupted)
        } finally {
            Thread.interrupted()
        }
    }

    private fun runNativeScenario(scenario: Scenario) {
        val result = File(temporaryFolder, "result.txt")
        val output = File(temporaryFolder, "worker.log")
        val process =
            ProcessBuilder(
                File(System.getProperty("java.home"), "bin/java").path,
                "-Djava.io.tmpdir=${temporaryFolder.absolutePath}",
                "-cp",
                checkNotNull(System.getProperty("rsdroidTesting.testClasspath")),
                ClassLoaderLibraryWorker::class.java.name,
                scenario.name,
                result.absolutePath,
            ).redirectErrorStream(true).redirectOutput(output).start()
        try {
            assertTrue(process.waitFor(45, SECONDS), "Native scenario '$scenario' timed out: ${output.readText()}")
            // Native logging redirects stdout/stderr, so the worker writes failures separately.
            val report = if (result.exists()) result.readText() else "No result file.\n${output.readText()}"
            assertEquals(0, process.exitValue(), report)
            assertEquals("OK", report)
        } finally {
            if (process.isAlive) process.destroyForcibly()
            assertTrue(process.waitFor(10, SECONDS), "Native worker did not exit")
        }
    }
}
