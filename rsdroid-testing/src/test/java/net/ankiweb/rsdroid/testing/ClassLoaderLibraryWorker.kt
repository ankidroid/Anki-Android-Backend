// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb.rsdroid.testing

import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import kotlin.system.exitProcess

/** A fresh JVM isolates native ownership, system properties, and native stdout/stderr redirection. */
object ClassLoaderLibraryWorker {
    enum class Scenario {
        /** Two live classloaders can each call JNI, including after repeated setup. */
        INDEPENDENT,
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val (scenarioName, resultPath) = args
        val result = File(resultPath)
        try {
            RustBackendLoader.maxOnDiskLibraryCopies = 1
            when (Scenario.valueOf(scenarioName)) {
                Scenario.INDEPENDENT -> independentClassLoaders()
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
