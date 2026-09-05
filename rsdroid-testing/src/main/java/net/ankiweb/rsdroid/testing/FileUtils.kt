// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb.rsdroid.testing

import java.io.File
import java.io.IOException

/**
 * Copies this file to [destination] [atomically][createAtomically].
 *
 * @throws IOException [destination] could neither be created nor found
 */
internal fun File.copyAtomicallyTo(destination: File) {
    destination.createAtomically { temporary -> copyTo(temporary, overwrite = true) }
}

/**
 * Creates this file in an atomic manner.
 *
 * If another process creates this file first, their file is left in place.
 *
 * @param write populates the temporary file which becomes this file
 * @throws IOException this file could neither be created nor found
 */
internal fun File.createAtomically(write: (File) -> Unit) {
    val temporary = File.createTempFile("$nameWithoutExtension-", ".tmp", parentFile)
    try {
        write(temporary)
        // this fails on Windows if another process created this file first
        if (!temporary.renameTo(this) && !exists()) {
            throw IOException("failed to move $temporary to $this")
        }
    } finally {
        temporary.delete()
    }
}
