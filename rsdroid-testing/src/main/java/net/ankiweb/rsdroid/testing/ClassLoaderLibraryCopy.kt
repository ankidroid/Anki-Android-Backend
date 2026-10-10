// SPDX-License-Identifier: GPL-3.0-or-later

/**
 * Workaround for each JVM classloader requiring a distinct path to a library file: each classloader
 * is assigned a separate on-disk copy of a library if it requires it.
 *
 * @see loadCopyForCurrentClassLoader
 */
package net.ankiweb.rsdroid.testing

import java.io.File

/**
 * The maximum number of copies of the library on disk (excluding the original)
 *
 * Each copy allocates ~60MB of space.
 * This limit may need to be increased in future if there are more than 6 classloaders in a JVM
 * which require the library.
 *
 * [loadCopyForCurrentClassLoader] will throw [IllegalStateException] if this limit is breached.
 */
internal var maxCopies: Int
    get() = System.getProperty("net.ankiweb.rsdroid.testing.maxCopies")?.toIntOrNull()?.coerceAtLeast(1) ?: 5
    set(value) {
        require(value >= 1) { "at least one copy of the library is needed" }
        System.setProperty("net.ankiweb.rsdroid.testing.maxCopies", value.toString())
    }

/** A cursor pointing to the next 'likely-available' library. */
private var nextCopy: Int
    // Use a system property to share it between classloaders
    get() = System.getProperty("net.ankiweb.rsdroid.testing.nextCopy")?.toIntOrNull() ?: 1
    set(value) {
        System.setProperty("net.ankiweb.rsdroid.testing.nextCopy", value.toString())
    }

/** Whether Java refused to load a library because another classloader owns the file */
internal val UnsatisfiedLinkError.isOwnedByAnotherClassLoader: Boolean
    get() = message?.contains("already loaded in another classloader") == true

/**
 * Loads a copy of the library at [path] which no other classloader in this process owns.
 *
 * Up to [maxCopies] copies are numbered and reused.
 *
 * Copies are left behind for later runs: [path] contains a checksum of the library, so a copy of it
 * is only ever reused for the same content.
 *
 * This method is responsible for calling [Runtime.load].
 *
 * @param path a library file which another classloader owns
 * @throws UnsatisfiedLinkError no copy of the library could be loaded
 * @throws IllegalStateException [maxCopies] classloaders already hold a copy
 */
internal fun loadCopyForCurrentClassLoader(path: String) {
    val library = File(path)
    val loaded = library.loadUnownedCopy() ?: library.loadCopyFreedByGc()
    checkNotNull(loaded) {
        "More than $maxCopies classloaders require a distinct copy of the library. " +
            "See RustBackendLoader.maxOnDiskLibraryCopies for tradeoffs."
    }
    // the next classloader starts after the copy this one now owns
    nextCopy = loaded.number % maxCopies + 1
}

/**
 * Loads the first copy of this library which no classloader owns.
 *
 * @return the loaded copy, or `null` if every copy is owned
 */
private fun File.loadUnownedCopy(): LibraryCopy? = copiesToTry().firstOrNull { copy -> copy.loadIfUnowned() }

/**
 * [loadUnownedCopy], after asking the JVM to collect discarded classloaders.
 *
 * @return the loaded copy, or `null` if every copy remained owned
 */
private fun File.loadCopyFreedByGc(): LibraryCopy? {
    repeat(10) {
        // GC is a hint - try a few times
        System.gc()
        // The JVM closes the collected classloader's libraries asynchronously
        Thread.sleep(50)
        loadUnownedCopy()?.let { return it }
    }
    return null
}

/** A numbered copy of a library, which at most one classloader may own */
private class LibraryCopy(
    val number: Int,
    private val file: File,
) {
    /**
     * Loads this copy into the classloader which loaded this class.
     *
     * @return whether it was loaded: `false` if another classloader owns it
     * @throws UnsatisfiedLinkError the copy could not be loaded
     */
    fun loadIfUnowned(): Boolean =
        try {
            Runtime.getRuntime().load(file.absolutePath)
            true
        } catch (e: UnsatisfiedLinkError) {
            if (!e.isOwnedByAnotherClassLoader) {
                throw e
            }
            false
        }
}

/**
 * The copies of this library, in the order to try them.
 *
 * [nextCopy] comes first: a classloader owns at most one copy, so the copy after the one which last
 * worked is normally free, making this a single load. The rest follow, wrapping around, as a copy is
 * freed when its classloader is collected - Robolectric evicts sandboxes as a suite runs.
 *
 * The original (number 0) comes last: the caller found it to be owned, but its owner may be
 * collected during the sweep, or by the GC in [loadCopyFreedByGc].
 *
 * The sequence is lazy, so a copy is only made once every copy before it is found to be owned.
 */
private fun File.copiesToTry(): Sequence<LibraryCopy> {
    val copies = maxCopies
    val first = nextCopy.coerceIn(1, copies)
    return ((first..copies) + (1..<first) + 0)
        .asSequence()
        .map { number -> LibraryCopy(number, numberedCopy(number)) }
}

/**
 * @return copy number [index] of this library, creating it if required. Copy 0 is the original.
 */
private fun File.numberedCopy(index: Int): File {
    if (index == 0) return this
    // the extension is retained: `Runtime.load` requires it on Windows
    val copy = File(parentFile, "$nameWithoutExtension-$index.$extension")
    if (!copy.exists()) {
        copyAtomicallyTo(copy)
    }
    return copy
}
