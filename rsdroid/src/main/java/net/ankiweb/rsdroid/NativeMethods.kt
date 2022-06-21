package net.ankiweb.rsdroid

import android.content.Context
import android.os.Build
import android.system.Os
import androidx.annotation.CheckResult
import java.lang.RuntimeException

object NativeMethods {
    private var hasSetUp = false
    val isRoboUnitTest: Boolean
        get() = "robolectric" == Build.FINGERPRINT

    @JvmStatic
    @Synchronized
    fun ensureSetup(context: Context) {
        if (hasSetUp) {
            return
        }

        if (!isRoboUnitTest) {
            // Prevent sqlite throwing error 6410 due to the lack of /tmp
            val dir = context.cacheDir
            Os.setenv("TMPDIR", dir.path, false)
            // Then load library
            System.loadLibrary("rsdroid")
        } else {
            // Test harness will load the library for us.
        }

        hasSetUp = true
    }

    @CheckResult
    external fun runMethodRaw(backendPointer: Long, service: Int, method: Int, args: ByteArray?): Array<ByteArray?>?

    @CheckResult
    external fun openBackend(data: ByteArray?): Array<ByteArray?>?
    external fun closeBackend(backendPointer: Long)
}