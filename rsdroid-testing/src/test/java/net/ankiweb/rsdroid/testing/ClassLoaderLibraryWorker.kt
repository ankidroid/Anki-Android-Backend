// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb.rsdroid.testing

import java.io.File
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import kotlin.system.exitProcess

/** A fresh JVM isolates native ownership, system properties, and native stdout/stderr redirection. */
object ClassLoaderLibraryWorker {
    enum class Scenario {
        /** Two live classloaders can each call JNI, including after repeated setup. */
        INDEPENDENT,

        /** Collecting the copy's owner frees its slot while the original owner stays live. */
        REUSE_COPY,

        /** Collecting the original's owner frees its slot while the copy owner stays live. */
        REUSE_ORIGINAL,

        /** Two live owners fill the configured capacity and prevent a third from loading. */
        EXHAUSTION,
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val (scenarioName, resultPath) = args
        val result = File(resultPath)
        try {
            RustBackendLoader.maxOnDiskLibraryCopies = 1
            when (Scenario.valueOf(scenarioName)) {
                Scenario.INDEPENDENT -> independentClassLoaders()
                Scenario.REUSE_COPY -> reuseCollectedSlot(discardOriginal = false)
                Scenario.REUSE_ORIGINAL -> reuseCollectedSlot(discardOriginal = true)
                Scenario.EXHAUSTION -> retainedClassLoadersExhaustCapacity()
            }
            result.writeText("OK")
        } catch (failure: Throwable) {
            result.writeText(failure.stackTraceToString())
            exitProcess(1)
        }
    }

    private fun independentClassLoaders() {
        newClassLoader().use { first ->
            verifyNativeCall(first)
            newClassLoader().use { second ->
                verifyNativeCall(second)
                // Repeated setup must remain usable in both owners without consuming more slots.
                verifyNativeCall(first)
                verifyNativeCall(second)
                check(libraryFiles().size == 2)
            }
        }
    }

    private fun reuseCollectedSlot(discardOriginal: Boolean) {
        newClassLoader().use { retained ->
            val discarded = loadAndDiscard(retained, discardOriginal)
            val filesBefore = libraryFiles()
            check(filesBefore.size == 2)
            try {
                newClassLoader().use { replacement ->
                    // Let ensureSetup's GC retry reclaim the discarded owner; do not pre-collect it.
                    verifyNativeCall(replacement)
                    check(discarded.get() == null) { "The previous owner was not collected" }
                    check(libraryFiles() == filesBefore) { "Reuse created another library file" }
                    verifyNativeCall(retained)
                }
            } finally {
                Reference.reachabilityFence(retained)
            }
        }
    }

    private fun retainedClassLoadersExhaustCapacity() {
        newClassLoader().use { original ->
            verifyNativeCall(original)
            newClassLoader().use { copy ->
                verifyNativeCall(copy)
                try {
                    newClassLoader().use { excess ->
                        val failure = runCatching { verifyNativeCall(excess) }.exceptionOrNull()
                        check(failure is IllegalStateException && failure.message?.contains("capacity: 2") == true) {
                            "Expected capacity exhaustion, got $failure"
                        }
                    }
                    verifyNativeCall(original)
                    verifyNativeCall(copy)
                    check(libraryFiles().size == 2)
                } finally {
                    Reference.reachabilityFence(original)
                    Reference.reachabilityFence(copy)
                }
            }
        }
    }

    // Returning only a weak reference keeps the old loader out of the GC retry's stack frames.
    private fun loadAndDiscard(
        retained: URLClassLoader,
        discardOriginal: Boolean,
    ): WeakReference<URLClassLoader> =
        newClassLoader().use { loader ->
            val first = if (discardOriginal) loader else retained
            val second = if (discardOriginal) retained else loader
            try {
                verifyNativeCall(first)
                verifyNativeCall(second)
                WeakReference(loader)
            } finally {
                // Both slots must be occupied before either owner becomes collectable.
                Reference.reachabilityFence(first)
                Reference.reachabilityFence(second)
            }
        }

    private fun newClassLoader(): URLClassLoader {
        val urls =
            System
                .getProperty("java.class.path")
                .split(File.pathSeparator)
                .map { File(it).toURI().toURL() }
                .toTypedArray()
        return URLClassLoader(urls, null)
    }

    private fun verifyNativeCall(loader: URLClassLoader) {
        val probe = loader.loadClass(NativeLibraryProbe::class.java.name)
        check(probe.classLoader === loader)
        try {
            probe.getMethod("verify").invoke(null)
        } catch (failure: InvocationTargetException) {
            throw failure.targetException
        }
    }

    private fun libraryFiles(): Set<String> =
        File(System.getProperty("java.io.tmpdir"))
            .listFiles()!!
            .filter { it.extension in setOf("so", "dylib", "dll") }
            .map { it.name }
            .toSet()
}
