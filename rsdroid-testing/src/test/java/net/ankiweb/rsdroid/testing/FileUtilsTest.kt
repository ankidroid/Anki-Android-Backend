// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb.rsdroid.testing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

class FileUtilsTest {
    @TempDir
    lateinit var temporaryFolder: File

    @Test
    fun existingDestinationIsPreserved() {
        val destination = File(temporaryFolder, "library.bin").apply { writeText("winner") }
        destination.createAtomically { it.writeText("loser") }
        assertEquals("winner", destination.readText())
        assertNoTemporaryFiles()
    }

    @Test
    fun concurrentWritersPublishOneCompleteFile() {
        val destination = File(temporaryFolder, "library.bin")
        val writing = CountDownLatch(1)
        val finishWriting = CountDownLatch(1)
        val secondStarted = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first =
                executor.submit {
                    destination.createAtomically {
                        it.writeText("first")
                        writing.countDown()
                        check(finishWriting.await(10, SECONDS))
                        it.appendText(" complete")
                    }
                }
            assertTrue(writing.await(10, SECONDS))
            assertFalse(destination.exists())
            val second =
                executor.submit {
                    secondStarted.countDown()
                    destination.createAtomically { it.writeText("second") }
                }
            assertTrue(secondStarted.await(10, SECONDS))
            finishWriting.countDown()
            first.get(10, SECONDS)
            second.get(10, SECONDS)
            assertEquals("first complete", destination.readText())
            assertNoTemporaryFiles()
        } finally {
            finishWriting.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun failedWriteLeavesNoDestinationOrTemporaryFile() {
        val destination = File(temporaryFolder, "library.bin")
        assertThrows(IOException::class.java) {
            destination.createAtomically {
                it.writeText("partial")
                throw IOException("write failed")
            }
        }
        assertFalse(destination.exists())
        assertNoTemporaryFiles()
        destination.createAtomically { it.writeText("retry") }
        assertEquals("retry", destination.readText())
    }

    private fun assertNoTemporaryFiles() {
        assertTrue(temporaryFolder.listFiles()!!.none { it.extension == "tmp" })
    }
}
