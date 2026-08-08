// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb.rsdroid.testing

import java.io.File

/**
 * Copies this file to [destination] in an atomic manner: the copy is made under a temporary name,
 * so [destination] is never seen partially written by a process which opens it as it appears.
 *
 * If another process creates [destination] first, their file is left in place.
 */
internal fun File.copyAtomicallyTo(destination: File) {
    val temporary = File.createTempFile("${destination.nameWithoutExtension}-", ".tmp", destination.parentFile)
    try {
        copyTo(temporary, overwrite = true)
        // this fails on Windows if another process created `destination` first
        temporary.renameTo(destination)
    } finally {
        temporary.delete()
    }
}
