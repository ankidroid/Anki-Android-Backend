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

        // Prevent sqlite throwing error 6410 due to the lack of /tmp
        if (!isRoboUnitTest) {
            val dir = context.cacheDir
            Os.setenv("TMPDIR", dir.path, false)
        }

        try {
            System.loadLibrary("rsdroid")
        } catch (e: UnsatisfiedLinkError) {
            if (!isRoboUnitTest) {
                throw RuntimeException("backend load failed")
            }
            // In Robolectric, assume setup works (setupException == null) if the library throws.
            // As the library is loaded at a later time (or a failure will be quickly found).
            
            // fixme: is this roboelectric special case still required?
        } finally {
            hasSetUp = true
        }
    }

    @CheckResult
    external fun runMethodRaw(backendPointer: Long, service: Int, method: Int, args: ByteArray?): Array<ByteArray?>?

    @CheckResult
    external fun openBackend(data: ByteArray?): Array<ByteArray?>?
    external fun closeBackend(backendPointer: Long)
}