// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb.rsdroid.testing

import java.io.File
import java.util.concurrent.TimeUnit.SECONDS

/** Separate JVM entry point for testing the interprocess publication lock. */
object FileCreationWorker {
    @JvmStatic
    fun main(args: Array<String>) {
        val (destination, contents, started, writing, release) = args
        File(started).writeText("started")
        File(destination).createAtomically { temporary ->
            temporary.writeText(contents)
            File(writing).writeText("writing")
            awaitFile(File(release))
        }
    }

    fun awaitFile(file: File) {
        val deadline = System.nanoTime() + SECONDS.toNanos(10)
        while (!file.exists()) {
            check(System.nanoTime() < deadline) { "timed out waiting for $file" }
            Thread.sleep(10)
        }
    }
}
