// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb.rsdroid.testing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class LibraryCopyFileTest {
    @TempDir
    lateinit var temporaryFolder: File

    @Test
    fun damagedCopiesAreReplacedBeforeLoading() {
        val original = File(temporaryFolder, "library-checksum.dll").apply { writeText("complete library") }
        val copy = File(temporaryFolder, "library-checksum-1.dll")
        for (damaged in listOf("", "truncated", "oversized incomplete library")) {
            copy.writeText(damaged)
            assertEquals(copy, original.numberedCopy(1))
            assertEquals(original.readText(), copy.readText())
        }
    }

    @Test
    fun validCopyIsReused() {
        val original = File(temporaryFolder, "library-checksum.dll").apply { writeText("complete library") }
        val copy = original.numberedCopy(1)
        assertTrue(copy.setLastModified(1_000_000))
        val modified = copy.lastModified()
        original.numberedCopy(1)
        // Also exercise a writer which found damage before another writer repaired it.
        original.copyAtomicallyTo(copy)
        assertEquals(modified, copy.lastModified())
        assertEquals(original.readText(), copy.readText())
    }

    @Test
    fun originalIsReturnedWithoutCreatingACopy() {
        val original = File(temporaryFolder, "library-checksum.dll").apply { createNewFile() }
        assertEquals(original, original.numberedCopy(0))
        assertEquals(listOf(original), temporaryFolder.listFiles()!!.toList())
    }

    @Test
    fun creationReportsDiskUsageButReuseIsSilent() {
        val original = File(temporaryFolder, "library-checksum.dll").apply { writeText("library") }
        val output = ByteArrayOutputStream()
        val previousErr = System.err
        try {
            PrintStream(output).use { stream ->
                System.setErr(stream)
                val copy = original.numberedCopy(1)
                assertEquals(
                    "rsdroid-testing: created library copy 1/$maxCopies: ${copy.absolutePath} (7 bytes)",
                    output.toString().trim(),
                )
                output.reset()
                original.numberedCopy(1)
                assertEquals("", output.toString())
            }
        } finally {
            System.setErr(previousErr)
        }
    }
}
