// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb.rsdroid.testing

import net.ankiweb.rsdroid.NativeMethods

/** Loaded together with the loader and JNI declarations by each isolated classloader. */
object NativeLibraryProbe {
    @JvmStatic
    fun verify() {
        RustBackendLoader.ensureSetup()
        // An invalid protobuf tag exercises JNI without allocating a backend needing disposal.
        val result = checkNotNull(NativeMethods.openBackend(byteArrayOf(0)))
        check(result.size == 2 && result[0] == null && result[1]?.isNotEmpty() == true) {
            "Expected the native backend to reject the malformed BackendInit"
        }
    }
}
