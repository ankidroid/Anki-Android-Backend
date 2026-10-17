// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb.rsdroid.testing

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE

/**
 * Copies this file to [destination] [atomically][createAtomically].
 *
 * @return whether this caller published the copy
 * @throws IOException [destination] could neither be created nor found
 */
internal fun File.copyAtomicallyTo(destination: File): Boolean {
    val expectedSize = Files.size(toPath())
    return destination.createAtomically(isValid = { it.isFile && it.length() == expectedSize }) { temporary ->
        copyTo(temporary, overwrite = true)
    }
}

/**
 * Creates this file in an atomic manner.
 *
 * If another caller creates a valid file first, their file is left in place.
 * A persistent sidecar lock file coordinates writers across processes and classloaders.
 *
 * @param write populates the temporary file which becomes this file
 * @param isValid checks whether an existing file can be reused, while holding the creation lock
 * @return whether this caller published the file
 * @throws IOException this file could neither be created nor found
 */
internal fun File.createAtomically(
    isValid: (File) -> Boolean = { it.exists() },
    write: (File) -> Unit,
): Boolean =
    withCreationLock {
        if (isValid(this)) return@withCreationLock false
        val temporary = File.createTempFile("$nameWithoutExtension-", ".tmp", parentFile)
        try {
            write(temporary)
            // ATOMIC_MOVE alone does not prevent replacement: the lock protects this move.
            Files.move(temporary.toPath(), toPath(), ATOMIC_MOVE)
            true
        } finally {
            temporary.delete()
        }
    }

/** Serializes [action] across classloaders and processes using a persistent sidecar lock. */
private fun <T> File.withCreationLock(action: () -> T): T {
    // File locks overlap within a JVM instead of waiting, so also share a monitor across classloaders.
    val lockFile = File("$canonicalPath.lock")
    return synchronized(lockFile.path.intern()) {
        RandomAccessFile(lockFile, "rw").use { lock ->
            lock.channel.lock().use {
                action()
            }
        }
    }
}
