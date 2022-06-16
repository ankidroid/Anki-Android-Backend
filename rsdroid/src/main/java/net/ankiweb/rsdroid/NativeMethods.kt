package net.ankiweb.rsdroid

import android.os.Build
import androidx.annotation.CheckResult
import net.ankiweb.rsdroid.RustBackendFailedException

object NativeMethods {
    private var hasSetUp = false
    private var setupException: RustBackendFailedException? = null
    val isRoboUnitTest: Boolean
        get() = "robolectric" == Build.FINGERPRINT

    @JvmStatic
    @Synchronized
    @Throws(RustBackendFailedException::class)
    fun ensureSetup() {
        if (hasSetUp) {
            if (setupException != null) {
                throw setupException!!
            }
            return
        }
        try {
            System.loadLibrary("rsdroid")
        } catch (e: UnsatisfiedLinkError) {
            if (!isRoboUnitTest) {
                setupException = RustBackendFailedException(e)
                throw setupException!!
            }
            // In Robolectric, assume setup works (setupException == null) if the library throws.
            // As the library is loaded at a later time (or a failure will be quickly found).
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